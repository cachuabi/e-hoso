import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { api, type Detail, type Page, type Procedure } from '../api';
import { formatDateTime, STATUS_TEXT, type DossierStatus } from '../labels';
import { ErrorBox, StatusTag } from '../ui';
import { useAction, useLoad } from '../hooks';
import { ComponentFiles, FormFields } from './DossierParts';

export function MyDossiers() {
  const [status, setStatus] = useState('');
  const { data, error } = useLoad(
    () => api.get<Page>(`/dossiers?size=100${status ? `&status=${status}` : ''}`),
    status,
  );
  return (
    <>
      <h2 className="page">Hồ sơ của tôi</h2>
      <ErrorBox message={error} />
      <div className="field">
        <label htmlFor="filter">Lọc theo trạng thái</label>
        <select id="filter" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">Tất cả</option>
          {Object.entries(STATUS_TEXT).map(([k, v]) => (
            <option key={k} value={k}>
              {v}
            </option>
          ))}
        </select>
      </div>
      <table className="rs">
        <thead>
          <tr>
            <th>Mã</th>
            <th>Thủ tục</th>
            <th>Trạng thái</th>
            <th>Hẹn trả</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {data?.items.map((d) => (
            <tr key={d.id}>
              <td data-th="Mã">{d.dossierCode ?? d.referenceCode ?? 'Chưa nộp'}</td>
              <td data-th="Thủ tục">{d.procedureName}</td>
              <td data-th="Trạng thái">
                <StatusTag status={d.status} />
              </td>
              <td data-th="Hẹn trả">{formatDateTime(d.dueAt)}</td>
              <td data-th="Thao tác">
                <Link to={`/ho-so/${d.id}`}>Xem</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {data?.items.length === 0 && (
        <p className="muted">
          Chưa có hồ sơ. <Link to="/thu-tuc">Chọn thủ tục để nộp</Link>.
        </p>
      )}
    </>
  );
}

export function NewDossier() {
  const { code = '' } = useParams();
  const navigate = useNavigate();
  const { data: procedure, error: loadError } = useLoad(
    () => api.get<Procedure>(`/procedures/${code}`),
    code,
  );
  const [values, setValues] = useState<Record<string, unknown>>({});
  const { busy, error, run } = useAction();
  const create = () =>
    run(async () => {
      const created = await api.post<Detail>('/dossiers', {
        procedureCode: code,
        formData: values,
      });
      navigate(`/ho-so/${created.summary.id}`);
    });
  return (
    <>
      <h2 className="page">Nộp hồ sơ: {procedure?.name}</h2>
      <ErrorBox message={loadError ?? error} />
      {procedure && (
        <form
          className="card"
          onSubmit={(e) => {
            e.preventDefault();
            void create();
          }}
        >
          <FormFields fields={procedure.formSchema.fields} values={values} onChange={setValues} />
          <p className="muted">Bước tiếp theo: đính kèm tệp và nộp hồ sơ.</p>
          <button className="btn" disabled={busy}>
            Lưu nháp và tiếp tục
          </button>
        </form>
      )}
    </>
  );
}

export function CitizenDetail() {
  const { id = '' } = useParams();
  const {
    data: dossier,
    error: loadError,
    reload,
  } = useLoad(() => api.get<Detail>(`/dossiers/${id}`), id);
  return dossier ? (
    <CitizenDetailBody key={dossier.version} dossier={dossier} reload={reload} />
  ) : (
    <ErrorBox message={loadError} />
  );
}

function CitizenDetailBody({ dossier, reload }: { dossier: Detail; reload: () => void }) {
  const { summary } = dossier;
  const { data: procedure } = useLoad(
    () => api.get<Procedure>(`/procedures/${summary.procedureCode}`),
    summary.procedureCode,
  );
  const { data: history } = useLoad(
    () =>
      api.get<{ status: DossierStatus; occurredAt: string }[]>(`/dossiers/${summary.id}/history`),
    summary.status,
  );
  const [values, setValues] = useState(dossier.formData);
  const { busy, error, run } = useAction();
  const editable = summary.status === 'DRAFT' || summary.status === 'NEED_SUPPLEMENT';
  const url = `/dossiers/${summary.id}`;
  const save = () => api.put<Detail>(url, { formData: values, version: dossier.version });
  const result = dossier.attachments.find((a) => a.kind === 'RESULT');
  return (
    <>
      <h2 className="page">Hồ sơ {summary.dossierCode ?? summary.referenceCode ?? 'nháp'}</h2>
      <ErrorBox message={error} />
      {summary.referenceCode && summary.status === 'SUBMITTED' && (
        <div className="alert done" role="status">
          Hồ sơ đã được nộp. Mã tham chiếu: <strong>{summary.referenceCode}</strong>.
        </div>
      )}
      {dossier.latestSupplement && (
        <div className="alert">
          <strong>Cần bổ sung:</strong> {dossier.latestSupplement.instruction}
          <ul className="plain">
            {dossier.latestSupplement.items.map((i) => (
              <li key={i.componentId}>
                {i.componentName}
                {i.note ? `: ${i.note}` : ''}
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="card">
        <dl>
          <dt>Thủ tục</dt>
          <dd>{summary.procedureName}</dd>
          <dt>Trạng thái</dt>
          <dd>
            <StatusTag status={summary.status} />
          </dd>
          <dt>Ngày hẹn trả</dt>
          <dd>{formatDateTime(summary.dueAt)}</dd>
          {dossier.resultExpiresOn && (
            <>
              <dt>Hết giá trị</dt>
              <dd>{dossier.resultExpiresOn}</dd>
            </>
          )}
        </dl>
        {result && dossier.returnMethod === 'ELECTRONIC' && (
          <div className="acts">
            <button className="btn" onClick={() => void api.openFile(result.id)}>
              Tải kết quả
            </button>
          </div>
        )}
      </div>
      {procedure && (
        <div className="card">
          <FormFields
            fields={procedure.formSchema.fields}
            values={values}
            onChange={setValues}
            readOnly={!editable}
          />
          {editable && (
            <div className="acts">
              <button
                className="btn alt"
                disabled={busy}
                onClick={() =>
                  void run(async () => {
                    await save();
                    reload();
                  })
                }
              >
                Lưu nháp
              </button>
            </div>
          )}
          <h3 className="mt">Tệp đính kèm</h3>
          <ComponentFiles
            dossier={dossier}
            components={procedure.components}
            editable={editable}
            onChanged={reload}
          />
        </div>
      )}
      <div className="acts">
        {editable && (
          <button
            className="btn"
            disabled={busy}
            onClick={() =>
              void run(async () => {
                await save();
                await api.post(summary.status === 'DRAFT' ? `${url}/submit` : `${url}/resubmit`);
                reload();
              })
            }
          >
            {summary.status === 'DRAFT' ? 'Nộp hồ sơ' : 'Nộp lại'}
          </button>
        )}
        {(summary.status === 'SUBMITTED' || summary.status === 'NEED_SUPPLEMENT') && (
          <button
            className="btn bad"
            disabled={busy}
            onClick={() =>
              void run(async () => {
                if (window.confirm('Bạn chắc chắn muốn rút hồ sơ?')) {
                  await api.post(`${url}/withdraw`);
                  reload();
                }
              })
            }
          >
            Rút hồ sơ
          </button>
        )}
      </div>
      {history && history.length > 0 && (
        <div className="card mt">
          <h3>Lịch sử trạng thái</h3>
          <ul className="plain">
            {history.map((h) => (
              <li key={h.occurredAt + h.status}>
                {STATUS_TEXT[h.status]}: {formatDateTime(h.occurredAt)}
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}
