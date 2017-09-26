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

