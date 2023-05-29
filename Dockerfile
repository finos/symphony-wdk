FROM openjdk:17-alpine
RUN jlink --no-header-files --no-man-pages --compress=2 --strip-java-debug-attributes --output /jre \
    --add-modules java.base,java.scripting,java.sql,jdk.unsupported,java.se,jdk.crypto.ec

FROM alpine:3.18
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'
COPY --from=0 /jre /jre
COPY artifact/*.jar /wdk.jar
WORKDIR /symphony

ENTRYPOINT [ "/jre/bin/java", "-jar", "/wdk.jar", "--spring.profiles.active=${PROFILE:default}" ]
