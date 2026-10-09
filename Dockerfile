# ==========================================================================
# Stage 1: Build MohanMart WAR using Maven 3.9 & Eclipse Temurin JDK 17
# ==========================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies first
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy application source and SQL migrations, then package WAR
COPY src ./src
COPY db ./db
RUN mvn -B clean package -DskipTests

# ==========================================================================
# Stage 2: Production Runtime — Apache Tomcat 9.0 (Servlet 4.0) on JRE 17
# ==========================================================================
FROM tomcat:9.0-jre17-temurin

# Remove default Tomcat webapps, disable TCP shutdown port 8005 (port="-1") so Render
# port-scanners/health probes never hit StandardServer, and bind HTTP Connector to 0.0.0.0:${port.http}
RUN rm -rf /usr/local/tomcat/webapps/* /usr/local/tomcat/webapps.dist \
    && sed -i 's/<Server port="8005" shutdown="SHUTDOWN">/<Server port="-1" shutdown="SHUTDOWN">/' /usr/local/tomcat/conf/server.xml \
    && sed -i 's/Connector port="8080"/Connector address="0.0.0.0" port="${port.http}"/' /usr/local/tomcat/conf/server.xml \
    && groupadd -r appuser && useradd -r -u 1001 -g appuser appuser \
    && mkdir -p /usr/local/tomcat/logs /usr/local/tomcat/temp /usr/local/tomcat/work

COPY --from=build /app/target/mohanmart.war /usr/local/tomcat/webapps/ROOT.war
COPY --from=build /app/db /usr/local/tomcat/db

RUN chown -R appuser:appuser /usr/local/tomcat

WORKDIR /usr/local/tomcat
USER appuser

ENV PORT=8080
ENV H2_CONSOLE_ENABLED=false
ENV SEED_DATABASE=true

EXPOSE 8080

# Bind Tomcat to 0.0.0.0:${PORT:-8080} and use exec so Tomcat receives SIGTERM cleanly
CMD ["sh", "-c", "export CATALINA_OPTS=\"${CATALINA_OPTS} -Dport.http=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom\" && exec catalina.sh run"]
