package org.kt.dao.impl;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kt.dao.ZonaLugarDAO;
import org.kt.exceptions.DBException;
import org.kt.model.ZonaLugar;
import org.kt.util.Conexion;
import org.kt.util.ManejadorExcepciones;

public class ZonaLugarDAOImpl implements ZonaLugarDAO {

    @Override
    public int insertar(ZonaLugar zona) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_insertar_zonalugar(?, ?, ?)}")) {

                statement.setInt(1, zona.getIdEvento());
                statement.setString(2, zona.getNombre());
                statement.setBigDecimal(3, zona.getPrecio());

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        int id = resultado.getInt("id_zona");
                        zona.setIdZona(id);
                        return id;
                    }

                    throw new SQLException(
                            "El procedimiento no devolvió el ID de la zona.");
                }
            } catch (SQLException e) {
                throw registrarError("No se pudo insertar la zona.", e);
            }
        }
    }

    @Override
    public void actualizar(ZonaLugar zona) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_actualizar_zonalugar(?, ?, ?)}")) {

                statement.setInt(1, zona.getIdZona());
                statement.setString(2, zona.getNombre());
                statement.setBigDecimal(3, zona.getPrecio());

                statement.execute();
            } catch (SQLException e) {
                throw registrarError("No se pudo actualizar la zona.", e);
            }
        }
    }

    @Override
    public void eliminar(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_eliminar_zonalugar(?)}")) {

                statement.setInt(1, id);
                statement.execute();
            } catch (SQLException e) {
                throw registrarError("No se pudo eliminar la zona.", e);
            }
        }
    }

    @Override
    public List<ZonaLugar> listar() throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            List<ZonaLugar> zonas = new ArrayList<>();

            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_listar_zonalugar()}");
                    ResultSet resultado = statement.executeQuery()) {

                while (resultado.next()) {
                    zonas.add(mapear(resultado));
                }

                return zonas;
            } catch (SQLException e) {
                throw registrarError("No se pudieron listar las zonas.", e);
            }
        }
    }

    @Override
    public Optional<ZonaLugar> buscarPorId(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_buscar_zonalugar(?)}")) {

                statement.setInt(1, id);

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        return Optional.of(mapear(resultado));
                    }

                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw registrarError("No se pudo buscar la zona.", e);
            }
        }
    }

    private ZonaLugar mapear(ResultSet resultado) throws SQLException {
        return new ZonaLugar(
                resultado.getInt("id_zona"),
                resultado.getInt("id_evento"),
                resultado.getString("nombre"),
                resultado.getBigDecimal("precio")
        );
    }

    private DBException registrarError(
            String mensaje, SQLException causa) {
        ManejadorExcepciones.manejar(mensaje, causa);
        return new DBException(mensaje, causa);
    }
}