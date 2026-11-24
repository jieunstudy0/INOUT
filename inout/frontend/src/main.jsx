import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App.jsx';

// PWA: 운영 빌드에서만 서비스 워커를 등록한다 (개발 중에는 캐시 때문에 수정이 안 보이는 일을 막는다).
// 서비스 워커는 HTTPS(또는 localhost)에서만 동작한다.
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
