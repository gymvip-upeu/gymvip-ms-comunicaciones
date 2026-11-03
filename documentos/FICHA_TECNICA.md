# Ficha Técnica — Microservicio de Comunicaciones (GYM VIP)

> **Ubicación Canónica:** `gymvip-ms-comunicaciones/documentos/FICHA_TECNICA.md`  
> **Servicio:** `gymvip-comunicaciones`  
> **Puerto por Defecto:** `8085`  

---

## 1. Stack Tecnológico

| Componente | Tecnología Seleccionada | Versión |
| :--- | :--- | :--- |
| **Lenguaje** | Java (LTS) | 21 / 24 |
| **Framework** | Spring Boot | 3.4.3 |
| **ORM / Persistencia** | Spring Data JPA / Hibernate | 6.6.8.Final |
| **Base de Datos** | H2 (Memoria / Pruebas) / PostgreSQL compatible | 2.3.232 |
| **Herramienta de Construcción** | Apache Maven con Wrapper | 3.9.10 |
| **Cliente HTTP** | Spring RestClient | 6.2 |
| **Pruebas** | JUnit 5 / Spring Boot Test / MockMvc | 3.4.3 |

---

## 2. Variables de Entorno y Configuración

| Variable de Entorno | Propósito | Valor por Defecto |
| :--- | :--- | :--- |
| `PORT` | Puerto de escucha HTTP | `8085` |
| `COMMS_MODE` | Modo de operación (`simulation` o `production`) | `simulation` |
| `COMMS_SERVICE_TOKEN` | Token de autenticación servicio a servicio | `gymvip-internal-token-2026` |
| `WHATSAPP_PROVIDER` | Proveedor activo (`meta` o `simulation`) | `meta` |
| `WHATSAPP_API_VERSION` | Versión de Meta Graph API | `v20.0` |
| `WHATSAPP_PHONE_ID` | Identificador de número de teléfono en Meta | `""` (vía env) |
| `WHATSAPP_ACCESS_TOKEN` | Token de acceso de usuario del sistema Meta | `""` (vía env) |
| `WHATSAPP_WABA_ID` | WhatsApp Business Account ID | `""` (vía env) |
| `WHATSAPP_HANDLE` | Identificador público @usuario del negocio | `@vipcenterfit` |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Token para handshake de webhook | `gymvip_meta_webhook_token_2026` |
| `WHATSAPP_APP_SECRET` | Secreto para verificación de firma HMAC-SHA256 | `""` (vía env) |

---

## 3. Instrucciones de Ejecución Local (PowerShell)

```powershell
Set-Location gymvip-comunicaciones

# 1. Compilación y pruebas unitarias
.\mvnw.cmd test

# 2. Iniciar servidor local (puerto 8085)
.\mvnw.cmd spring-boot:run

# 3. Comprobar salud del microservicio
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/health" -Method Get
```

---

## 4. Estrategia de Despliegue en la Nube (Render)

- **Tipo de Servicio:** Web Service en Render.
- **Entorno de Ejecución:** Dockerfile o Java Native.
- **Puerto:** `8085` (o variable `$PORT` asignada por Render).
- **Ruta de Health Check:** `/api/v1/health`
- **Aislamiento:** La base de datos es interna o conectada a PostgreSQL de Render para persistencia entre despliegues.
