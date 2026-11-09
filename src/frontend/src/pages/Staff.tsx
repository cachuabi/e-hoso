import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { api, type Detail, type Page, type Procedure } from '../api';
import {
  ACTION_TEXT,
  DEADLINE_TEXT,
  fileProblem,
  formatDateTime,
  ROLE_TEXT,
  STATUS_TEXT,
} from '../labels';
import { useSession } from '../session';
import { DeadlineTag, ErrorBox, StatusTag } from '../ui';
import { useAction, useLoad } from '../hooks';

export function StaffList() {
  const { session } = useSession();
  const initial = { CBMC: 'SUBMITTED', LEADER: 'PENDING_APPROVAL' }[session?.role as string] ?? '';
  const [status, setStatus] = useState(initial);
  const [label, setLabel] = useState('');
  const query = `size=100${status ? `&status=${status}` : ''}${label ? `&deadlineLabel=${label}` : ''}`;
  const { data, error } = useLoad(() => api.get<Page>(`/staff/dossiers?${query}`), query);
  return (
    <>
      <h2 className="page">Danh sách hồ sơ</h2>
      <ErrorBox message={error} />
      <div className="grid">
        <div className="field">
          <label htmlFor="status">Trạng thái</label>
          <select id="status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Tất cả</option>
            {Object.entries(STATUS_TEXT)
              .filter(([k]) => k !== 'DRAFT')
              .map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="deadline">Hạn xử lý</label>
          <select id="deadline" value={label} onChange={(e) => setLabel(e.target.value)}>
            <option value="">Tất cả</option>
            {Object.entries(DEADLINE_TEXT).map(([k, v]) => (
              <option key={k} value={k}>
                {v.text}
              </option>
            ))}
          </select>
        </div>
      </div>
      <table className="rs">
        <thead>
          <tr>
            <th>Mã</th>
            <th>Thủ tục</th>
            <th>Người nộp</th>
            <th>Trạng thái</th>
            <th>Nộp lúc</th>
            <th>Hẹn trả</th>
            <th>Hạn xử lý</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {data?.items.map((d) => (
            <tr key={d.id}>
              <td data-th="Mã">{d.dossierCode ?? d.referenceCode}</td>
              <td data-th="Thủ tục">{d.procedureName}</td>
              <td data-th="Người nộp">{d.applicantName}</td>
              <td data-th="Trạng thái">
                <StatusTag status={d.status} />
              </td>
              <td data-th="Nộp lúc">{formatDateTime(d.submittedAt)}</td>
              <td data-th="Hẹn trả">{formatDateTime(d.dueAt)}</td>
              <td data-th="Hạn xử lý">
                <DeadlineTag label={d.deadlineLabel} />
              </td>
              <td data-th="Thao tác">
                <Link to={`/can-bo/ho-so/${d.id}`}>Xử lý</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {data?.items.length === 0 && <p className="muted">Không có hồ sơ.</p>}
    </>
  );
}

function TextArea(props: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="field">
      <label htmlFor={props.id}>
        {props.label} <span className="req">*</span>
      </label>
      <textarea
        id={props.id}
        rows={2}
        value={props.value}
        onChange={(e) => props.onChange(e.target.value)}
      />
    </div>
  );
}

function IntakeActions({ dossier, reload }: { dossier: Detail; reload: () => void }) {
  const navigate = useNavigate();
  const { data: procedure } = useLoad(
    () => api.get<Procedure>(`/procedures/${dossier.summary.procedureCode}`),
    dossier.summary.procedureCode,
  );
  const [checked, setChecked] = useState(false);
  const [missing, setMissing] = useState<string[]>([]);
  const [instruction, setInstruction] = useState('');
  const [reason, setReason] = useState('');
  const { busy, error, run } = useAction();
  const url = `/staff/dossiers/${dossier.summary.id}`;
  const version = dossier.version;
  return (
    <>
      <ErrorBox message={error} />
      <div className="card">
        <h3>Tiếp nhận</h3>
        {dossier.requiresOriginalCheck && (
          <div className="check">
            <input
              id="original"
              type="checkbox"
              checked={checked}
              onChange={(e) => setChecked(e.target.checked)}
            />
            <label htmlFor="original">Đã đối chiếu bản sao với bản chính người dân mang đến</label>
          </div>
        )}
        <button
          className="btn"
          disabled={busy}
          onClick={() =>
            void run(async () => {
              await api.post(`${url}/accept`, { originalChecked: checked, version });
              navigate(`/can-bo/ho-so/${dossier.summary.id}/phieu-hen`);
            })
          }
        >
          Tiếp nhận
        </button>
      </div>
      <div className="card">
        <h3>Yêu cầu bổ sung</h3>
        {procedure?.components.map((c) => (
          <div className="check" key={c.id}>
            <input
              id={`m-${c.id}`}
              type="checkbox"
              checked={missing.includes(c.id)}
              onChange={(e) =>
                setMissing(
                  e.target.checked ? [...missing, c.id] : missing.filter((x) => x !== c.id),
                )
              }
            />
            <label htmlFor={`m-${c.id}`}>{c.name}</label>
          </div>
        ))}
        <TextArea
          id="instruction"
          label="Hướng dẫn"
          value={instruction}
          onChange={setInstruction}
        />
        <button
          className="btn alt"
          disabled={busy}
          onClick={() =>
            void run(async () => {
              await api.post(`${url}/request-supplement`, {
                instruction,
                items: missing.map((componentId) => ({ componentId })),
                version,
              });
              reload();
            })
          }
        >
          Gửi yêu cầu bổ sung
        </button>
      </div>
      <div className="card">
        <h3>Từ chối tiếp nhận</h3>
        <TextArea id="reason" label="Lý do" value={reason} onChange={setReason} />
        <button
          className="btn bad"
          disabled={busy}
          onClick={() =>
            void run(async () => {
              await api.post(`${url}/reject`, { reason, version });
              reload();
            })
          }
        >
          Từ chối
        </button>
      </div>
    </>
  );
}

function AssignAction({ dossier, reload }: { dossier: Detail; reload: () => void }) {
  const { data } = useLoad(
    () => api.get<{ id: string; fullName: string; activeDossiers: number }[]>('/staff/officers'),
    'officers',
  );
  const [officer, setOfficer] = useState('');
  const { busy, error, run } = useAction();
  return (
    <div className="card">
      <h3>Phân công</h3>
      <ErrorBox message={error} />
      <div className="field">
        <label htmlFor="officer">
          Công chức xử lý <span className="req">*</span>
        </label>
        <select id="officer" value={officer} onChange={(e) => setOfficer(e.target.value)}>
          <option value="">Chọn</option>
          {data?.map((o) => (
            <option key={o.id} value={o.id}>
              {o.fullName} (đang xử lý {o.activeDossiers} hồ sơ)
            </option>
          ))}
        </select>
      </div>
      <button
        className="btn"
        disabled={busy || !officer}
        onClick={() =>
          void run(async () => {
            await api.post(`/staff/dossiers/${dossier.summary.id}/assign`, {
              officerId: officer,
              version: dossier.version,
            });
            reload();
          })
        }
      >
        Phân công
      </button>
    </div>
  );
}

function ReturnAction({ dossier, reload }: { dossier: Detail; reload: () => void }) {
  const [method, setMethod] = useState('COUNTER');
  const { busy, error, run } = useAction();
  return (
    <div className="card">
      <h3>Trả kết quả</h3>
      <ErrorBox message={error} />
      <div className="field">
        <label htmlFor="method">Hình thức trả</label>
        <select id="method" value={method} onChange={(e) => setMethod(e.target.value)}>
          <option value="COUNTER">Tại bộ phận một cửa</option>
          <option value="ELECTRONIC">Bản điện tử</option>
        </select>
      </div>
      <button
        className="btn"
        disabled={busy}
        onClick={() =>
          void run(async () => {
            await api.post(`/staff/dossiers/${dossier.summary.id}/return-result`, {
              method,
              version: dossier.version,
            });
            reload();
          })
        }
      >
        Trả kết quả
      </button>
    </div>
  );
}

function ReviewActions({ dossier, reload }: { dossier: Detail; reload: () => void }) {
  const [opinion, setOpinion] = useState(dossier.reviewOpinion ?? '');
  const [kind, setKind] = useState(dossier.resultKind ?? 'ISSUED');
  const [place, setPlace] = useState('');
  const { busy, error, run } = useAction();
  const url = `/staff/dossiers/${dossier.summary.id}`;
  const upload = (file: File | undefined) =>
    run(async () => {
      if (!file) return;
      const problem = fileProblem(file.name, file.size);
      if (problem || !file.name.toLowerCase().endsWith('.pdf')) {
        throw new Error(problem ?? 'Dự thảo phải là tệp PDF.');
      }
      await api.upload(dossier.summary.id, file, 'DRAFT');
      reload();
    });
  return (
    <>
      <ErrorBox message={error} />
      {dossier.sendBackReason && (
        <div className="alert">
          <strong>Lãnh đạo trả lại:</strong> {dossier.sendBackReason}
        </div>
      )}
      <div className="card">
        <h3>Thẩm định</h3>
        <TextArea id="opinion" label="Ý kiến thẩm định" value={opinion} onChange={setOpinion} />
        <div className="field">
          <label htmlFor="kind">Loại kết quả</label>
          <select id="kind" value={kind} onChange={(e) => setKind(e.target.value)}>
            <option value="ISSUED">Cấp giấy</option>
            <option value="REFUSED">Từ chối giải quyết</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="draft">
            Dự thảo kết quả (PDF) <span className="req">*</span>
          </label>
          <input
            id="draft"
            type="file"
            accept=".pdf"
            onChange={(e) => void upload(e.target.files?.[0])}
          />
        </div>
        <button
          className="btn"
          disabled={busy}
          onClick={() =>
            void run(async () => {
              await api.post(`${url}/submit-approval`, {
                opinion,
                resultKind: kind,
                version: dossier.version,
              });
              reload();
            })
          }
        >
          Trình phê duyệt
        </button>
      </div>
      {dossier.allowsVerification && !dossier.verificationFlag && (
        <div className="card">
          <h3>Cần xác minh</h3>
          <p className="muted">Cộng 3 ngày làm việc vào ngày hẹn trả, chỉ một lần.</p>
          <div className="field">
            <label htmlFor="place">
              Nơi xác minh <span className="req">*</span>
            </label>
            <input id="place" value={place} onChange={(e) => setPlace(e.target.value)} />
          </div>
          <button
            className="btn alt"
            disabled={busy}
            onClick={() =>
              void run(async () => {
                await api.post(`${url}/verification`, { place, version: dossier.version });
                reload();
              })
            }
          >
            Đánh dấu cần xác minh
          </button>
        </div>
      )}
    </>
  );
}

function ApprovalActions({ dossier, reload }: { dossier: Detail; reload: () => void }) {
  const [password, setPassword] = useState('');
  const [reason, setReason] = useState('');
  const { busy, error, run } = useAction();
  const url = `/staff/dossiers/${dossier.summary.id}`;
  return (
    <>
      <ErrorBox message={error} />
      <div className="card">
        <h3>Phê duyệt</h3>
        <div className="field">
          <label htmlFor="sign">
            Nhập lại mật khẩu để ký mô phỏng <span className="req">*</span>
          </label>
          <input
            id="sign"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button
          className="btn"
          disabled={busy}
          onClick={() =>
            void run(async () => {
              await api.post(`${url}/approve`, { password, version: dossier.version });
              reload();
            })
          }
        >
          Phê duyệt
        </button>
      </div>
      <div className="card">
        <h3>Trả lại cho công chức</h3>
        <TextArea id="back" label="Lý do trả lại" value={reason} onChange={setReason} />
        <button
          className="btn alt"
          disabled={busy}
          onClick={() =>
            void run(async () => {
              await api.post(`${url}/send-back`, { reason, version: dossier.version });
              reload();
            })
          }
        >
          Trả lại
        </button>
      </div>
    </>
  );
}

export function StaffDetail() {
  const { id = '' } = useParams();
  const { session } = useSession();
  const {
    data: dossier,
    error,
    reload,
  } = useLoad(() => api.get<Detail>(`/staff/dossiers/${id}`), id);
  const { data: logs } = useLoad(
    () =>
      api.get<{ action: string; actorRole: string; note: string | null; occurredAt: string }[]>(
        `/staff/dossiers/${id}/logs`,
      ),
    `${id}-${dossier?.version}`,
  );
  const { data: procedure } = useLoad(
    () =>
      dossier
        ? api.get<Procedure>(`/procedures/${dossier.summary.procedureCode}`)
        : Promise.resolve(null),
    dossier?.summary.procedureCode ?? '',
  );
  if (!dossier) return <ErrorBox message={error} />;
  const { summary } = dossier;
  const labelOf = (key: string) =>
    procedure?.formSchema.fields.find((f) => f.key === key)?.label ?? key;
  const role = session?.role;
  return (
    <>
      <h2 className="page">Hồ sơ {summary.dossierCode ?? summary.referenceCode}</h2>
      <ErrorBox message={error} />
      <div className="card">
        <dl>
          <dt>Thủ tục</dt>
          <dd>{summary.procedureName}</dd>
          <dt>Người nộp</dt>
          <dd>{summary.applicantName}</dd>
          <dt>Trạng thái</dt>
          <dd>
            <StatusTag status={summary.status} />
          </dd>
          <dt>Hẹn trả</dt>
          <dd>{formatDateTime(summary.dueAt)}</dd>
          <dt>Hạn xử lý</dt>
          <dd>
            <DeadlineTag label={summary.deadlineLabel} />
          </dd>
          {summary.assignedOfficerName && (
            <>
              <dt>Công chức</dt>
              <dd>{summary.assignedOfficerName}</dd>
            </>
          )}
          {dossier.reviewOpinion && (
            <>
              <dt>Ý kiến thẩm định</dt>
              <dd>{dossier.reviewOpinion}</dd>
            </>
          )}
          {dossier.verificationPlace && (
            <>
              <dt>Nơi xác minh</dt>
              <dd>{dossier.verificationPlace}</dd>
            </>
          )}
          {dossier.onTime !== null && (
            <>
              <dt>Kết quả xử lý</dt>
              <dd>{dossier.onTime ? 'Đúng hạn' : 'Trễ hạn'}</dd>
            </>
          )}
          {Object.entries(dossier.formData).map(([k, v]) => (
            <div key={k} className="contents">
              <dt>{labelOf(k)}</dt>
              <dd>{String(v)}</dd>
            </div>
          ))}
        </dl>
        <h3 className="mt">Tệp</h3>
        <div className="acts">
          {dossier.attachments.map((a) => (
            <button key={a.id} className="btn alt" onClick={() => void api.openFile(a.id)}>
              {a.fileName} (
              {a.kind === 'DOSSIER' ? 'hồ sơ' : a.kind === 'DRAFT' ? 'dự thảo' : 'kết quả'})
            </button>
          ))}
        </div>
      </div>
      {role === 'CBMC' && summary.status === 'SUBMITTED' && (
        <IntakeActions dossier={dossier} reload={reload} />
      )}
      {role === 'CBMC' && summary.status === 'ACCEPTED' && (
        <>
          <div className="acts mb">
            <Link className="btn alt" to={`/can-bo/ho-so/${summary.id}/phieu-hen`}>
              In phiếu hẹn trả
            </Link>
          </div>
          <AssignAction dossier={dossier} reload={reload} />
        </>
      )}
      {role === 'CBMC' && summary.status === 'APPROVED' && (
        <ReturnAction dossier={dossier} reload={reload} />
      )}
      {role === 'CCTP' && summary.status === 'IN_PROGRESS' && (
        <ReviewActions dossier={dossier} reload={reload} />
      )}
      {role === 'LEADER' && summary.status === 'PENDING_APPROVAL' && (
        <ApprovalActions dossier={dossier} reload={reload} />
      )}
      {logs && (
        <div className="card">
          <h3>Nhật ký xử lý</h3>
          <ul className="plain">
            {logs.map((l, index) => (
              <li key={index}>
                {formatDateTime(l.occurredAt)}: {ACTION_TEXT[l.action] ?? l.action} (
                {ROLE_TEXT[l.actorRole] ?? l.actorRole}){l.note ? `, ${l.note}` : ''}
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}

export function Receipt() {
  const { id = '' } = useParams();
  const { data, error } = useLoad(() => api.get<Detail>(`/staff/dossiers/${id}`), id);
  if (!data) return <ErrorBox message={error} />;
  return (
    <>
      <h2 className="page">Phiếu hẹn trả</h2>
      <div className="receipt">
        <dl>
          <dt>Mã hồ sơ</dt>
          <dd>{data.summary.dossierCode}</dd>
          <dt>Thủ tục</dt>
          <dd>{data.summary.procedureName}</dd>
          <dt>Người nộp</dt>
          <dd>{data.summary.applicantName}</dd>
          <dt>Hẹn trả</dt>
          <dd>
            <strong>{formatDateTime(data.summary.dueAt)}</strong>
          </dd>
        </dl>
      </div>
      <div className="acts">
        <button className="btn" onClick={() => window.print()}>
          In phiếu
        </button>
        <Link className="btn alt" to={`/can-bo/ho-so/${id}`}>
          Quay lại hồ sơ
        </Link>
      </div>
    </>
  );
}
