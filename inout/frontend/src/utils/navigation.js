/**
 * React 밖(axios 인터셉터, 토스트 버튼)에서 화면을 이동하기 위한 연결 장치.
 * Router 안의 NavigationBridge가 useNavigate()로 얻은 함수를 등록한다.
 */
let navigator = null;

export function setNavigator(fn) {
  navigator = fn;
}

export function navigateTo(path) {
  if (navigator) navigator(path);
  else window.location.assign(path);
}
