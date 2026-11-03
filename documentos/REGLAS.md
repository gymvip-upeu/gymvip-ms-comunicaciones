# Reglas de Negocio de Comunicaciones y Recordatorios — GYM VIP

> **Ubicación Canónica:** `gymvip-ms-comunicaciones/documentos/REGLAS.md`  
> **Ámbito:** Sprint 2 (HU-14, HU-17, HU-18)  
> **Zona Horaria Oficial:** `America/Lima` (UTC-5)  

---

## 1. Reglas de Recordatorios de Vencimiento (HU-14)

1. **Anticipaciones Aprobadas (Días Previos):**
   - El sistema evalúa avisos de vencimiento con anticipaciones de: **3, 5, 7 y 15 días** calendario antes del fin de cobertura (`end_date - current_date`).
   - Se despacha un único recordatorio por miembro para cada regla de fecha.
2. **Avisos Posteriores al Vencimiento (Días Vencidos):**
   - Para membresías recién vencidas, se admiten recordatorios durante los **primeros 5 días posteriores al vencimiento** (`current_date - end_date BETWEEN 1 AND 5`).
3. **Exclusión de Membresías Renovadas (Invariante Crítica):**
   - Si un miembro ya renovó su membresía y cuenta con un periodo de cobertura posterior pagado y activo (`start_date > end_date` y `cancelled_at_utc IS NULL`), **queda estrictamente prohibido enviarle recordatorios de la fecha anterior ya superada**.
4. **Exclusión de Periodos Cancelados:**
   - Todo periodo con `cancelled_at_utc IS NOT NULL` es ignorado por el motor de evaluación.
5. **Control de Activación Administrativa:**
   - El Administrador puede activar o desactivar (`enabled: boolean`) el motor de recordatorios globalmente y por canal desde el frontend.
6. **Idempotencia Estricta por Regla y Fecha:**
   - La clave de idempotencia se genera bajo el formato determinista:  
     `REM-{periodId}-{ruleType}-{targetDate}-{channel}`.  
     Si ya existe un despacho registrado con esta clave, el sistema omite el envío para prevenir saturación al cliente.

---

## 2. Reglas de Envío y WhatsApp Business (HU-17)

1. **Consentimiento Explícito por Canal:**
   - El envío por WhatsApp requiere autorización previa del miembro (`consent_whatsapp = true`). El consentimiento otorgado para correo electrónico o notificaciones no puede asumirse automáticamente como autorización de WhatsApp.
2. **Plantillas Oficiales de Utilidad (Utility Templates):**
   - Los mensajes fuera de la ventana de 24 horas solo pueden enviarse mediante plantillas previamente aprobadas por Meta. No se admiten textos libres ni promociones no autorizadas.
3. **Diferenciación de Identificadores:**
   - **ID Emisor / Phone Number ID:** Es el identificador numérico oficial de 15 dígitos en Meta Cloud API para el despacho técnico.
   - **@usuario comercial (`@vipcenterfit`):** Es un identificador público de la marca para descubrimiento. **NO reemplaza el número telefónico de destino en el API de envío**.
4. **Política de Reintentos:**
   - Errores de red transitorios (HTTP 429, timeouts, 503) permiten hasta **3 reintentos automáticos** con retroceso exponencial (backoff).
   - Errores permanentes (número inexistente, plantilla deshabilitada, bloqueo por usuario) detienen los reintentos y marcan el intento como `FALLIDO`.

---

## 3. Reglas de Estado y Trazabilidad (HU-18)

1. **Ciclo de Vida de Estados:**
   - `SOLICITADO` -> `ACEPTADO_PROVEEDOR` -> `ENVIADO` -> `ENTREGADO` -> `LEIDO`.
   - En caso de anomalía: `FALLIDO` o `DESCONOCIDO`.
2. **Transparencia en Recepción:**
   - El personal con roles `ADMIN` y `RECEPCION` puede consultar el historial de envíos para saber si un cliente ya fue notificado antes de atenderlo en mostrador.
3. **Reintento Manual:**
   - Exclusivo para usuarios con rol `ADMIN` sobre comunicaciones en estado `FALLIDO`.
