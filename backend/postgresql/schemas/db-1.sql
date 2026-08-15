CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users
(
    id            UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    login         VARCHAR(64) UNIQUE  NOT NULL,
    email         VARCHAR(128) UNIQUE NOT NULL,
    password_hash VARCHAR(255)        NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE projects
(
    id           UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    author_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    chat_history JSONB        NOT NULL    DEFAULT '[]'::jsonb,
    is_private   BOOLEAN      NOT NULL    DEFAULT TRUE,
    last_version_key VARCHAR(256),
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE project_versions
(
    id          UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    project_id  UUID    NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    version_num INTEGER NOT NULL,
    s3_file_key VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (project_id, version_num)
);


CREATE TABLE subscriptions
(
    subscriber_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_user_id  UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (subscriber_id, target_user_id),
    CHECK (subscriber_id <> target_user_id)
);



CREATE INDEX idx_projects_author_id ON projects (author_id);
CREATE INDEX idx_projects_public ON projects (created_at DESC) WHERE is_private = FALSE;

CREATE INDEX idx_project_versions_list ON project_versions (project_id, version_num DESC);

CREATE INDEX idx_subscriptions_target ON subscriptions (target_user_id);

CREATE INDEX idx_projects_author_created ON projects (author_id, created_at DESC) WHERE is_private = FALSE;
