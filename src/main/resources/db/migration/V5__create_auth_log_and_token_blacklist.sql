CREATE TABLE authentication_log (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES app_user(id),
    username VARCHAR(50) NOT NULL,
    action VARCHAR(30) NOT NULL CHECK (action IN ('LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT', 'TOKEN_REFRESH', 'PASSWORD_CHANGE', 'ACCOUNT_LOCKED')),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    details VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_auth_log_user_action
    ON authentication_log (user_id, action, created_at);

CREATE INDEX idx_auth_log_created_at
    ON authentication_log (created_at);

CREATE TABLE token_blacklist (
    id UUID PRIMARY KEY,
    jti VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    blacklisted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(100)
);

CREATE INDEX idx_token_blacklist_jti
    ON token_blacklist (jti);

CREATE INDEX idx_token_blacklist_expires_at
    ON token_blacklist (expires_at);
