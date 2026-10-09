# ==========================================================================
# Stage 1: Build MohanMart WAR using Maven & Eclipse Temurin JDK 17
# ==========================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
COPY db ./db
RUN mvn -B clean package -DskipTests

# ==========================================================================
# Stage 2: Run on Apache Tomcat 9.0 (Servlet 4.0 / javax.servlet) with JRE 17
# ==========================================================================
FROM tomcat:9.0-jre17-temurin

# Remove default Tomcat webapps and deploy MohanMart as ROOT (and /mohanmart)
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/mohanmart.war /usr/local/tomcat/webapps/ROOT.war
COPY --from=build /app/target/mohanmart.war /usr/local/tomcat/webapps/mohanmart.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
