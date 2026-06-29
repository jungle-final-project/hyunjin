import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AdminShell, DataTable, MetricCard, Panel, StateMessage, StatusBadge } from '../../../components/ui';
import { getAdminDashboard } from '../adminApi';

function countLabel(value: number | null | undefined) {
  return `${value ?? 0}건`;
}

export function AdminDashboardPage() {
  const { data: dashboard, isError, isLoading } = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: getAdminDashboard
  });

  if (isLoading) {
    return (
      <AdminShell title="운영 대시보드">
        <StateMessage type="info" title="대시보드 로딩 중" body="운영 지표를 불러오고 있습니다." />
      </AdminShell>
    );
  }

  if (isError || !dashboard) {
    return (
      <AdminShell title="운영 대시보드">
        <StateMessage type="warn" title="대시보드 조회 실패" body="관리자 대시보드 API 응답을 불러오지 못했습니다." />
      </AdminShell>
    );
  }

  const statusLabel = dashboard.degraded ? '주의' : '정상';
  const generatedAt = dashboard.generatedAt ?? '갱신 시간 없음';

  return (
    <AdminShell title="운영 대시보드">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard label="진행 중 Agent" value={countLabel(dashboard.agentRunning)} tone="orange" />
        <MetricCard label="미해결 AS" value={countLabel(dashboard.openTickets)} tone="orange" />
        <MetricCard label="실행 중 Price Job" value={countLabel(dashboard.priceJobsRunning)} tone="blue" />
        <MetricCard label="운영 상태" value={statusLabel} tone={dashboard.degraded ? 'orange' : 'green'} />
      </div>
      <div className="mt-5 grid grid-cols-1 gap-5 xl:grid-cols-[1fr_420px]">
        <Panel title="최근 Agent 세션">
          <DataTable columns={['id', 'user', 'status', 'action']} rows={[
            { id: '00000000-0000-4000-8000-000000003001', user: 'user@example.com', status: <StatusBadge status="PASS" />, action: <Link className="font-bold text-brand-blue" to="/admin/agent-sessions/00000000-0000-4000-8000-000000003001">상세</Link> },
            { id: 'session-1002', user: 'dev@example.com', status: <StatusBadge status="WARN" />, action: '대기' }
          ]} />
        </Panel>
        <Panel title="운영 상태">
          <StateMessage
            type={dashboard.degraded ? 'warn' : 'success'}
            title={dashboard.degraded ? '운영 지표 확인 필요' : '운영 상태 정상'}
            body={`마지막 갱신: ${generatedAt}`}
          />
        </Panel>
      </div>
    </AdminShell>
  );
}
