-- Enable uuid extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- firms
CREATE TABLE firms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    industry    VARCHAR(100),
    size        VARCHAR(50),
    goals       TEXT[],
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- users
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    company_name  VARCHAR(255),
    firm_id       UUID REFERENCES firms(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_firm_id ON users(firm_id);

-- token_blacklist (for logout / revocation)
CREATE TABLE token_blacklist (
    token_jti  VARCHAR(255) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_token_blacklist_expires ON token_blacklist(expires_at);

-- reports
CREATE TABLE reports (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id      UUID NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'pending',
    summary      TEXT,
    score        INTEGER,
    tags         TEXT[],
    s3_key       VARCHAR(512),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_reports_firm_id ON reports(firm_id);
CREATE INDEX idx_reports_firm_status ON reports(firm_id, status);
CREATE INDEX idx_reports_created_at ON reports(firm_id, created_at DESC);
CREATE INDEX idx_reports_completed_at ON reports(firm_id, completed_at DESC);

-- findings
CREATE TABLE findings (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    severity  VARCHAR(50),
    title     VARCHAR(255),
    detail    TEXT
);

CREATE INDEX idx_findings_report_id ON findings(report_id);

-- recommendations
CREATE TABLE recommendations (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    priority  VARCHAR(50),
    title     VARCHAR(255),
    detail    TEXT,
    effort    VARCHAR(50),
    impact    VARCHAR(50)
);

CREATE INDEX idx_recommendations_report_id ON recommendations(report_id);

-- chat_messages
CREATE TABLE chat_messages (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firm_id    UUID NOT NULL REFERENCES firms(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content    TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_firm_id ON chat_messages(firm_id, created_at ASC);

-- notifications
CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    body       TEXT,
    type       VARCHAR(50) NOT NULL DEFAULT 'info',
    read       BOOLEAN NOT NULL DEFAULT FALSE,
    href       VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id, read) WHERE read = FALSE;
