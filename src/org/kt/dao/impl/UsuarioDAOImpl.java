package org.kt.dao.impl;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kt.dao.UsuarioDAO;
import org.kt.exceptions.DBException;
import org.kt.model.Usuario;
import org.kt.util.Conexion;
import org.kt.util.ManejadorExcepciones;

public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public int insertar(Usuario usuario) throws DBException {
        validarHash(usuario);

        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_insertar_usuario(?, ?, ?, ?, ?)}")) {

                statement.setString(1, usuario.getNombre());
                statement.setString(2, usuario.getUsername());
                statement.setString(3, usuario.getPasswordHash());
                statement.setString(4, usuario.getRol());
                statement.setBoolean(5, usuario.isActivo());

                int id;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (!resultado.next()) {
                        throw new SQLException(
                                "No se devolvió el ID del usuario.");
                    }

                    id = resultado.getInt("id_usuario");
                }

                consumirResultados(statement);
                usuario.setIdUsuario(id);
                return id;

            } catch (SQLException e) {
                throw registrarError("No se pudo insertar el usuario.", e);
            }
        }
    }

    @Override
    public void actualizar(Usuario usuario) throws DBException {
        validarHash(usuario);

        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall(
                            "{call sp_actualizar_usuario(?, ?, ?, ?, ?, ?)}")) {

                statement.setInt(1, usuario.getIdUsuario());
                statement.setString(2, usuario.getNombre());
                statement.setString(3, usuario.getUsername());
                statement.setString(4, usuario.getPasswordHash());
                statement.setString(5, usuario.getRol());
                statement.setBoolean(6, usuario.isActivo());

                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo actualizar el usuario.", e);
            }
        }
    }

    @Override
    public void eliminar(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_eliminar_usuario(?)}")) {

                statement.setInt(1, id);
                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo eliminar el usuario.", e);
            }
        }
    }

    @Override
    public List<Usuario> listar() throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            List<Usuario> usuarios = new ArrayList<>();

            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_listar_usuario()}")) {

                try (ResultSet resultado = statement.executeQuery()) {
                    while (resultado.next()) {
                        usuarios.add(mapear(resultado, false));
                    }
                }

                consumirResultados(statement);
                return usuarios;

            } catch (SQLException e) {
                throw registrarError("No se pudieron listar los usuarios.", e);
            }
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_buscar_usuario(?)}")) {

                statement.setInt(1, id);
                Usuario usuario = null;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        usuario = mapear(resultado, false);
                    }
                }

                consumirResultados(statement);
                return Optional.ofNullable(usuario);

            } catch (SQLException e) {
                throw registrarError("No se pudo buscar el usuario.", e);
            }
        }
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username)
            throws DBException {

        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_buscar_usuario_login(?)}")) {

                statement.setString(1, username);
                Usuario usuario = null;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        usuario = mapear(resultado, true);
                    }
                }

                consumirResultados(statement);
                return Optional.ofNullable(usuario);

            } catch (SQLException e) {
                throw registrarError(
                        "No se pudo buscar el usuario para iniciar sesión.", e);
            }
        }
    }

    private Usuario mapear(ResultSet resultado, boolean paraLogin)
            throws SQLException {

        String passwordHash = paraLogin
                ? resultado.getString("password_hash")
                : null;

        boolean activo = paraLogin
                ? true
                : resultado.getBoolean("activo");

        return new Usuario(
                resultado.getInt("id_usuario"),
                resultado.getString("nombre"),
                resultado.getString("username"),
                passwordHash,
                resultado.getString("rol"),
                activo
        );
    }

    private void validarHash(Usuario usuario) throws DBException {
        if (usuario.getPasswordHash() == null
                || usuario.getPasswordHash().isBlank()) {
            throw new DBException(
                    "Debes proporcionar el hash de la contraseña.");
        }
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