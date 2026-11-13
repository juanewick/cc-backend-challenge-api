package com.cc.brokerage.adapter.in.web;

import java.util.Map;

final class ErrorCodes {

    private static final Map<String, String> BY_MESSAGE = Map.ofEntries(
            Map.entry("La cuenta no existe", "ACCOUNT_NOT_FOUND"),
            Map.entry("El activo no existe", "INSTRUMENT_NOT_FOUND"),
            Map.entry("La moneda no es operable", "INSTRUMENT_NOT_TRADABLE"),
            Map.entry("La orden no existe", "ORDER_NOT_FOUND"),
            Map.entry("La orden no pertenece a esa cuenta", "ORDER_NOT_OWNED"),
            Map.entry("Solo se cancelan órdenes abiertas", "ORDER_NOT_OPEN"),
            Map.entry("La cuenta no es válida", "INVALID_ACCOUNT"),
            Map.entry("El ticker no es válido", "INVALID_TICKER"),
            Map.entry("La orden no es válida", "INVALID_ORDER_ID"),
            Map.entry("La búsqueda no tiene texto", "SEARCH_EMPTY"),
            Map.entry("La búsqueda supera el máximo permitido", "SEARCH_TOO_LONG"),
            Map.entry("La búsqueda no es válida", "SEARCH_INVALID"),
            Map.entry("El lado debe ser BUY o SELL", "INVALID_SIDE"),
            Map.entry("El tipo de orden es obligatorio", "INVALID_TYPE"),
            Map.entry("La cantidad debe ser un entero de 1 a 2147483647", "INVALID_SIZE"),
            Map.entry("El monto debe ser mayor que cero", "INVALID_AMOUNT"),
            Map.entry("El monto supera el máximo permitido", "INVALID_AMOUNT"),
            Map.entry("El monto no es un número válido", "INVALID_AMOUNT"),
            Map.entry("El precio es obligatorio y debe ser mayor que cero", "INVALID_PRICE"),
            Map.entry("El precio supera el máximo permitido", "INVALID_PRICE"),
            Map.entry("El precio no es un número válido", "INVALID_PRICE"),
            Map.entry("El importe debe ser mayor que cero", "INVALID_AMOUNT"),
            Map.entry("El importe supera el máximo permitido", "INVALID_AMOUNT"),
            Map.entry("Debe indicarse exactamente una forma de cantidad: acciones o monto", "EXCLUSIVE_QUANTITY"),
            Map.entry("No se registran ingresos ni egresos de pesos nuevos", "CASH_MOVEMENT")
    );

    private ErrorCodes() {
    }

    static String ofMessage(String message) {
        if (message == null) {
            return "UNREADABLE";
        }
        return BY_MESSAGE.getOrDefault(message, "INVALID_REQUEST");
    }
}
