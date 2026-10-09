package org.kt.dao;

import org.kt.exceptions.DBException;
import org.kt.model.Asiento;

public interface AsientoDAO extends CrudDAO<Asiento> {

    Asiento reservar(int idAsiento) throws DBException;

    void liberarReserva(int idAsiento, String tokenReserva)
            throws DBException;
}