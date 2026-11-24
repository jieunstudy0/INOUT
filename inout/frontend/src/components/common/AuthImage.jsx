import { useEffect, useState } from 'react';

/**
 * 로그인해야 볼 수 있는 업로드 사진(/uploads/...)을 보여 준다.
 *
 * <img src="/uploads/..">는 로그인 토큰(Authorization 헤더)을 보내지 못해 401이 난다.
 * 그래서 토큰을 붙여 fetch로 받아 브라우저 안의 임시 주소(blob:)로 바꿔 보여 준다.
 * 사진 주소는 바뀌지 않으므로, 한 번 받은 사진은 화면을 옮겨도 다시 받지 않도록 기억해 둔다.
 */
const cache = new Map(); // url -> Promise<blobUrl | null>

function load(url) {
  if (!cache.has(url)) {
    const token = localStorage.getItem('accessToken');
    cache.set(url, fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      .then((res) => (res.ok ? res.blob() : null))
      .then((blob) => (blob ? URL.createObjectURL(blob) : null))
      .catch(() => null)
      .then((result) => {
        if (!result) cache.delete(url); // 실패하면 다음에 다시 시도한다
        return result;
      }));
  }
  return cache.get(url);
}

/** 보호된 경로(/uploads)만 토큰을 붙여 받고, 그 밖의 주소(기본 이미지 등)는 그대로 쓴다. */
export default function AuthImage({ src, fallback, alt = '', ...rest }) {
  const needsAuth = !!src && src.startsWith('/uploads/');
  const [resolved, setResolved] = useState({ src: null, from: null });

  useEffect(() => {
    if (!needsAuth) return undefined;
    let alive = true;
    load(src).then((blobUrl) => { if (alive) setResolved({ src: blobUrl, from: src }); });
    return () => { alive = false; };
  }, [src, needsAuth]);

  let shown;
  if (!src) shown = fallback;
  else if (!needsAuth) shown = src;
  else if (resolved.from === src) shown = resolved.src || fallback;
  else shown = null; // 받는 중

  if (!shown) return <span aria-hidden className={`block bg-slate-100 ${rest.className || ''}`} style={rest.style} />;
  return <img src={shown} alt={alt} {...rest} />;
}
