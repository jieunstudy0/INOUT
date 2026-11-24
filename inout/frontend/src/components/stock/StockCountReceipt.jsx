/**
 * 80mm 영수증 프린터용 재고 실사표.
 *
 * 화면에서는 미리보기로 보이고, 인쇄할 때는 이 영역만 출력된다 (index.css의 @media print 규칙).
 * 사진을 AI가 읽기 쉽도록 실재고 칸은 굵은 네모 칸으로, 줄 번호는 크게 찍는다.
 */
const REASON_LABEL = {
  LOW_STOCK: '부족',
  RECENT_USE: '최근사용',
};

function formatDateTime(value) {
  if (!value) return '-';
  const d = new Date(value);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function StockCountReceipt({ sheet }) {
  if (!sheet) return null;

  return (
    <div className="stock-count-receipt">
      <div className="scr-title">재고 실사표</div>
      <div className="scr-code">{sheet.sheetCode}</div>
      <div className="scr-meta">{sheet.storeName} · 출력 기준 {formatDateTime(sheet.createdAt)} · {sheet.lines.length}품목</div>

      <table className="scr-table">
        <thead>
          <tr>
            <th className="scr-no">No</th>
            <th className="scr-name">상품명</th>
            <th className="scr-loc">위치</th>
            <th className="scr-sys">기록</th>
            <th className="scr-real">실재고</th>
          </tr>
        </thead>
        <tbody>
          {sheet.lines.map((line) => (
            <tr key={line.lineId}>
              <td className="scr-no">{String(line.lineNo).padStart(2, '0')}</td>
              <td className="scr-name">
                <div className="scr-item">{line.itemName}</div>
                <div className="scr-sub">
                  {REASON_LABEL[line.reason]}
                  {line.unitDescription ? ` · ${line.unitDescription}` : ''}
                </div>
              </td>
              <td className="scr-loc">{line.location || '-'}</td>
              <td className="scr-sys">{line.systemQty}</td>
              <td className="scr-real"><div className="scr-box" /></td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="scr-guide">
        <div>· 실재고 칸에 숫자만 또렷하게 적어 주세요.</div>
        <div>· 세지 못한 상품은 칸을 비워 두세요.</div>
        <div>· 고칠 때는 두 줄을 긋고 옆에 다시 쓰세요.</div>
      </div>
      <div className="scr-sign">
        <span>확인자 ____________</span>
        <span>시각 ______</span>
      </div>
    </div>
  );
}
