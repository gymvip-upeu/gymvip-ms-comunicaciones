# Catálogo Formal de APIs — Microservicio de Comunicaciones (GYM VIP)

> **Ubicación Canónica:** `gymvip-ms-comunicaciones/documentos/APIDOCUMENTATION.md`  
> **Servicio:** `gymvip-comunicaciones` (Puerto `8085`)  
> **Versión:** 1.0 — Sprint 2 (HU-17 y HU-18)  
> **Fecha:** 03 de Noviembre de 2026  

---

## 1. Protocolo de Autenticación Servicio-a-Servicio

Las llamadas internas provenientes de `gymvip-backend` deben incluir el token de servicio en la cabecera:
```http
X-Service-Key: gymvip-internal-token-2026
```
O cabecera estándar de autorización Bearer.

---

## 2. Endpoints de Despacho de Mensajes (`/api/v1/messages`)

### 2.1 Envío de WhatsApp (HU-17 T05)
- **Método y Ruta:** `POST /api/v1/messages/whatsapp`
- **Content-Type:** `application/json`
- **Idempotencia:** Mediante el campo `requestId` en el cuerpo. Si ya fue procesado, retorna el registro existente sin duplicar el despacho hacia Meta API.

#### Cuerpo de la Solicitud (`application/json`)
```json
{
  "requestId": "REM-PERIOD-UUID-3DAYS-2026-11-10-WSP",
  "phoneNumber": "+51987654321",
  "templateName": "recordatorio_vencimiento",
  "language": "es",
  "parameters": [
    "Valeria Mendoza Quiroz",
    "Plan VIP Mensual",
    "2026-11-10",
    "7"
  ],
  "consentWhatsapp": true,
  "clientId": "5b61fd94-8ea8-473d-a398-66db8a117056",
  "clientName": "Valeria Mendoza Quiroz",
  "membershipId": "e1f0b09e-7164-42f8-b3bf-4d10ba35f7e1"
}
```

#### Respuesta Exitosa (`HTTP 200 OK`)
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "requestId": "REM-PERIOD-UUID-3DAYS-2026-11-10-WSP",
  "channel": "WHATSAPP",
  "recipientMasked": "+519****21",
  "clientName": "Valeria Mendoza Quiroz",
  "templateName": "recordatorio_vencimiento",
  "status": "ENVIADO",
  "externalMessageId": "wamid.sim_61eed7efaa4e4b8d",
  "errorMessage": null,
  "attemptCount": 1,
  "createdAt": "2026-11-03T00:52:04.900",
  "sentAt": "2026-11-03T00:52:04.919"
}
```

#### Errores Comunes
- `400 Bad Request`: Número telefónico inválido, plantilla no aprobada o parámetros faltantes.
- `400 Bad Request`: Miembro sin consentimiento de WhatsApp (`consentWhatsapp: false`).

---

## 3. Webhook de WhatsApp Meta Cloud API (`/api/v1/webhooks/whatsapp`)

### 3.1 Verificación del Webhook (Handshake GET)
- **Método:** `GET /api/v1/webhooks/whatsapp`
- **Query Params:**
  - `hub.mode=subscribe`
  - `hub.verify_token=<token_configurado>`
  - `hub.challenge=<desafio_numerico>`
- **Respuestas:**
  - `200 OK`: Devuelve el texto plano del `hub.challenge`.
  - `403 Forbidden`: Si el token no coincide.

### 3.2 Recepción de Eventos de Estado y Mensajes (POST)
- **Método:** `POST /api/v1/webhooks/whatsapp`
- **Headers:** `X-Hub-Signature-256: sha256=...` (opcional si `WHATSAPP_APP_SECRET` está activo).
- **Procesamiento:**
  - `statuses[].status`: actualiza a `ENVIADO`, `ENTREGADO`, `LEIDO` o `FALLIDO` (con mensaje de error).
  - `messages[]`: registra mensajes entrantes y los deja listos para HU-16.
- **Respuesta:** `200 OK` con `{ "status": "PROCESSED" }`.

---

## 4. Endpoints de Consulta de Estados (HU-18 T06)

### 4.1 Listado Filtrado de Comunicaciones
- **Método:** `GET /api/v1/communications`
- **Query Params:**
  - `status`: `TODOS`, `SOLICITADO`, `ACEPTADO_PROVEEDOR`, `ENVIADO`, `ENTREGADO`, `LEIDO`, `FALLIDO`.
  - `channel`: `TODOS`, `WHATSAPP`, `EMAIL`, `APP`.
  - `search`: Texto para buscar por nombre de cliente o número.
  - `page`: Número de página (default `0`).
  - `size`: Tamaño de página (default `20`).

#### Respuesta Exitosa (`HTTP 200 OK`)
```json
{
  "content": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "requestId": "REM-PERIOD-UUID-3DAYS-2026-11-10-WSP",
      "channel": "WHATSAPP",
      "recipientMasked": "+519****21",
      "clientId": "5b61fd94-8ea8-473d-a398-66db8a117056",
      "clientName": "Valeria Mendoza Quiroz",
      "membershipId": "e1f0b09e-7164-42f8-b3bf-4d10ba35f7e1",
      "templateName": "recordatorio_vencimiento",
      "payloadSummary": "Plantilla: recordatorio_vencimiento, Parámetros: [...]",
      "status": "ENTREGADO",
      "externalMessageId": "wamid.sim_61eed7efaa4e4b8d",
      "errorMessage": null,
      "attemptCount": 1,
      "createdAt": "2026-11-03T00:52:04.900",
      "sentAt": "2026-11-03T00:52:04.919",
      "deliveredAt": "2026-11-03T00:52:05.450",
      "readAt": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "size": 20
}
```

### 4.2 Reintento Manual de Comunicación Fallida
- **Método:** `POST /api/v1/communications/{id}/retry`
- **Respuestas:**
  - `200 OK`: Intento re-despachado con `attemptCount` incrementado.
  - `400 Bad Request`: Si el estado no era `FALLIDO` o superó el límite de 3 intentos.
  - `404 Not Found`: Si el ID no existe.

---

## 5. Endpoint de Salud Cloud
- **Método:** `GET /api/v1/health`
- **Público:** Sí (para Render / monitoreo).
- **Respuesta (`200 OK`):**
```json
{
  "service": "gymvip-comunicaciones",
  "status": "UP",
  "mode": "simulation",
  "whatsappProvider": "meta",
  "timestamp": "2026-11-03T00:52:00.000Z"
}
```
