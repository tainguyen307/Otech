import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg'],
      manifest: {
        name: 'Otech Social Marketplace',
        short_name: 'Otech',
        description: 'A social marketplace for giving useful things a second life.',
        theme_color: '#087cf5',
        background_color: '#f5f7fa',
        display: 'standalone',
        start_url: '/',
        icons: [],
      },
    }),
  ],
})
