FROM amazoncorretto:17
COPY workflow-bot-app/build/libs/*.jar wdk.jar
ENTRYPOINT ["java", "-Dspring.config.additional-location=./symphony/",  "-jar", "wdk.jar", "--spring.profiles.active=${PROFILE:default}"]
