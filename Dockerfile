FROM eclipse-temurin:21-jdk-jammy  
COPY target/*.jar spring-order-service.jar  
ENTRYPOINT ["java","-jar","/spring-order-service.jar"]

