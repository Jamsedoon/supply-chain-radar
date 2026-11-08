# ---------- Stage 1: build the .jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only the Gradle files first, then download libraries.
# Docker remembers this step, so later builds skip the download
# unless build.gradle changes.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon

# Now copy the code and build the .jar (tests already ran on your laptop).
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- Stage 2: the small box that actually runs ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run as the all-powerful "root" user inside the box.
RUN useradd --system radar
USER radar

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]