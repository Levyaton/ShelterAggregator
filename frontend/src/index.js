import React from 'react';
import { createRoot } from 'react-dom/client';
import DogGrid from './components/DogGrid.tsx';
import './components/DogGrid.css';

const container = document.getElementById('root');
createRoot(container).render(<DogGrid />);