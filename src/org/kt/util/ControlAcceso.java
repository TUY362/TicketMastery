package org.kt.util;

public final class ControlAcceso {

    public enum Modulo {
        EVENTOS,
        ZONAS,
        ASIENTOS,
        CLIENTES,
        VENTAS,
        VALIDACION
    }

    private ControlAcceso() {
    }

    public static boolean puedeAcceder(Modulo modulo) {
        SessionContext sesion = SessionContext.getInstancia();

        synchronized (sesion) {
            if (modulo == null || !sesion.haySesionActiva()) {
                return false;
            }

            String rol = sesion.getRol();

            if (rol == null) {
                return false;
            }

            return switch (rol) {
                case "ORGANIZADOR" ->
                    modulo == Modulo.EVENTOS
                    || modulo == Modulo.ZONAS
                    || modulo == Modulo.ASIENTOS;

                case "TAQUILLERO" ->
                    modulo == Modulo.CLIENTES
                    || modulo == Modulo.VENTAS;

                case "VALIDADOR" ->
                    modulo == Modulo.VALIDACION;

                default -> false;
            };
        }
    }

    public static void exigirAcceso(Modulo modulo) {
        if (!puedeAcceder(modulo)) {
            throw new SecurityException(
                    "No tienes permiso para acceder a este módulo."
            );
        }
    }
}