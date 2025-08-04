// imagePipelineStore.js
import { makeAutoObservable, reaction, runInAction } from 'mobx';
import {fetchImages} from "./DogCarouselV2.jsx";

class ImagePipelineStore {
    initialized = false;
    masterStack = [];
    bufferStack = [];
    recycleStack = [];
    rowQueues = [[], [], []];

    // config
    visibleCount = 8;
    bufferCount = 3;
    threshold = 40;    // prefetch when below double batch
    batchSize = 40;
    maxRecycle = 100;

    constructor() {
        makeAutoObservable(this);
        reaction(
            () => this.masterStack.length,
            len => {
                if (len < this.threshold) {
                    this.refillMaster();
                }
            }
        );
    }

    initializeStacks(initial, buffer) {
        runInAction(() => {
            this.masterStack = [...initial];
            this.bufferStack = [...buffer];
            this.recycleStack = [];
            this.rowQueues = [[], [], []];
            for (let row = 0; row < 3; row++) {
                this.fillRow(row);
            }
            this.initialized = true;
        });
    }

    /**
     * Fill a row to visible+buffer count, drawing from master or buffer.
     * Do NOT use recycle here; recycle only on network failure.
     */
    fillRow(row) {
        const target = this.visibleCount + this.bufferCount;
        while (this.rowQueues[row].length < target) {
            let next;
            if (this.masterStack.length > 0) {
                next = this.masterStack.shift();
            } else if (this.bufferStack.length > 0) {
                next = this.bufferStack.shift();
            } else {
                // no fresh images to draw, stop
                break;
            }
            this.rowQueues[row].push(next);
        }
    }

    /**
     * Called when an image leaves a row: cascade or recycle
     */
    popRow(row) {
        const img = this.rowQueues[row].shift();
        if (!img) return;
        if (row < 2) {
            this.rowQueues[row + 1].push(img);
            this.fillRow(row + 1);
        } else {
            // only recycle from the last row on exit
            this.recycleStack.unshift(img);
            if (this.recycleStack.length > this.maxRecycle) {
                this.recycleStack.pop();
            }
        }
        this.fillRow(row);
    }

    /**
     * Refill masterStack: try network first; on error fallback to buffer or recycle
     */
    async refillMaster() {
        try {
            console.log('[ImagePipeline] Fetching new images…');
            const more = await fetchImages(this.batchSize);
            console.log('[ImagePipeline] Got', more.length, 'new images');
            runInAction(() => {
                this.masterStack.push(...more);
            });
        } catch (err) {
            console.warn('[ImagePipeline] Network failed, falling back…', err);
            runInAction(() => {
                if (this.bufferStack.length >= this.batchSize) {
                    console.log('[ImagePipeline] Refill from bufferStack');
                    this.masterStack.push(...this.bufferStack.splice(0, this.batchSize));
                } else if (this.recycleStack.length > 0) {
                    console.log('[ImagePipeline] Refill from recycleStack');
                    // shuffle recycleStack
                    for (let i = this.recycleStack.length - 1; i > 0; i--) {
                        const j = Math.floor(Math.random() * (i + 1));
                        [this.recycleStack[i], this.recycleStack[j]] = [this.recycleStack[j], this.recycleStack[i]];
                    }
                    const take = this.recycleStack.splice(0, this.batchSize);
                    this.masterStack.push(...take);
                }
            });
        }
    }
}

export const imagePipelineStore = new ImagePipelineStore();
