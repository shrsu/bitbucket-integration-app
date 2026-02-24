FROM e-cpos-docker-prod-local.docker.lowes.com/certified-images/alpine/v3.19/openjdk:v17
USER root
LABEL maintainer="DL-GO-EOC-PROJECT-TEAM@lowes.com"

WORKDIR /app
COPY target/*.jar app.jar

  # Change ownership
RUN  adduser --uid 10101 -S eor && \
chown -R 10101 /app && \
chown -R 10101 /tmp

EXPOSE 8080

  # guest user
USER 10101
ENTRYPOINT [ "sh","-c","java ${JAVA_OPTS} -jar /app/app.jar"  ]
