# gradle:7.3.1-jdk17 이미지를 기반으로 함
FROM openjdk:17

# 작업 디렉토리 설정
WORKDIR /home/gradle/project

# Spring 소스 코드를 이미지에 복사
COPY . .

# gradlew를 이용한 프로젝트 필드
RUN ./gradlew clean build -x test

# DATABASE_URL을 환경 변수로 삽입
ENV DATABASE_URL=jdbc:mysql://localhost:3306/everyide

# 빌드 결과 jar 파일을 실행
CMD ["java", "-jar", "/home/gradle/project/build/libs/web-ide-0.0.1-SNAPSHOT.jar"]
