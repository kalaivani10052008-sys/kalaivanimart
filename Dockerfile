# Multi-Stage Dockerfile for KalaivaniMart
# Stage 1: Build Application with Maven and Java 17
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime Environment with Apache Tomcat 9
FROM tomcat:9.0-jdk17-temurin
WORKDIR /usr/local/tomcat

# Remove default Tomcat webapps
RUN rm -rf webapps/*

# Copy built WAR as both kalaivanimart.war and ROOT.war for seamless root access
COPY --from=builder /app/target/kalaivanimart.war webapps/kalaivanimart.war
COPY --from=builder /app/target/kalaivanimart.war webapps/ROOT.war

# Set environment defaults
ENV PORT=8080
ENV DB_TYPE=mysql

# Healthcheck checking actual database and servlet connectivity
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/kalaivanimart/health || exit 1

EXPOSE 8080

# Entrypoint script adjusting Tomcat connector port dynamically for Render ($PORT)
CMD sh -c "sed -i 's/port=\"8080\"/port=\"'\"${PORT:-8080}\"'\"/g' conf/server.xml && bin/catalina.sh run"
