export default function EmptyState({ message = '데이터가 없습니다.', icon }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-slate-400">
      {icon ?? (
        <svg className="w-14 h-14 mb-4 text-slate-200" fill="none" viewBox="0 0 24 24" strokeWidth="1.25" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round"
            d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" />
        </svg>
      )}
      <p className="text-sm font-medium">{message}</p>
    </div>
  );
}
