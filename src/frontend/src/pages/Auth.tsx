import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { api, errorText, type Role, type Session } from '../api';
import { useSession } from '../session';
import { ErrorBox } from '../ui';
import { useAction } from '../hooks';

const HOME: Record<Role, string> = {
  CITIZEN: '/ho-so',
  CBMC: '/can-bo/ho-so',
  CCTP: '/can-bo/ho-so',
  LEADER: '/can-bo/ho-so',
  ADMIN: '/quan-tri/tai-khoan',
};

function Field(props: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  autoComplete?: string;
  hint?: string;
}) {
  return (
    <div className="field">
      <label htmlFor={props.id}>
        {props.label} <span className="req">*</span>
      </label>
      <input
        id={props.id}
        required
        type={props.type ?? 'text'}
        autoComplete={props.autoComplete}
        value={props.value}
        onChange={(e) => props.onChange(e.target.value)}
      />
      {props.hint && <div className="hint">{props.hint}</div>}
    </div>
  );
}

export function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const { signIn } = useSession();
  const navigate = useNavigate();
  const { busy, error, run } = useAction();
  const submit = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      const session = await api.post<Session>('/auth/login', { email, password });
      signIn(session);
      navigate(HOME[session.role]);
    });
  };
  return (
    <>
      <h2 className="page">Đăng nhập</h2>
      <ErrorBox message={error} />
      <form className="card" onSubmit={submit}>
        <Field
          id="email"
          label="Email"
          type="email"
          autoComplete="username"
          value={email}
          onChange={setEmail}
        />
        <Field
          id="password"
          label="Mật khẩu"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={setPassword}
        />
        <button className="btn" disabled={busy}>
          Đăng nhập
        </button>
        <p className="muted">
          <Link to="/quen-mat-khau">Quên mật khẩu?</Link> · <Link to="/dang-ky">Đăng ký</Link>
        </p>
      </form>
    </>
  );
}

export function Register() {
  const [form, setForm] = useState({
    fullName: '',
    nationalId: '',
    email: '',
    phone: '',
    password: '',
  });
  const [done, setDone] = useState(false);
  const { busy, error, run } = useAction();
  const set = (key: keyof typeof form) => (value: string) => setForm({ ...form, [key]: value });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await api.post('/auth/register', form);
      setDone(true);
    });
  };
  if (done) {
    return (
      <div className="alert done" role="status">
        Đã tạo tài khoản. Vui lòng bấm liên kết trong email trong vòng 24 giờ để kích hoạt.
      </div>
    );
  }
  return (
    <>
      <h2 className="page">Đăng ký tài khoản</h2>
      <ErrorBox message={error} />
      <form className="card" onSubmit={submit}>
        <Field id="fullName" label="Họ và tên" value={form.fullName} onChange={set('fullName')} />
        <Field
          id="nationalId"
          label="Số định danh cá nhân"
          hint="Đúng 12 chữ số. Chỉ dùng dữ liệu giả lập."
          value={form.nationalId}
          onChange={set('nationalId')}
        />
        <Field id="email2" label="Email" type="email" value={form.email} onChange={set('email')} />
        <Field id="phone" label="Số điện thoại" value={form.phone} onChange={set('phone')} />
        <Field
          id="password2"
          label="Mật khẩu"
          type="password"
          autoComplete="new-password"
          hint="Tối thiểu 8 ký tự, gồm chữ và số."
          value={form.password}
          onChange={set('password')}
        />
        <button className="btn" disabled={busy}>
          Đăng ký
        </button>
      </form>
    </>
  );
}

export function VerifyEmail() {
  const [params] = useSearchParams();
  const [message, setMessage] = useState('Đang xác thực…');
  const token = params.get('token') ?? '';
  useEffect(() => {
    api
      .post('/auth/verify-email', { token })
      .then(() => setMessage('Tài khoản đã được kích hoạt. Bạn có thể đăng nhập.'))
      .catch((e: unknown) => setMessage(errorText(e)));
  }, [token]);
  return (
    <div className="card" role="status">
      {message} <Link to="/dang-nhap">Đăng nhập</Link>
    </div>
  );
}

export function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [done, setDone] = useState(false);
  const { busy, error, run } = useAction();
  const submit = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await api.post('/auth/password-reset', { email });
      setDone(true);
    });
  };
  return (
    <>
      <h2 className="page">Quên mật khẩu</h2>
      <ErrorBox message={error} />
      {done ? (
        <div className="alert done" role="status">
          Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi và có hiệu lực 30 phút.
        </div>
      ) : (
        <form className="card" onSubmit={submit}>
          <Field id="email3" label="Email" type="email" value={email} onChange={setEmail} />
          <button className="btn" disabled={busy}>
            Gửi liên kết
          </button>
        </form>
      )}
    </>
  );
}

export function ResetPassword() {
  const [params] = useSearchParams();
  const [password, setPassword] = useState('');
  const [done, setDone] = useState(false);
  const { busy, error, run } = useAction();
  const submit = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await api.post('/auth/password-reset/confirm', {
        token: params.get('token') ?? '',
        newPassword: password,
      });
      setDone(true);
    });
  };
  if (done) {
    return (
      <div className="alert done" role="status">
        Đã đổi mật khẩu. <Link to="/dang-nhap">Đăng nhập</Link>
      </div>
    );
  }
  return (
    <>
      <h2 className="page">Đặt lại mật khẩu</h2>
      <ErrorBox message={error} />
      <form className="card" onSubmit={submit}>
        <Field
          id="newPassword"
          label="Mật khẩu mới"
          type="password"
          autoComplete="new-password"
          hint="Tối thiểu 8 ký tự, gồm chữ và số."
          value={password}
          onChange={setPassword}
        />
        <button className="btn" disabled={busy}>
          Đổi mật khẩu
        </button>
      </form>
    </>
  );
}
