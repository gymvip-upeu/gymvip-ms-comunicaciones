# Arquitectura del Microservicio de Comunicaciones — GYM VIP

> **Ubicación Canónica:** `gymvip-ms-comunicaciones/documentos/ARQUITECTURA_Y_MODELOS.md`  
> **Servicio:** `gymvip-comunicaciones`  
> **Sprint:** Sprint 2 (HU-14, HU-17, HU-18)  

---

## 1. Límites del Sistema y Flujo de Información

```
+---------------------------------------------------------------------------------------+
|                                    GYM VIP ECOSYSTEM                                  |
|                                                                                       |
|   +-----------------------+              +----------------------------------------+   |
|   |    gymvip-frontend    |              |             gymvip-backend             |   |
|   |  - Recordatorios (14) | <== JWT ===> |  - Evaluador de Vencimientos (HU-14)   |   |
|   |  - Comunicaciones(18) |              |  - Membresías & Consentimientos BD     |   |
|   +-----------------------+              +----------------------------------------+   |
|                                                              ||                       |
|                                                 HTTP REST (X-Service-Key)             |
|                                                 Idempotency-Key (requestId)           |
|                                                              \/                       |
|                                          +----------------------------------------+   |
|                                          |        gymvip-ms-comunicaciones        |   |
|                                          |  - Validador E.164 & Consentimiento    |   |
|                                          |  - Registro Auditoría Inmutable        |   |
|                                          |  - Adaptador WhatsApp (Meta / Sim)     |   |
|                                          |  - Webhook Receiver & Status Tracker   |   |
|                                          +----------------------------------------+   |
+--------------------------------------------------------------||-----------------------+
                                                               ||
                                                     Meta Cloud API (v20.0)
                                                     Webhooks (HTTPS POST)
                                                               \/
                                                   +------------------------+
                                                   |     Meta Graph API     |
                                                   |   WhatsApp Business    |
                                                   +------------------------+
```

---

## 2. Diagrama de Secuencia del Flujo de Notificación

```mermaid
sequenceDiagram
    autonumber
    participant Backend as gymvip-backend (Evaluador HU-14)
    participant Comms as gymvip-comunicaciones (Microservicio)
    participant Meta as Meta Cloud API (WhatsApp)
    participant User as Socio (WhatsApp)

    Backend->>Backend: Evalúa membresías (America/Lima)
    Backend->>Backend: Filtra por regla (3, 5, 7, 15d o vencida)
    Backend->>Backend: Descarta renovadas & valida consentimiento
    Backend->>Comms: POST /api/v1/messages/whatsapp (requestId, teléfono, plantilla, variables)
    Comms->>Comms: Verifica idempotencia por requestId
    Comms->>Comms: Normaliza teléfono E.164 y valida consentimiento
    Comms->>Comms: Guarda CommunicationAttempt (SOLICITADO)
    Comms->>Meta: POST /v20.0/{phone-id}/messages
    Meta-->>Comms: HTTP 200 OK (wamid)
    Comms->>Comms: Actualiza estado (ACEPTADO_PROVEEDOR / ENVIADO)
    Comms-->>Backend: HTTP 200 OK (CommunicationResponse)

    Meta->>User: Entrega mensaje al teléfono
    User->>User: Recibe y abre mensaje
    Meta->>Comms: Webhook POST /api/v1/webhooks/whatsapp (delivered / read)
    Comms->>Comms: Actualiza CommunicationAttempt (ENTREGADO -> LEIDO)
```

---

## 3. Modelo de Dominio de Intentos de Comunicación (ERD)

```mermaid
erDiagram
    COMMUNICATION_ATTEMPTS {
        uuid id PK
        varchar request_id UK
        varchar channel
        varchar recipient
        varchar recipient_masked
        uuid client_id
        varchar client_name
        uuid membership_id
        varchar template_name
        varchar payload_summary
        varchar status
        varchar external_message_id
        varchar error_message
        int attempt_count
        timestamp created_at
        timestamp updated_at
        timestamp sent_at
        timestamp delivered_at
        timestamp read_at
    }
```
