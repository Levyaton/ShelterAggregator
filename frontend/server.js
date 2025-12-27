import express from 'express';
import fetch from 'node-fetch';
import cors from 'cors';
import path from 'path';

const app = express();
const PORT = process.env.PORT || 5000;
const BACKEND = process.env.BACKEND_URL || 'http://localhost:8080';
const BACKEND_API_KEY = process.env.BACKEND_API_KEY || '';
const IMAGE_FETCH_TIMEOUT = 10000; // 10 seconds
const MAX_RETRIES = 3;

app.use(cors());

// Timeout promise helper
const timeoutPromise = (ms) => {
  return new Promise((_, reject) => {
    setTimeout(() => reject(new Error('Timeout')), ms);
  });
};

// Validate and convert image URL to data URI
async function validateImageUrl(imgUrl, dogId, retryCount = 0) {
  try {
    const imgRes = await Promise.race([
      fetch(imgUrl),
      timeoutPromise(IMAGE_FETCH_TIMEOUT)
    ]);

    // Check response status
    if (!imgRes.ok) {
      const statusCode = imgRes.status;
      const isPermanentFailure = statusCode === 404 || statusCode === 403 || statusCode === 410;

      if (isPermanentFailure) {
        console.error(`[Image Fetch] Permanent failure for dog ${dogId}: HTTP ${statusCode} - ${imgUrl}`);
        return { success: false, error: `HTTP ${statusCode}` };
      }

      // Transient HTTP error - retry if attempts remain
      if (retryCount < MAX_RETRIES) {
        console.warn(`[Image Fetch] Retry ${retryCount + 1}/${MAX_RETRIES} for dog ${dogId}: HTTP ${statusCode} - ${imgUrl}`);
        return validateImageUrl(imgUrl, dogId, retryCount + 1);
      } else {
        console.error(`[Image Fetch] Failed after ${MAX_RETRIES + 1} attempts for dog ${dogId}: HTTP ${statusCode} - ${imgUrl}`);
        return { success: false, error: `HTTP ${statusCode} after ${MAX_RETRIES + 1} attempts` };
      }
    }

    // Check content-type BEFORE reading buffer to ensure it's actually an image
    const contentType = imgRes.headers.get('content-type') || '';
    const isImage = contentType.startsWith('image/');

    if (!isImage) {
      console.error(`[Image Fetch] Non-image content type for dog ${dogId}: ${contentType} - ${imgUrl}`);
      // If it's an HTML error page, log it clearly
      if (contentType.includes('text/html')) {
        console.error(`[Image Fetch] Received HTML error page instead of image for dog ${dogId}: ${imgUrl}`);
      }
      return { success: false, error: `Non-image content type: ${contentType}` };
    }

    const buffer = await imgRes.buffer();
    const base64 = buffer.toString('base64');
    const dataUri = `data:${contentType};base64,${base64}`;
    return { success: true, dataUri };
  } catch (err) {
    if (err.message === 'Timeout') {
      if (retryCount < MAX_RETRIES) {
        console.warn(`[Image Fetch] Timeout, retry ${retryCount + 1}/${MAX_RETRIES} for dog ${dogId}: ${imgUrl}`);
        return validateImageUrl(imgUrl, dogId, retryCount + 1);
      } else {
        console.error(`[Image Fetch] Timeout after ${MAX_RETRIES + 1} attempts for dog ${dogId}: ${imgUrl}`);
        return { success: false, error: 'Timeout' };
      }
    }
    console.error(`[Image Fetch] Error for dog ${dogId}: ${err.message} - ${imgUrl}`);
    return { success: false, error: err.message };
  }
}

// Report unavailable dogs to backend
async function reportUnavailableDogs(dogIds) {
  if (!dogIds || dogIds.length === 0 || !BACKEND_API_KEY) {
    return;
  }

  try {
    const response = await fetch(`${BACKEND}/dogs/reportUnavailable`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': BACKEND_API_KEY
      },
      body: JSON.stringify({ dogIds })
    });

    if (!response.ok) {
      console.error(`[Proxy] Failed to report unavailable dogs: HTTP ${response.status}`);
    } else {
      console.log(`[Proxy] Reported ${dogIds.length} unavailable dog(s) to backend`);
    }
  } catch (err) {
    console.error(`[Proxy] Error reporting unavailable dogs: ${err.message}`);
  }
}

