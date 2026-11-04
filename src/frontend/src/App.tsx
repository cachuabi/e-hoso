import { useEffect, useMemo, useState } from 'react';
import { BrowserRouter, Navigate, NavLink, Route, Routes } from 'react-router';
import { api, setAccessToken, type Role, type Session } from './api';
import { AdminHolidays, AdminProcedures, AdminUsers } from './pages/Admin';
import { CitizenDetail, MyDossiers, NewDossier } from './pages/Citizen';
import { Dashboard } from './pages/Dashboard';
import { ForgotPassword, Login, Register, ResetPassword, VerifyEmail } from './pages/Auth';
import { Lookup, Procedures } from './pages/Public';
import { Receipt, StaffDetail, StaffList } from './pages/Staff';
import { SessionContext, useSession } from './session';

const MENU: Record<Role | 'GUEST', [string, string][]> = {
  GUEST: [
    ['/thu-tuc', 'Thủ tục'],
    ['/tra-cuu', 'Tra cứu'],
    ['/dang-nhap', 'Đăng nhập'],
    ['/dang-ky', 'Đăng ký'],
  ],
  CITIZEN: [
    ['/thu-tuc', 'Thủ tục'],
    ['/tra-cuu', 'Tra cứu'],
    ['/ho-so', 'Hồ sơ của tôi'],
  ],
  CBMC: [['/can-bo/ho-so', 'Hồ sơ']],
  CCTP: [['/can-bo/ho-so', 'Hồ sơ được giao']],
  LEADER: [
    ['/can-bo/ho-so', 'Hồ sơ'],
    ['/can-bo/dashboard', 'Dashboard'],
  ],
  ADMIN: [
    ['/quan-tri/tai-khoan', 'Tài khoản'],
    ['/quan-tri/thu-tuc', 'Thủ tục'],
    ['/quan-tri/ngay-nghi', 'Ngày nghỉ'],
  ],
};

const HOME: Record<Role, string> = {
  CITIZEN: '/ho-so',
  CBMC: '/can-bo/ho-so',
  CCTP: '/can-bo/ho-so',
  LEADER: '/can-bo/ho-so',
  ADMIN: '/quan-tri/tai-khoan',
};

function Guard({ roles, children }: { roles: Role[]; children: React.ReactNode }) {
  const { session } = useSession();
  if (!session) return <Navigate to="/dang-nhap" replace />;
  if (!roles.includes(session.role)) return <Navigate to={HOME[session.role]} replace />;
  return <>{children}</>;
}

function Shell() {
  const { session, signOut } = useSession();
  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } finally {
      signOut();
    }
  };
  return (
    <>
      <header className="top">
        <h1>e-HoSo</h1>
        <small>Hồ sơ hành chính trực tuyến cấp xã{session ? ` · ${session.fullName}` : ''}</small>
      </header>
      <nav className="menu" aria-label="Điều hướng chính">
        {MENU[session?.role ?? 'GUEST'].map(([to, text]) => (
          <NavLink key={to} to={to}>
            {text}
          </NavLink>
        ))}
        {session && (
          <button className="btn alt" onClick={logout}>
            Đăng xuất
          </button>
        )}
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/thu-tuc" replace />} />
          <Route path="/thu-tuc" element={<Procedures />} />
          <Route path="/tra-cuu" element={<Lookup />} />
          <Route path="/dang-nhap" element={<Login />} />
          <Route path="/dang-ky" element={<Register />} />
          <Route path="/xac-thuc" element={<VerifyEmail />} />
          <Route path="/quen-mat-khau" element={<ForgotPassword />} />
          <Route path="/dat-lai-mat-khau" element={<ResetPassword />} />
          <Route
            path="/ho-so"
            element={
              <Guard roles={['CITIZEN']}>
                <MyDossiers />
              </Guard>
            }
          />
          <Route
            path="/ho-so/moi/:code"
            element={
              <Guard roles={['CITIZEN']}>
                <NewDossier />
              </Guard>
            }
          />
          <Route
            path="/ho-so/:id"
            element={
              <Guard roles={['CITIZEN']}>
                <CitizenDetail />
              </Guard>
            }
          />
          <Route
            path="/can-bo/ho-so"
            element={
              <Guard roles={['CBMC', 'CCTP', 'LEADER']}>
                <StaffList />
              </Guard>
            }
          />
          <Route
            path="/can-bo/ho-so/:id"
            element={
              <Guard roles={['CBMC', 'CCTP', 'LEADER']}>
                <StaffDetail />
              </Guard>
            }
          />
          <Route
            path="/can-bo/ho-so/:id/phieu-hen"
            element={
              <Guard roles={['CBMC']}>
                <Receipt />
              </Guard>
            }
          />
          <Route
            path="/can-bo/dashboard"
            element={
              <Guard roles={['LEADER']}>
                <Dashboard />
              </Guard>
            }
          />
          <Route
            path="/quan-tri/tai-khoan"
            element={
              <Guard roles={['ADMIN']}>
                <AdminUsers />
              </Guard>
            }
          />
          <Route
            path="/quan-tri/thu-tuc"
            element={
              <Guard roles={['ADMIN']}>
                <AdminProcedures />
              </Guard>
            }
          />
          <Route
            path="/quan-tri/ngay-nghi"
            element={
              <Guard roles={['ADMIN']}>
                <AdminHolidays />
              </Guard>
            }
          />
          <Route path="*" element={<p>Không tìm thấy trang.</p>} />
        </Routes>
      </main>
    </>
  );
}

function App() {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let alive = true;
    api
      .restore()
      .catch(() => null)
      .then((restored) => {
        if (!alive) return;
        if (restored) {
          setAccessToken(restored.accessToken);
          setSession(restored);
        }
        setReady(true);
      });
    return () => {
      alive = false;
    };
  }, []);

  const value = useMemo(
    () => ({
      session,
      signIn: (next: Session) => {
        setAccessToken(next.accessToken);
        setSession(next);
      },
      signOut: () => {
        setAccessToken(null);
        setSession(null);
      },
    }),
    [session],
  );

  if (!ready) return <p role="status">Đang tải…</p>;
  return (
    <SessionContext.Provider value={value}>
      <BrowserRouter>
        <Shell />
      </BrowserRouter>
    </SessionContext.Provider>
  );
}

export default App;
