import { useState, type FormEvent } from 'react';
import { api, type Procedure, type Role } from '../api';
import { ErrorBox } from '../ui';
import { useAction, useLoad } from '../hooks';

interface StaffUser {
  id: string;
  fullName: string;
  email: string;
  role: Role;
  status: string;
}

const ROLE_TEXT: Record<string, string> = {
  CBMC: 'Cán bộ một cửa',
  CCTP: 'Công chức tư pháp – hộ tịch',
  LEADER: 'Lãnh đạo',
  ADMIN: 'Quản trị viên',
};

export function AdminUsers() {
  const {
    data,
    error: loadError,
    reload,
  } = useLoad(() => api.get<StaffUser[]>('/admin/users'), 'users');
  const [form, setForm] = useState({ fullName: '', email: '', role: 'CBMC', password: '' });
  const { busy, error, run } = useAction();
  const create = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await api.post('/admin/users', form);
      setForm({ fullName: '', email: '', role: 'CBMC', password: '' });
      reload();
    });
  };
  const patch = (id: string, body: object) =>
    run(async () => {
      await api.patch(`/admin/users/${id}`, body);
      reload();
    });
  return (
    <>
      <h2 className="page">Tài khoản cán bộ</h2>
      <ErrorBox message={loadError ?? error} />
      <table className="rs">
        <thead>
          <tr>
            <th>Họ tên</th>
            <th>Email</th>
            <th>Vai trò</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((u) => (
            <tr key={u.id}>
              <td data-th="Họ tên">{u.fullName}</td>
              <td data-th="Email">{u.email}</td>
              <td data-th="Vai trò">
                <label htmlFor={`r-${u.id}`} className="muted">
                  Đổi vai trò
                </label>{' '}
                <select
                  id={`r-${u.id}`}
                  value={u.role}
                  onChange={(e) => void patch(u.id, { role: e.target.value })}
                >
                  {Object.entries(ROLE_TEXT).map(([k, v]) => (
                    <option key={k} value={k}>
                      {v}
                    </option>
                  ))}
                </select>
              </td>
              <td data-th="Trạng thái">{u.status === 'LOCKED' ? 'Đã khoá' : 'Đang hoạt động'}</td>
              <td data-th="Thao tác">
                <button
                  className="btn alt"
                  disabled={busy}
                  onClick={() =>
                    void patch(u.id, { status: u.status === 'LOCKED' ? 'ACTIVE' : 'LOCKED' })
                  }
                >
                  {u.status === 'LOCKED' ? 'Mở khoá' : 'Khoá'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form className="card mt" onSubmit={create}>
        <h3>Tạo tài khoản</h3>
        {(
          [
            ['fullName', 'Họ tên', 'text'],
            ['email', 'Email', 'email'],
            ['password', 'Mật khẩu ban đầu', 'password'],
          ] as const
        ).map(([key, label, type]) => (
          <div className="field" key={key}>
            <label htmlFor={`n-${key}`}>
              {label} <span className="req">*</span>
            </label>
            <input
              id={`n-${key}`}
              type={type}
              required
              autoComplete="off"
              value={form[key]}
              onChange={(e) => setForm({ ...form, [key]: e.target.value })}
            />
          </div>
        ))}
        <div className="field">
          <label htmlFor="n-role">Vai trò</label>
          <select
            id="n-role"
            value={form.role}
            onChange={(e) => setForm({ ...form, role: e.target.value })}
          >
            {Object.entries(ROLE_TEXT).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
        </div>
        <button className="btn" disabled={busy}>
          Tạo
        </button>
      </form>
    </>
  );
}

function ProcedureRow({ procedure, onSaved }: { procedure: Procedure; onSaved: () => void }) {
  const [unit, setUnit] = useState(procedure.deadlineUnit);
  const [days, setDays] = useState(String(procedure.deadlineDays ?? 1));
  const [cutoff, setCutoff] = useState(procedure.applies15hCutoff);
  const { busy, error, run } = useAction();
  const id = procedure.code;
  return (
    <div className="card">
      <h3>{procedure.name}</h3>
      <ErrorBox message={error} />
      <div className="field">
        <label htmlFor={`u-${id}`}>Đơn vị thời hạn</label>
        <select
          id={`u-${id}`}
          value={unit}
          onChange={(e) => setUnit(e.target.value as Procedure['deadlineUnit'])}
        >
          <option value="SAME_DAY">Trong ngày</option>
          <option value="WORKING_DAYS">Số ngày làm việc</option>
        </select>
      </div>
      {unit === 'WORKING_DAYS' && (
        <div className="field">
          <label htmlFor={`d-${id}`}>Số ngày</label>
          <input
            id={`d-${id}`}
            type="number"
            min={1}
            value={days}
            onChange={(e) => setDays(e.target.value)}
          />
        </div>
      )}
      <div className="check">
        <input
          id={`c-${id}`}
          type="checkbox"
          checked={cutoff}
          onChange={(e) => setCutoff(e.target.checked)}
        />
        <label htmlFor={`c-${id}`}>Áp dụng mốc 15 giờ</label>
      </div>
      <button
        className="btn"
        disabled={busy}
        onClick={() =>
          void run(async () => {
            await api.put(`/admin/procedures/${id}`, {
              deadlineUnit: unit,
              deadlineDays: unit === 'SAME_DAY' ? null : Number(days),
              applies15hCutoff: cutoff,
            });
            onSaved();
          })
        }
      >
        Lưu
      </button>
    </div>
  );
}

export function AdminProcedures() {
  const { data, error, reload } = useLoad(() => api.get<Procedure[]>('/procedures'), 'list');
  return (
    <>
      <h2 className="page">Cấu hình thủ tục</h2>
      <ErrorBox message={error} />
      {data?.map((p) => (
        <ProcedureRow key={p.code} procedure={p} onSaved={reload} />
      ))}
    </>
  );
}

interface Holiday {
  day: string;
  type: string;
  description: string | null;
}

export function AdminHolidays() {
  const {
    data,
    error: loadError,
    reload,
  } = useLoad(() => api.get<Holiday[]>('/admin/holidays'), 'holidays');
  const [day, setDay] = useState('');
  const [type, setType] = useState('HOLIDAY');
  const [description, setDescription] = useState('');
  const { busy, error, run } = useAction();
  return (
    <>
      <h2 className="page">Lịch ngày nghỉ</h2>
      <ErrorBox message={loadError ?? error} />
      <table className="rs">
        <thead>
          <tr>
            <th>Ngày</th>
            <th>Loại</th>
            <th>Mô tả</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((h) => (
            <tr key={h.day}>
              <td data-th="Ngày">{h.day}</td>
              <td data-th="Loại">{h.type === 'HOLIDAY' ? 'Nghỉ lễ' : 'Làm bù'}</td>
              <td data-th="Mô tả">{h.description}</td>
              <td data-th="Thao tác">
                <button
                  className="btn alt"
                  disabled={busy}
                  onClick={() =>
                    void run(async () => {
                      await api.remove(`/admin/holidays/${h.day}`);
                      reload();
                    })
                  }
                >
                  Xoá
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        className="card mt"
        onSubmit={(e) => {
          e.preventDefault();
          void run(async () => {
            await api.post('/admin/holidays', { day, type, description });
            setDay('');
            setDescription('');
            reload();
          });
        }}
      >
        <div className="field">
          <label htmlFor="h-day">
            Ngày <span className="req">*</span>
          </label>
          <input
            id="h-day"
            type="date"
            required
            value={day}
            onChange={(e) => setDay(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="h-type">Loại</label>
          <select id="h-type" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="HOLIDAY">Nghỉ lễ</option>
            <option value="MAKEUP_WORKDAY">Làm bù</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="h-desc">Mô tả</label>
          <input id="h-desc" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <button className="btn" disabled={busy}>
          Thêm
        </button>
      </form>
    </>
  );
}
