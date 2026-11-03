# Estado del Proyecto — Sprint 2 (HU-14, HU-17, HU-18)

> **Ubicación Canónica:** `gymvip-ms-comunicaciones/documentos/ESTADO_PROYECTO.md`  
> **Responsable:** André Acosta  
> **Incremento:** Sprint 2 (Comunicaciones, WhatsApp y Recordatorios)  
> **Fecha:** 03 de Noviembre de 2026  

---

## 1. Trazabilidad de Historias de Usuario

| HU | Nombre de Historia | Responsable | Estado | Alcance Implementado |
| :--- | :--- | :--- | :--- | :--- |
| **HU-14** | Configurar recordatorios (WEB / MICRO) | André Acosta | **Terminada (Web/Backend)** | Modelo de configuración, anticipaciones de 3, 5, 7, 15 días, primeros 5 días post-vencimiento, evaluación automática con zona America/Lima, exclusión de renovadas, idempotencia, API y pantalla React. |
| **HU-17** | Comunicaciones WhatsApp (MICRO) | André Acosta | **Terminada (Código) / Bloqueada para Envíos Reales por Credenciales** | Adaptador Meta Cloud API v20 y simulador explícito, registro inmutable, normalización E.164, validación de consentimiento, plantillas aprobadas, webhook de verificación y estados, reintentos con backoff. Envíos reales supeditados a cuenta Meta aprobada. |
| **HU-18** | Estado de comunicaciones (WEB / MICRO) | André Acosta | **Terminada (Web/Backend/Micro)** | API de consulta de intentos, filtros por estado, canal y búsqueda, actualización vía webhooks, pantalla React con badges Dark & Gold, modal de detalles y reintento autorizado. |

---

## 2. Detalle de Tareas (31 Tareas del Sprint 2)

### HU-14: Configurar recordatorios (T01–T10)
- **T01:** Analizar reglas de recordatorios -> **Implementada y Documentada**
- **T02:** Diseñar interfaz de configuración -> **Implementada en React**
- **T03:** Implementar formulario React -> **Implementada con validaciones**
- **T04:** Crear modelo de persistencia -> **Implementada en Flyway V9**
- **T05:** Desarrollar API Spring Boot -> **Implementada con RBAC ADMIN**
- **T06:** Evaluar vencimientos automáticamente -> **Implementada con exclusión de renovaciones**
- **T07:** Controlar duplicados y renovaciones -> **Implementada con idempotencyKey única**
- **T08:** Integrar microservicio de comunicaciones -> **Implementada vía RestClient**
- **T09:** Integrar React con la API -> **Implementada con feedback visual**
- **T10:** Realizar pruebas funcionales e integrales -> **Probada con suites unitarias**

### HU-17: Comunicaciones WhatsApp (T01–T11)
- **T01:** Analizar requisitos de integración -> **Documentada Meta Cloud API**
- **T02:** Diseñar flujo de comunicaciones -> **Documentado en secuencia y C4**
- **T03:** Configurar conexión con WhatsApp Business -> **Implementada con selector Meta / Simulación**
- **T04:** Implementar gestión de plantillas -> **Implementada con 4 plantillas aprobadas**
- **T05:** Desarrollar servicio de envío automático -> **Implementada con ciclo de vida completo**
- **T06:** Validar destinatarios y consentimiento -> **Implementada con normalización E.164**
- **T07:** Registro y control de intentos -> **Implementada con enmascaramiento en logs**
- **T08:** Tratamiento de errores y nuevos intentos -> **Implementada con reintentos acotados (máx 3)**
- **T09:** Recepción de respuestas por Webhook -> **Implementada verificación y captura de estados**
- **T10:** Integrar con recordatorios HU-14 y FAQ HU-16 -> **Implementada interfaz desacoplada**
- **T11:** Pruebas funcionales e integrales -> **8/8 pruebas automatizadas aprobadas**

### HU-18: Estado de comunicaciones (T01–T10)
- **T01:** Analizar reglas de consulta -> **Documentada matriz de estados y roles**
- **T02:** Diseñar interfaz de seguimiento -> **Implementada en React Dark & Gold**
- **T03:** Implementar listado en React -> **Implementada tabla responsiva con badges**
- **T04:** Implementar filtros en React -> **Implementada barra de filtros y búsqueda**
- **T05:** Almacenamiento de estados -> **Reutiliza communication_attempts sin duplicidad**
- **T06:** Desarrollar API de consulta en Spring Boot -> **Implementada paginación y gateway**
- **T07:** Actualización de estados por eventos -> **Sincronizada con webhook de Meta**
- **T08:** Consulta de errores y seguimiento -> **Modal con error saneado y reintento**
- **T09:** Integrar interfaz con API -> **Conexión completa con estados de carga y error**
- **T10:** Pruebas funcionales e integrales -> **Probada en microservicio y frontend**

---

## 3. Dependencias y Bloqueos de Negocio Fuera de Código

1. **Alta de Cuenta Comercial de WhatsApp (Meta Business Manager):**
   - Requiere personería jurídica o verificación comercial de VIP CENTER FIT.
   - Requiere compra/asignación del número oficial de WhatsApp.
2. **Aprobación de Plantillas en Meta:**
   - Las 4 plantillas diseñadas (`recordatorio_vencimiento`, `aviso_vencido`, `renovacion_confirmada`, `bienvenida_socio`) deben someterse a revisión en el portal de Meta for Developers.
3. **App Móvil (KMP):**
   - La entrega de avisos en la app cliente pertenece formalmente al Sprint 3.
