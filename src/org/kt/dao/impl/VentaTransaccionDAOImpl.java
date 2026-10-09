package org.kt.dao.impl;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kt.dao.VentaTransaccionDAO;
import org.kt.exceptions.DBException;
import org.kt.model.VentaTransaccion;
import org.kt.util.Conexion;
import org.kt.util.ManejadorExcepciones;

public class VentaTransaccionDAOImpl implements VentaTransaccionDAO {

    @Override
    public int insertar(VentaTransaccion venta) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_insertar_ventatransaccion(?, ?)}")) {

                statement.setInt(1, venta.getIdCliente());
                statement.setInt(2, venta.getIdTaquillero());

                int id;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (!resultado.next()) {
                        throw new SQLException(
                                "No se devolvió el ID de la venta.");
                    }

                    id = resultado.getInt("id_venta");
                }

                consumirResultados(statement);
                venta.setIdVenta(id);
                venta.setEstado("PENDIENTE");
                venta.setReferenciaPago(null);
                venta.setFechaPago(null);
                venta.setFechaCreacion(null);
                return id;

            } catch (SQLException e) {
                throw registrarError("No se pudo insertar la venta.", e);
            }
        }
    }

    @Override
    public void actualizar(VentaTransaccion venta) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall(
                            "{call sp_actualizar_ventatransaccion(?, ?)}")) {

                statement.setInt(1, venta.getIdVenta());
                statement.setInt(2, venta.getIdCliente());

                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo actualizar la venta.", e);
            }
        }
    }

    @Override
    public void eliminar(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_eliminar_ventatransaccion(?)}")) {

                statement.setInt(1, id);
                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo eliminar la venta.", e);
            }
        }
    }

    @Override
    public List<VentaTransaccion> listar() throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            List<VentaTransaccion> ventas = new ArrayList<>();

            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_listar_ventatransaccion()}")) {

                try (ResultSet resultado = statement.executeQuery()) {
                    while (resultado.next()) {
                        ventas.add(mapear(resultado));
                    }
                }

                consumirResultados(statement);
                return ventas;

            } catch (SQLException e) {
                throw registrarError("No se pudieron listar las ventas.", e);
            }
        }
    }

    @Override
    public Optional<VentaTransaccion> buscarPorId(int id)
            throws DBException {

        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_buscar_ventatransaccion(?)}")) {

                statement.setInt(1, id);
                VentaTransaccion venta = null;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        venta = mapear(resultado);
                    }
                }

                consumirResultados(statement);
                return Optional.ofNullable(venta);

            } catch (SQLException e) {
                throw registrarError("No se pudo buscar la venta.", e);
            }
        }
    }

    private VentaTransaccion mapear(ResultSet resultado)
            throws SQLException {

        Timestamp fechaCreacion =
                resultado.getTimestamp("fecha_creacion");

        Timestamp fechaPago =
                resultado.getTimestamp("fecha_pago");

        return new VentaTransaccion(
                resultado.getInt("id_venta"),
                resultado.getInt("id_cliente"),
                resultado.getInt("id_taquillero"),
                fechaCreacion == null
                        ? null
                        : fechaCreacion.toLocalDateTime(),
                resultado.getString("estado"),
                resultado.getString("referencia_pago"),
                fechaPago == null
                        ? null
                        : fechaPago.toLocalDateTime()
        );
    }

    private void consumirResultados(CallableStatement statement)
            throws SQLException {

        while (true) {
            boolean hayResultado = statement.getMoreResults(
                    CallableStatement.CLOSE_CURRENT_RESULT);

            if (!hayResultado && statement.getUpdateCount() == -1) {
                break;
            }
        }
    }

    private DBException registrarError(
            String mensaje, SQLException causa) {
        ManejadorExcepciones.manejar(mensaje, causa);
        return new DBException(mensaje, causa);
    }
}