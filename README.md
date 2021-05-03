# bdozer-api

## Modules

- `bdozer-api-common`
  Shared data and domain classes
- `bdozer-api-web`
  Spring Boot server that exposes REST APIs
- `bdozer-api-factbase`
  Worker program that listens on MQ queues for SEC filings to parse
- `bdozer-api-factbase-core`
  The business logic code that knows how to parse SEC filings and save them as `Fact` objects

## Generating the TypeScript SDK

```bash
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-axios \
  -o ~/bdozer/client
```

## Sync data from environments

To dump:

```bash
mongodump \
--uri='mongodb+srv://<user>:<pass>@<host>/?retryWrites=true&w=majority' \
-d '<from env>' \
-c '<table>'
```

To restore:

```bash
mongorestore \
--uri='mongodb+srv://<user>:<pass>@<host>/?retryWrites=true&w=majority' \
-d '<to env>' \
-c '<table>' \
'dump/<from env>/<table>.bson'
```
