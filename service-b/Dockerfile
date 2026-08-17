FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY libs/company-otel-business ./libs/company-otel-business
RUN mvn -f libs/company-otel-business/pom.xml -B install -DskipTests
COPY service-b/pom.xml ./service-b/pom.xml
COPY service-b/src ./service-b/src
RUN mvn -f service-b/pom.xml -B package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /work
COPY --from=build /workspace/service-b/target/quarkus-app/ /work/

EXPOSE 8080
CMD ["sh", "-c", "if [ \"$OTEL_ENABLED\" = \"false\" ]; then export QUARKUS_OTEL_SDK_DISABLED=true; else export QUARKUS_OTEL_SDK_DISABLED=false; fi; exec java -jar /work/quarkus-run.jar"]
