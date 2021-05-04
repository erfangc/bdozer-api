# bdozer-api

## Modules

- `bdozer-api-common`
  Shared data and domain classes
- `bdozer-api-web`
  Spring Boot server that exposes REST APIs
- `bdozer-api-factbase-worker`
  Worker program that listens on MQ queues for SEC filings to parse
- `bdozer-api-factbase-core`
  The business logic code that knows how to parse SEC filings and save them as `Fact` objects
- `bdozer-api-models`
  Core `Model` and `Cell` translation and evaluation logic. Akin to spreadsheet graph calculation
- `bdozer-api-stockanalysis`
  Uses `bdozer-api-models` to perform stock analysis and post-processing and derived analytics  
- `bdozer-api-ml-worker`
  Console applications that runs in the background to prepare models and perform other tasks that 
  prepare data for automation and machine learning
  
## Generating the TypeScript SDK

```bash
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-axios \
  -o ~/bdozer/client
```

## Sync Data from Environments

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
