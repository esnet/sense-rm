-- Database: "sense-n-rm"

-- DROP DATABASE "sense-n-rm";

-- CREATE DATABASE "sense-n-rm"
--  WITH OWNER = "sense-n-rm"
--      ENCODING = 'UTF8'
--       LC_COLLATE = 'C'
--       LC_CTYPE = 'C'
--       CONNECTION LIMIT = -1;

-- Table: subscriptions

-- DROP TABLE subscriptions;

CREATE TABLE subscriptions
(
  ddsurl character varying(255) NOT NULL,
  created bigint NOT NULL,
  href character varying(255),
  last_audit bigint NOT NULL,
  last_modified bigint NOT NULL,
  last_successful_audit bigint NOT NULL,
  CONSTRAINT subscriptions_pkey PRIMARY KEY (ddsurl)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE subscriptions
  OWNER TO "sense-n-rm";

- Table: documents

-- DROP TABLE documents;

CREATE TABLE documents
(
  id character varying(255) NOT NULL,
  document text,
  document_id character varying(255),
  expires bigint NOT NULL,
  last_discovered bigint NOT NULL,
  nsa character varying(255),
  type character varying(255),
  CONSTRAINT documents_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE documents
  OWNER TO "sense-n-rm";

-- Table: model

-- DROP TABLE model;

CREATE TABLE model
(
  id bigint NOT NULL,
  base text,
  model_id character varying(255),
  topology_id character varying(255),
  version bigint NOT NULL,
  CONSTRAINT model_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model
  OWNER TO "sense-n-rm";


-- Table: subscriptions

-- DROP TABLE subscriptions;

CREATE TABLE subscriptions
(
  ddsurl character varying(255) NOT NULL,
  created bigint NOT NULL,
  href character varying(255),
  last_audit bigint NOT NULL,
  last_modified bigint NOT NULL,
  last_successful_audit bigint NOT NULL,
  CONSTRAINT subscriptions_pkey PRIMARY KEY (ddsurl)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE subscriptions
  OWNER TO "sense-n-rm";

-- Table: delta

-- DROP TABLE delta;

CREATE TABLE delta
(
  id bigint NOT NULL,
  CONSTRAINT delta_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE delta
  OWNER TO "sense-n-rm";


