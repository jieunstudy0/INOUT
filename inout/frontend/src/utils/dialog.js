/**
 * 화면 안에 그리는 확인·입력 창.
 *
 * 브라우저 기본 창(window.confirm / prompt / alert)은 앱 안 브라우저나 홈 화면에 설치한 앱(PWA),
 * 일부 웹뷰에서 뜨지 않고 바로 "취소"로 끝난다. 그러면 삭제·승인 버튼이 아무 반응 없이 멈춘다.
 * 그래서 모든 확인은 이 함수로 묻는다. 실제 창은 DialogHost가 그린다.
 *
 *   if (!(await confirmDialog('삭제할까요?'))) return;
 *   const reason = await promptDialog('반려 사유를 입력해 주세요.', { defaultValue: '재고 부족' });
 */

let listener = null;
const queue = [];

function open(request) {
  return new Promise((resolve) => {
    const item = { ...request, resolve };
    if (listener) listener(item);
    else queue.push(item);
  });
}

/** DialogHost가 창을 그리기 위해 등록한다. 등록 전에 들어온 요청도 넘겨준다. */
export function subscribeDialog(fn) {
  listener = fn;
  while (queue.length) fn(queue.shift());
  return () => { if (listener === fn) listener = null; };
}

const DANGER_WORDS = ['삭제', '반려', '취소', '비활성', '로그아웃'];

/**
 * 예/아니요 확인. true = 확인, false = 취소.
 * options: { title, confirmLabel, cancelLabel, danger } — danger를 생략하면 문구로 판단한다(삭제·반려 등).
 */
export function confirmDialog(message, options = {}) {
  const danger = options.danger ?? DANGER_WORDS.some((w) => message.includes(w));
  return open({ kind: 'confirm', message, ...options, danger });
}

/**
 * 한 줄 입력. 입력값(앞뒤 공백 제거) 또는 취소하면 null.
 * options: { title, defaultValue, placeholder, required, confirmLabel }
 */
export function promptDialog(message, options = {}) {
  return open({ kind: 'prompt', message, ...options });
}

/** 알림만 보여 주고 확인을 누르면 끝난다. */
export function alertDialog(message, options = {}) {
  return open({ kind: 'alert', message, ...options });
}
