package org.kt.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.kt.dao.EventoDAO;
import org.kt.dao.impl.EventoDAOImpl;
import org.kt.model.Evento;
import org.kt.util.ControlAcceso;
import org.kt.util.ControlAcceso.Modulo;

public class EventoController {

    private static final Logger LOGGER =
            Logger.getLogger(EventoController.class.getName());

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtLugar;

    @FXML
    private DatePicker dpFecha;

    @FXML
    private TextField txtHora;

    @FXML
    private CheckBox chkActivo;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnActualizar;

    @FXML
    private Button btnEliminar;

    @FXML
    private Button btnLimpiar;

    @FXML
    private Button btnRecargar;

    @FXML
    private Label lblMensaje;

    @FXML
    private TableView<Evento> tablaEventos;

    @FXML
    private TableColumn<Evento, Integer> colId;

    @FXML
    private TableColumn<Evento, String> colNombre;

    @FXML
    private TableColumn<Evento, String> colFechaHora;

    @FXML
    private TableColumn<Evento, String> colLugar;

    @FXML
    private TableColumn<Evento, String> colActivo;

    private final EventoDAO eventoDAO = new EventoDAOImpl();

    private final ObservableList<Evento> eventos =
            FXCollections.observableArrayList();

    private Evento seleccionado;
    private boolean ocupado;

