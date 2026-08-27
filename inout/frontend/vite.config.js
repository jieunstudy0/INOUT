import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        // Refresh Token(HttpOnly) Set-Cookie / Cookie 헤더 전달
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            if (req.headers.cookie) {
              proxyReq.setHeader('cookie', req.headers.cookie);
            }
          });
        },
      },
      // /oauth2/authorization/* 만 백엔드로 프록시한다.
      // /oauth2/callback 은 React SPA 라우트(OAuth2CallbackPage)이므로
      // 프록시 대상에서 제외해야 브라우저가 index.html → React Router로 처리한다.
      '/oauth2/authorization': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/login/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
