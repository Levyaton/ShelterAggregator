import React, { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import './DogCarousel.css';

const IMAGES_PER_ROW = 8;
const ROW_BUFFER = 12;
const ROW_TOTAL = IMAGES_PER_ROW + ROW_BUFFER;
const ROW_COUNT = 3;
const EXTRA_BATCH = 100;
const STACK_REFILL_THRESHOLD = 70;
const PIXELS_PER_FRAME = 0.35;
const MAX_RECYCLED = 200;

const ROW_DEFS = [
  { key: 'row-top', dir: -1 },
  { key: 'row-middle', dir: 1 },
  { key: 'row-bottom', dir: -1 }
];

function useInterval(callback, delay) {
  const savedCallback = useRef();
  useEffect(() => { savedCallback.current = callback; }, [callback]);
  useEffect(() => {
    if (delay === null) return;
    const id = setInterval(() => savedCallback.current(), delay);
    return () => clearInterval(id);
  }, [delay]);
}

function AnimatedDots() {
  return (
    <span className="dots">
      <span className="dot dot-1">.</span>
      <span className="dot dot-2">.</span>
      <span className="dot dot-3">.</span>
    </span>
  );
}

function randomSplice(arr) {
  if (!arr.length) return null;
  const idx = Math.floor(Math.random() * arr.length);
  return arr.splice(idx, 1)[0];
}

export default function DogCarousel() {
  const [isReady, setIsReady] = useState(false);
  const [rows, setRows] = useState(() => ROW_DEFS.map(() => []));
  const [stack, setStack] = useState([]);
  const [positions, setPositions] = useState(() => ROW_DEFS.map(() => 0));
  const [hovered, setHovered] = useState({ rowIdx: null, imgIdx: null, url: null });

  const recycledRef = useRef([]);
  const imageWidths = useRef(ROW_DEFS.map(() => Array(ROW_TOTAL + 2).fill(0)));
  const fetchingMore = useRef(false);

  useEffect(() => {
    (async () => {
      const total = ROW_TOTAL * ROW_COUNT + EXTRA_BATCH;
      const { data: urls } = await axios.get(`/api/dogs?size=${total}`);
      await Promise.all(
        urls.map(u => new Promise(res => {
          const img = new window.Image();
          img.onload = img.onerror = res;
          img.src = u;
        }))
      );
      const newRows = [];
      for (let i = 0; i < ROW_COUNT; i++) {
        newRows.push(urls.slice(i * ROW_TOTAL, (i + 1) * ROW_TOTAL));
      }
      setRows(newRows);
      setStack(urls.slice(ROW_COUNT * ROW_TOTAL));
      recycledRef.current = [];
      setIsReady(true);
    })();
  }, []);

  useEffect(() => {
    setPositions(ROW_DEFS.map(() => 0));
  }, [isReady]);

  useInterval(() => {
    if (!isReady) return;

    setRows(prevRows => {
      let newRows = prevRows.map(row => [...row]);
      let newStack = [...stack];
      let recycled = recycledRef.current.slice();
      let newPositions = [...positions];

      for (let rowIdx = 0; rowIdx < ROW_COUNT; rowIdx++) {
        const dir = ROW_DEFS[rowIdx].dir;
        newPositions[rowIdx] += dir * PIXELS_PER_FRAME;

        const container = document.getElementById(ROW_DEFS[rowIdx].key + '-content');
        if (!container) continue;
        const imgs = container.children;
        for (let j = 0; j < imgs.length; j++) {
          imageWidths.current[rowIdx][j] = imgs[j].offsetWidth || 0;
        }

        if (dir < 0) {
          const firstImgWidth = imageWidths.current[rowIdx][0] || 0;
          if (Math.abs(newPositions[rowIdx]) >= firstImgWidth + 16) {
            let removed = newRows[rowIdx].shift();
            if (rowIdx === 0 && removed) {
              if (!recycled.includes(removed)) {
                recycled.push(removed);
                if (recycled.length > MAX_RECYCLED) recycled = recycled.slice(-MAX_RECYCLED);
              }
            }
            let next;
            if (rowIdx === 0) {
              if (newStack.length > 0) next = newStack.shift();
              else if (recycled.length > 0) next = randomSplice(recycled);
              else if (removed) next = removed;
              else if (newRows[rowIdx].length) next = newRows[rowIdx][newRows[rowIdx].length - 1];
            } else {
              if (newRows[rowIdx - 1].length > 0) next = newRows[rowIdx - 1].shift();
              else if (recycled.length > 0) next = randomSplice(recycled);
              else if (removed) next = removed;
              else if (newRows[rowIdx].length) next = newRows[rowIdx][newRows[rowIdx].length - 1];
            }
            if (next) newRows[rowIdx].push(next);
            newPositions[rowIdx] = 0;
          }
        } else if (dir > 0) {
          const lastIdx = newRows[rowIdx].length - 1;
          const lastImgWidth = imageWidths.current[rowIdx][lastIdx] || 0;
          if (Math.abs(newPositions[rowIdx]) >= lastImgWidth + 16) {
            let removed = newRows[rowIdx].pop();
            if (rowIdx === 0 && removed) {
              if (!recycled.includes(removed)) {
                recycled.push(removed);
                if (recycled.length > MAX_RECYCLED) recycled = recycled.slice(-MAX_RECYCLED);
              }
            }
            let next;
            if (rowIdx === 0) {
              if (newStack.length > 0) next = newStack.shift();
              else if (recycled.length > 0) next = randomSplice(recycled);
              else if (removed) next = removed;
              else if (newRows[rowIdx].length) next = newRows[rowIdx][0];
            } else {
              if (newRows[rowIdx - 1].length > 0) next = newRows[rowIdx - 1].pop();
              else if (recycled.length > 0) next = randomSplice(recycled);
              else if (removed) next = removed;
              else if (newRows[rowIdx].length) next = newRows[rowIdx][0];
            }
            if (next) newRows[rowIdx].unshift(next);
            newPositions[rowIdx] = 0;
          }
        }

        while (newRows[rowIdx].length < ROW_TOTAL) {
          let next;
          if (rowIdx === 0) {
            if (newStack.length > 0) next = newStack.shift();
            else if (recycled.length > 0) next = randomSplice(recycled);
            else if (newRows[rowIdx].length) next = newRows[rowIdx][newRows[rowIdx].length - 1];
          } else {
            if (newRows[rowIdx - 1].length > 0) next = newRows[rowIdx - 1].shift();
            else if (recycled.length > 0) next = randomSplice(recycled);
            else if (newRows[rowIdx].length) next = newRows[rowIdx][newRows[rowIdx].length - 1];
          }
          if (next) newRows[rowIdx].push(next);
          else break;
        }
      }

      setPositions(newPositions);
      setStack(newStack);
      recycledRef.current = recycled;

      if (newStack.length < STACK_REFILL_THRESHOLD && !fetchingMore.current) {
        fetchingMore.current = true;
        (async () => {
          const { data: urls } = await axios.get(`/api/dogs?size=${EXTRA_BATCH}`);
          await Promise.all(
            urls.map(u => new Promise(res => {
              const img = new window.Image();
              img.onload = img.onerror = res;
              img.src = u;
            }))
          );
          setStack(prev => [...prev, ...urls]);
          fetchingMore.current = false;
        })();
      }
      return newRows;
    });
  }, 1000 / 60);

  function handleHover(rowIdx, imgIdx, url) {
    setHovered({ rowIdx, imgIdx, url });
  }

  function handleUnhover() {
    setHovered({ rowIdx: null, imgIdx: null, url: null });
  }

  return (
    <div className="carousel-wrapper">
      {!isReady && (
        <div className="loading-overlay">
          <div>
            <div style={{ fontWeight: 'bold', fontSize: '1.7rem' }}>
              Připravuji pejsky
              <AnimatedDots />
            </div>
            <div style={{ marginTop: '1.2rem', color: '#666', fontSize: '1.3rem' }}>
              Chvilku strpení, prosím
            </div>
          </div>
        </div>
      )}
      <h1 className="title">Adoptuj mě, prosím!</h1>
      <div className="snake">
        {rows.map((row, rowIdx) => (
          <div
            key={ROW_DEFS[rowIdx].key}
            className="snake-row"
            style={{ overflow: 'hidden', height: '10rem' }}
          >
            <div
              id={ROW_DEFS[rowIdx].key + '-content'}
              className="row-content"
              style={{
                transform: `translateX(${positions[rowIdx]}px)`,
                transition: 'transform 0s linear',
                willChange: 'transform'
              }}
            >
              {row.slice(0, IMAGES_PER_ROW).map((url, i) => (
                url &&
                <img
                  key={i}
                  src={url}
                  alt="Dog"
                  className={
                    "dog-image" +
                    (hovered &&
                      hovered.rowIdx === rowIdx &&
                      hovered.imgIdx === i
                      ? " dog-image-hovered"
                      : "")
                  }
                  draggable={false}
                  style={{ userSelect: 'none', pointerEvents: 'auto' }}
                  onMouseEnter={() => handleHover(rowIdx, i, url)}
                  onMouseLeave={handleUnhover}
                />
              ))}
            </div>
          </div>
        ))}
      </div>
      {hovered.url && (
        <div className="dog-image-overlay" onMouseLeave={handleUnhover}>
          <img src={hovered.url} alt="Preview" className="dog-image-fullsize" />
        </div>
      )}
    </div>
  );
}