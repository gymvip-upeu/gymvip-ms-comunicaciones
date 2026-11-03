# Especificación de Diseño y Pantallas — Sprint 2 (HU-14 y HU-18)

> **Ubicación Canónica:** `gymvip-ms-comunicaciones/documentos/DESIGN.md`  
> **Tema:** Figma Dark & Gold Luxury Fitness (`#0A0F1D`, `#D4AF37`)  

---

## 1. Mapeo de Vistas del Frontend Web

Las interfaces de usuario operan dentro del repositorio `gymvip-frontend` y consumen los endpoints de backend y comunicaciones:

1. **Pantalla de Configuración de Recordatorios (HU-14 T02 / T03 / T09):**
   - **Archivo:** [`gymvip-frontend/src/pages/ReminderConfigPage.jsx`](file:///c:/Users/XD/Documents/Proyectos%20Profesionales/GYMVIP/gymvip-frontend/src/pages/ReminderConfigPage.jsx)
   - **Ruta SPA:** `/recordatorios/configuracion`
   - **Acceso:** Exclusivo `ADMIN`
   - **Componentes:**
     - Toggle maestro: "Activar Recordatorios Automáticos".
     - Grupo de checkboxes: Días de anticipación (3, 5, 7, 15 días).
     - Selector numérico: Días posteriores al vencimiento (1 a 5 días).
     - Selección de canales: WhatsApp y Notificaciones en App.
     - Botón "Guardar Parámetros" y botón "Ejecutar Evaluación Ahora" (on-demand).
     - Tabla histórica de cortes evaluados.

2. **Pantalla de Estado de Comunicaciones (HU-18 T02 / T03 / T04 / T09):**
   - **Archivo:** [`gymvip-frontend/src/pages/CommunicationsPage.jsx`](file:///c:/Users/XD/Documents/Proyectos%20Profesionales/GYMVIP/gymvip-frontend/src/pages/CommunicationsPage.jsx)
   - **Ruta SPA:** `/comunicaciones`
   - **Acceso:** `ADMIN` y `RECEPCION`
   - **Componentes:**
     - Barra de filtros: Estado (`TODOS`, `SOLICITADO`, `ENVIADO`, `ENTREGADO`, `LEIDO`, `FALLIDO`), Canal (`TODOS`, `WHATSAPP`, `EMAIL`, `APP`), búsqueda por nombre de cliente.
     - Badges de estado con color temático Dark & Gold:
       - `SOLICITADO` / `ACEPTADO_PROVEEDOR`: Amarillo ámbar (`#F59E0B`).
       - `ENVIADO`: Azul cielo (`#38BDF8`).
       - `ENTREGADO`: Verde lima (`#22C55E`).
       - `LEIDO`: Oro Luxury (`#D4AF37`).
       - `FALLIDO`: Rojo carmesí (`#EF4444`).
     - Botón de acción: "Ver Detalle" (abre modal con tiempos de entrega y mensaje de error saneado) y "Reintentar" (solo para `ADMIN` en intentos fallidos).

---

## 2. Paleta de Colores y Tokens Visuales

| Token | Valor CSS | Propósito |
| :--- | :--- | :--- |
| `--color-bg-primary` | `#0A0F1D` | Fondo general de la aplicación |
| `--color-surface-card` | `#131B2E` | Fondo de tarjetas y tablas |
| `--color-gold` | `#D4AF37` | Acento principal y títulos destacados |
| `--color-gold-hover` | `#B89628` | Estado hover de botones primarios |
| `--color-border-subtle` | `rgba(212, 175, 55, 0.15)` | Bordes sutiles dorados |
| `--color-status-success` | `#10B981` | Éxito y entrega confirmada |
| `--color-status-danger` | `#EF4444` | Fallos de red o errores |
