FROM node:lts-alpine AS build

WORKDIR /app

# Copy local code to the container image.
COPY . ./

RUN git clone git@github.com:ChatTriggers/website-frontend.git

RUN cd website-frontend
RUN yarn build

RUN mv build/* ../static/frontend

# Use the Eclipse temurin alpine official image
# https://hub.docker.com/_/eclipse-temurin
FROM eclipse-temurin:17-jdk-alpine

# Create and change to the app directory.
WORKDIR /app

# Build the app.
RUN ./gradlew uberJar

# Run the app by dynamically finding the JAR file in the target directory
CMD ["sh", "-c", "java -jar build/libs/*.jar --production"]
