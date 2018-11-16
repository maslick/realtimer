FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY build/libs/realtimer-db.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]