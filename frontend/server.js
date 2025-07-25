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

    const dataUris = await Promise.all(
      dogs.map(async dog => {
        const info = dog.dogInfo || {};
        const externalId = info.externalId;
        const urls = info.imageUrls;
        if (!Array.isArray(urls) || urls.length === 0) {
          return null;
        }
        const imgUrl = urls[0];
        try {
          const imgRes = await fetch(imgUrl);
          const buffer = await imgRes.buffer();
          const contentType = imgRes.headers.get('content-type') || 'image/jpeg';
          const base64 = buffer.toString('base64');
          return `data:${contentType};base64,${base64}`;
        } catch (err) {
          return null;
        }
      })
    );

    res.json(dataUris);

  } catch (e) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

app.listen(PORT, () => {
  console.log(`Node.js proxy listening on port ${PORT}`);
});