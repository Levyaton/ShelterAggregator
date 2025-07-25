import React from 'react';
import { createRoot } from 'react-dom/client';
import DogCarousel from './components/DogCarousel.js';
import './components/DogCarousel.css';

const container = document.getElementById('root');
createRoot(container).render(<DogCarousel />);