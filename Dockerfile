# Build the application using Maven
FROM maven:3.9.9-amazoncorretto-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# copy source code and build the application
COPY src/main ./src/main
RUN mvn clean package -DskipTests

# Second stage: Create the runtime image
FROM amazoncorretto:17

# Set the working directory in the container
WORKDIR /app
COPY application.properties .

# Copy the JAR file from the first stage
COPY --from=build /app/target/cloud-transition-tools-0.0.1-SNAPSHOT.jar cloud-transition-tools.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "cloud-transition-tools.jar"]
