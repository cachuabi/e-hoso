import { useState } from 'react';
import { api } from '../api';
import { STATUS_TEXT, type DossierStatus } from '../labels';
import { ErrorBox } from '../ui';
import { useLoad } from '../hooks';

interface Stat {
  name: string;
  onTime: number;
  late: number;
  onTimeRate: number | null;
}

interface DashboardData {
  byStatus: Record<string, number>;
  byProcedure: Stat[];
  byOfficer: Stat[];
  overdueNow: number;
}

function monthStart(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
}

function StatTable({ title, stats }: { title: string; stats: Stat[] }) {
  return (
    <div className="card">
      <h3>{title}</h3>
      <table className="rs">
        <thead>
          <tr>
            <th>Tên</th>
            <th>Đúng hạn</th>
            <th>Trễ hạn</th>
            <th>Tỷ lệ đúng hạn</th>
          </tr>
        </thead>
        <tbody>
          {stats.map((s) => (
            <tr key={s.name}>
              <td data-th="Tên">{s.name}</td>
              <td data-th="Đúng hạn">{s.onTime}</td>
              <td data-th="Trễ hạn">{s.late}</td>
              <td data-th="Tỷ lệ đúng hạn">
                {s.onTimeRate === null ? '–' : `${(s.onTimeRate * 100).toFixed(1)}%`}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {stats.length === 0 && <p className="muted">0 hồ sơ trong khoảng thời gian này.</p>}
    </div>
  );
}

export function Dashboard() {
  const [from, setFrom] = useState(monthStart());
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10));
  const query = `from=${from}&to=${to}`;
  const { data, error } = useLoad(() => api.get<DashboardData>(`/dashboard?${query}`), query);
  const exportCsv = async () => {
    const blob = await api.csv(`/dashboard/export.csv?${query}`);
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'dashboard.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  };
  return (
    <>
      <h2 className="page">Dashboard</h2>
      <ErrorBox message={error} />
      <div className="card">
        <div className="grid">
          <div className="field">
            <label htmlFor="from">Từ ngày</label>
            <input id="from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="to">Đến ngày</label>
            <input id="to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </div>
        </div>
        <button className="btn alt" onClick={() => void exportCsv()}>
          Xuất CSV
        </button>
      </div>
      {data && (
        <>
          <div className="grid">
            <div className="card">
              <div className="muted">Đang quá hạn</div>
              <div className="kpi">{data.overdueNow}</div>
            </div>
            {Object.entries(data.byStatus).map(([k, v]) => (
              <div className="card" key={k}>
                <div className="muted">{STATUS_TEXT[k as DossierStatus] ?? k}</div>
                <div className="kpi">{v}</div>
              </div>
            ))}
          </div>
          <StatTable title="Đúng hạn, trễ hạn theo thủ tục" stats={data.byProcedure} />
          <StatTable title="Đúng hạn, trễ hạn theo công chức" stats={data.byOfficer} />
        </>
      )}
    </>
  );
}
