import { DEADLINE_TEXT, STATUS_TEXT, statusTone, type DossierStatus } from './labels';

export function StatusTag({ status }: { status: DossierStatus }) {
  return <span className={`tag ${statusTone(status)}`}>{STATUS_TEXT[status]}</span>;
}

export function DeadlineTag({ label }: { label: string | null }) {
  if (!label || !DEADLINE_TEXT[label]) return <>–</>;
  const { text, tone } = DEADLINE_TEXT[label];
  return <span className={`tag ${tone}`}>{text}</span>;
}

export function ErrorBox({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div className="alert error" role="alert">
      {message}
    </div>
  );
}
