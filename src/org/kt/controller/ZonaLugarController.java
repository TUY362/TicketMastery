package org.kt.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.kt.dao.EventoDAO;
import org.kt.dao.ZonaLugarDAO;
import org.kt.dao.impl.EventoDAOImpl;
import org.kt.dao.impl.ZonaLugarDAOImpl;
import org.kt.model.Evento;
import org.kt.model.ZonaLugar;
import org.kt.util.ControlAcceso;
import org.kt.util.ControlAcceso.Modulo;

public class ZonaLugarController {

    private static final Logger LOGGER =
            Logger.getLogger(ZonaLugarController.class.getName());

    @FXML
    private ComboBox<Evento> cmbEvento;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtPrecio;

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
    private TableView<ZonaLugar> tablaZonas;

    @FXML
    private TableColumn<ZonaLugar, Integer> colId;

    @FXML
    private TableColumn<ZonaLugar, String> colEvento;

    @FXML
    private TableColumn<ZonaLugar, String> colNombre;

    @FXML
    private TableColumn<ZonaLugar, BigDecimal> colPrecio;

    private final ZonaLugarDAO zonaDAO = new ZonaLugarDAOImpl();
    private final EventoDAO eventoDAO = new EventoDAOImpl();

    private final ObservableList<ZonaLugar> zonas =
            FXCollections.observableArrayList();

    private final ObservableList<Evento> eventos =
            FXCollections.observableArrayList();

    private ZonaLugar seleccionada;
    private boolean ocupado;

    private record Datos(
            List<Evento> eventos,
            List<ZonaLugar> zonas) {
    }

