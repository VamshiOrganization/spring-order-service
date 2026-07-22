FROM eclipse-temurin:21-jdk-jammy  
COPY target/spring-order-service-0.0.1-SNAPSHOT.jar spring-order-service.jar  
ENTRYPOINT ["java","-jar","/spring-order-service.jar"]

