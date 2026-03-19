CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'ANALYST', 'RISK_MANAGER', 'AUDITOR')),
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_locked BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    lock_time TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_app_user_role
    ON app_user (role);

CREATE INDEX idx_app_user_enabled
    ON app_user (enabled) WHERE enabled = true;

CREATE TABLE role_permission (
    id UUID PRIMARY KEY,
    role VARCHAR(30) NOT NULL,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_role_permission_role_resource_action UNIQUE (role, resource, action)
);
