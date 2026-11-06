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

# Iniciar Spring Boot con zona horaria America/Lima y mapeo de puerto dinámico
ENTRYPOINT ["sh", "-c", "java -Duser.timezone=America/Lima -Dserver.port=${PORT} -jar app.jar"]
