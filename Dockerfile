FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Start-up tuned for a small free-tier instance: C1-only JIT and a serial GC
# boot noticeably faster and use less memory than the defaults.
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75", "-Xshare:auto", "-Dspring.jmx.enabled=false", "-jar", "app.jar"]
