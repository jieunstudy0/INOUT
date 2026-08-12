const DURATION = 4000;

const typeConfig = {
  success: {
    bg: 'bg-white',
    bar: 'bg-emerald-500',
    icon: '<svg class="w-5 h-5 text-emerald-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>',
    label: '완료',
  },
  error: {
    bg: 'bg-white',
    bar: 'bg-rose-500',
    icon: '<svg class="w-5 h-5 text-rose-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/></svg>',
    label: '오류',
  },
  warning: {
    bg: 'bg-white',
    bar: 'bg-amber-400',
    icon: '<svg class="w-5 h-5 text-amber-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"/></svg>',
    label: '주의',
  },
  info: {
    bg: 'bg-white',
    bar: 'bg-blue-500',
    icon: '<svg class="w-5 h-5 text-blue-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z"/></svg>',
    label: '안내',
  },
};

function getContainer() {
  return document.getElementById('toast-container');
}

function dismiss(el) {
  el.classList.remove('toast-enter');
  el.classList.add('toast-leave');
  setTimeout(() => {
    if (el.parentNode) el.parentNode.removeChild(el);
  }, 320);
}

function show(message, type = 'info', duration = DURATION) {
  const container = getContainer();
  if (!container) return;

  const cfg = typeConfig[type] || typeConfig.info;

  const wrapper = document.createElement('div');
  wrapper.className = [
    'pointer-events-auto',
    'flex flex-col overflow-hidden',
    'rounded-xl shadow-lg shadow-slate-200/80',
    'border border-slate-100',
    cfg.bg,
    'toast-enter',
  ].join(' ');

  const bar = document.createElement('div');
  bar.className = 'h-1 ' + cfg.bar;
  wrapper.appendChild(bar);

  const body = document.createElement('div');
  body.className = 'flex items-start gap-3 px-4 py-3';

  const iconWrap = document.createElement('div');
  iconWrap.className = 'mt-0.5';
  iconWrap.innerHTML = cfg.icon;

  const textWrap = document.createElement('div');
  textWrap.className = 'flex-1 min-w-0';

  const labelEl = document.createElement('p');
  labelEl.className = 'text-xs font-semibold text-slate-500 uppercase tracking-wider mb-0.5';
  labelEl.textContent = cfg.label;

  const msgEl = document.createElement('p');
  msgEl.className = 'text-sm text-slate-700 break-words';
  msgEl.textContent = message;

  textWrap.appendChild(labelEl);
  textWrap.appendChild(msgEl);

  const closeBtn = document.createElement('button');
  closeBtn.className = 'text-slate-300 hover:text-slate-600 transition-colors mt-0.5 shrink-0';
  closeBtn.innerHTML = '<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>';
  closeBtn.onclick = () => dismiss(wrapper);

  body.appendChild(iconWrap);
  body.appendChild(textWrap);
  body.appendChild(closeBtn);
  wrapper.appendChild(body);

  container.appendChild(wrapper);

  let timer = setTimeout(() => dismiss(wrapper), duration);
  wrapper.addEventListener('mouseenter', () => clearTimeout(timer));
  wrapper.addEventListener('mouseleave', () => {
    timer = setTimeout(() => dismiss(wrapper), 1500);
  });
}

export const Toast = {
  show,
  success: (msg, dur) => show(msg, 'success', dur),
  error:   (msg, dur) => show(msg, 'error',   dur),
  warning: (msg, dur) => show(msg, 'warning', dur),
  info:    (msg, dur) => show(msg, 'info',    dur),
};
