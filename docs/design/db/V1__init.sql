-- e-HoSo: lược đồ khởi tạo (D13). Tham chiếu: SRS v1.1 mục 8; SDD v1.0 mục 6.
-- Mọi thời điểm dùng timestamptz (NFR-10). Chỉ dùng dữ liệu giả lập (NFR-04).

CREATE TABLE app_user (
    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name              varchar(150) NOT NULL,
    email                  varchar(254) NOT NULL UNIQUE,
    phone                  varchar(15),
    national_id            char(12) UNIQUE,
    password_hash          varchar(72) NOT NULL,
    role                   varchar(10) NOT NULL
        CHECK (role IN ('CITIZEN', 'CBMC', 'CCTP', 'LEADER', 'ADMIN')),
    status                 varchar(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED')),
    failed_logins          int NOT NULL DEFAULT 0 CHECK (failed_logins >= 0),
    locked_until           timestamptz,
    failed_approvals       int NOT NULL DEFAULT 0 CHECK (failed_approvals >= 0),
    approval_locked_until  timestamptz,
    created_at             timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_national_id CHECK (
        (role = 'CITIZEN' AND national_id IS NOT NULL AND national_id ~ '^[0-9]{12}$')
        OR (role <> 'CITIZEN' AND national_id IS NULL)
    )
);

CREATE TABLE auth_token (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES app_user (id),
    purpose     varchar(10) NOT NULL CHECK (purpose IN ('VERIFY', 'RESET', 'REFRESH')),
    token_hash  char(64) NOT NULL UNIQUE,
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_auth_token_user ON auth_token (user_id, purpose);

CREATE TABLE procedure (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code                    varchar(30) NOT NULL UNIQUE,
    name                    varchar(200) NOT NULL,
    deadline_unit           varchar(15) NOT NULL
        CHECK (deadline_unit IN ('SAME_DAY', 'WORKING_DAYS')),
    deadline_days           int,
    applies_15h_cutoff      boolean NOT NULL DEFAULT false,
    allows_verification     boolean NOT NULL DEFAULT false,
    requires_original_check boolean NOT NULL DEFAULT false,
    result_validity_months  int CHECK (result_validity_months > 0),
    documents_to_present    text,
    form_schema             jsonb NOT NULL,
    CONSTRAINT ck_procedure_deadline CHECK (
        (deadline_unit = 'SAME_DAY' AND deadline_days IS NULL)
        OR (deadline_unit = 'WORKING_DAYS' AND deadline_days IS NOT NULL AND deadline_days > 0)
    )
);

CREATE TABLE dossier_component (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    procedure_id  uuid NOT NULL REFERENCES procedure (id),
    name          varchar(200) NOT NULL,
    requirement   varchar(12) NOT NULL CHECK (requirement IN ('REQUIRED', 'CONDITIONAL')),
    allowed_types varchar(100) NOT NULL DEFAULT 'application/pdf,image/jpeg,image/png',
    sort_order    int NOT NULL DEFAULT 0,
    UNIQUE (procedure_id, name)
);

CREATE TABLE dossier (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_code      char(11) UNIQUE CHECK (reference_code ~ '^TC-[A-Z0-9]{8}$'),
    dossier_code        char(14) UNIQUE CHECK (dossier_code ~ '^HS-[0-9]{6}-[0-9]{4}$'),
    procedure_id        uuid NOT NULL REFERENCES procedure (id),
    applicant_id        uuid NOT NULL REFERENCES app_user (id),
    assigned_officer_id uuid REFERENCES app_user (id),
    form_data           jsonb NOT NULL DEFAULT '{}',
    status              varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'NEED_SUPPLEMENT', 'REJECTED', 'WITHDRAWN', 'ACCEPTED',
        'IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'RETURNED')),
    created_at          timestamptz NOT NULL DEFAULT now(),
    submitted_at        timestamptz,
    accepted_at         timestamptz,
    due_at              timestamptz,
    result_at           timestamptz,
    returned_at         timestamptz,
    original_checked    boolean NOT NULL DEFAULT false,
    verification_flag   boolean NOT NULL DEFAULT false,
    verification_place  varchar(300),
    supplement_count    int NOT NULL DEFAULT 0 CHECK (supplement_count >= 0),
    review_opinion      text,
    result_kind         varchar(8) CHECK (result_kind IN ('ISSUED', 'REFUSED')),
    result_expires_on   date,
    return_method       varchar(10) CHECK (return_method IN ('COUNTER', 'ELECTRONIC')),
    version             bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_dossier_submitted CHECK (
        status = 'DRAFT' OR (reference_code IS NOT NULL AND submitted_at IS NOT NULL)),
    CONSTRAINT ck_dossier_accepted CHECK (
        status NOT IN ('ACCEPTED', 'IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'RETURNED')
        OR (dossier_code IS NOT NULL AND accepted_at IS NOT NULL AND due_at IS NOT NULL)),
    CONSTRAINT ck_dossier_assigned CHECK (
        status NOT IN ('IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'RETURNED')
        OR assigned_officer_id IS NOT NULL),
    CONSTRAINT ck_dossier_result CHECK (
        status NOT IN ('APPROVED', 'RETURNED') OR (result_at IS NOT NULL AND result_kind IS NOT NULL)),
    CONSTRAINT ck_dossier_returned CHECK (
        status <> 'RETURNED' OR (returned_at IS NOT NULL AND return_method IS NOT NULL)),
    CONSTRAINT ck_dossier_verification CHECK (NOT verification_flag OR verification_place IS NOT NULL)
);
CREATE INDEX ix_dossier_queue ON dossier (status, submitted_at);
CREATE INDEX ix_dossier_applicant ON dossier (applicant_id, status);
CREATE INDEX ix_dossier_officer ON dossier (assigned_officer_id, status);
CREATE INDEX ix_dossier_due ON dossier (due_at) WHERE result_at IS NULL;
CREATE INDEX ix_dossier_result ON dossier (result_at);

CREATE TABLE attachment (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id    uuid NOT NULL REFERENCES dossier (id),
    component_id  uuid REFERENCES dossier_component (id),
    kind          varchar(8) NOT NULL DEFAULT 'DOSSIER' CHECK (kind IN ('DOSSIER', 'DRAFT', 'RESULT')),
    file_name     varchar(255) NOT NULL,
    mime_type     varchar(30) NOT NULL
        CHECK (mime_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    size_bytes    bigint NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    storage_key   varchar(120) NOT NULL UNIQUE,
    uploaded_by   uuid NOT NULL REFERENCES app_user (id),
    created_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_attachment_component CHECK (kind <> 'DOSSIER' OR component_id IS NOT NULL),
    CONSTRAINT ck_attachment_pdf_result CHECK (kind = 'DOSSIER' OR mime_type = 'application/pdf')
);
CREATE INDEX ix_attachment_dossier ON attachment (dossier_id, kind);

CREATE TABLE supplement_request (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id    uuid NOT NULL REFERENCES dossier (id),
    instruction   text NOT NULL,
    requested_by  uuid NOT NULL REFERENCES app_user (id),
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_supplement_dossier ON supplement_request (dossier_id, created_at DESC);

CREATE TABLE supplement_item (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id    uuid NOT NULL REFERENCES supplement_request (id),
    component_id  uuid NOT NULL REFERENCES dossier_component (id),
    note          varchar(500),
    UNIQUE (request_id, component_id)
);

CREATE TABLE processing_log (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dossier_id    uuid NOT NULL REFERENCES dossier (id),
    actor_id      uuid NOT NULL REFERENCES app_user (id),
    actor_role    varchar(10) NOT NULL,
    action        varchar(20) NOT NULL,
    from_status   varchar(20),
    to_status     varchar(20) NOT NULL,
    note          text,
    occurred_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_log_dossier ON processing_log (dossier_id, occurred_at);

CREATE TABLE admin_audit_log (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id        uuid NOT NULL REFERENCES app_user (id),
    action          varchar(30) NOT NULL,
    target_user_id  uuid REFERENCES app_user (id),
    note            text,
    occurred_at     timestamptz NOT NULL DEFAULT now()
);

-- FR-23: nhật ký chỉ thêm, chặn ở cả tầng CSDL.
CREATE FUNCTION forbid_log_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Nhật ký chỉ được thêm, không sửa, không xoá (FR-23)';
END;
$$;
CREATE TRIGGER tg_processing_log_append_only
    BEFORE UPDATE OR DELETE ON processing_log
    FOR EACH ROW EXECUTE FUNCTION forbid_log_change();
CREATE TRIGGER tg_processing_log_no_truncate
    BEFORE TRUNCATE ON processing_log
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_log_change();
CREATE TRIGGER tg_admin_audit_append_only
    BEFORE UPDATE OR DELETE ON admin_audit_log
    FOR EACH ROW EXECUTE FUNCTION forbid_log_change();
CREATE TRIGGER tg_admin_audit_no_truncate
    BEFORE TRUNCATE ON admin_audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_log_change();

CREATE TABLE holiday (
    day          date PRIMARY KEY,
    type         varchar(15) NOT NULL CHECK (type IN ('HOLIDAY', 'MAKEUP_WORKDAY')),
    description  varchar(200)
);

CREATE TABLE notification (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    dossier_id       uuid REFERENCES dossier (id),
    recipient_email  varchar(254) NOT NULL,
    type             varchar(30) NOT NULL,
    payload          jsonb,
    status           varchar(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts         int NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at  timestamptz NOT NULL DEFAULT now(),
    last_error       varchar(500),
    created_at       timestamptz NOT NULL DEFAULT now(),
    sent_at          timestamptz
);
CREATE INDEX ix_notification_pending ON notification (next_attempt_at) WHERE status = 'PENDING';

CREATE TABLE dossier_code_counter (
    code_date  date PRIMARY KEY,
    last_seq   int NOT NULL CHECK (last_seq BETWEEN 1 AND 9999)
);
