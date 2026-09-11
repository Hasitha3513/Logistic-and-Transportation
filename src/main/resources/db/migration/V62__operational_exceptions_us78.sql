CREATE TABLE operational_exception_case (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    case_reference VARCHAR(16) NOT NULL,
    source_event_id UUID NOT NULL,
    source_module VARCHAR(24) NOT NULL,
    source_type VARCHAR(80) NOT NULL,
    source_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    summary_code VARCHAR(80) NOT NULL,
    correlation_id VARCHAR(128),
    category VARCHAR(24) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    response_due_at TIMESTAMPTZ NOT NULL,
    resolution_due_at TIMESTAMPTZ NOT NULL,
    next_escalation_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    assignment_type VARCHAR(24),
    assigned_user_id UUID,
    assigned_role_code VARCHAR(80),
    escalation_level VARCHAR(8) NOT NULL DEFAULT 'L0',
    resolution_note VARCHAR(2000),
    resolution_result_reference VARCHAR(160),
    resolved_by UUID,
    closed_by UUID,
    resolution_validated BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_operational_exception_case_tenant_id UNIQUE (id, tenant_id),
    CONSTRAINT uq_operational_exception_case_reference UNIQUE (tenant_id, case_reference),
    CONSTRAINT uq_operational_exception_source_event UNIQUE (tenant_id, source_event_id),
    CONSTRAINT ck_operational_exception_reference CHECK (case_reference ~ '^OEX-[0-9A-HJKMNP-TV-Z]{12}$'),
    CONSTRAINT ck_operational_exception_source_module CHECK (source_module IN ('ROUTING', 'DELIVERY')),
    CONSTRAINT ck_operational_exception_category CHECK (category IN ('OPERATIONAL', 'SAFETY', 'COMPLIANCE', 'CUSTOMER', 'FINANCIAL', 'TECHNICAL', 'SECURITY')),
    CONSTRAINT ck_operational_exception_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_operational_exception_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_operational_exception_assignment_type CHECK (assignment_type IS NULL OR assignment_type IN ('ROLE_QUEUE', 'USER')),
    CONSTRAINT ck_operational_exception_assignment_target CHECK (
        (assignment_type IS NULL AND assigned_user_id IS NULL AND assigned_role_code IS NULL)
        OR (assignment_type = 'USER' AND assigned_user_id IS NOT NULL AND assigned_role_code IS NULL)
        OR (assignment_type = 'ROLE_QUEUE' AND assigned_user_id IS NULL AND assigned_role_code IS NOT NULL)
    ),
    CONSTRAINT ck_operational_exception_escalation CHECK (escalation_level IN ('L0', 'L1', 'L2', 'L3')),
    CONSTRAINT ck_operational_exception_sla_order CHECK (response_due_at <= resolution_due_at),
    CONSTRAINT ck_operational_exception_version CHECK (version >= 0)
);

CREATE INDEX idx_operational_exception_status_opened
    ON operational_exception_case (tenant_id, status, occurred_at DESC, id);
CREATE INDEX idx_operational_exception_severity_status
    ON operational_exception_case (tenant_id, severity, status, occurred_at DESC);
CREATE INDEX idx_operational_exception_assigned_user
    ON operational_exception_case (tenant_id, assigned_user_id, status) WHERE assigned_user_id IS NOT NULL;
CREATE INDEX idx_operational_exception_assigned_role
    ON operational_exception_case (tenant_id, assigned_role_code, status) WHERE assigned_role_code IS NOT NULL;
CREATE INDEX idx_operational_exception_response_due
    ON operational_exception_case (tenant_id, response_due_at, status) WHERE acknowledged_at IS NULL;
CREATE INDEX idx_operational_exception_resolution_due
    ON operational_exception_case (tenant_id, resolution_due_at, status) WHERE resolved_at IS NULL;
CREATE INDEX idx_operational_exception_escalation_due
    ON operational_exception_case (tenant_id, next_escalation_at, status) WHERE next_escalation_at IS NOT NULL;
CREATE INDEX idx_operational_exception_source
    ON operational_exception_case (tenant_id, source_module, source_id);

CREATE TABLE operational_exception_assignment_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    case_id UUID NOT NULL,
    from_type VARCHAR(24),
    from_user_id UUID,
    from_role_code VARCHAR(80),
    to_type VARCHAR(24),
    to_user_id UUID,
    to_role_code VARCHAR(80),
    actor_id UUID NOT NULL,
    actor_username VARCHAR(128) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_operational_assignment_case_tenant FOREIGN KEY (case_id, tenant_id)
        REFERENCES operational_exception_case (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT ck_operational_assignment_from_type CHECK (from_type IS NULL OR from_type IN ('ROLE_QUEUE', 'USER')),
    CONSTRAINT ck_operational_assignment_to_type CHECK (to_type IS NULL OR to_type IN ('ROLE_QUEUE', 'USER'))
);
CREATE INDEX idx_operational_assignment_history_case
    ON operational_exception_assignment_history (tenant_id, case_id, occurred_at DESC, id);

