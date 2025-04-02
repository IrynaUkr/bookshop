# Use an official Java runtime as a parent image
FROM amazoncorretto:21.0.4-alpine3.18

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file into the container
COPY target/bookshop-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]


# --- run postgres in docker---
# Use the official PostgreSQL image
#FROM postgres:latest
#
## Set environment variables for PostgreSQL
#ENV POSTGRES_DB=bookshop
#ENV POSTGRES_USER=postgres
#ENV POSTGRES_PASSWORD=123
#
## Expose the PostgreSQL port
#EXPOSE 5432