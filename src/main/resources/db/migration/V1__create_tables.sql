CREATE TABLE users (
                       id          UUID PRIMARY KEY,
                       username    VARCHAR(50)  NOT NULL UNIQUE,
                       password    VARCHAR(255) NOT NULL,
                       role        VARCHAR(20)  NOT NULL
);

CREATE TABLE access_grants (
                               id              UUID PRIMARY KEY,
                               user_id         UUID         NOT NULL REFERENCES users(id),
                               resource_name   VARCHAR(255) NOT NULL,
                               issued_at       TIMESTAMP WITH TIME ZONE NOT NULL,
                               expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
                               revoked         BOOLEAN      NOT NULL DEFAULT false
);

CREATE INDEX idx_access_grants_user_id ON access_grants(user_id);
CREATE INDEX idx_access_grants_expiration ON access_grants(expires_at, revoked);