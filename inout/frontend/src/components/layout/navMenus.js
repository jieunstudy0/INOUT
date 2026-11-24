/**
 * 상단 메뉴 구성 (역할별).
 * 대표 메뉴(group)에 마우스를 올리면 세부 메뉴(items)가 펼쳐진다. desc는 펼친 메뉴에 보이는 한 줄 설명.
 * quick은 펼친 메뉴 오른쪽에 강조해서 보여 주는 바로가기다.
 */
export const EMP_MENU = {
  home: '/emp/dashboard',
  consoleLabel: '현장 직원',
  groups: [
    {
      key: 'stock',
      label: '매장 재고',
      items: [
        { path: '/emp/stock-use', label: '매장 재고 · 사용', desc: '우리 매장 재고를 보고, 쓴 만큼 바로 기록합니다' },
        { path: '/emp/receipts', label: '수령 확인', desc: '도착한 물건의 실제 수량을 확인합니다' },
        { path: '/emp/stock-counts', label: '재고 실사', desc: '실사 영수증 출력, 사진으로 실재고 채우기' },
      ],
    },
    {
      key: 'order',
      label: '발주',
      items: [
        { path: '/emp/stocks', label: '본사 상품 조회', desc: '발주할 수 있는 본사 상품과 재고' },
        { path: '/emp/cart', label: '장바구니', desc: '담아 둔 상품을 발주 기안으로 올립니다' },
        { path: '/emp/orders', label: '발주 내역', desc: '내가 올린 발주의 진행 상태' },
      ],
    },
    {
      key: 'support',
      label: '요청 · 소통',
      items: [
        { path: '/emp/requests', label: '수정 요청', desc: '현장에서 고칠 것을 점주·본사에 요청합니다' },
        { path: '/emp/inquiries', label: '문의 사항', desc: '점주·본사에 묻고 답을 받습니다' },
        { path: '/emp/notifications', label: '알림', desc: '처리 결과와 새 소식' },
      ],
    },
    {
      key: 'me',
      label: '내 근무',
      items: [
        { path: '/emp/leaves', label: '연차 관리', desc: '연차 신청과 남은 연차' },
        { path: '/emp/deposit', label: '매장 예치금', desc: '매장 예치금 잔액과 사용 내역' },
        { path: '/emp/profile', label: '내 정보', desc: '연락처·비밀번호 변경' },
      ],
    },
  ],
  quick: [
    { path: '/emp/receipts', label: '수령 확인', desc: '물건이 도착했다면' },
    { path: '/emp/stock-counts', label: '오늘 재고 실사', desc: '매일 9시 준비' },
  ],
};

export const OWNER_MENU = {
  home: '/owner/dashboard',
  consoleLabel: '가맹점주',
  groups: [
    {
      key: 'stock',
      label: '매장 재고',
      items: [
        { path: '/owner/store-stocks', label: '매장 재고 관리', desc: '조정·처음 등록·안전재고·보관 위치' },
        { path: '/owner/receipts', label: '수령 확인', desc: '도착한 물건의 실제 수량을 확인합니다' },
        { path: '/owner/stock-counts', label: '실사 승인', desc: '직원이 제출한 실재고를 확인하고 반영합니다' },
      ],
    },
    {
      key: 'order',
      label: '발주 · 배송',
      items: [
        { path: '/owner/orders', label: '발주 조회', desc: '직원 기안 검토·결제와 본사 승인 진행 상태' },
        { path: '/owner/deliveries', label: '배송 조회', desc: '배송 상태와 운송장 확인' },
      ],
    },
    {
      key: 'staff',
      label: '직원',
      items: [
        { path: '/owner/users', label: '직원 관리', desc: '매장 직원 계정과 상태' },
        { path: '/owner/leaves', label: '연차 관리', desc: '직원 연차 승인·반려' },
      ],
    },
    {
      key: 'support',
      label: '요청 · 소통',
      items: [
        { path: '/owner/requests', label: '수정 요청', desc: '직원 요청 처리, 본사에 요청 보내기' },
        { path: '/owner/inquiries', label: '문의 관리', desc: '직원 문의 답변, 본사 문의' },
        { path: '/owner/notifications', label: '알림', desc: '처리할 일과 새 소식' },
      ],
    },
    {
      key: 'money',
      label: '예치금',
      items: [
        { path: '/owner/deposit', label: '예치금 관리', desc: '잔액·충전 신청·사용 내역' },
      ],
    },
  ],
  quick: [
    { path: '/owner/stock-counts', label: '실사 승인', desc: '제출된 실사 확인' },
    { path: '/owner/requests', label: '받은 수정 요청', desc: '직원이 보낸 요청' },
  ],
};

export const ADMIN_MENU = {
  home: '/admin/dashboard',
  consoleLabel: '본사 관리자',
  groups: [
    {
      key: 'order',
      label: '발주 · 배송',
      items: [
        { path: '/admin/orders', label: '발주 관리', desc: '매장 발주 승인·반려' },
        { path: '/admin/delivery', label: '배송 관리', desc: '운송장 발급과 배송 상태' },
      ],
    },
    {
      key: 'stock',
      label: '상품 · 재고',
      items: [
        { path: '/admin/stock', label: '재고 관리', desc: '본사 상품 등록·입고·조정' },
      ],
    },
    {
      key: 'support',
      label: '매장 지원',
      items: [
        { path: '/admin/requests', label: '매장 수정 요청', desc: '배송 이상·상품 정보 오류 처리' },
        { path: '/admin/inquiries', label: '문의 사항', desc: '매장 문의 답변' },
        { path: '/admin/notifications', label: '알림', desc: '처리할 일과 새 소식' },
      ],
    },
    {
      key: 'people',
      label: '인사',
      items: [
        { path: '/admin/users', label: '직원 관리', desc: '전체 계정과 소속 매장' },
        { path: '/admin/leaves', label: '연차 현황', desc: '전사 연차 현황' },
      ],
    },
    {
      key: 'money',
      label: '예치금',
      items: [
        { path: '/admin/deposit', label: '예치금 관리', desc: '매장 충전 승인·잔액 현황' },
      ],
    },
  ],
  quick: [
    { path: '/admin/orders', label: '발주 승인', desc: '결제 완료된 발주' },
    { path: '/admin/requests', label: '매장 수정 요청', desc: '본사 처리 요청' },
  ],
};

/** 현재 주소가 속한 대표 메뉴 key (상세 화면 /admin/orders/12 같은 주소도 포함) */
export function activeGroupKey(menu, pathname) {
  for (const g of menu.groups) {
    if (g.items.some((i) => pathname === i.path || pathname.startsWith(`${i.path}/`))) return g.key;
  }
  return null;
}
