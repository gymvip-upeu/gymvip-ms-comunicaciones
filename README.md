# GYM VIP — Microservicio de Comunicaciones (`gymvip-ms-comunicaciones`)

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green.svg)]()
[![Meta WhatsApp](https://img.shields.io/badge/Meta%20Graph%20API-v25.0-success.svg)]()
[![Resend Email](https://img.shields.io/badge/Resend%20API-v2-blue.svg)]()
[![Branch](https://img.shields.io/badge/branch-ACOSTA--MEJORAS--Y--ACTUALIZACIONES-yellow.svg)]()
[![License](https://img.shields.io/badge/license-Academic%20Proprietary-lightgrey.svg)]()

> **Ubicación en el Workspace:** `gymvip-comunicaciones/`  
> **Organización Oficial:** [gymvip-upeu/gymvip-ms-comunicaciones](https://github.com/gymvip-upeu/gymvip-ms-comunicaciones)  
> **Documentación Centralizada:** `../documentos/`  
> **Fecha de Actualización:** 23 de Noviembre de 2026 | Revisión Canónica v2.5

---

## 1. Nombre del Componente
**GYM VIP Communications & Notification Microservice** (`gymvip-ms-comunicaciones`).

---

## 2. Propósito
Gestionar de forma asíncrona, desacoplada y confiable el despacho de notificaciones externas del gimnasio **VIP CENTER FIT**, abarcando recordatorios automáticos de vencimiento por WhatsApp (Meta Cloud API), avisos de corte y confirmaciones de pago mediante correo electrónico transaccional (Resend API), con soporte de simulación segura de costo cero.

---

## 3. Responsabilidades
- **Despacho WhatsApp Oficial:** Envío mediante Meta Cloud API con normalización telefónica internacional E.164 (`+519XXXXXXXX`), identificadores BSUID y usernames Meta.
- **Gestión de Plantillas Aprobadas:** Mapeo de parámetros para las 4 plantillas canónicas (`recordatorio_vencimiento`, `aviso_vencido`, `renovacion_confirmada`, `bienvenida_socio`).
- **Recepción de Webhooks Meta:** Procesamiento bidireccional con verificación de handshake `hub.challenge` y validación de firma criptográfica HMAC SHA-256 (`X-Hub-Signature-256`).
- **Seguimiento de Ciclo de Vida:** Actualización de estados del mensaje (`SOLICITADO`, `ACEPTADO_PROVEEDOR`, `ENVIADO`, `ENTREGADO`, `LEIDO`, `FALLIDO`).
- **Correo Transaccional:** Envío mediante Resend API con plantillas HTML responsivas.
- **Modo Simulación:** Operación transparente sin saldo ni credenciales externas para pruebas locales y CI/CD.

---

## 4. Arquitectura
Microservicio desacoplado basado en Spring Boot 3 con arquitectura hexagonal/adaptadores:
- **Controladores REST y Webhook:** Puntos de entrada para el backend principal y para los servidores de Meta.
- **Servicios de Despacho (`service`):** Lógica de cola, validación de consentimiento del socio y políticas de reintento.
- **Adaptadores de Proveedor (`provider`):** Implementaciones intercambiables (`MetaWhatsAppSender`, `ResendEmailSender`, `SimulationSender`).
- **Persistencia (`repository`):** Esquema dedicado `comms` en PostgreSQL para trazabilidad inmutable de envíos.

---

## 5. Tecnologías y Versiones Reales
| Tecnología | Versión Oficial | Propósito |
|---|---|---|
| **Java Development Kit** | OpenJDK 21 LTS | Plataforma de ejecución |
| **Spring Boot** | 3.4.3 | Framework del microservicio |
| **Meta Graph API** | v25.0 | API oficial de mensajería WhatsApp Business |
| **Resend Java SDK / REST** | v2 compatible | API de correo transaccional de alta entregabilidad |
| **PostgreSQL Database** | 17.x / 18.x compatible | Persistencia de eventos de mensajería |
| **HMAC SHA-256** | Estándar Java Security | Validación de autenticidad de webhooks |

---

## 6. Estructura de Carpetas
```text
gymvip-comunicaciones/
├── mvnw / mvnw.cmd              # Maven Wrapper
├── pom.xml                      # Dependencias y plugins
├── CONTRIBUTING.md              # Estándar obligatorio de commits
├── README.md                    # Esta documentación canónica
└── src/
    ├── main/
    │   ├── java/com/gymvip/comms/
    │   │   ├── config/          # Configuración de adaptadores y perfiles
    │   │   ├── controller/      # WebhookController, MessageDispatchController
    │   │   ├── dto/             # Contratos de solicitud y respuesta
    │   │   ├── model/           # Entidades de despacho y estado
    │   │   ├── provider/        # Adaptadores Meta, Resend y Simulación
    │   │   └── service/         # Orquestación de mensajería
    │   └── resources/
    │       ├── application.properties      # Configuración de canales
    │       └── db/migration/               # Migraciones Flyway del esquema comms
    └── test/                    # Pruebas unitarias de adaptadores y webhook
```

---

## 7. Funcionalidades
1. **Recordatorios de Vencimiento (HU-14):** Envíos programados a 15, 7, 5 y 3 días previos al corte de membresía.
2. **Avisos de Corte Inmediato:** Notificación al socio tras 1 a 5 días de vencimiento sin renovación.
3. **Confirmación de Cobranza:** Resumen de operación y nuevo período contratado.
4. **Webhook Bidireccional de Meta (HU-17):** Actualización inmediata de confirmaciones de lectura.
5. **Modo Simulación Cero Costo:** Pruebas de integración sin consumir créditos de Meta ni Resend.

---

## 8. Usuarios y Roles Correspondientes
- **Servicio Interno:** Consumido exclusivamente por `gymvip-backend` mediante cabecera de autenticación de servicio (`X-Internal-Secret`).
- **Administradores:** Consulta de trazabilidad y reintento de fallidos desde la interfaz administrativa web.

---

## 9. Requisitos Previos
- **JDK:** OpenJDK 21 LTS.
- **Maven:** Versión 3.9+ o Maven Wrapper.
- **PostgreSQL:** Acceso a base de datos compartida o esquema `comms` local.

---

## 10. Instalación desde Cero
```powershell
# 1. Clonar el repositorio
git clone https://github.com/gymvip-upeu/gymvip-ms-comunicaciones.git
Set-Location gymvip-ms-comunicaciones

# 2. Situarse en la rama de trabajo
git checkout ACOSTA-MEJORAS-Y-ACTUALIZACIONES

# 3. Crear archivo de variables locales (.env) a partir del ejemplo
Copy-Item .env.example .env

# 4. Compilar dependencias
.\mvnw.cmd clean compile
```

---

## 11. Variables de Entorno
Configurar en `.env` (utilizar placeholders seguros sin secretos reales en Git):
```properties
# Modo de Operación (simulation | production)
COMMS_MODE=simulation

# Puerto de Ejecución Local
SERVER_PORT=8082

# Conexión a Base de Datos
DB_URL=jdbc:postgresql://localhost:5432/gymvip_db
DB_USERNAME=gymvip_user
DB_PASSWORD=placeholder_seguro_db_pass

# Credenciales Meta Cloud API (WhatsApp)
WHATSAPP_PHONE_ID=placeholder_phone_number_id
WHATSAPP_ACCESS_TOKEN=placeholder_meta_access_token
WHATSAPP_WEBHOOK_VERIFY_TOKEN=placeholder_verify_token
WHATSAPP_APP_SECRET=placeholder_app_secret_hmac

# Credenciales Resend (Correo)
RESEND_API_KEY=re_placeholder_api_key
```

---

## 12. Configuración Local
Para desarrollo y evaluación sin coste, mantener `COMMS_MODE=simulation`. Los mensajes se registrarán en los logs del servidor con un ID único de despacho (`deliveryId`) sin salir a la red telefónica.

---

## 13. Comandos de Ejecución
```powershell
.\mvnw.cmd spring-boot:run
```
El microservicio escuchará en `http://localhost:8082`.

---

## 14. Comandos de Compilación
```powershell
.\mvnw.cmd clean package -DskipTests
```

---

## 15. Pruebas
```powershell
.\mvnw.cmd test
```

---

## 16. Integraciones
- **Meta Cloud API (WhatsApp Business):** Endpoints oficiales de mensajes y templates v25.0.
- **Resend API:** Despacho SMTP/REST de correos institucionales.
- **gymvip-backend:** Recepción de eventos desde la cola transaccional Outbox.

---

## 17. Seguridad
- Verificación criptográfica obligatoria de firmas HMAC SHA-256 en webhooks entrantes.
- Enmascaramiento preventivo de números telefónicos y datos personales en logs (`+519****21`).
- Aislamiento estricto de secretos en variables de entorno del servidor.

---

## 18. Despliegue
- **Plataforma Productiva:** Render Web Service / Background Worker.
- **Healthcheck:** `GET /actuator/health`.

---

## 19. Solución de Problemas Frecuentes
- **Error `403 Forbidden` en Webhook Meta:** El token `hub.verify_token` configurado en Meta no coincide exactamente con `WHATSAPP_WEBHOOK_VERIFY_TOKEN`.
- **Error de Formato Telefónico:** Asegurarse de que el número comience con código de país (`+51` para Perú) sin espacios ni guiones.

---

## 20. Limitaciones Conocidas
- En modo producción, el despacho por WhatsApp requiere plantillas pre-aprobadas por Meta en el Business Manager.

---

## 21. Enlaces hacia los Otros Repositorios
- **Backend API:** [gymvip-backend](https://github.com/gymvip-upeu/gymvip-backend)
- **Frontend Web:** [gymvip-frontend](https://github.com/gymvip-upeu/gymvip-frontend)
- **App Móvil:** [gymvip-mobile](https://github.com/gymvip-upeu/gymvip-mobile)
- **Portal Público:** [gymvip-Landing-Page](https://github.com/gymvip-upeu/gymvip-Landing-Page)

---

## 22. Enlaces hacia Diagramas
- **Flujos de Notificaciones:** `../documentos/diagramas/09-Flujos-Funcionales/05_flujo_comunicaciones_recordatorios_whatsapp_push.puml`

---

## 23. Enlaces hacia ESTADO_PROYECTO.md
- Documento Central: [`../documentos/ESTADO_PROYECTO.md`](../documentos/ESTADO_PROYECTO.md)

---

## 24. Fecha y Revisión de Referencia
- **Fecha:** 23 de Noviembre de 2026.
- **Revisión de Ingeniería:** Consolidación Maestra Sprint 1 & 2 / Release Candidate v2.5.
- **Autoría:** Equipo de Ingeniería GYM VIP / VIP CENTER FIT.
