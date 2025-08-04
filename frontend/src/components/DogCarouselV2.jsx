// DogCarouselV2.jsx
import React, { useEffect, useState } from 'react';
import { loadingAnimation, hideLoadingAnimation } from './Components.js';
import { imagePipelineStore } from './ImagePipelineStore.js';
import { observer } from 'mobx-react-lite';
import TickerRow from "./TickerRow.jsx"; // Import the observer component

const petImageUrl = "/api/dogs";
const INITIAL_IMAGE_COUNT = 33;
const BUFFER_SIZE = 60;

export async function fetchImages(count) {
    const response = await fetch(`${petImageUrl}?size=${count}`);
    if (!response.ok) {
        throw new Error('Failed to fetch images');
    }
    const urls = await response.json();
    return Promise.all(
        urls.map(url => preloadImage(url))
    );
}

export async function preloadImage(url) {
    return new Promise((resolve, reject) => {
        const img = new window.Image();
        img.onload = () => resolve(img);
        img.onerror = reject;
        img.src = url;
    });
}

const DogCarouselV2 = observer(() => {
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchImages(INITIAL_IMAGE_COUNT + BUFFER_SIZE)
            .then(images => {
                const imageStack = images.slice(0, INITIAL_IMAGE_COUNT);
                const buffer     = images.slice(INITIAL_IMAGE_COUNT);
                imagePipelineStore.initializeStacks(imageStack, buffer);
            })
            .catch(err => {
                console.error('Error loading initial dog images:', err);
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading || !imagePipelineStore.initialized) {
        return loadingAnimation();
    } else {
        hideLoadingAnimation();
    }

    return <TickerRow />; // Render the correct component
});

export default DogCarouselV2;