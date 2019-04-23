FROM gradle:5.1-jdk as builder

WORKDIR /home/gradle
ADD . /home/gradle
RUN gradle bootJar

FROM openjdk:8u191-jre-alpine
COPY --from=builder /home/gradle/build/libs/*.jar /tmp/jsql-api-provider.jar

