package org.kt.controller;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.kt.dao.BoletoDAO;
import org.kt.dao.impl.BoletoDAOImpl;
import org.kt.model.Boleto;
import org.kt.util.Conexion;
import org.kt.util.ControlAcceso;
import org.kt.util.ControlAcceso.Modulo;
import org.kt.util.SessionContext;

public class ValidacionController {

    private static final Logger LOGGER =
            Logger.getLogger(ValidacionController.class.getName());

    @FXML
    private Label lblValidador;

    @FXML
    private TextField txtCodigo;

    @FXML
    private Button btnValidar;

    @FXML
    private Button btnLimpiar;

    @FXML
    private Label lblMensaje;

    @FXML
    private VBox panelResultado;

    @FXML
    private Label lblDetalle;

    private final BoletoDAO boletoDAO = new BoletoDAOImpl();

    private boolean ocupado;
    private int idValidador;

    @FXML
    private void initialize() {
        ControlAcceso.exigirAcceso(Modulo.VALIDACION);

        SessionContext sesion = SessionContext.getInstancia();

        idValidador = sesion.getIdUsuario();

        lblValidador.setText(
                "Validador: " + sesion.getNombreUsuario()
        );
    }

    @FXML
    private void validar() {
        if (ocupado) {
            return;
        }

        ocultarResultado();

        try {
            comprobarAcceso();
        } catch (SecurityException excepcion) {
            mostrarMensaje(excepcion.getMessage(), true);
            return;
        }

        String codigo = txtCodigo.getText().trim();

        if (!codigo.matches("[A-Za-z0-9]{32}")) {
            mostrarMensaje(
                    "Ingresa el código completo de 32 "
                            + "caracteres alfanuméricos.",
                    true
            );

            txtCodigo.requestFocus();
            return;
        }

        cambiarEstado(true);
        mostrarMensaje("Validando boleto...", false);

        Task<Boleto> tarea = new Task<>() {

            @Override
            protected Boleto call() throws Exception {
                Conexion gestor = Conexion.getInstancia();

                synchronized (gestor) {
                    synchronized (gestor.getConexion()) {
                        comprobarAcceso();

                        return boletoDAO.actualizar(
                                codigo,
                                idValidador
                        );
                    }
                }
            }
        };

        tarea.setOnSucceeded(event -> {
            cambiarEstado(false);

            try {
                comprobarAcceso();
            } catch (SecurityException excepcion) {
                mostrarMensaje(excepcion.getMessage(), true);
                return;
            }

            Boleto boleto = tarea.getValue();

            if (boleto == null
                    || !"UTILIZADO".equals(boleto.getEstado())
                    || boleto.getFechaValidacion() == null) {

                mostrarMensaje(
                        "No se pudo confirmar el estado del boleto. "
                                + "Verifica su estado antes de permitir "
                                + "el ingreso.",
                        true
                );

                return;
            }

            lblDetalle.setText(
                    "Boleto: " + boleto.getIdBoleto()
                    + "\nVenta: " + boleto.getIdVenta()
                    + "\nAsiento ID: " + boleto.getIdAsiento()
                    + "\nCódigo: " + boleto.getCodigo()
                    + "\nEstado: " + boleto.getEstado()
                    + "\nFecha de validación (UTC): "
                    + boleto.getFechaValidacion()
                            .toString().replace('T', ' ')
            );

            panelResultado.setVisible(true);
            panelResultado.setManaged(true);

            mostrarMensaje(
                    "Ingreso autorizado. Boleto marcado como utilizado.",
                    false
            );

            txtCodigo.clear();
            txtCodigo.requestFocus();
        });

        tarea.setOnFailed(event -> {
            cambiarEstado(false);
            ocultarResultado();

            mostrarMensaje(
                    "No se confirmó la entrada. El boleto puede "
                            + "ser inválido, estar utilizado o anulado, "
                            + "o existir un problema de conexión. "
                            + "Verifica su estado antes de permitir "
                            + "el ingreso.",
                    true
            );

            LOGGER.log(
                    Level.SEVERE,
                    "No se pudo confirmar la validación del boleto.",
                    tarea.getException()
            );

            txtCodigo.selectAll();
            txtCodigo.requestFocus();
        });

        Thread hilo = new Thread(tarea, "validacion-boleto");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void limpiar() {
        if (ocupado) {
            return;
        }

        txtCodigo.clear();
        lblMensaje.setText("");
        ocultarResultado();
        txtCodigo.requestFocus();
    }

    private void ocultarResultado() {
        panelResultado.setVisible(false);
        panelResultado.setManaged(false);
        lblDetalle.setText("");
    }

    private void comprobarAcceso() {
        ControlAcceso.exigirAcceso(Modulo.VALIDACION);

        if (!Objects.equals(
                SessionContext.getInstancia().getIdUsuario(),
                idValidador)) {

            throw new SecurityException(
                    "La sesión cambió. Abre nuevamente el módulo."
            );
        }
    }

    private void cambiarEstado(boolean valor) {
        ocupado = valor;

        txtCodigo.setDisable(valor);
        btnValidar.setDisable(valor);
        btnLimpiar.setDisable(valor);

        if (txtCodigo.getScene() != null
                && txtCodigo.getScene().getRoot()
                instanceof BorderPane menu
                && menu.getLeft() != null) {

            menu.getLeft().setDisable(valor);
        }
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setStyle(
                "-fx-font-size: 17px; -fx-font-weight: bold; "
                        + (error
                                ? "-fx-text-fill: #b91c1c;"
                                : "-fx-text-fill: #166534;")
        );

        lblMensaje.setText(mensaje);
    }
}