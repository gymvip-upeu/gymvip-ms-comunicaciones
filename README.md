# Microservicio de Comunicaciones — GYM VIP

**Estado:** Servicio desacoplado de despacho de notificaciones y mensajería.  
**Propósito:** Gestionar de forma asíncrona y confiable los envíos de correos de activación de cuenta (HU-05) y notificaciones de recordatorio de membresías vía WhatsApp (HU-13, HU-16), aislando la infraestructura externa del núcleo de negocio.

---

## 1. Modo de Prueba y Seguridad (Entorno Seguro Sin Costos)

Por defecto, este servicio opera en **Modo Simulado (`SIMULATION_MODE=true`)**:
- **Cero envíos reales:** No se despachan mensajes a redes telefónicas ni a servidores SMTP externos.
- **Sin credenciales requeridas:** No necesita tokens de Meta Cloud API, cuentas Twilio pagadas ni contraseñas de correo para compilar o ejecutar pruebas.
- **Trazabilidad en Log / Memoria:** Toda solicitud recibida genera un evento en consola y en memoria con un ID de despacho único (`deliveryId`), facilitando las pruebas de extremo a extremo.

---

## 2. Configuración y Conexión Futura (Meta Cloud API / Twilio / SendGrid)

Para habilitar proveedores productivos una vez autorizados los presupuestos y contratos:

```properties
# gymvip-comunicaciones / application.properties

# Modo de operación
comms.mode=production # Valores: simulation | production

# Proveedor WhatsApp (Meta Cloud API)
whatsapp.provider=meta
whatsapp.meta.api-version=v20.0
whatsapp.meta.phone-number-id=${WHATSAPP_PHONE_ID:}
whatsapp.meta.access-token=${WHATSAPP_ACCESS_TOKEN:}

# Proveedor Alternativo (Twilio)
# whatsapp.provider=twilio
# twilio.account-sid=${TWILIO_ACCOUNT_SID:}
# twilio.auth-token=${TWILIO_AUTH_TOKEN:}

# Proveedor Correo (SMTP / SendGrid)
mail.provider=smtp
spring.mail.host=${SMTP_HOST:smtp.sendgrid.net}
spring.mail.port=${SMTP_PORT:587}
spring.mail.username=${SMTP_USERNAME:}
spring.mail.password=${SMTP_PASSWORD:}
```

---

## 3. Contratos de API de Comunicaciones

### `POST /api/v1/messages/email`
- Encola y envía un correo transaccional (enlace de activación o aviso).
- **Payload:**
  ```json
  {
    "to": "socio@ejemplo.pe",
    "subject": "Activa tu cuenta en GYM VIP",
    "template": "ACTIVATION_LINK",
    "variables": {
      "name": "Carlos",
      "activationUrl": "https://gymvip.pe/activar?token=xyz123"
    }
  }
  ```

### `POST /api/v1/messages/whatsapp`
- Encola y envía un mensaje de plantilla a un número de WhatsApp verificado.
- **Payload:**
  ```json
  {
    "phoneNumber": "+51987654321",
    "templateName": "gymvip_reminder_expiration",
    "language": "es",
    "parameters": ["Carlos", "5 días", "15 de Octubre"]
  }
  ```
