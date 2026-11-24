import { DEFAULT_ITEM_IMAGE } from '../../utils/images';
import AuthImage from './AuthImage';
import { BADGE_TONE_CLS as TONES } from '../../utils/stockStatus';

/**
 * 상품 카드 — 사진 + 이름·상태 표시 + 재고를 한눈에.
 * (참고 화면의 상품 목록처럼 사진 칸은 연회색 테두리·10px 모서리, 상태 표시는 연한 바탕 + 같은 계열 글자)
 *
 * badges: [{ label, tone: 'navy' | 'teal' | 'orange' | 'red' }]
 * stock:  { value, unit, tone } — 큰 글씨로 보이는 재고 숫자
 * children: 카드 아래쪽에 붙일 버튼·입력칸
 */
const STOCK_TONES = { normal: 'text-slate-900', low: 'text-amber-600', out: 'text-rose-600' };

export default function ProductCard({ imageUrl, name, sub, description, badges = [], stock, stockLabel = '재고', onClick, children, dimmed = false }) {
  const Clickable = onClick ? 'button' : 'div';
  return (
    <div className={`flex flex-col ${dimmed ? 'opacity-50' : ''}`}>
      <Clickable type={onClick ? 'button' : undefined} onClick={onClick} className="group text-left">
        <div className="aspect-[4/3] rounded-[10px] border border-slate-200 bg-slate-50 overflow-hidden">
          <AuthImage src={imageUrl} fallback={DEFAULT_ITEM_IMAGE} alt={name} loading="lazy"
            className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-[1.03]" />
        </div>
        <div className="pt-3.5 px-1">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-[15px] font-semibold text-slate-900 group-hover:underline underline-offset-4">{name}</span>
            {badges.map((b) => (
              <span key={b.label} className={`px-1.5 py-0.5 rounded-[10px] text-[11px] ${TONES[b.tone] || TONES.gray}`}>{b.label}</span>
            ))}
          </div>
          {stock && (
            <p className={`mt-2 text-xl font-semibold tabular-nums ${STOCK_TONES[stock.tone] || STOCK_TONES.normal}`}>
              <span className="text-xs font-normal text-slate-500 mr-1.5">{stockLabel}</span>
              {Number(stock.value).toLocaleString()}
              <span className="text-sm font-normal text-slate-500 ml-1">{stock.unit || '개'}</span>
            </p>
          )}
          {sub && <p className="mt-1 text-sm text-slate-600">{sub}</p>}
          {description && <p className="mt-1 text-xs text-slate-500 line-clamp-2">{description}</p>}
        </div>
      </Clickable>
      {children && <div className="mt-3 px-1">{children}</div>}
    </div>
  );
}
