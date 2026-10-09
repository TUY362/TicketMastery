package org.kt.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Conexion {

    private static Conexion instancia;
    private Connection conexion;

    private Conexion() {
          ManejadorExcepciones.instalar();  
    }

    public static synchronized Conexion getInstancia() {
        if (instancia == null) {
            instancia = new Conexion();
        }
        return instancia;
    }

    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            Properties propiedades = new Properties();

            try (InputStream archivo =
                    Conexion.class.getResourceAsStream("/db.properties")) {

                if (archivo == null) {
                    throw new SQLException("No se encontró db.properties.");
                }

                propiedades.load(archivo);

            } catch (IOException e) {
                throw new SQLException("No se pudo leer db.properties.", e);
            }

            String url = propiedades.getProperty("db.url");
            String usuario = propiedades.getProperty("db.user");
            String password = propiedades.getProperty("db.password");

            if (url == null || url.isBlank()
                    || usuario == null || usuario.isBlank()
                    || password == null) {
                throw new SQLException(
                    "Faltan propiedades de conexión en db.properties."
                );
            }

            conexion = DriverManager.getConnection(url, usuario, password);
        }

        return conexion;
    }

    public synchronized void cerrarConexion() throws SQLException {
        if (conexion != null) {
            try {
                conexion.close();
            } finally {
                conexion = null;
            }
        }
    }
}