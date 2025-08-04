import React from 'react';
import { createRoot } from 'react-dom/client';
import './components/DogCarousel.css';
import DogCarouselV2 from "./components/DogCarouselV2.jsx";

const container = document.getElementById('root');
createRoot(container).render(<DogCarouselV2 />);