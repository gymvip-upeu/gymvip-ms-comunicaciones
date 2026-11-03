package com.gymvip.comms.domain;

/**
 * Estados observables del ciclo de vida de una comunicación.
 * HU-17 T05 y HU-18 T01.
 */
public enum CommunicationStatus {
    SOLICITADO,
    ACEPTADO_PROVEEDOR,
    ENVIADO,
    ENTREGADO,
    LEIDO,
    FALLIDO,
    DESCONOCIDO
}
