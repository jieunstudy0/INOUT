import AuthImage from './AuthImage';

/** 직원 사진. 사진이 없으면 이름 첫 글자를 보여 준다. size: px */
export default function Avatar({ src, name, size = 40 }) {
  const style = { width: size, height: size };
  if (src) {
    return <AuthImage src={src} alt={name || '직원 사진'} style={style} className="rounded-full object-cover border border-slate-200 shrink-0" />;
  }
  return (
    <span style={{ ...style, fontSize: Math.round(size * 0.4) }}
      className="rounded-full bg-indigo-50 text-indigo-600 font-semibold flex items-center justify-center shrink-0">
      {(name || '?').charAt(0)}
    </span>
  );
}
