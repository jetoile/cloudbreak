-- // BUG-106826_authorization_domain_model
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS tenant (
    id              bigserial NOT NULL,
    name            VARCHAR (255) NOT NULL,

    CONSTRAINT      tenant_id                               PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS organization (
    id              bigserial NOT NULL,
    name            VARCHAR (255),
    tenant_id       int8 NOT NULL,

    CONSTRAINT      pk_organization_id                      PRIMARY KEY (id),
    CONSTRAINT      fk_organization_tenant                  FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE IF NOT EXISTS organization_properties (
    id              bigserial NOT NULL,
    property_name   VARCHAR (255) NOT NULL,
    property_value  VARCHAR (255) NOT NULL,
    organization_id int8 NOT NULL,

    CONSTRAINT      pk_organization_property_id             PRIMARY KEY (id),
    CONSTRAINT      fk_organization_property_organization   FOREIGN KEY (organization_id) REFERENCES organization(id)
);

CREATE TABLE IF NOT EXISTS users (
--  maybe a varchar ID would be better?
    id              bigserial NOT NULL,
    name            VARCHAR (255),
    email           VARCHAR (255) NOT NULL UNIQUE,
    company         VARCHAR (255),
    is_cb_admin     BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_role     VARCHAR (255) NOT NULL,
    tenant_id       int8 NOT NULL,

    CONSTRAINT      pk_user_id                              PRIMARY KEY (id),
    CONSTRAINT      fk_user_tenant                          FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE IF NOT EXISTS user_to_organization (
    id              bigserial NOT NULL,
    role            VARCHAR (255),
    user_id         int8 NOT NULL,
    organization_id int8 NOT NULL,

    CONSTRAINT      pk_user_to_organization_id              PRIMARY KEY (id),
    CONSTRAINT      fk_user_to_organization_user            FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT      fk_user_to_organization_org             FOREIGN KEY (organization_id) REFERENCES organization(id)
);

ALTER TABLE stack
    ADD organization_id int8,
    ADD CONSTRAINT fk_stack_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE recipe
    ADD organization_id int8,
    ADD CONSTRAINT fk_recipe_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

ALTER TABLE blueprint
    ADD organization_id int8,
    ADD CONSTRAINT fk_blueprint_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

-- TODO: should add organization column to every table

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack
    DROP CONSTRAINT fk_stack_organization,
    DROP COLUMN organization_id;

ALTER TABLE recipe
    DROP CONSTRAINT fk_recipe_organization,
    DROP COLUMN organization_id;

ALTER TABLE blueprint
    DROP CONSTRAINT fk_blueprint_organization,
    DROP COLUMN organization_id;

DROP TABLE IF EXISTS user_to_organization;
DROP TABLE IF EXISTS organization_properties;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS organization;
DROP TABLE IF EXISTS tenant;
