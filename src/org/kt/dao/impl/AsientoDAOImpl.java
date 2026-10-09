package org.kt.dao.impl;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kt.dao.AsientoDAO;
import org.kt.exceptions.DBException;
import org.kt.model.Asiento;
import org.kt.util.Conexion;
import org.kt.util.ManejadorExcepciones;

public class AsientoDAOImpl implements AsientoDAO {

    @Override
    public int insertar(Asiento asiento) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_insertar_asiento(?, ?, ?)}")) {

                statement.setInt(1, asiento.getIdZona());
                statement.setString(2, asiento.getFila());
                statement.setInt(3, asiento.getNumero());

                int id;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (!resultado.next()) {
                        throw new SQLException(
                                "No se devolvió el ID del asiento.");
                    }

                    id = resultado.getInt("id_asiento");
                }

                consumirResultados(statement);
                asiento.setIdAsiento(id);
                return id;

            } catch (SQLException e) {
                throw registrarError("No se pudo insertar el asiento.", e);
            }
        }
    }

    @Override
    public void actualizar(Asiento asiento) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_actualizar_asiento(?, ?, ?)}")) {

                statement.setInt(1, asiento.getIdAsiento());
                statement.setString(2, asiento.getFila());
                statement.setInt(3, asiento.getNumero());

                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo actualizar el asiento.", e);
            }
        }
    }

    @Override
    public void eliminar(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_eliminar_asiento(?)}")) {

                statement.setInt(1, id);
                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo eliminar el asiento.", e);
            }
        }
    }

    @Override
    public List<Asiento> listar() throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            List<Asiento> asientos = new ArrayList<>();

            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_listar_asiento()}")) {

                try (ResultSet resultado = statement.executeQuery()) {
                    while (resultado.next()) {
                        asientos.add(mapear(resultado));
                    }
                }

                consumirResultados(statement);
                return asientos;

            } catch (SQLException e) {
                throw registrarError("No se pudieron listar los asientos.", e);
            }
        }
    }

    @Override
    public Optional<Asiento> buscarPorId(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_buscar_asiento(?)}")) {

                statement.setInt(1, id);
                Asiento asiento = null;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        asiento = mapear(resultado);
                    }
                }

                consumirResultados(statement);
                return Optional.ofNullable(asiento);

            } catch (SQLException e) {
                throw registrarError("No se pudo buscar el asiento.", e);
            }
        }
    }

    @Override
    public Asiento reservar(int idAsiento) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            Asiento asiento = buscarPorId(idAsiento).orElseThrow(
                    () -> new DBException("El asiento no existe.")
            );

            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_reservar_asiento(?)}")) {

                statement.setInt(1, idAsiento);

                try (ResultSet resultado = statement.executeQuery()) {
                    if (!resultado.next()) {
                        throw new SQLException(
                                "No se devolvieron los datos de la reserva.");
                    }

                    asiento.setTokenReserva(
                            resultado.getString("token_reserva"));

                    Timestamp vencimiento =
                            resultado.getTimestamp("reserva_hasta");

                    asiento.setReservaHasta(
                            vencimiento == null
                                    ? null
                                    : vencimiento.toLocalDateTime());
                }

                consumirResultados(statement);
                return asiento;

            } catch (SQLException e) {
                throw registrarError("No se pudo reservar el asiento.", e);
            }
        }
    }

    @Override
    public void liberarReserva(int idAsiento, String tokenReserva)
            throws DBException {

        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_liberar_reserva(?, ?)}")) {

                statement.setInt(1, idAsiento);
                statement.setString(2, tokenReserva);

                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo liberar la reserva.", e);
            }
        }
    }

    private Asiento mapear(ResultSet resultado) throws SQLException {
        Timestamp vencimiento = resultado.getTimestamp("reserva_hasta");

        return new Asiento(
                resultado.getInt("id_asiento"),
                resultado.getInt("id_zona"),
                resultado.getString("fila"),
                resultado.getInt("numero"),
                resultado.getString("token_reserva"),
                vencimiento == null
                        ? null
                        : vencimiento.toLocalDateTime()
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