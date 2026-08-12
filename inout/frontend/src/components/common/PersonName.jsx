/**
 * 작성자/신청자 이름 — "(퇴사자)" 병기 시 흐린 스타일
 */
export default function PersonName({ name, className = '' }) {
  if (name == null || name === '') {
    return <span className={className || 'text-slate-400'}>-</span>;
  }
  const text = String(name);
  const resigned = text.includes('(퇴사자)');
  return (
    <span className={`${resigned ? 'text-slate-400' : ''} ${className}`.trim()}>
      {text}
    </span>
  );
}
