package org.kt.controller;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kt.dao.UsuarioDAO;
import org.kt.dao.impl.UsuarioDAOImpl;
import org.kt.model.Usuario;
import org.kt.util.PasswordUtil;
import org.kt.util.SessionContext;

public class LoginController {

    private static final Logger LOGGER =
            Logger.getLogger(LoginController.class.getName());

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMensaje;

    @FXML
    private Button btnIngresar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    @FXML
    private void initialize() {
        txtUsuario.setOnAction(event -> txtPassword.requestFocus());
        txtPassword.setOnAction(event -> iniciarSesion());
    }

    @FXML
    private void iniciarSesion() {
        if (btnIngresar.isDisabled()) {
            return;
        }

        String username = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        lblMensaje.setText("");

        if (username.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Ingresa tu usuario y contraseña.");
            return;
        }

        cambiarEstado(true);
        lblMensaje.setText("Verificando credenciales...");

        Task<Optional<Usuario>> tarea = new Task<>() {

            @Override
            protected Optional<Usuario> call() throws Exception {
                Optional<Usuario> encontrado =
                        usuarioDAO.buscarPorUsername(username);

                if (encontrado.isEmpty()) {
                    return Optional.empty();
                }

                Usuario usuario = encontrado.get();

                boolean passwordValida = PasswordUtil.verificar(
                        password,
                        usuario.getPasswordHash()
                );

                return passwordValida
                        ? Optional.of(usuario)
                        : Optional.empty();
            }
        };

        tarea.setOnSucceeded(event -> {
            cambiarEstado(false);
            txtPassword.clear();

            Optional<Usuario> resultado = tarea.getValue();

            if (resultado.isEmpty()) {
                lblMensaje.setText("Usuario o contraseña incorrectos.");
                txtPassword.requestFocus();
                return;
            }

            Usuario usuario = resultado.get();

            try {
                SessionContext.getInstancia().iniciarSesion(
                        usuario.getIdUsuario(),
                        usuario.getNombre(),
                        usuario.getRol()
                );

                mostrarInicio();

            } catch (IllegalArgumentException excepcion) {
                SessionContext.getInstancia().cerrarSesion();

                lblMensaje.setText(
                        "La cuenta no tiene un rol válido."
                );

                LOGGER.log(
                        Level.WARNING,
                        "Sesión rechazada",
                        excepcion
                );
            }
        });

        tarea.setOnFailed(event -> {
            cambiarEstado(false);
            txtPassword.clear();

            lblMensaje.setText(
                    "No se pudo iniciar sesión. Revisa la conexión."
            );

            LOGGER.log(
                    Level.SEVERE,
                    "Error al iniciar sesión",
                    tarea.getException()
            );
        });

        Thread hilo = new Thread(tarea, "autenticacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cambiarEstado(boolean ocupado) {
        btnIngresar.setDisable(ocupado);
        txtUsuario.setDisable(ocupado);
        txtPassword.setDisable(ocupado);
    }

    private void mostrarInicio() {
        try {
            java.net.URL recurso = getClass().getResource(
                    "/org/kt/view/MenuPrincipal.fxml"
            );

            if (recurso == null) {
                throw new IOException(
                        "No se encontró MenuPrincipal.fxml."
                );
            }

            FXMLLoader loader = new FXMLLoader(recurso);
            Parent menu = loader.load();

            Stage ventana = (Stage) txtUsuario
                    .getScene().getWindow();

            ventana.getScene().setRoot(menu);
            ventana.setWidth(1000);
            ventana.setHeight(650);
            ventana.centerOnScreen();

        } catch (IOException excepcion) {
            SessionContext.getInstancia().cerrarSesion();

            lblMensaje.setText(
                    "No se pudo abrir el menú principal."
            );

            LOGGER.log(
                    Level.SEVERE,
                    "Error al cargar el menú principal",
                    excepcion
            );
        }
    }
}