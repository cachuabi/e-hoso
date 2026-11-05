export type DossierStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'NEED_SUPPLEMENT'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'RETURNED';

export const STATUS_TEXT: Record<DossierStatus, string> = {
  DRAFT: 'Nháp',
  SUBMITTED: 'Đã nộp',
  NEED_SUPPLEMENT: 'Chờ bổ sung',
  REJECTED: 'Từ chối tiếp nhận',
  WITHDRAWN: 'Đã rút',
  ACCEPTED: 'Đã tiếp nhận',
  IN_PROGRESS: 'Đang xử lý',
  PENDING_APPROVAL: 'Chờ phê duyệt',
  APPROVED: 'Đã có kết quả',
  RETURNED: 'Đã trả kết quả',
};

export function statusTone(status: DossierStatus): string {
  if (status === 'REJECTED') return 'bad';
  if (status === 'NEED_SUPPLEMENT' || status === 'WITHDRAWN') return 'warn';
  if (status === 'APPROVED' || status === 'RETURNED') return 'ok';
  return 'info';
}

export const DEADLINE_TEXT: Record<string, { text: string; tone: string }> = {
  ON_TIME: { text: 'Còn hạn', tone: 'ok' },
  DUE_SOON: { text: 'Sắp đến hạn', tone: 'warn' },
  OVERDUE: { text: 'Quá hạn', tone: 'bad' },
};

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '–';
  const date = new Date(iso);
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
}

export const ACTION_TEXT: Record<string, string> = {
  SUBMIT: 'Nộp hồ sơ',
  RESUBMIT: 'Nộp lại sau bổ sung',
  WITHDRAW: 'Rút hồ sơ',
  REQUEST_SUPPLEMENT: 'Yêu cầu bổ sung',
  REJECT: 'Từ chối tiếp nhận',
  ACCEPT: 'Tiếp nhận',
  ASSIGN: 'Phân công',
  MARK_VERIFICATION: 'Gia hạn do xác minh',
  SUBMIT_APPROVAL: 'Trình phê duyệt',
  SEND_BACK: 'Trả lại công chức',
  APPROVE: 'Phê duyệt',
  RETURN_RESULT: 'Trả kết quả',
};

export const ROLE_TEXT: Record<string, string> = {
  CITIZEN: 'Người dân',
  CBMC: 'Cán bộ một cửa',
  CCTP: 'Công chức tư pháp – hộ tịch',
  LEADER: 'Lãnh đạo',
  ADMIN: 'Quản trị viên',
};

export const MAX_FILE_BYTES = 10 * 1024 * 1024;
const ALLOWED_EXTENSIONS = ['.pdf', '.jpg', '.jpeg', '.png'];

export function fileProblem(name: string, size: number): string | null {
  const lower = name.toLowerCase();
  if (!ALLOWED_EXTENSIONS.some((ext) => lower.endsWith(ext))) {
    return 'Chỉ nhận tệp PDF, JPG hoặc PNG.';
  }
  if (size > MAX_FILE_BYTES) return 'Tệp vượt quá 10 MB.';
  return null;
}
