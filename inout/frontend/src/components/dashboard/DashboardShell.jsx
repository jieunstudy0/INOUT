/**
 * 3역할 공통 대시보드 셸 — 페이지 헤더 + 3구역 슬롯
 * [1] KPI 4열  /  [2] 메인 비주얼  /  [3] 최근 활동 테이블
 */
export default function DashboardShell({
  title,
  subtitle,
  actions,
  kpiSlot,
  mainSlot,
  activitySlot,
}) {
  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-800">{title}</h2>
          {subtitle && <div className="mt-1 text-sm text-slate-500">{subtitle}</div>}
        </div>
        {actions && <div className="flex items-center gap-2 flex-wrap">{actions}</div>}
      </div>

      {/* 1구역: KPI */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {kpiSlot}
      </div>

      {/* 2구역: 메인 비주얼 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {mainSlot}
      </div>

      {/* 3구역: 최근 활동 */}
      {activitySlot}
    </div>
  );
}
