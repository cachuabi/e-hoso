import type { DossierStatus } from './labels';

export type Role = 'CITIZEN' | 'CBMC' | 'CCTP' | 'LEADER' | 'ADMIN';

export interface Session {
  accessToken: string;
  expiresIn: number;
  role: Role;
  fullName: string;
}

export interface Problem {
  status: number;
  code?: string;
  detail?: string;
  errors?: { field: string; message: string }[];
  currentStatus?: DossierStatus;
}

export class ApiError extends Error {
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(problem.detail ?? 'Có lỗi xảy ra.');
    this.problem = problem;
  }
}

export interface Component {
  id: string;
  name: string;
  requirement: 'REQUIRED' | 'CONDITIONAL';
}

export interface FormField {
  key: string;
  label: string;
  type: 'text' | 'date' | 'number' | 'select';
  required: boolean;
  options?: string[];
}

export interface Procedure {
  code: string;
  name: string;
  deadlineUnit: 'SAME_DAY' | 'WORKING_DAYS';
  deadlineDays: number | null;
  applies15hCutoff: boolean;
  documentsToPresent: string | null;
  formSchema: { fields: FormField[] };
  components: Component[];
}

export interface Summary {
  id: string;
  referenceCode: string | null;
  dossierCode: string | null;
  procedureCode: string;
  procedureName: string;
  status: DossierStatus;
  submittedAt: string | null;
  dueAt: string | null;
  deadlineLabel: string | null;
  assignedOfficerName: string | null;
  applicantName: string | null;
}

export interface FileView {
  id: string;
  componentId: string | null;
  fileName: string;
  sizeBytes: number;
  kind: 'DOSSIER' | 'DRAFT' | 'RESULT';
}

export interface Detail {
  summary: Summary;
  formData: Record<string, unknown>;
  attachments: FileView[];
  supplementCount: number;
  latestSupplement: {
    instruction: string;
    items: { componentId: string; componentName: string; note: string | null }[];
  } | null;
  requiresOriginalCheck: boolean;
  allowsVerification: boolean;
  verificationFlag: boolean;
  verificationPlace: string | null;
  reviewOpinion: string | null;
  sendBackReason: string | null;
  resultKind: string | null;
  resultAt: string | null;
  onTime: boolean | null;
  resultExpiresOn: string | null;
  returnMethod: string | null;
  version: number;
}

export interface Page {
  items: Summary[];
  total: number;
}

let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

async function send(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  return fetch(`/api/v1${path}`, { ...init, headers, credentials: 'same-origin' });
}

async function refreshSession(): Promise<boolean> {
  const response = await fetch('/api/v1/auth/refresh', {
    method: 'POST',
    credentials: 'same-origin',
  });
  if (!response.ok) return false;
  const session = (await response.json()) as Session;
  accessToken = session.accessToken;
  return true;
}

async function parse<T>(response: Response): Promise<T> {
  if (response.ok) {
    if (response.status === 204 || response.status === 202) return undefined as T;
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }
  let problem: Problem = { status: response.status };
  try {
    problem = { ...problem, ...((await response.json()) as Problem) };
  } catch {
    problem.detail = 'Có lỗi xảy ra, vui lòng thử lại.';
  }
  if (response.status === 403 && !problem.detail) problem.detail = 'Bạn không có quyền thực hiện.';
  throw new ApiError(problem);
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response = await send(path, init);
  if (response.status === 401 && accessToken && (await refreshSession())) {
    response = await send(path, init);
  }
  return parse<T>(response);
}

const jsonInit = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown = {}) => request<T>(path, jsonInit('POST', body)),
  put: <T>(path: string, body: unknown) => request<T>(path, jsonInit('PUT', body)),
  patch: <T>(path: string, body: unknown) => request<T>(path, jsonInit('PATCH', body)),
  remove: (path: string) => request<void>(path, { method: 'DELETE' }),
  upload: (dossierId: string, file: File, kind: string, componentId?: string) => {
    const form = new FormData();
    form.append('file', file);
    form.append('kind', kind);
    if (componentId) form.append('componentId', componentId);
    return request<FileView>(`/dossiers/${dossierId}/attachments`, { method: 'POST', body: form });
  },
  openFile: async (id: string) => {
    const { url } = await request<{ url: string }>(`/attachments/${id}/download-url`);
    window.open(url, '_blank', 'noopener');
  },
  csv: async (path: string): Promise<Blob> => {
    const response = await send(path, {});
    if (!response.ok) await parse(response);
    return response.blob();
  },
  restore: async (): Promise<Session | null> => {
    const response = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      credentials: 'same-origin',
    });
    if (!response.ok) return null;
    return (await response.json()) as Session;
  },
};

export function errorText(error: unknown): string {
  if (error instanceof ApiError) {
    const fields = error.problem.errors?.map((e) => e.message).join(' ');
    return fields || error.message;
  }
  return 'Không kết nối được máy chủ.';
}
