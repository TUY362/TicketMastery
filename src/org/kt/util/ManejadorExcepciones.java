package org.kt.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ManejadorExcepciones {

    private static final Logger LOGGER =
            Logger.getLogger(ManejadorExcepciones.class.getName());

    private static boolean instalado;

    private ManejadorExcepciones() {
    }

    public static synchronized void instalar() {
        if (instalado) {
            return;
        }

        Thread.setDefaultUncaughtExceptionHandler(
            (hilo, error) -> LOGGER.log(
                Level.SEVERE,
                "Error no controlado en el hilo " + hilo.getName(),
                error
            )
        );

        instalado = true;
    }

    public static void manejar(String mensaje, Throwable error) {
        LOGGER.log(Level.SEVERE, mensaje, error);
    }

    public static void informar(String mensaje) {
        LOGGER.log(Level.INFO, mensaje);
    }
}