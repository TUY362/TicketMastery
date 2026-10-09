package org.kt.dao.impl;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kt.dao.EventoDAO;
import org.kt.exceptions.DBException;
import org.kt.model.Evento;
import org.kt.util.Conexion;
import org.kt.util.ManejadorExcepciones;

public class EventoDAOImpl implements EventoDAO {

    @Override
    public int insertar(Evento evento) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_insertar_evento(?, ?, ?, ?)}")) {

                statement.setString(1, evento.getNombre());
                statement.setTimestamp(
                        2, Timestamp.valueOf(evento.getFechaHora()));
                statement.setString(3, evento.getLugar());
                statement.setBoolean(4, evento.isActivo());

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        int id = resultado.getInt("id_evento");
                        evento.setIdEvento(id);
                        return id;
                    }

                    throw new SQLException(
                            "El procedimiento no devolvió el ID del evento.");
                }
            } catch (SQLException e) {
                throw registrarError("No se pudo insertar el evento.", e);
            }
        }
    }

    @Override
    public void actualizar(Evento evento) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall(
                            "{call sp_actualizar_evento(?, ?, ?, ?, ?)}")) {

                statement.setInt(1, evento.getIdEvento());
                statement.setString(2, evento.getNombre());
                statement.setTimestamp(
                        3, Timestamp.valueOf(evento.getFechaHora()));
                statement.setString(4, evento.getLugar());
                statement.setBoolean(5, evento.isActivo());

                statement.execute();
            } catch (SQLException e) {
                throw registrarError("No se pudo actualizar el evento.", e);
            }
        }
    }

    @Override
    public void eliminar(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_eliminar_evento(?)}")) {

                statement.setInt(1, id);
                statement.execute();
            } catch (SQLException e) {
                throw registrarError("No se pudo eliminar el evento.", e);
            }
        }
    }

    @Override
    public List<Evento> listar() throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            List<Evento> eventos = new ArrayList<>();

            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_listar_evento()}");
                    ResultSet resultado = statement.executeQuery()) {

                while (resultado.next()) {
                    eventos.add(mapear(resultado));
                }

                return eventos;
            } catch (SQLException e) {
                throw registrarError("No se pudieron listar los eventos.", e);
            }
        }
    }

    @Override
    public Optional<Evento> buscarPorId(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_buscar_evento(?)}")) {

                statement.setInt(1, id);

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        return Optional.of(mapear(resultado));
                    }

                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw registrarError("No se pudo buscar el evento.", e);
            }
        }
    }

    private Evento mapear(ResultSet resultado) throws SQLException {
        return new Evento(
                resultado.getInt("id_evento"),
                resultado.getString("nombre"),
                resultado.getTimestamp("fecha_hora").toLocalDateTime(),
                resultado.getString("lugar"),
                resultado.getBoolean("activo")
        );
    }

    private DBException registrarError(
            String mensaje, SQLException causa) {
        ManejadorExcepciones.manejar(mensaje, causa);
        return new DBException(mensaje, causa);
    }
}