# Use a Maven base image to build the application
FROM maven:3.9.6-eclipse-temurin-21 as builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven POM file and download dependencies (layer caching)
COPY pom.xml /app/
RUN mvn dependency:go-offline

# Copy the source code to the container
COPY src /app/src/

# Build the project
RUN mvn clean package -DskipTests

# Use a smaller JDK base image for running the application
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the built jar file from the builder stage
COPY --from=builder /app/target/your-kotlin-project.jar /app/your-kotlin-project.jar

# Copy additional resources if needed
# COPY config /app/config

# Set environment variables (can be overridden by Docker Compose or runtime)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Expose the application port
EXPOSE 8080

# Specify the command to run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar your-kotlin-project.jar"]
