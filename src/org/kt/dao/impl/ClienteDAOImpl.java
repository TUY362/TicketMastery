package org.kt.dao.impl;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kt.dao.ClienteDAO;
import org.kt.exceptions.DBException;
import org.kt.model.Cliente;
import org.kt.util.Conexion;
import org.kt.util.ManejadorExcepciones;

public class ClienteDAOImpl implements ClienteDAO {

    @Override
    public int insertar(Cliente cliente) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_insertar_cliente(?, ?, ?)}")) {

                statement.setString(1, cliente.getNombre());
                statement.setString(2, cliente.getCorreo());
                statement.setString(3, cliente.getTelefono());

                int id;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (!resultado.next()) {
                        throw new SQLException(
                                "No se devolvió el ID del cliente.");
                    }

                    id = resultado.getInt("id_cliente");
                }

                consumirResultados(statement);
                cliente.setIdCliente(id);
                return id;

            } catch (SQLException e) {
                throw registrarError("No se pudo insertar el cliente.", e);
            }
        }
    }

    @Override
    public void actualizar(Cliente cliente) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_actualizar_cliente(?, ?, ?, ?)}")) {

                statement.setInt(1, cliente.getIdCliente());
                statement.setString(2, cliente.getNombre());
                statement.setString(3, cliente.getCorreo());
                statement.setString(4, cliente.getTelefono());

                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo actualizar el cliente.", e);
            }
        }
    }

    @Override
    public void eliminar(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_eliminar_cliente(?)}")) {

                statement.setInt(1, id);
                statement.execute();
                consumirResultados(statement);

            } catch (SQLException e) {
                throw registrarError("No se pudo eliminar el cliente.", e);
            }
        }
    }

    @Override
    public List<Cliente> listar() throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            List<Cliente> clientes = new ArrayList<>();

            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_listar_cliente()}")) {

                try (ResultSet resultado = statement.executeQuery()) {
                    while (resultado.next()) {
                        clientes.add(mapear(resultado));
                    }
                }

                consumirResultados(statement);
                return clientes;

            } catch (SQLException e) {
                throw registrarError("No se pudieron listar los clientes.", e);
            }
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) throws DBException {
        Conexion gestor = Conexion.getInstancia();

        synchronized (gestor) {
            try (CallableStatement statement = gestor.getConexion()
                    .prepareCall("{call sp_buscar_cliente(?)}")) {

                statement.setInt(1, id);
                Cliente cliente = null;

                try (ResultSet resultado = statement.executeQuery()) {
                    if (resultado.next()) {
                        cliente = mapear(resultado);
                    }
                }

                consumirResultados(statement);
                return Optional.ofNullable(cliente);

            } catch (SQLException e) {
                throw registrarError("No se pudo buscar el cliente.", e);
            }
        }
    }

    private Cliente mapear(ResultSet resultado) throws SQLException {
        return new Cliente(
                resultado.getInt("id_cliente"),
                resultado.getString("nombre"),
                resultado.getString("correo"),
                resultado.getString("telefono")
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