FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /work
COPY --from=build /app/target/quarkus-app/ /work/

EXPOSE 8080
CMD ["sh", "-c", "if [ \"$OTEL_ENABLED\" = \"false\" ]; then export QUARKUS_OTEL_SDK_DISABLED=true; else export QUARKUS_OTEL_SDK_DISABLED=false; fi; exec java -jar /work/quarkus-run.jar"]
