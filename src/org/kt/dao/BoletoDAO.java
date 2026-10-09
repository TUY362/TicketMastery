package org.kt.dao;

import java.util.List;
import java.util.Optional;
import org.kt.exceptions.DBException;
import org.kt.model.Boleto;

public interface BoletoDAO {

    Boleto insertar(int idVenta, int idAsiento,
            String tokenReserva, String referenciaPago)
            throws DBException;

    Boleto actualizar(String codigo, int idValidador)
            throws DBException;

    void eliminar(int idBoleto) throws DBException;

    List<Boleto> listar() throws DBException;

    Optional<Boleto> buscarPorId(int idBoleto)
            throws DBException;
}