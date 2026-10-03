# =========================================================================
# Stage 1: Build GraalVM Native Executable
# =========================================================================
FROM ghcr.io/graalvm/native-image-community:21-ol9 AS builder

WORKDIR /build

# 1. Cache Maven Wrapper & POM dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

# 2. Copy source code
COPY src ./src

# 3. Compile AOT (Ahead-of-Time) Native Binary
# Note: GitHub Actions runner ke memory limit ko respect karne ke liye Xmx set kiya
ENV MAVEN_OPTS="-Xmx5g"
RUN ./mvnw -Pnative native:compile -DskipTests

# =========================================================================
# Stage 2: Ultra-minimal AWS Lambda Runtime
# =========================================================================
FROM public.ecr.aws/lambda/provided:al2023

# AWS Lambda Web Adapter: API Gateway requests ko localhost:8080 pe forward karega
COPY --from=public.ecr.aws/awsguru/aws-lambda-adapter:0.8.4 /lambda-adapter /opt/extensions/lambda-adapter

WORKDIR /var/task

# Stage 1 se compiled standalone native binary copy karo
COPY --from=builder /build/target/app ./app
RUN chmod +x ./app

# Web adapter configuration
ENV PORT=8080
ENV READINESS_CHECK_PORT=8080

# Application start command
CMD ["./app"]