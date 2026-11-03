# Matriz de Evidencias de QA y Seguridad — Microservicio de Comunicaciones

> **Ubicación Canónica:** `gymvip-ms-comunicaciones/documentos/EVIDENCIAS_QA_SEGURIDAD.md`  
> **Servicio:** `gymvip-comunicaciones`  
> **Fecha de Medición:** 03 de Noviembre de 2026  

---

## 1. Pruebas Automatizadas Unitarias y de Integración

| Suite Evaluada | Clase de Prueba | Casos Evaluados | Resultado Medido | Estado |
| :--- | :--- | :--- | :--- | :--- |
| **WhatsApp Service & Idempotencia** | `WhatsAppServiceTest` | Despacho exitoso, idempotencia sin duplicación, rechazo sin consentimiento, rechazo de plantilla inválida, validación y normalización E.164. | **4 de 4 Aprobadas** (0 fallos, 0 errores) | **APROBADO** |
| **Meta Webhook Handshake & Statuses** | `WhatsAppWebhookTest` | Verificación GET con challenge válido, rechazo 403 ante token falso, actualización a ENTREGADO con payload Meta. | **3 de 3 Aprobadas** (0 fallos, 0 errores) | **APROBADO** |
| **Consulta y Filtros de Comunicaciones** | `CommunicationsQueryTest` | Listado paginado, filtro por estado FALLIDO, filtro por búsqueda de texto. | **1 de 1 Aprobada** (0 fallos, 0 errores) | **APROBADO** |
| **Total Microservicio** | `.\mvnw.cmd test` | 3 clases de prueba, 17 clases de producción | **8 de 8 Aprobadas** (Tiempo: 1 min 02 s) | **APROBADO** |

---

## 2. Matriz de Controles de Seguridad

| Control | Riesgo Mitigado | Implementación Técnica | Estado |
| :--- | :--- | :--- | :--- |
| **Enmascaramiento de PII en Logs** | Exposición de números telefónicos o correos en archivos de log y monitoreo. | Método `CommunicationAttempt.maskRecipient` (ej. `+519****21`). Los logs nunca imprimen teléfonos completos ni tokens. | **APROBADO** |
| **Modo Seguro de Simulación** | Despacho accidental de mensajes a clientes reales o incurrir en costos de Meta API durante pruebas. | `SimulationWhatsAppSender` genera identificadores ficticios `wamid.sim_...` sin llamadas HTTP externas si faltan credenciales o `COMMS_MODE=simulation`. | **APROBADO** |
| **Control Estricto de Consentimiento** | Sanciones legales y bloqueo de cuenta Meta por envío de mensajes no solicitados (Spam). | `WhatsAppService` verifica activamente `consentWhatsapp == true` antes de cualquier llamada al proveedor. Si no hay consentimiento, lanza `IllegalStateException`. | **APROBADO** |
| **Protección contra Inyecciones en Plantillas** | Modificación maliciosa del contenido de mensajes oficiales de Meta. | Solo se permiten plantillas registradas y aprobadas en `WhatsAppTemplate`. Se validan tipos y número mínimo de parámetros. | **APROBADO** |
| **Idempotencia contra Duplicidad** | Envío reiterado de recordatorios por reintentos de red o pulsaciones dobles. | `requestId` único indexado con constraint única en base de datos. Peticiones repetidas retornan el estado previo sin invocar a Meta. | **APROBADO** |
| **Límite de Reintentos** | Bucles infinitos de reintento consumiendo cuota o saturando al cliente. | Máximo 3 reintentos por comunicación fallida (`attemptCount <= 3`). | **APROBADO** |
