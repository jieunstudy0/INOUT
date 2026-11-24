import { NavLink, useLocation } from 'react-router-dom';
import { activeGroupKey } from './navMenus';

/**
 * 본문 왼쪽 메뉴 — 지금 보고 있는 대표 메뉴(예: 매장 재고)의 세부 메뉴를 세로로 보여 준다.
 * 상단 메뉴에서 골라도 되고, 같은 묶음 안에서는 여기서 바로 옮겨 다닐 수 있다.
 * 홈(대시보드)처럼 어느 묶음에도 속하지 않는 화면에서는 보이지 않는다 (홈은 대시보드의 바로가기 카드로 이동한다).
 * 휴대폰·태블릿(lg 미만)에서는 숨긴다 (햄버거 메뉴가 같은 역할을 한다).
 */
export default function SideNav({ menu }) {
  const { pathname } = useLocation();
  const key = activeGroupKey(menu, pathname);
  const group = menu.groups.find((g) => g.key === key);

  if (!group) return null;

  return (
    <aside className="hidden lg:block w-56 shrink-0 self-start sticky top-[124px]" aria-label="세부 메뉴">
      <Section title={group.label} items={group.items} />
    </aside>
  );
}

function Section({ title, items }) {
  return (
    <div>
      <p className={`pb-3 border-b border-slate-900 text-slate-900 text-base`}>{title}</p>
      <ul>
        {items.map((item) => (
          <li key={item.path} className="border-b border-slate-200">
            <NavLink
              to={item.path}
              className={({ isActive }) =>
                `block my-1.5 px-3 rounded-md transition-colors py-3.5 text-[15px] ${
                  isActive
                    ? 'bg-indigo-50 text-slate-900 font-semibold'
                    : 'text-slate-700 hover:bg-slate-50 hover:text-slate-900'
                }`
              }
            >
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </div>
  );
}
