import React from "react";

function Dots() {
    return (
        <span className="dots">
        <span className="dot dot-1">.</span>
        <span className="dot dot-2">.</span>
        <span className="dot dot-3">.</span>
        </span>
    );
}


export function loadingAnimation() {
    return (
        <div className="loading-overlay">
            <div>
                <div style={{ fontWeight: 'bold', fontSize: '1.7rem' }}>
                    Připravuji pejsky
                    <Dots />
                </div>
                <div style={{ marginTop: '1.2rem', color: '#666', fontSize: '1.3rem' }}>
                    Chvilku strpení, prosím
                </div>
            </div>
        </div>
    );
}

export function hideLoadingAnimation() {
    const loadingOverlay = document.querySelector('.loading-overlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'none';
    }
}