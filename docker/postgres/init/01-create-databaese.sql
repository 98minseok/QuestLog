DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'questlog') THEN
      CREATE ROLE questlog LOGIN PASSWORD 'questlog';
   END IF;

   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'keycloak') THEN
      CREATE ROLE keycloak LOGIN PASSWORD 'keycloak';
   END IF;
END
$$;

ALTER DATABASE questlog OWNER TO questlog;

CREATE DATABASE keycloak OWNER keycloak;