app.get('/api/dogs', async (req, res) => {
  const raw = req.query.size;
  const requestedSize = parseInt(raw, 10) || 10;
  const maxRetryAttempts = 10; // Prevent infinite loops
  let attemptCount = 0;

  try {
    let successfulDogs = [];
    let allFailedDogIds = new Set();

    while (successfulDogs.length < requestedSize && attemptCount < maxRetryAttempts) {
      attemptCount++;
      const neededCount = requestedSize - successfulDogs.length;
      
      // Fetch dogs from backend
      const url = `${BACKEND}/dogs?randomise=true&size=${neededCount}`;
      const response = await fetch(url);
      if (!response.ok) {
        return res.status(response.status).json({ error: 'Error fetching dogs' });
      }
      const dogs = await response.json();

      if (!Array.isArray(dogs) || dogs.length === 0) {
        console.warn(`[Proxy] No more dogs available from backend`);
        break;
      }

      // Validate all images for each dog
      const validationResults = await Promise.all(
        dogs.map(async (dog) => {
          const info = dog.dogInfo || {};
          const urls = info.imageUrls;
          if (!Array.isArray(urls) || urls.length === 0) {
            return { dog, success: false, reason: 'No image URLs' };
          }

          // Validate ALL images for this dog
          const imageValidations = await Promise.all(
            urls.map(url => validateImageUrl(url, dog.internalId))
          );

          // Check if ALL images are valid
          const allValid = imageValidations.every(result => result.success);
          if (!allValid) {
            const failedUrls = urls.filter((_, idx) => !imageValidations[idx].success);
            console.warn(`[Proxy] Dog ${dog.internalId} has ${failedUrls.length} failed image(s) out of ${urls.length}`);
            return { dog, success: false, reason: 'Image validation failed' };
          }

          // Convert all images to data URIs
          const dataUriUrls = urls.map((url, idx) => imageValidations[idx].dataUri);
          
          return {
            dog,
            success: true,
            data: {
              id: dog.internalId,
              url: dataUriUrls[0], // Primary image URL (first one)
              imageUrls: dataUriUrls, // All image URLs as data URIs
              name: info.name,
              description: info.description,
              breedGuess: info.breedGuess,
              sex: info.sex,
              estimatedAgeInYears: info.estimatedAgeInYears,
              currentWeight: info.currentWeight,
              estimatedFinalWeightMin: info.estimatedFinalWeightMin,
              estimatedFinalWeightMax: info.estimatedFinalWeightMax,
              dogAddress: info.dogAddress,
              shelterUrl: info.shelterUrl
            }
          };
        })
      );

      // Separate successful and failed dogs
      const successful = validationResults.filter(r => r.success);
      const failed = validationResults.filter(r => !r.success);

      successfulDogs.push(...successful.map(r => r.data));
      failed.forEach(r => allFailedDogIds.add(r.dog.internalId));

      // Report failed dogs to backend
      if (failed.length > 0) {
        const failedIds = Array.from(failed.map(r => r.dog.internalId));
        await reportUnavailableDogs(failedIds);
      }

      // If we got fewer dogs than requested and some failed, we need to retry
      if (successfulDogs.length < requestedSize && failed.length > 0) {
        console.log(`[Proxy] Got ${successful.length} successful, ${failed.length} failed. Need ${requestedSize - successfulDogs.length} more. Retrying...`);
        continue;
      }

      // If we got enough successful dogs, break
      if (successfulDogs.length >= requestedSize) {
        break;
      }
    }

    // Return exactly the requested number (trim if we got more)
    const result = successfulDogs.slice(0, requestedSize);
    res.json(result);

  } catch (e) {
    console.error('Error fetching dogs:', e);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.listen(PORT, () => {
  console.log(`Node.js proxy listening on port ${PORT}`);
});