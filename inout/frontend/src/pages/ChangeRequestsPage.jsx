import { useState, useEffect, useCallback } from 'react';
import { getMyChangeRequests, getChangeRequestInbox, changeRequestStatus } from '../api/changeRequestApi';
import { Toast } from '../utils/toast';
import { REQUEST_STATUS_META } from '../utils/changeRequest';
import { formatDateTime } from '../utils/stockCount';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import ChangeRequestForm from '../components/request/ChangeRequestForm';

function StatusPill({ status }) {
  const meta = REQUEST_STATUS_META[status] || { label: status, cls: '' };
  return <span className={`px-2 py-0.5 text-[11px] font-semibold rounded-full border ${meta.cls}`}>{meta.label}</span>;
}

/**
 * 수정 요청 목록 (role = 'emp' | 'owner' | 'admin').
 * - 직원: 보낸 요청 + 새 요청
 * - 점주: 받은 요청(직원 → 점주) + 보낸 요청(점주 → 본사)
 * - 본사: 받은 요청(매장 → 본사)
 */
export default function ChangeRequestsPage({ role = 'emp' }) {
  const tabs = role === 'emp' ? ['mine'] : role === 'owner' ? ['inbox', 'mine'] : ['inbox'];
  const [tab, setTab] = useState(tabs[0]);
  const [onlyOpen, setOnlyOpen] = useState(true);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openId, setOpenId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [busy, setBusy] = useState(false);
  // 답변·반려 사유는 팝업(prompt) 대신 펼친 카드 안의 입력칸에서 받는다 (일부 브라우저·앱 화면은 prompt를 막는다).
  const [replies, setReplies] = useState({});

  const load = useCallback(() => {
    setLoading(true);
    const req = tab === 'inbox' ? getChangeRequestInbox(null) : getMyChangeRequests();
    req.then((data) => setItems(data?.content || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [tab]);

  useEffect(() => { load(); }, [load]);

  const visible = tab === 'inbox' && onlyOpen ? items.filter((r) => r.status === 'REQUESTED' || r.status === 'IN_PROGRESS') : items;

  const act = async (r, status) => {
    const message = (replies[r.requestId] || '').trim() || null;
    if (status === 'REJECTED' && !message) {
      Toast.warning('반려 사유를 답변 칸에 입력해 주세요.');
      return;
    }
    setBusy(true);
    try {
      await changeRequestStatus(r.requestId, status, message);
      Toast.success('요청 상태를 바꿨습니다.');
      setReplies((prev) => ({ ...prev, [r.requestId]: '' }));
      load();
    } catch (err) {
      if (err?.resultCode === 'ALREADY_PROCESSED') load();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-4 max-w-3xl mx-auto">
      <div className="flex items-end justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-800">수정 요청</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            {role === 'admin' ? '매장에서 본사로 보낸 요청입니다. 기존 기능으로 고친 뒤 완료로 바꿔 주세요.'
              : role === 'owner' ? '직원이 보낸 요청을 처리하고, 본사에 보낼 요청을 남길 수 있습니다.'
                : '현장에서 고칠 게 보이면 바로 남겨 주세요. 처리되면 알림으로 알려 드립니다.'}
          </p>
        </div>
        {role !== 'admin' && (
          <button onClick={() => setShowForm(true)} className="shrink-0 px-4 py-2.5 text-sm font-semibold bg-slate-800 text-white rounded-xl hover:bg-slate-900">
            새 요청
          </button>
        )}
      </div>

      <div className="flex items-center justify-between gap-2">
        {tabs.length > 1 ? (
          <div className="flex gap-1 p-1 bg-slate-100 rounded-xl">
            {tabs.map((t) => (
              <button key={t} onClick={() => setTab(t)}
                className={`px-3 py-1.5 text-xs font-semibold rounded-lg ${tab === t ? 'bg-white shadow-sm text-slate-800' : 'text-slate-500'}`}>
                {t === 'inbox' ? '받은 요청' : '보낸 요청'}
              </button>
            ))}
          </div>
        ) : <span />}
        {tab === 'inbox' && (
          <label className="flex items-center gap-2 text-xs text-slate-600">
            <input type="checkbox" checked={onlyOpen} onChange={(e) => setOnlyOpen(e.target.checked)} /> 처리 안 된 것만
          </label>
        )}
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner size="lg" /></div>
      ) : visible.length === 0 ? (
        <EmptyState message={tab === 'inbox' ? '처리할 요청이 없습니다.' : '보낸 요청이 없습니다.'} />
      ) : (
        <ul className="space-y-2">
          {visible.map((r) => (
            <li key={r.requestId} className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <button onClick={() => setOpenId(openId === r.requestId ? null : r.requestId)} className="w-full text-left px-4 py-3">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm font-semibold text-slate-800 truncate">{r.title}</span>
                  <StatusPill status={r.status} />
                </div>
                <div className="text-xs text-slate-500 mt-1 truncate">
                  {r.categoryLabel}{r.refLabel ? ` · ${r.refLabel}` : ''} · {tab === 'inbox' ? `${r.storeName} ${r.requesterName}` : (r.handler === 'HQ' ? '본사로' : '점주에게')} · {formatDateTime(r.createdAt)}
                </div>
              </button>
              {openId === r.requestId && (
                <div className="border-t border-slate-100 px-4 py-3 space-y-3">
                  <p className="text-sm text-slate-700 whitespace-pre-wrap">{r.content}</p>
                  {r.photoUrl && (
                    <a href={r.photoUrl} target="_blank" rel="noreferrer">
                      <img src={r.photoUrl} alt="첨부 사진" className="max-h-60 rounded-xl border border-slate-200" />
                    </a>
                  )}
                  {r.responseMessage && (
                    <div className="px-3 py-2 rounded-xl bg-slate-50 text-xs text-slate-600">
                      <b>{r.handledByName}</b>: {r.responseMessage}
                    </div>
                  )}
                  {tab === 'inbox' && r.canHandle && (
                    <textarea rows={2} maxLength={255} value={replies[r.requestId] || ''}
                      onChange={(e) => setReplies((prev) => ({ ...prev, [r.requestId]: e.target.value }))}
                      placeholder="답변 (반려할 때는 사유 필수, 요청한 사람에게 보입니다)"
                      className="w-full px-3 py-2 text-sm border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/40 resize-none" />
                  )}
                  {tab === 'inbox' && r.canHandle && (
                    <div className="grid grid-cols-3 gap-2">
                      {r.status === 'REQUESTED' && (
                        <button disabled={busy} onClick={() => act(r, 'IN_PROGRESS')} className="py-2 text-xs font-semibold rounded-lg bg-amber-50 text-amber-700">처리 시작</button>
                      )}
                      <button disabled={busy} onClick={() => act(r, 'DONE')} className="py-2 text-xs font-semibold rounded-lg bg-emerald-50 text-emerald-700">완료</button>
                      <button disabled={busy} onClick={() => act(r, 'REJECTED')} className="py-2 text-xs font-semibold rounded-lg bg-rose-50 text-rose-700">반려</button>
                    </div>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {showForm && (
        <ChangeRequestForm role={role} onClose={() => setShowForm(false)} onDone={() => { setTab('mine'); load(); }} />
      )}
    </div>
  );
}
