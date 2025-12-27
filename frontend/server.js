import express from 'express';
import fetch from 'node-fetch';
import cors from 'cors';
import path from 'path';

const app = express();
const PORT = process.env.PORT || 5000;
const BACKEND = process.env.BACKEND_URL || 'http://localhost:8080';

app.use(cors());

app.get('/api/dogs', async (req, res) => {
  const raw = req.query.size;
  const size = parseInt(raw, 10) || 10;

  try {
    const url = `${BACKEND}/dogs?randomise=true&size=${size}`;
    const response = await fetch(url);
    if (!response.ok) {
      return res.status(response.status).json({ error: 'Error fetching dogs' });
    }
    const dogs = await response.json();

    // Transform backend response to frontend format - pass through all image URLs
    const enriched = dogs
      .map((dog) => {
        const info = dog.dogInfo || {};
        const urls = info.imageUrls;
        if (!Array.isArray(urls) || urls.length === 0) {
          return null;
        }
        const imgUrl = urls[0]; // Use first image URL as primary
        
        // Log if we receive a data URI (shouldn't happen if backend returns real URLs)
        if (imgUrl && imgUrl.startsWith('data:')) {
          console.warn(`[Proxy] Received data URI from backend for dog ${dog.internalId}: ${imgUrl.substring(0, 100)}...`);
        }
        
        return {
          id: dog.internalId,
          url: imgUrl, // Primary image URL (first one)
          imageUrls: urls, // All image URLs for carousel
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
        };
      })
      .filter(Boolean);

    res.json(enriched);

  } catch (e) {
    console.error('Error fetching dogs:', e);
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.listen(PORT, () => {
  console.log(`Node.js proxy listening on port ${PORT}`);
});