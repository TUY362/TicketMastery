package org.kt.dao.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kt.dao.BoletoDAO;
import org.kt.exceptions.DBException;
import org.kt.model.Boleto;
import org.kt.util.Conexion;

public class BoletoDAOImpl implements BoletoDAO {

    @Override
    public Boleto insertar(
            int idVenta,
            int idAsiento,
            String tokenReserva,
            String referenciaPago) throws DBException {

        try {
            Connection conexion = Conexion.getInstancia().getConexion();

            synchronized (conexion) {
                int idBoleto;

                try (CallableStatement statement = conexion.prepareCall(
                        "{CALL sp_insertar_boleto(?, ?, ?, ?)}")) {

                    statement.setInt(1, idVenta);
                    statement.setInt(2, idAsiento);
                    statement.setString(3, tokenReserva);
                    statement.setString(4, referenciaPago);

                    try (ResultSet resultado = statement.executeQuery()) {
                        if (!resultado.next()) {
                            throw new DBException(
                                    "No se recibió el identificador del boleto.");
                        }

                        idBoleto = resultado.getInt("id_boleto");
                    }

                    consumirResultados(statement);
                }

                return buscarPorId(idBoleto).orElseThrow(
                        () -> new DBException(
                                "No se encontró el boleto registrado."));
            }

        } catch (SQLException excepcion) {
            throw new DBException(
                    "No se pudo registrar el boleto.", excepcion);
        }
    }

    @Override
    public Boleto actualizar(
            String codigo,
            int idValidador) throws DBException {

        try {
            Connection conexion = Conexion.getInstancia().getConexion();

            synchronized (conexion) {
                int idBoleto;

                try (CallableStatement statement = conexion.prepareCall(
                        "{CALL sp_actualizar_boleto(?, ?)}")) {

                    statement.setString(1, codigo);
                    statement.setInt(2, idValidador);

                    try (ResultSet resultado = statement.executeQuery()) {
                        if (!resultado.next()) {
                            throw new DBException(
                                    "No se recibió el boleto validado.");
                        }

                        idBoleto = resultado.getInt("id_boleto");
                    }

                    consumirResultados(statement);
                }

                return buscarPorId(idBoleto).orElseThrow(
                        () -> new DBException(
                                "No se encontró el boleto validado."));
            }

        } catch (SQLException excepcion) {
            throw new DBException(
                    "No se pudo validar el boleto.", excepcion);
        }
    }

    @Override
    public void eliminar(int idBoleto) throws DBException {

        try {
            Connection conexion = Conexion.getInstancia().getConexion();

            synchronized (conexion) {
                try (CallableStatement statement = conexion.prepareCall(
                        "{CALL sp_eliminar_boleto(?)}")) {

                    statement.setInt(1, idBoleto);
                    statement.execute();

                    consumirResultados(statement);
                }
            }

        } catch (SQLException excepcion) {
            throw new DBException(
                    "No se pudo anular el boleto.", excepcion);
        }
    }

    @Override
    public List<Boleto> listar() throws DBException {
        List<Boleto> boletos = new ArrayList<>();

        try {
            Connection conexion = Conexion.getInstancia().getConexion();

            synchronized (conexion) {
                try (CallableStatement statement = conexion.prepareCall(
                        "{CALL sp_listar_boleto()}")) {

                    try (ResultSet resultado = statement.executeQuery()) {
                        while (resultado.next()) {
                            boletos.add(mapear(resultado));
                        }
                    }

                    consumirResultados(statement);
                }
            }

        } catch (SQLException excepcion) {
            throw new DBException(
                    "No se pudieron listar los boletos.", excepcion);
        }

        return boletos;
    }

    @Override
    public Optional<Boleto> buscarPorId(int idBoleto) throws DBException {
        Boleto boleto = null;

        try {
            Connection conexion = Conexion.getInstancia().getConexion();

            synchronized (conexion) {
                try (CallableStatement statement = conexion.prepareCall(
                        "{CALL sp_buscar_boleto(?)}")) {

                    statement.setInt(1, idBoleto);

                    try (ResultSet resultado = statement.executeQuery()) {
                        if (resultado.next()) {
                            boleto = mapear(resultado);
                        }
                    }

                    consumirResultados(statement);
                }
            }

        } catch (SQLException excepcion) {
            throw new DBException(
                    "No se pudo buscar el boleto.", excepcion);
        }

        return Optional.ofNullable(boleto);
    }

    private Boleto mapear(ResultSet resultado) throws SQLException {
        Boleto boleto = new Boleto();

        boleto.setIdBoleto(resultado.getInt("id_boleto"));
        boleto.setIdVenta(resultado.getInt("id_venta"));
        boleto.setIdAsiento(resultado.getInt("id_asiento"));
        boleto.setCodigo(resultado.getString("codigo"));
        boleto.setPrecioPagado(resultado.getBigDecimal("precio_pagado"));
        boleto.setEstado(resultado.getString("estado"));

        Timestamp fechaEmision = resultado.getTimestamp("fecha_emision");

        boleto.setFechaEmision(
                fechaEmision == null
                        ? null
                        : fechaEmision.toLocalDateTime());

        Timestamp fechaValidacion = resultado.getTimestamp(
                "fecha_validacion");

        boleto.setFechaValidacion(
                fechaValidacion == null
                        ? null
                        : fechaValidacion.toLocalDateTime());

        int idValidador = resultado.getInt("id_validador");

        boleto.setIdValidador(
                resultado.wasNull() ? null : idValidador);

        return boleto;
    }

    private void consumirResultados(
            CallableStatement statement) throws SQLException {

        while (true) {
            boolean tieneResultado = statement.getMoreResults(
                    Statement.CLOSE_CURRENT_RESULT);

            if (!tieneResultado && statement.getUpdateCount() == -1) {
                break;
            }
        }
    }
}