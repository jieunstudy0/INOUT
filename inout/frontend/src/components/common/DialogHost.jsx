import { useEffect, useRef, useState } from 'react';
import { subscribeDialog } from '../../utils/dialog';

/**
 * utils/dialog의 confirmDialog·promptDialog·alertDialog 창을 그린다. App에 한 번만 둔다.
 * 여러 요청이 겹치면 차례대로 하나씩 보여 준다.
 */
export default function DialogHost() {
  const [items, setItems] = useState([]);
  const current = items[0];

  useEffect(() => subscribeDialog((item) => setItems((list) => [...list, item])), []);

  const close = (value) => {
    current.resolve(value);
    setItems((list) => list.slice(1));
  };

  if (!current) return null;
  return <DialogBox key={items.length + current.message} item={current} onClose={close} />;
}

function DialogBox({ item, onClose }) {
  const [value, setValue] = useState(item.defaultValue ?? '');
  const inputRef = useRef(null);
  const okRef = useRef(null);
  const isPrompt = item.kind === 'prompt';
  const isAlert = item.kind === 'alert';
  const trimmed = value.trim();
  const blocked = isPrompt && item.required !== false && !trimmed;

  const cancel = () => onClose(isPrompt ? null : isAlert ? undefined : false);
  const ok = () => {
    if (blocked) return;
    onClose(isPrompt ? trimmed : isAlert ? undefined : true);
  };

  useEffect(() => {
    (isPrompt ? inputRef.current : okRef.current)?.focus();
    const onKey = (e) => { if (e.key === 'Escape') cancel(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const okCls = item.danger
    ? 'bg-rose-600 hover:bg-rose-700 text-white'
    : 'bg-indigo-600 hover:bg-indigo-700 text-white';

  return (
    <div className="fixed inset-0 z-[110] flex items-center justify-center p-4 bg-slate-900/40" onMouseDown={cancel}>
      <div role="dialog" aria-modal="true" aria-label={item.title || '확인'}
        className="w-full max-w-sm bg-white rounded-2xl shadow-xl p-6" onMouseDown={(e) => e.stopPropagation()}>
        {item.title && <p className="text-lg text-slate-900 mb-2">{item.title}</p>}
        <p className="text-[15px] text-slate-700 whitespace-pre-line break-keep leading-relaxed">{item.message}</p>

        {isPrompt && (
          <input ref={inputRef} value={value} placeholder={item.placeholder}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') ok(); }}
            className="mt-4 w-full px-3 py-2.5 text-sm border border-slate-300 rounded-[10px] focus:outline-none focus:border-indigo-600" />
        )}

        <div className="mt-6 flex justify-end gap-2">
          {!isAlert && (
            <button type="button" onClick={cancel}
              className="px-5 py-2 text-sm rounded-full border border-slate-300 text-slate-700 hover:bg-slate-50">
              {item.cancelLabel || '취소'}
            </button>
          )}
          <button ref={okRef} type="button" onClick={ok} disabled={blocked}
            className={`px-5 py-2 text-sm rounded-full disabled:opacity-40 ${okCls}`}>
            {item.confirmLabel || '확인'}
          </button>
        </div>
      </div>
    </div>
  );
}
