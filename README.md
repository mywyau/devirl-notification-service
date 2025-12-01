# devirl_auth_service

Auth backend microservice codebase

### Order of setup scripts:

To run the app locally and run integration tests

To set up the postgres db please run scripts from repo:

### dev-irl-database-setup

1. ./setup_postgres.sh
1. ./setup_postgres_it.sh
2. ./setup_flyway_migrations.sh

(Note to run and test locally this is not necessarily needed but can be ran to check the docker container build)
To build the Application docker container locally:

```
 ./setup_app.sh
```

### connect to redis

### run redis test container for integration testing, port: 6380

```
docker run --name redis-test-container -p 6380:6379 -d redis
```

#### Run redis-server on port 6379:

```
redis-server
```

#### Run redis-cli to enter cli

```
redis-cli
```

```
keys *
```

```
get <keyId>
```

```
del <keyId>
```

### To run the app locally

```
./run.sh
```

or

```
sbt run
```

### To run the unit tests

```
./run_tests.sh
```

or

```
sbt test
```

### To run the integration tests

```
./run_tests.sh
```

or

```
sbt test
```

### Application Config:

APP_ENV environment variable controls which app config is ran with "local" being the default when running the application locally 

Running integration tests APP_ENV is set to "integration" in build.sbt, hence referencing:

```
devirl_auth_service/src/main/resources/application.integration.conf
```

### Production App Config

Production build dockerfile ENV APP_ENV=prod hence referencing: 

```
devirl_auth_service/src/main/resources/application.prod.conf
```


### To run only a single test suite in the integration tests:

Please remember to include the package/path for the shared resources,
the shared resources is needed to help WeaverTests locate the shared resources needed for the tests

```
./itTestOnly QuestRegistrationControllerISpec controllers.ControllerSharedResource
```

---

### To clear down docker container for app and orphans

```
docker-compose down --volumes --remove-orphans
```

---

### To connect to postgresql database

```
psql -h localhost -p 5433 -U dev_auth_user -d dev_auth_db
```

#### App Database Password:

```
turnip
```

### To connect to TEST postgresql Database

```
psql -h localhost -p 5434 -U dev_auth_test_user -d dev_auth_test_db
```

#### TEST Database Password:

```
turnip
```

---

### Set base search path for schema

Only needed if using multiple schemas in the db. At the moment we are using public so no need beforehand accidentally set a new schema in flyway conf

```
ALTER ROLE shared_user SET search_path TO share_schema, public;
```
---

### Mermaid Wireframe Diagrams

To view any mermaid diagrams 

```
command+shift+v 
```

### Mermaid diagram notes export to images (for READMEs, docs, or Confluence)

This can be in a separate repo so we do not install the dependency here.

```
npm install -g @mermaid-js/mermaid-cli
```