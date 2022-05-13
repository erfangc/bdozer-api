FROM openjdk:11-slim

WORKDIR /app
ADD target/bdozer-api-batch-jobs-0.0.1-SNAPSHOT.jar /app

CMD ["java", "-cp", "bdozer-api-batch-jobs-0.0.1-SNAPSHOT.jar", "co.bdozer.jobs.SyncZacksDataKt"]
