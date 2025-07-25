# Frontend Module

This module contains the user interface and client-side logic for the ShelterAggregator application.

## Purpose

- **Presentation Layer**: Provides the React-based UI for browsing and filtering adoptable dogs.
- **API Proxy**: Uses a Node.js/Express dev server to proxy requests to the backend, handle CORS, and convert remote images to Data URIs.
- **Local Simulation**: Supports a fully simulated environment for frontend development without hitting real shelter APIs.

## Technologies & Dependencies

- **Runtime**: Node.js 16+ (LTS recommended)
- **Framework**: React 18
- **Dev Server**: Express 4.x (built into the project)
- **Bundler**: Create React App (react-scripts 5.0.1)

**Key Packages**:

- UI: `react`, `react-dom`, `axios`, `react-fast-marquee`
- Development: `concurrently`, `cross-env`, `cors`, `express`, `node-fetch`

## Folder Structure

```
frontend/
├─ public/             # Static assets, index.html
├─ src/                # React source files
│  ├─ components/      # UI components (cards, filters, lists)
│  ├─ pages/           # Route-level pages (Home, Detail)
│  ├─ api/             # API client wrappers (calls to /api/dogs)
│  └─ App.jsx          # Main application entry
├─ server/             # Node.js/Express proxy server
│  └─ index.js         # Proxy routes and image handler
├─ package.json        # Scripts, dependencies
└─ README.md           # This file
```

## Scripts & Commands

- **Install**: `npm install`
- **Start Development**: `npm run dev`
  - Runs React on `http://localhost:3001` and proxy on `http://localhost:5000` concurrently
- **Build for Production**: `npm run build`
- **Start Production Server**: `npm start`

## API Routes (Proxy)

| Method | Route           | Description                                                            |
| ------ | --------------- | ---------------------------------------------------------------------- |
| GET    | `/api/dogs`     | Fetches dog listings from backend; returns JSON + Data URIs for images |
| GET    | `/api/dogs/:id` | Fetches details for a single dog, including embedded image data        |

## Running Locally

1. Ensure Node.js 16+ is installed.
2. In the `frontend/` directory:
   ```bash
   npm install
   npm run dev
   ```
3. Open `http://localhost:3001` to view the app.

## Testing

- **Component Tests**: Add tests in `src/__tests__/` using Jest & React Testing Library.

---

*See the parent project README for overall setup and backend instructions.*

