# 공개 데모용 이미지 (Render 무료 플랜: 메모리 512MB)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ebs-planner-0.1.0.jar app.jar
ENV JAVA_OPTS="-Xmx300m -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar app.jar --spring.profiles.active=demo,public"]
