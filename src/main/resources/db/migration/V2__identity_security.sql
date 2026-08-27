CREATE TABLE app_permission (
    code VARCHAR(100) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE app_user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES app_role(id) ON DELETE CASCADE
);

CREATE TABLE app_role_permission (
    role_id UUID NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission_code),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES app_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_code) REFERENCES app_permission(code)
);

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by UUID,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_expiry ON refresh_token(expires_at);

INSERT INTO app_permission (code, description, active) VALUES
    ('IDENTITY_READ', 'Read users, roles, and the current identity', TRUE),
    ('IDENTITY_MANAGE', 'Create, update, deactivate, and assign identity data', TRUE);
