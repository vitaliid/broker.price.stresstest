FROM openjdk:17

WORKDIR /build

COPY ./build/libs/*.jar ./

EXPOSE 8080
CMD ["java", "-jar", "-Dspring.profiles.active=dev", "brokerpricestresstest-0.0.1.jar"]