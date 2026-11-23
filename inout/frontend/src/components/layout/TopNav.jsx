import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { activeGroupKey } from './navMenus';

const Chevron = ({ className = 'w-3.5 h-3.5' }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth="2.2" stroke="currentColor" aria-hidden="true">
    <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
  </svg>
);

/**
 * 상단 메뉴 (모든 역할 공용).
 * - 로고를 누르면 역할별 홈(대시보드)으로 간다.
 * - 대표 메뉴에 마우스를 올리면(또는 키보드로 이동·탭하면) 헤더 아래로 세부 메뉴가 넓게 펼쳐진다.
 * - 휴대폰·태블릿(lg 미만)에서는 햄버거 버튼으로 메뉴 패널을 연다.
 *
 * utility: 맨 위 얇은 줄에 놓을 정보(지점·예치금·이름 등)
 * actions: 메뉴 줄 오른쪽에 놓을 버튼(알림·로그아웃 등)
 */
export default function TopNav({ menu, utility, actions, mobileActions }) {
  const location = useLocation();
  const [openKey, setOpenKey] = useState(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const closeTimer = useRef(null);
  const activeKey = activeGroupKey(menu, location.pathname);
  const openGroup = menu.groups.find((g) => g.key === openKey);

  // 화면을 옮기면 펼친 메뉴를 닫는다.
  const [lastPath, setLastPath] = useState(location.pathname);
  if (lastPath !== location.pathname) {
    setLastPath(location.pathname);
    setOpenKey(null);
    setDrawerOpen(false);
  }

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') {
        setOpenKey(null);
        setDrawerOpen(false);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const open = (key) => {
    clearTimeout(closeTimer.current);
    setOpenKey(key);
  };
  // 메뉴와 펼친 영역 사이를 지나갈 때 깜빡이지 않도록 조금 늦게 닫는다.
  const scheduleClose = () => {
    clearTimeout(closeTimer.current);
    closeTimer.current = setTimeout(() => setOpenKey(null), 120);
  };

  return (
    <header className="sticky top-0 z-40 bg-white" onMouseLeave={scheduleClose}>
      {/* ── 맨 위 얇은 줄: 소속·예치금·이름 (데스크톱) ── */}
      <div className="hidden lg:block border-b border-slate-100">
        <div className="max-w-7xl mx-auto px-8 h-9 flex items-center justify-end gap-5 text-xs text-slate-600">
          {utility}
        </div>
      </div>

      {/* ── 메뉴 줄 ── */}
      <div className="border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 lg:px-8 h-16 lg:h-[72px] flex items-center gap-4 lg:gap-10">
          <button type="button" onClick={() => setDrawerOpen(true)} aria-label="메뉴 열기"
            className="lg:hidden -ml-1 p-2 rounded-lg text-slate-700 hover:bg-slate-50">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" strokeWidth="1.8" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
            </svg>
          </button>

          <Link to={menu.home} className="flex items-center gap-2.5 shrink-0" aria-label="홈으로">
            <span className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white text-sm font-bold tracking-tight">IN</span>
            <span className="leading-tight">
              <span className="block text-lg font-semibold text-slate-900 tracking-tight">INOUT</span>
              <span className="hidden sm:block text-[10px] text-slate-500">{menu.consoleLabel}</span>
            </span>
          </Link>

          <nav className="hidden lg:flex items-stretch self-stretch gap-1" aria-label="주 메뉴">
            {menu.groups.map((g) => {
              const highlighted = openKey === g.key || (!openKey && activeKey === g.key);
              return (
                <button key={g.key} type="button"
                  onMouseEnter={() => open(g.key)}
                  onFocus={() => open(g.key)}
                  onClick={() => (openKey === g.key ? setOpenKey(null) : open(g.key))}
                  aria-expanded={openKey === g.key}
                  className={`relative px-4 text-[17px] transition-colors ${highlighted ? 'text-slate-900 font-semibold' : 'text-slate-700 hover:text-slate-900'}`}>
                  {g.label}
                  <span className={`absolute left-4 right-4 -bottom-px h-[3px] bg-indigo-600 transition-opacity ${highlighted ? 'opacity-100' : 'opacity-0'}`} />
                </button>
              );
            })}
          </nav>

          <div className="ml-auto flex items-center gap-2 lg:gap-3">
            <div className="hidden lg:flex items-center gap-3">{actions}</div>
            <div className="lg:hidden flex items-center gap-1">{mobileActions ?? actions}</div>
          </div>
        </div>
      </div>

      {/* ── 펼친 세부 메뉴 (데스크톱) ── */}
      {openGroup && (
        <div className="hidden lg:block absolute left-0 right-0 top-full bg-white border-b border-slate-200 shadow-lg"
          onMouseEnter={() => open(openGroup.key)}>
          <div className="max-w-7xl mx-auto px-8 py-9 flex gap-10">
            <div className="flex-1 grid grid-cols-3 gap-x-10 gap-y-7">
              {openGroup.items.map((item) => (
                <NavLink key={item.path} to={item.path} onClick={() => setOpenKey(null)} className="group block">
                  {({ isActive }) => (
                    <>
                      <span className={`flex items-center gap-1 text-[15px] font-semibold ${isActive ? 'text-indigo-600' : 'text-slate-900'} group-hover:underline underline-offset-4`}>
                        {item.label} <Chevron />
                      </span>
                      <span className="block mt-1.5 text-sm text-slate-500">{item.desc}</span>
                    </>
                  )}
                </NavLink>
              ))}
            </div>
            <div className="w-64 shrink-0 space-y-3">
              {menu.quick.map((q) => (
                <Link key={q.path} to={q.path} onClick={() => setOpenKey(null)}
                  className="block rounded-xl bg-indigo-50 px-5 py-4 hover:bg-indigo-100 transition-colors">
                  <span className="flex items-center gap-1 text-sm font-semibold text-slate-900">{q.label} <Chevron /></span>
                  <span className="block mt-1 text-xs text-slate-500">{q.desc}</span>
                </Link>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ── 휴대폰·태블릿 메뉴 패널 ── */}
      {drawerOpen && (
        <div className="lg:hidden fixed inset-0 z-50">
          <div className="absolute inset-0 bg-slate-900/40" onClick={() => setDrawerOpen(false)} />
          <div className="absolute inset-y-0 left-0 w-[86%] max-w-sm bg-white flex flex-col">
            <div className="h-16 px-5 flex items-center justify-between border-b border-slate-200">
              <Link to={menu.home} className="flex items-center gap-2.5" onClick={() => setDrawerOpen(false)}>
                <span className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white text-sm font-bold">IN</span>
                <span className="text-lg font-semibold text-slate-900">INOUT</span>
              </Link>
              <button type="button" onClick={() => setDrawerOpen(false)} aria-label="메뉴 닫기" className="p-2 text-slate-500">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth="2" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="px-5 py-3 bg-slate-50 text-xs text-slate-600 flex flex-wrap gap-x-4 gap-y-1">{utility}</div>
            <nav className="flex-1 overflow-y-auto px-5 py-4 space-y-6" aria-label="주 메뉴">
              {menu.groups.map((g) => (
                <div key={g.key}>
                  <p className="text-xs font-semibold text-slate-400 mb-2">{g.label}</p>
                  <ul className="space-y-0.5">
                    {g.items.map((item) => (
                      <li key={item.path}>
                        <NavLink to={item.path} onClick={() => setDrawerOpen(false)}
                          className={({ isActive }) => `flex items-center justify-between py-2.5 text-[15px] ${isActive ? 'text-indigo-600 font-semibold' : 'text-slate-900'}`}>
                          {item.label} <Chevron className="w-4 h-4 text-slate-300" />
                        </NavLink>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </nav>
          </div>
        </div>
      )}
    </header>
  );
}
