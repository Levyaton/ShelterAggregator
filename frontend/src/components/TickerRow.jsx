// TickerRow.jsx
import React, { useRef, useEffect, useCallback } from 'react';
import Marquee from 'react-fast-marquee';
import { observer } from 'mobx-react-lite';
import { imagePipelineStore } from './ImagePipelineStore.js';

function RowMarquee({ rowIndex }) {
    const isReverse   = rowIndex === 1;
    const rootRef     = useRef(null);
    const observerRef = useRef(null);

    // Always render exactly 11 items
    const queue = imagePipelineStore.rowQueues[rowIndex].slice(0, 11);

    // Called when an image scrolls off
    const handleExit = useCallback(id => {
        requestAnimationFrame(() => imagePipelineStore.popRow(rowIndex));
    }, [rowIndex]);

    // 1) Create one IntersectionObserver for this row
    useEffect(() => {
        if (!rootRef.current) return;
        const obs = new IntersectionObserver(
            entries => {
                entries.forEach(entry => {
                    const { isIntersecting, boundingClientRect, rootBounds, target } = entry;
                    const leftGone  = !isIntersecting && boundingClientRect.right <= rootBounds.left;
                    const rightGone = !isIntersecting && boundingClientRect.left  >= rootBounds.right;
                    if (( !isReverse && leftGone ) || ( isReverse && rightGone )) {
                        const id = target.getAttribute('data-id');
                        handleExit(id);
                        obs.unobserve(target);
                    }
                });
            },
            { root: rootRef.current, threshold: 0 }
        );
        observerRef.current = obs;
        return () => obs.disconnect();
    }, [handleExit, isReverse]);

    // 2) After each render, observe all current images in the marquee
    useEffect(() => {
        const obs = observerRef.current;
        const container = rootRef.current;
        if (!obs || !container) return;
        // Clear any previous observations
        obs.disconnect();
        // Wait one frame so Marquee has painted the images
        requestAnimationFrame(() => {
            const imgs = container.querySelectorAll('img[data-id]');
            imgs.forEach(img => obs.observe(img));
        });
    }, [queue]);

    return (
        <Marquee
            ref={rootRef}
            speed={50}
            gradient={false}
            pauseOnHover
            play
            direction={isReverse ? 'right' : 'left'}
        >
            {queue.map((item, idx) => (
                <img
                    key={idx}
                    data-id={item.id}
                    src={item.src}
                    alt={item.alt || ''}
                    style={{
                        height: '10rem',
                        margin: '0 0.5rem',
                        borderRadius: '0.5rem',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
                        transition: 'transform 0.2s ease',
                    }}
                    onMouseEnter={e => (e.currentTarget.style.transform = 'scale(1.05)')}
                    onMouseLeave={e => (e.currentTarget.style.transform = 'scale(1)')}
                />
            ))}
        </Marquee>
    );
}

export default observer(function QueueSwiperRows() {
    return (
        <div
            className="carousel-container"
            style={{ display: 'flex', flexDirection: 'column', gap: '3rem' }}
        >
            <RowMarquee rowIndex={0} />
            <RowMarquee rowIndex={1} />
            <RowMarquee rowIndex={2} />
        </div>
    );
});