    @FXML
    private void initialize() {
        ControlAcceso.exigirAcceso(Modulo.EVENTOS);

        colId.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getIdEvento()
                ));

        colNombre.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getNombre()
                ));

        colFechaHora.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getFechaHora()
                                .toString().replace('T', ' ')
                ));

        colLugar.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getLugar()
                ));

        colActivo.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().isActivo() ? "Sí" : "No"
                ));

        tablaEventos.setItems(eventos);

        tablaEventos.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {
                    seleccionado = actual;

                    if (actual != null) {
                        txtNombre.setText(actual.getNombre());
                        txtLugar.setText(actual.getLugar());

                        dpFecha.setValue(
                                actual.getFechaHora().toLocalDate()
                        );

                        txtHora.setText(
                                actual.getFechaHora()
                                        .toLocalTime().toString()
                        );

                        chkActivo.setSelected(actual.isActivo());
                    }

                    actualizarBotones();
                });

        recargar();
    }

    @FXML
    private void guardar() {
        if (!autorizar()) {
            return;
        }

        try {
            Evento evento = leerFormulario(0);

            ejecutar(
                    () -> eventoDAO.insertar(evento),
                    id -> {
                        limpiar();
                        cargarEventos(
                                "Evento guardado con ID " + id + "."
                        );
                    },
                    "No se pudo guardar el evento."
            );

        } catch (IllegalArgumentException excepcion) {
            mostrarError(excepcion.getMessage());
        }
    }

    @FXML
    private void actualizar() {
        if (!autorizar()) {
            return;
        }

        if (seleccionado == null) {
            mostrarError("Selecciona un evento.");
            return;
        }

        try {
            Evento evento = leerFormulario(
                    seleccionado.getIdEvento()
            );

            ejecutar(
                    () -> {
                        eventoDAO.actualizar(evento);
                        return true;
                    },
                    resultado -> {
                        limpiar();
                        cargarEventos("Evento actualizado.");
                    },
                    "No se pudo actualizar el evento."
            );

        } catch (IllegalArgumentException excepcion) {
            mostrarError(excepcion.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        if (!autorizar()) {
            return;
        }

        if (seleccionado == null) {
            mostrarError("Selecciona un evento.");
            return;
        }

        int id = seleccionado.getIdEvento();

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmacion.setTitle("Eliminar evento");
        confirmacion.setHeaderText(
                "¿Eliminar " + seleccionado.getNombre() + "?"
        );

        confirmacion.setContentText(
                "Esta acción no se puede deshacer."
        );

        confirmacion.initOwner(
                tablaEventos.getScene().getWindow()
        );

        if (confirmacion.showAndWait()
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        ejecutar(
                () -> {
                    eventoDAO.eliminar(id);
                    return true;
                },
                resultado -> {
                    limpiar();
                    cargarEventos("Evento eliminado.");
                },
                "No se pudo eliminar. El evento podría tener "
                        + "zonas relacionadas."
        );
    }

    @FXML
    private void limpiar() {
        if (ocupado) {
            return;
        }

        tablaEventos.getSelectionModel().clearSelection();
        seleccionado = null;

        txtNombre.clear();
        txtLugar.clear();
        dpFecha.setValue(null);
        txtHora.clear();
        chkActivo.setSelected(true);
        lblMensaje.setText("");

        actualizarBotones();
    }

    @FXML
    private void recargar() {
        if (!autorizar()) {
            return;
        }

        cargarEventos("Eventos cargados.");
    }

    private void cargarEventos(String mensaje) {
        ejecutar(
                () -> eventoDAO.listar(),
                lista -> {
                    limpiar();
                    eventos.setAll(lista);

                    lblMensaje.setStyle(
                            "-fx-text-fill: #166534;"
                    );

                    lblMensaje.setText(mensaje);
                },
                "No se pudo actualizar la tabla. Si acabas de "
                        + "guardar un cambio, pulsa Recargar "
                        + "antes de repetirlo."
        );
    }

    private Evento leerFormulario(int id) {
        String nombre = txtNombre.getText().trim();
        String lugar = txtLugar.getText().trim();
        String horaTexto = txtHora.getText().trim();

        if (nombre.isEmpty() || lugar.isEmpty()
                || dpFecha.getValue() == null
                || horaTexto.isEmpty()) {

            throw new IllegalArgumentException(
                    "Completa nombre, lugar, fecha y hora."
            );
        }

        if (nombre.length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre admite hasta 120 caracteres."
            );
        }

        if (lugar.length() > 150) {
            throw new IllegalArgumentException(
                    "El lugar admite hasta 150 caracteres."
            );
        }

        LocalTime hora;

        try {
            hora = LocalTime.parse(horaTexto);
        } catch (DateTimeParseException excepcion) {
            throw new IllegalArgumentException(
                    "Escribe una hora válida, por ejemplo 18:30."
            );
        }

        LocalDateTime fechaHora = LocalDateTime.of(
                dpFecha.getValue(),
                hora
        );

        return new Evento(
                id,
                nombre,
                fechaHora,
                lugar,
                chkActivo.isSelected()
        );
    }

    private boolean autorizar() {
        if (ocupado) {
            return false;
        }

        try {
            ControlAcceso.exigirAcceso(Modulo.EVENTOS);
            return true;

        } catch (SecurityException excepcion) {
            mostrarError(excepcion.getMessage());
            return false;
        }
    }

    private <T> void ejecutar(
            Callable<T> operacion,
            Consumer<T> alCompletar,
            String mensajeError) {

        if (!autorizar()) {
            return;
        }

        cambiarEstado(true);
        lblMensaje.setStyle("-fx-text-fill: #475569;");
        lblMensaje.setText("Procesando...");

        Task<T> tarea = new Task<>() {

            @Override
            protected T call() throws Exception {
                ControlAcceso.exigirAcceso(Modulo.EVENTOS);
                return operacion.call();
            }
        };

        tarea.setOnSucceeded(event -> {
            cambiarEstado(false);

            if (!ControlAcceso.puedeAcceder(Modulo.EVENTOS)) {
                mostrarError("La sesión ya no tiene acceso.");
                return;
            }

            alCompletar.accept(tarea.getValue());
        });

        tarea.setOnFailed(event -> {
            cambiarEstado(false);
            mostrarError(mensajeError);

            LOGGER.log(
                    Level.SEVERE,
                    mensajeError,
                    tarea.getException()
            );
        });

        Thread hilo = new Thread(tarea, "operacion-eventos");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cambiarEstado(boolean valor) {
        ocupado = valor;

        txtNombre.setDisable(valor);
        txtLugar.setDisable(valor);
        dpFecha.setDisable(valor);
        txtHora.setDisable(valor);
        chkActivo.setDisable(valor);
        tablaEventos.setDisable(valor);
        btnLimpiar.setDisable(valor);
        btnRecargar.setDisable(valor);

        actualizarBotones();
    }

    private void actualizarBotones() {
        btnGuardar.setDisable(
                ocupado || seleccionado != null
        );

        btnActualizar.setDisable(
                ocupado || seleccionado == null
        );

        btnEliminar.setDisable(
                ocupado || seleccionado == null
        );
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setStyle("-fx-text-fill: #b91c1c;");
        lblMensaje.setText(mensaje);
    }
}