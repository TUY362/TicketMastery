package org.kt.controller;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kt.util.ControlAcceso;
import org.kt.util.ControlAcceso.Modulo;
import org.kt.util.SessionContext;

public class MenuPrincipalController {

    private static final Logger LOGGER =
            Logger.getLogger(MenuPrincipalController.class.getName());

    @FXML
    private Label lblUsuario;

    @FXML
    private Label lblRol;

    @FXML
    private Button btnEventos;

    @FXML
    private Button btnZonas;

    @FXML
    private Button btnAsientos;

    @FXML
    private Button btnClientes;

    @FXML
    private Button btnVentas;

    @FXML
    private Button btnValidacion;

    @FXML
    private StackPane contenedorPrincipal;

    @FXML
    private void initialize() {
        SessionContext sesion = SessionContext.getInstancia();

        if (!sesion.haySesionActiva()) {
            throw new SecurityException(
                    "Debes iniciar sesión para abrir el menú."
            );
        }

        lblUsuario.setText("Usuario: " + sesion.getNombreUsuario());
        lblRol.setText("Rol: " + sesion.getRol());

        configurarBoton(btnEventos, Modulo.EVENTOS);
        configurarBoton(btnZonas, Modulo.ZONAS);
        configurarBoton(btnAsientos, Modulo.ASIENTOS);
        configurarBoton(btnClientes, Modulo.CLIENTES);
        configurarBoton(btnVentas, Modulo.VENTAS);
        configurarBoton(btnValidacion, Modulo.VALIDACION);
    }

    private void configurarBoton(Button boton, Modulo modulo) {
        boolean permitido = ControlAcceso.puedeAcceder(modulo);

        boton.setVisible(permitido);
        boton.setManaged(permitido);
        boton.setDisable(!permitido);
    }

    @FXML
    private void abrirEventos() {
        abrirModulo(Modulo.EVENTOS, "Evento.fxml");
    }

    @FXML
    private void abrirZonas() {
        abrirModulo(Modulo.ZONAS, "ZonaLugar.fxml");
    }

    @FXML
    private void abrirAsientos() {
        abrirModulo(Modulo.ASIENTOS, "Asiento.fxml");
    }

    @FXML
    private void abrirClientes() {
        abrirModulo(Modulo.CLIENTES, "Cliente.fxml");
    }

    @FXML
    private void abrirVentas() {
        abrirModulo(Modulo.VENTAS, "Venta.fxml");
    }

    @FXML
    private void abrirValidacion() {
        abrirModulo(Modulo.VALIDACION, "Validacion.fxml");
    }

    private void abrirModulo(Modulo modulo, String archivo) {
        try {
            ControlAcceso.exigirAcceso(modulo);

            URL recurso = getClass().getResource(
                    "/org/kt/view/" + archivo
            );

            if (recurso == null) {
                mostrarMensaje(
                        Alert.AlertType.INFORMATION,
                        "Pantalla pendiente",
                        "Esta pantalla todavía no está creada."
                );
                return;
            }

            FXMLLoader loader = new FXMLLoader(recurso);
            Parent vista = loader.load();

            contenedorPrincipal.getChildren().setAll(vista);

        } catch (SecurityException excepcion) {
            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Acceso denegado",
                    excepcion.getMessage()
            );

        } catch (IOException excepcion) {
            LOGGER.log(
                    Level.SEVERE,
                    "No se pudo cargar " + archivo,
                    excepcion
            );

            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo abrir la pantalla."
            );
        }
    }

    @FXML
    private void cerrarSesion() {
        SessionContext.getInstancia().cerrarSesion();

        contenedorPrincipal.getChildren().clear();
        lblUsuario.setText("Sesión cerrada");
        lblRol.setText("");

        configurarBoton(btnEventos, Modulo.EVENTOS);
        configurarBoton(btnZonas, Modulo.ZONAS);
        configurarBoton(btnAsientos, Modulo.ASIENTOS);
        configurarBoton(btnClientes, Modulo.CLIENTES);
        configurarBoton(btnVentas, Modulo.VENTAS);
        configurarBoton(btnValidacion, Modulo.VALIDACION);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/kt/view/Login.fxml")
            );

            Parent login = loader.load();

            Stage ventana = (Stage) contenedorPrincipal
                    .getScene().getWindow();

            ventana.getScene().setRoot(login);
            ventana.setWidth(480);
            ventana.setHeight(540);
            ventana.centerOnScreen();

        } catch (IOException excepcion) {
            LOGGER.log(
                    Level.SEVERE,
                    "No se pudo cargar el login",
                    excepcion
            );

            mostrarMensaje(
                    Alert.AlertType.ERROR,
                    "Sesión cerrada",
                    "No se pudo abrir el login. Reinicia la aplicación."
            );
        }
    }

    private void mostrarMensaje(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.initOwner(contenedorPrincipal.getScene().getWindow());
        alerta.showAndWait();
    }
}