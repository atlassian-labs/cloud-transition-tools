# Build the application using Maven
FROM maven:3.9.9-amazoncorretto-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy all required source code and libraries
COPY libs ./libs
COPY pom.xml .
COPY src/main ./src/main

# Install the required libraries and build the application
RUN mvn install -f libs/pom.xml && mvn clean package -DskipTests

# Second stage: Create the runtime image
FROM amazoncorretto:17

# Set the working directory in the container
WORKDIR /app
COPY application.properties .

# Copy the JAR file from build stage
COPY --from=build /app/target/cloud-transition-tools-*.jar cloud-transition-tools.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "cloud-transition-tools.jar"]
