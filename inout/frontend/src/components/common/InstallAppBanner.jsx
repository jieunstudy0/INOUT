import { useEffect, useState } from 'react';

const DISMISS_KEY = 'inout.installBanner.dismissedAt';
const DISMISS_DAYS = 14;

function isStandalone() {
  return window.matchMedia?.('(display-mode: standalone)').matches || window.navigator.standalone === true;
}

function isIos() {
  return /iphone|ipad|ipod/i.test(window.navigator.userAgent);
}

function recentlyDismissed() {
  const at = Number(localStorage.getItem(DISMISS_KEY) || 0);
  return at && Date.now() - at < DISMISS_DAYS * 86400 * 1000;
}

/**
 * 휴대폰에서 "홈 화면에 앱으로 설치" 안내.
 * - 안드로이드(크롬): 브라우저가 주는 설치 이벤트로 [설치] 버튼을 띄운다.
 * - 아이폰(사파리): 자동 설치가 없어 공유 → "홈 화면에 추가" 방법을 알려 준다.
 * 이미 앱으로 열었거나, 닫은 지 14일이 안 됐으면 보이지 않는다.
 */
export default function InstallAppBanner() {
  const [deferred, setDeferred] = useState(null);
  const [show, setShow] = useState(() => !isStandalone() && !recentlyDismissed() && isIos() && window.innerWidth < 1024);

  useEffect(() => {
    const onPrompt = (e) => {
      e.preventDefault();
      if (isStandalone() || recentlyDismissed()) return;
      setDeferred(e);
      setShow(true);
    };
    window.addEventListener('beforeinstallprompt', onPrompt);
    return () => window.removeEventListener('beforeinstallprompt', onPrompt);
  }, []);

  if (!show) return null;

  const dismiss = () => {
    localStorage.setItem(DISMISS_KEY, String(Date.now()));
    setShow(false);
  };

  const install = async () => {
    if (!deferred) return;
    deferred.prompt();
    await deferred.userChoice.catch(() => null);
    setDeferred(null);
    setShow(false);
  };

  return (
    <div className="lg:hidden mb-3 flex items-start gap-3 rounded-2xl border border-indigo-200 bg-indigo-50 px-4 py-3">
      <img src="/pwa-192.png" alt="" className="w-9 h-9 rounded-xl shrink-0" />
      <div className="min-w-0 flex-1 text-xs text-indigo-900">
        <p className="font-semibold">INOUT을 앱처럼 설치해 두세요</p>
        {deferred ? (
          <p className="mt-0.5 text-indigo-700">홈 화면에서 바로 열리고, 수령 확인·실사를 빠르게 할 수 있습니다.</p>
        ) : (
          <p className="mt-0.5 text-indigo-700">사파리 아래쪽 <b>공유</b> 버튼 → <b>홈 화면에 추가</b>를 눌러 주세요.</p>
        )}
      </div>
      <div className="flex flex-col gap-1 shrink-0">
        {deferred && (
          <button onClick={install} className="px-3 py-1.5 text-xs font-bold rounded-lg bg-indigo-600 text-white">설치</button>
        )}
        <button onClick={dismiss} className="px-3 py-1.5 text-xs text-indigo-600">닫기</button>
      </div>
    </div>
  );
}
