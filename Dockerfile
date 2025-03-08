FROM node:lts-alpine AS frontend-build

WORKDIR /build

RUN apk add --no-cache git

RUN git clone https://github.com/ChatTriggers/website-frontend.git

RUN yarn --cwd website-frontend/
RUN yarn --cwd website-frontend/ build

RUN mkdir -p /app/static/frontend
RUN mv website-frontend/build/* /app/static/frontend

FROM gradle:8.13-jdk17 AS backend-build

WORKDIR /build

COPY . ./

RUN ./gradlew uberJar

RUN cp build/libs/*.jar /app

FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Run the app by dynamically finding the JAR file in the target directory
CMD ["sh", "-c", "java -jar ./*.jar --production"]
