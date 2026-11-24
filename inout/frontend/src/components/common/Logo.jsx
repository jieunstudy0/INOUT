/**
 * INOUT 로고.
 * 마크: 짙은 네이비 사각형 안에 엇갈린 두 화살표 — 위(흰색)는 나가는 물건(OUT), 아래(하늘색)는 들어오는 물건(IN).
 * 워드마크 옆에 역할 이름(가맹점주·현장 직원·본사 관리자)을 같은 크기·굵기로, 색만 연하게 붙인다.
 */
export function LogoMark({ size = 34, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" className={`shrink-0 ${className}`} aria-hidden="true">
      <rect width="32" height="32" rx="9" fill="#051766" />
      <g fill="none" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
        <path d="M8 12.5H22.5M18.6 8.7L22.5 12.5L18.6 16.3" stroke="#ffffff" />
        <path d="M24 19.5H9.5M13.4 15.7L9.5 19.5L13.4 23.3" stroke="#7ea6e3" />
      </g>
    </svg>
  );
}

/**
 * size: 'md'(헤더) | 'lg'(로그인 화면)
 * label: 역할 이름. 없으면 워드마크만.
 */
export default function Logo({ label, size = 'md', className = '' }) {
  const text = size === 'lg' ? 'text-[28px]' : 'text-[16px] sm:text-[21px]';
  return (
    <span className={`inline-flex items-center gap-2.5 ${className}`}>
      <LogoMark size={size === 'lg' ? 44 : 34} />
      <span className={`${text} leading-none font-bold tracking-[-0.02em] whitespace-nowrap`}>
        <span className="text-slate-900">INOUT</span>
        {label && <span className="ml-2 text-slate-400">{label}</span>}
      </span>
    </span>
  );
}
