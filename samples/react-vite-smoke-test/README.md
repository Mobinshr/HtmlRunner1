# React + Vite Smoke Test

This small app checks whether HTML Runner can load a production Vite build from a selected project folder.

## Build

Requires Node.js and npm:

```bash
npm install
npm run build
```

The production files are written to `dist/`.

## Test in HTML Runner

1. Copy the complete `dist` folder to the phone.
2. In HTML Runner, choose **Project Folder** and select that `dist` folder.
3. Open `index.html` from the selected project.
4. Confirm the counter increments and resets.
5. Open **تست مسیر** and confirm the second page appears.
6. Confirm the local SVG logo and styling are visible.

The Vite config uses `base: './'` so generated assets use relative paths. HashRouter is used so route navigation does not depend on server-side URL rewriting.

This tests the production build, not the Vite development server.
