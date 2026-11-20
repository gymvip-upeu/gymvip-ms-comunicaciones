# Integración E03 MA/RM

Rama de integración compartida con backend y móvil. Se incorporaron las mejoras de Acosta conservando el recorrido de FAQ y recordatorios E03.

Se conserva la plantilla vencido de cuatro parámetros y la distinción de correo simulado. La consulta utiliza Criteria API de Acosta y validación de filtros/paginación. COMMS_SERVICE_TOKEN debe coincidir con el backend; los proveedores de QA están en simulation. La suite y package pasaron (22 pruebas).

Para configuración y compatibilidad de base consultar docs/INTEGRACION_E03.md del backend. Firebase Android requiere google-services.json del equipo; no se incluyó ningún archivo privado ni credenciales en estos repositorios.
