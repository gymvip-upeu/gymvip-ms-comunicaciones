# ==============================================================================
# GYM VIP (VIP CENTER FIT) — DOCKERFILE MICROSERVICIO COMUNICACIONES (RENDER)
# Base: Eclipse Temurin Java 21 LTS (Alpine Linux)
# Puerto predeterminado: 8085 (configurable dinámicamente con PORT por Render)
# ==============================================================================

# --- Etapa 1: Compilación de la aplicación ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copiar Maven Wrapper y pom.xml para cachear dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente y compilar el paquete ejecutable
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# --- Etapa 2: Imagen liviana de ejecución en producción ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Crear usuario y grupo de sistema sin privilegios para seguridad en contenedor
RUN addgroup -S gymvip && adduser -S gymvip -G gymvip

# Copiar el artefacto jar compilado desde la etapa builder
COPY --from=builder /workspace/target/*.jar app.jar
RUN chown -R gymvip:gymvip /app

# Ejecutar como usuario no privilegiado
USER gymvip

# Variables de entorno por defecto
ENV PORT=8085
ENV COMMS_MODE=simulation
ENV WHATSAPP_PROVIDER=meta
ENV TZ=America/Lima

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT}/api/v1/health" || exit 1

# Iniciar Spring Boot con exec para entrega correcta de SIGTERM y límites JVM para contenedor
ENTRYPOINT ["sh", "-c", "exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Duser.timezone=America/Lima -Dserver.address=0.0.0.0 -Dserver.port=${PORT} -jar app.jar"]
