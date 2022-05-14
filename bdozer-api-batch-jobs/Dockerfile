FROM openjdk:11-slim

WORKDIR /app
ADD target/bdozer-api-batch-jobs-0.0.1-SNAPSHOT.jar /app
ADD russell-1000-constituents.txt /app/bdozer-api-batch-jobs/russell-1000-constituents.txt
CMD ["java", "-cp", "bdozer-api-batch-jobs-0.0.1-SNAPSHOT.jar", "co.bdozer.jobs.SyncZacksDataKt"]
