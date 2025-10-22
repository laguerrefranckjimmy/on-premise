import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'build', // ✅ Output folder matches your Dockerfile (build/)
  },
  server: {
    port: 3000,
    open: true,
  },
})
