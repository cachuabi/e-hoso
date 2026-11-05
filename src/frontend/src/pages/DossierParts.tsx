import { useState } from 'react';
import { api, errorText, type Component, type Detail, type FormField } from '../api';
import { fileProblem } from '../labels';

export function FormFields(props: {
  fields: FormField[];
  values: Record<string, unknown>;
  onChange: (values: Record<string, unknown>) => void;
  readOnly?: boolean;
}) {
  const { fields, values, onChange, readOnly } = props;
  return (
    <>
      {fields.map((f) => {
        const value = String(values[f.key] ?? '');
        const update = (next: string) => onChange({ ...values, [f.key]: next });
        const id = `f-${f.key}`;
        return (
          <div className="field" key={f.key}>
            <label htmlFor={id}>
              {f.label} {f.required && <span className="req">*</span>}
            </label>
            {f.type === 'select' ? (
              <select
                id={id}
                value={value}
                disabled={readOnly}
                onChange={(e) => update(e.target.value)}
              >
                <option value="">Chọn</option>
                {f.options?.map((o) => (
                  <option key={o}>{o}</option>
                ))}
              </select>
            ) : (
              <input
                id={id}
                type={f.type === 'number' ? 'number' : f.type === 'date' ? 'date' : 'text'}
                value={value}
                readOnly={readOnly}
                onChange={(e) => update(e.target.value)}
              />
            )}
          </div>
        );
      })}
    </>
  );
}

export function ComponentFiles(props: {
  dossier: Detail;
  components: Component[];
  editable: boolean;
  onChanged: () => void;
}) {
  const { dossier, components, editable, onChanged } = props;
  const [problem, setProblem] = useState<string | null>(null);
  const pick = async (componentId: string, file: File | undefined) => {
    if (!file) return;
    const invalid = fileProblem(file.name, file.size);
    setProblem(invalid);
    if (invalid) return;
    try {
      await api.upload(dossier.summary.id, file, 'DOSSIER', componentId);
      onChanged();
    } catch (e) {
      setProblem(errorText(e));
    }
  };
  const remove = async (id: string) => {
    try {
      await api.remove(`/attachments/${id}`);
      onChanged();
    } catch (e) {
      setProblem(errorText(e));
    }
  };
  return (
    <>
      {components.map((c) => (
        <div className="field" key={c.id}>
          <label htmlFor={`c-${c.id}`}>
            {c.name} {c.requirement === 'REQUIRED' && <span className="req">*</span>}
          </label>
          {dossier.attachments
            .filter((a) => a.componentId === c.id)
            .map((a) => (
              <div key={a.id}>
                <button type="button" className="btn alt" onClick={() => void api.openFile(a.id)}>
                  {a.fileName}
                </button>{' '}
                {editable && (
                  <button type="button" className="btn alt" onClick={() => void remove(a.id)}>
                    Xoá
                  </button>
                )}
              </div>
            ))}
          {editable && (
            <input
              id={`c-${c.id}`}
              type="file"
              accept=".pdf,.jpg,.jpeg,.png"
              onChange={(e) => void pick(c.id, e.target.files?.[0])}
            />
          )}
        </div>
      ))}
      <div className="hint">PDF, JPG, PNG; mỗi tệp tối đa 10 MB, cả hồ sơ tối đa 30 MB.</div>
      {problem && (
        <div className="err" role="alert">
          {problem}
        </div>
      )}
    </>
  );
}
