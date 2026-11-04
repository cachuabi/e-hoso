import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { api, type Procedure } from '../api';
import { formatDateTime, STATUS_TEXT, type DossierStatus } from '../labels';
import { useSession } from '../session';
import { ErrorBox, StatusTag } from '../ui';
import { useAction, useLoad } from '../hooks';

function deadlineText(p: Procedure): string {
  return p.deadlineUnit === 'SAME_DAY' ? 'Trong ngày' : `${p.deadlineDays} ngày làm việc`;
}

export function Procedures() {
  const { data, error } = useLoad(() => api.get<Procedure[]>('/procedures'), 'procedures');
  const { session } = useSession();
  return (
    <>
      <h2 className="page">Danh mục thủ tục</h2>
      <ErrorBox message={error} />
      <div className="grid">
        {data?.map((p) => (
          <div className="card" key={p.code}>
            <h3>{p.name}</h3>
            <p className="muted">Thời hạn: {deadlineText(p)}</p>
            <ul className="plain">
              {p.components.map((c) => (
                <li key={c.id}>
                  {c.name} ({c.requirement === 'REQUIRED' ? 'bắt buộc' : 'có điều kiện'})
                </li>
              ))}
            </ul>
            {p.documentsToPresent && (
              <p className="muted">Giấy tờ cần xuất trình: {p.documentsToPresent}</p>
            )}
            <div className="acts">
              <Link className="btn" to={session ? `/ho-so/moi/${p.code}` : '/dang-nhap'}>
                Nộp hồ sơ
              </Link>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}

interface PublicStatus {
  procedureName: string;
  status: DossierStatus;
  milestones: { status: DossierStatus; occurredAt: string }[];
  dueAt: string | null;
}

export function Lookup() {
  const [code, setCode] = useState('');
  const [last4, setLast4] = useState('');
  const [result, setResult] = useState<PublicStatus | null>(null);
  const { busy, error, run } = useAction();
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setResult(null);
    void run(async () => {
      setResult(await api.post<PublicStatus>('/public/lookup', { code, nationalIdLast4: last4 }));
    });
  };
  return (
    <>
      <h2 className="page">Tra cứu hồ sơ</h2>
      <form className="card" onSubmit={submit}>
        <div className="field">
          <label htmlFor="code">
            Mã tham chiếu hoặc mã hồ sơ <span className="req">*</span>
          </label>
          <input id="code" required value={code} onChange={(e) => setCode(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="last4">
            4 số cuối số định danh <span className="req">*</span>
          </label>
          <input
            id="last4"
            required
            inputMode="numeric"
            maxLength={4}
            pattern="[0-9]{4}"
            value={last4}
            onChange={(e) => setLast4(e.target.value)}
          />
        </div>
        <button className="btn" disabled={busy}>
          Tra cứu
        </button>
      </form>
      <ErrorBox message={error} />
      {result && (
        <div className="card">
          <h3>Kết quả</h3>
          <dl>
            <dt>Thủ tục</dt>
            <dd>{result.procedureName}</dd>
            <dt>Trạng thái</dt>
            <dd>
              <StatusTag status={result.status} />
            </dd>
            <dt>Ngày hẹn trả</dt>
            <dd>{formatDateTime(result.dueAt)}</dd>
          </dl>
          <h3 style={{ marginTop: 12 }}>Các mốc</h3>
          <ul className="plain">
            {result.milestones.map((m) => (
              <li key={m.occurredAt + m.status}>
                {STATUS_TEXT[m.status]}: {formatDateTime(m.occurredAt)}
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}
