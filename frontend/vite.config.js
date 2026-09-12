import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The React app calls the Spring Boot API on http://localhost:8080.
// Relative "/api" URLs are forwarded there by Vite, which is why no CORS setup
// is needed while developing (the backend still enables CORS, so calling
// http://localhost:8080 directly works too - see frontend/.env.example).
const API_TARGET = 'http://localhost:8080'

const proxy = {
  '/api': {
    target: API_TARGET,
    changeOrigin: true,
    // Without this, a stopped backend shows up as a mysterious empty
    // "500 Internal Server Error" in the browser. This turns it into a real
    // sentence the user can act on, in the same JSON shape the API uses.
    configure(proxyServer) {
      proxyServer.on('error', (error, request, response) => {
        if (response && !response.headersSent) {
          response.writeHead(503, { 'Content-Type': 'application/json' })
          response.end(
            JSON.stringify({
              status: 503,
              message: `The backend is not reachable at ${API_TARGET}. Start it with start-backend.bat, or: cd backend && .\\mvnw.cmd spring-boot:run`,
              errors: null,
            }),
          )
        } else if (response) {
          response.end()
        }
      })
    },
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: false,
    // Allow access through a tunnel or another machine on the LAN.
    // Harmless for normal use on http://localhost:5173.
    allowedHosts: true,
    proxy,
  },
  // Same forwarding for `npm run preview`, which serves the production build.
  preview: {
    port: 4173,
    allowedHosts: true,
    proxy,
  },
})