    @FXML
    private void initialize() {
        ControlAcceso.exigirAcceso(Modulo.ZONAS);

        cmbEvento.setItems(eventos);

        cmbEvento.setConverter(new StringConverter<Evento>() {

            @Override
            public String toString(Evento evento) {
                if (evento == null) {
                    return "";
                }

                return evento.getIdEvento()
                        + " - " + evento.getNombre();
            }

            @Override
            public Evento fromString(String texto) {
                return null;
            }
        });

        colId.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getIdZona()
                ));

        colEvento.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        nombreEvento(
                                dato.getValue().getIdEvento()
                        )
                ));

        colNombre.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getNombre()
                ));

        colPrecio.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getPrecio()
                ));

        tablaZonas.setItems(zonas);

        tablaZonas.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {
                    seleccionada = actual;

                    if (actual != null) {
                        txtNombre.setText(actual.getNombre());
                        txtPrecio.setText(
                                actual.getPrecio().toPlainString()
                        );

                        Evento evento = eventos.stream()
                                .filter(item ->
                                        item.getIdEvento()
                                        == actual.getIdEvento())
                                .findFirst()
                                .orElse(null);

                        cmbEvento.setValue(evento);
                    }

                    actualizarControles();
                });

        recargar();
    }

    private String nombreEvento(int idEvento) {
        return eventos.stream()
                .filter(evento ->
                        evento.getIdEvento() == idEvento)
                .map(evento ->
                        evento.getIdEvento()
                        + " - " + evento.getNombre())
                .findFirst()
                .orElse("Evento #" + idEvento);
    }

    @FXML
    private void guardar() {
        if (!autorizar()) {
            return;
        }

        try {
            ZonaLugar zona = leerFormulario(0);

            ejecutar(
                    () -> zonaDAO.insertar(zona),
                    id -> {
                        limpiar();
                        cargarDatos(
                                "Zona guardada con ID " + id + "."
                        );
                    },
                    "No se pudo guardar. Revisa la conexión "
                            + "y que el nombre no esté repetido "
                            + "dentro del evento."
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

        if (seleccionada == null) {
            mostrarError("Selecciona una zona.");
            return;
        }

        try {
            ZonaLugar zona = leerFormulario(
                    seleccionada.getIdZona()
            );

            ejecutar(
                    () -> {
                        zonaDAO.actualizar(zona);
                        return true;
                    },
                    resultado -> {
                        limpiar();
                        cargarDatos("Zona actualizada.");
                    },
                    "No se pudo actualizar. Revisa la conexión "
                            + "y que el nombre no esté repetido "
                            + "dentro del evento."
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

        if (seleccionada == null) {
            mostrarError("Selecciona una zona.");
            return;
        }

        int id = seleccionada.getIdZona();

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmacion.setTitle("Eliminar zona");
        confirmacion.setHeaderText(
                "¿Eliminar " + seleccionada.getNombre() + "?"
        );

        confirmacion.setContentText(
                "Esta acción no se puede deshacer."
        );

        confirmacion.initOwner(
                tablaZonas.getScene().getWindow()
        );

        if (confirmacion.showAndWait()
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        ejecutar(
                () -> {
                    zonaDAO.eliminar(id);
                    return true;
                },
                resultado -> {
                    limpiar();
                    cargarDatos("Zona eliminada.");
                },
                "No se pudo eliminar. La zona podría tener "
                        + "asientos relacionados."
        );
    }

    @FXML
    private void limpiar() {
        if (ocupado) {
            return;
        }

        tablaZonas.getSelectionModel().clearSelection();
        seleccionada = null;

        cmbEvento.getSelectionModel().clearSelection();
        cmbEvento.setValue(null);

        txtNombre.clear();
        txtPrecio.clear();
        lblMensaje.setText("");

        actualizarControles();
    }

    @FXML
    private void recargar() {
        if (!autorizar()) {
            return;
        }

        cargarDatos("Zonas cargadas.");
    }

    private void cargarDatos(String mensaje) {
        ejecutar(
                () -> new Datos(
                        eventoDAO.listar(),
                        zonaDAO.listar()
                ),
                datos -> {
                    limpiar();
                    eventos.setAll(datos.eventos());
                    zonas.setAll(datos.zonas());

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

    private ZonaLugar leerFormulario(int id) {
        Evento evento = cmbEvento.getValue();
        String nombre = txtNombre.getText().trim();
        String precioTexto = txtPrecio.getText().trim();

        if (evento == null
                || nombre.isEmpty()
                || precioTexto.isEmpty()) {

            throw new IllegalArgumentException(
                    "Selecciona un evento y completa nombre y precio."
            );
        }

        if (nombre.length() > 80) {
            throw new IllegalArgumentException(
                    "El nombre admite hasta 80 caracteres."
            );
        }

        if (!precioTexto.matches("\\d+(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException(
                    "Escribe un precio como 125.00, "
                            + "sin comas y con máximo dos decimales."
            );
        }

        BigDecimal precio = new BigDecimal(precioTexto);

        if (precio.compareTo(BigDecimal.ZERO) <= 0
                || precio.compareTo(
                        new BigDecimal("99999999.99")) > 0) {

            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero "
                            + "y no superar 99999999.99."
            );
        }

        precio = precio.setScale(2, RoundingMode.UNNECESSARY);

        int idEvento = seleccionada == null
                ? evento.getIdEvento()
                : seleccionada.getIdEvento();

        return new ZonaLugar(
                id,
                idEvento,
                nombre,
                precio
        );
    }

    private boolean autorizar() {
        if (ocupado) {
            return false;
        }

        try {
            ControlAcceso.exigirAcceso(Modulo.ZONAS);
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

        ocupado = true;
        actualizarControles();

        lblMensaje.setStyle("-fx-text-fill: #475569;");
        lblMensaje.setText("Procesando...");

        Task<T> tarea = new Task<>() {

            @Override
            protected T call() throws Exception {
                ControlAcceso.exigirAcceso(Modulo.ZONAS);
                return operacion.call();
            }
        };

        tarea.setOnSucceeded(event -> {
            ocupado = false;
            actualizarControles();

            if (!ControlAcceso.puedeAcceder(Modulo.ZONAS)) {
                mostrarError("La sesión ya no tiene acceso.");
                return;
            }

            alCompletar.accept(tarea.getValue());
        });

        tarea.setOnFailed(event -> {
            ocupado = false;
            actualizarControles();
            mostrarError(mensajeError);

            LOGGER.log(
                    Level.SEVERE,
                    mensajeError,
                    tarea.getException()
            );
        });

        Thread hilo = new Thread(tarea, "operacion-zonas");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void actualizarControles() {
        cmbEvento.setDisable(
                ocupado || seleccionada != null
        );

        txtNombre.setDisable(ocupado);
        txtPrecio.setDisable(ocupado);
        tablaZonas.setDisable(ocupado);

        btnGuardar.setDisable(
                ocupado || seleccionada != null
        );

        btnActualizar.setDisable(
                ocupado || seleccionada == null
        );

        btnEliminar.setDisable(
                ocupado || seleccionada == null
        );

        btnLimpiar.setDisable(ocupado);
        btnRecargar.setDisable(ocupado);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setStyle("-fx-text-fill: #b91c1c;");
        lblMensaje.setText(mensaje);
    }
}