/*
 * INOUT 서비스 워커 — 홈 화면 설치(PWA)와 기본 오프라인 화면을 위한 최소 구성.
 *
 * 캐시 원칙
 * - API(/api/)·업로드 파일(/uploads/)·로그인(/oauth2/)은 절대 캐시하지 않는다. 재고·돈 숫자가 오래된 값으로 보이면 안 된다.
 * - 화면 이동(navigation)은 항상 네트워크를 먼저 쓰고, 끊겼을 때만 저장해 둔 첫 화면을 보여 준다.
 * - 빌드 산출물(/assets/, 파일명에 해시가 붙음)은 한 번 받으면 재사용한다.
 *
 * 푸시 알림은 아직 넣지 않았다 (서버 쪽 Web Push 라이브러리·VAPID 키 결정이 먼저 필요).
 */
const VERSION = 'inout-v1';
const SHELL = ['/', '/index.html', '/manifest.webmanifest', '/pwa-192.png', '/pwa-512.png'];
const NO_CACHE_PREFIXES = ['/api/', '/uploads/', '/oauth2/', '/login/oauth2/', '/actuator/'];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(VERSION).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (NO_CACHE_PREFIXES.some((p) => url.pathname.startsWith(p))) return;

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request).catch(() => caches.match('/index.html')),
    );
    return;
  }

  if (url.pathname.startsWith('/assets/')) {
    event.respondWith(
      caches.match(request).then((hit) => hit || fetch(request).then((res) => {
        if (res.ok) {
          const copy = res.clone();
          caches.open(VERSION).then((cache) => cache.put(request, copy));
        }
        return res;
      })),
    );
  }
});