CREATE TABLE operational_exception_corrective_action (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    case_id UUID NOT NULL,
    action_type VARCHAR(24) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    owner_type VARCHAR(24) NOT NULL,
    owner_user_id UUID,
    owner_role_code VARCHAR(80),
    due_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL,
    completed_at TIMESTAMPTZ,
    evidence_reference VARCHAR(160),
    cancellation_reason VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_operational_action_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_operational_action_case_tenant FOREIGN KEY (case_id, tenant_id)
        REFERENCES operational_exception_case (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT ck_operational_action_type CHECK (action_type IN ('CORRECTIVE', 'PREVENTIVE')),
    CONSTRAINT ck_operational_action_owner_type CHECK (owner_type IN ('ROLE_QUEUE', 'USER')),
    CONSTRAINT ck_operational_action_owner CHECK (
        (owner_type = 'USER' AND owner_user_id IS NOT NULL AND owner_role_code IS NULL)
        OR (owner_type = 'ROLE_QUEUE' AND owner_user_id IS NULL AND owner_role_code IS NOT NULL)
    ),
    CONSTRAINT ck_operational_action_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_operational_action_version CHECK (version >= 0)
);
CREATE INDEX idx_operational_action_case
    ON operational_exception_corrective_action (tenant_id, case_id, created_at, id);
CREATE INDEX idx_operational_action_due
    ON operational_exception_corrective_action (tenant_id, status, due_at) WHERE status IN ('OPEN', 'IN_PROGRESS');

CREATE TABLE operational_exception_rca (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    case_id UUID NOT NULL,
    cause_category VARCHAR(24) NOT NULL,
    root_cause_code VARCHAR(80) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    contributing_factors VARCHAR(2000),
    author_id UUID NOT NULL,
    approver_id UUID,
    approved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_operational_rca_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_operational_rca_case UNIQUE (tenant_id, case_id),
    CONSTRAINT fk_operational_rca_case_tenant FOREIGN KEY (case_id, tenant_id)
        REFERENCES operational_exception_case (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT ck_operational_rca_cause CHECK (cause_category IN ('PEOPLE', 'PROCESS', 'EQUIPMENT', 'EXTERNAL', 'SYSTEM_DATA', 'ENVIRONMENT', 'UNKNOWN')),
    CONSTRAINT ck_operational_rca_approval_sod CHECK (approver_id IS NULL OR approver_id <> author_id),
    CONSTRAINT ck_operational_rca_approval_complete CHECK ((approver_id IS NULL) = (approved_at IS NULL)),
    CONSTRAINT ck_operational_rca_version CHECK (version >= 0)
);

CREATE TABLE operational_exception_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    case_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    before_value VARCHAR(2000),
    after_value VARCHAR(2000),
    reason VARCHAR(2000),
    actor_id UUID NOT NULL,
    actor_username VARCHAR(128) NOT NULL,
    correlation_id VARCHAR(128),
    resulting_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_operational_history_case_tenant FOREIGN KEY (case_id, tenant_id)
        REFERENCES operational_exception_case (id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT ck_operational_history_version CHECK (resulting_version >= 0)
);
CREATE INDEX idx_operational_history_case
    ON operational_exception_history (tenant_id, case_id, occurred_at DESC, id);

INSERT INTO app_permission (code, description, active) VALUES
    ('OPERATIONAL_EXCEPTION_VIEW', 'View tenant-scoped operational exception queues and non-sensitive detail', TRUE),
    ('OPERATIONAL_EXCEPTION_MANAGE', 'Classify, acknowledge, start, manage actions, and resolve operational exceptions', TRUE),
    ('OPERATIONAL_EXCEPTION_ASSIGN', 'Assign and reassign operational exceptions', TRUE),
    ('OPERATIONAL_EXCEPTION_ESCALATE', 'Manually escalate operational exceptions', TRUE),
    ('OPERATIONAL_EXCEPTION_RCA', 'View, author, and approve root cause analysis subject to segregation of duties', TRUE),
    ('OPERATIONAL_EXCEPTION_CLOSE', 'Validate, close, reject resolution, and reopen operational exceptions', TRUE),
    ('OPERATIONAL_EXCEPTION_AUDIT_VIEW', 'View complete immutable operational exception history', TRUE)
ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description, active = TRUE;

INSERT INTO app_role (id, name, description, active)
SELECT queue.id, queue.name, queue.description, TRUE
FROM (VALUES
    ('78000000-0000-0000-0000-000000000001'::UUID, 'OPERATIONS_QUEUE', 'General operational exception queue'),
    ('78000000-0000-0000-0000-000000000002'::UUID, 'OPERATIONS_SAFETY_QUEUE', 'Safety operational exception queue'),
    ('78000000-0000-0000-0000-000000000003'::UUID, 'OPERATIONS_COMPLIANCE_QUEUE', 'Compliance operational exception queue'),
    ('78000000-0000-0000-0000-000000000004'::UUID, 'OPERATIONS_CUSTOMER_QUEUE', 'Customer operational exception queue'),
    ('78000000-0000-0000-0000-000000000005'::UUID, 'OPERATIONS_FINANCIAL_QUEUE', 'Financial operational exception queue'),
    ('78000000-0000-0000-0000-000000000006'::UUID, 'OPERATIONS_TECHNICAL_QUEUE', 'Technical operational exception queue'),
    ('78000000-0000-0000-0000-000000000007'::UUID, 'OPERATIONS_SECURITY_QUEUE', 'Security operational exception queue')
) AS queue(id, name, description)
WHERE NOT EXISTS (SELECT 1 FROM app_role role WHERE role.name = queue.name);

INSERT INTO app_role_permission (role_id, permission_code)
SELECT role.id, permission.code
FROM app_role role
CROSS JOIN (VALUES ('OPERATIONAL_EXCEPTION_VIEW'), ('OPERATIONAL_EXCEPTION_MANAGE')) AS permission(code)
WHERE role.name IN ('OPERATIONS_QUEUE', 'OPERATIONS_SAFETY_QUEUE', 'OPERATIONS_COMPLIANCE_QUEUE',
    'OPERATIONS_CUSTOMER_QUEUE', 'OPERATIONS_FINANCIAL_QUEUE', 'OPERATIONS_TECHNICAL_QUEUE',
    'OPERATIONS_SECURITY_QUEUE')
ON CONFLICT (role_id, permission_code) DO NOTHING;
