package org.kt.exceptions;

public class DBException extends AppException {

    public DBException(String mensaje) {
        super(mensaje);
    }

    public DBException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}