package org.kt.controller;

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
import org.kt.dao.AsientoDAO;
import org.kt.dao.EventoDAO;
import org.kt.dao.ZonaLugarDAO;
import org.kt.dao.impl.AsientoDAOImpl;
import org.kt.dao.impl.EventoDAOImpl;
import org.kt.dao.impl.ZonaLugarDAOImpl;
import org.kt.model.Asiento;
import org.kt.model.Evento;
import org.kt.model.ZonaLugar;
import org.kt.util.ControlAcceso;
import org.kt.util.ControlAcceso.Modulo;

public class AsientoController {

    private static final Logger LOGGER =
            Logger.getLogger(AsientoController.class.getName());

    @FXML
    private ComboBox<ZonaLugar> cmbZona;

    @FXML
    private TextField txtFila;

    @FXML
    private TextField txtNumero;

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
    private TableView<Asiento> tablaAsientos;

    @FXML
    private TableColumn<Asiento, Integer> colId;

    @FXML
    private TableColumn<Asiento, String> colZona;

    @FXML
    private TableColumn<Asiento, String> colFila;

    @FXML
    private TableColumn<Asiento, Integer> colNumero;

    @FXML
    private TableColumn<Asiento, String> colReservaHasta;

    private final AsientoDAO asientoDAO = new AsientoDAOImpl();
    private final ZonaLugarDAO zonaDAO = new ZonaLugarDAOImpl();
    private final EventoDAO eventoDAO = new EventoDAOImpl();

    private final ObservableList<Asiento> asientos =
            FXCollections.observableArrayList();

    private final ObservableList<ZonaLugar> zonas =
            FXCollections.observableArrayList();

    private final ObservableList<Evento> eventos =
            FXCollections.observableArrayList();

    private Asiento seleccionado;
    private boolean ocupado;

    private record Datos(
            List<Evento> eventos,
            List<ZonaLugar> zonas,
            List<Asiento> asientos) {
    }

    @FXML
    private void initialize() {
        ControlAcceso.exigirAcceso(Modulo.ASIENTOS);

        cmbZona.setItems(zonas);

        cmbZona.setConverter(new StringConverter<ZonaLugar>() {

            @Override
            public String toString(ZonaLugar zona) {
                return zona == null ? "" : describirZona(zona);
            }

            @Override
            public ZonaLugar fromString(String texto) {
                return null;
            }
        });

        colId.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getIdAsiento()
                ));

        colZona.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        nombreZona(dato.getValue().getIdZona())
                ));

        colFila.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getFila()
                ));

        colNumero.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getNumero()
                ));

        colReservaHasta.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getReservaHasta() == null
                                ? "Sin reserva registrada"
                                : dato.getValue().getReservaHasta()
                                        .toString().replace('T', ' ')
                ));

        tablaAsientos.setItems(asientos);

        tablaAsientos.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {
                    seleccionado = actual;

                    if (actual != null) {
                        txtFila.setText(actual.getFila());

                        txtNumero.setText(
                                String.valueOf(actual.getNumero())
                        );

                        ZonaLugar zona = zonas.stream()
                                .filter(item ->
                                        item.getIdZona()
                                        == actual.getIdZona())
                                .findFirst()
                                .orElse(null);

                        cmbZona.setValue(zona);
                    }

                    actualizarControles();
                });

        recargar();
    }

    private String describirZona(ZonaLugar zona) {
        String evento = eventos.stream()
                .filter(item ->
                        item.getIdEvento() == zona.getIdEvento())
                .map(Evento::getNombre)
                .findFirst()
                .orElse("Evento #" + zona.getIdEvento());

        return evento + " / " + zona.getNombre()
                + " (#" + zona.getIdZona() + ")";
    }

    private String nombreZona(int idZona) {
        return zonas.stream()
                .filter(zona -> zona.getIdZona() == idZona)
                .map(this::describirZona)
                .findFirst()
                .orElse("Zona #" + idZona);
    }

    @FXML
    private void guardar() {
        if (!autorizar()) {
            return;
        }

        try {
            Asiento asiento = leerFormulario(0);

            ejecutar(
                    () -> asientoDAO.insertar(asiento),
                    id -> {
                        limpiar();
                        cargarDatos(
                                "Asiento guardado con ID " + id + "."
                        );
                    },
                    "No se pudo guardar. Revisa la conexión "
                            + "y que la fila y número no estén "
                            + "repetidos dentro de la zona."
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
            mostrarError("Selecciona un asiento.");
            return;
        }

        try {
            Asiento asiento = leerFormulario(
                    seleccionado.getIdAsiento()
            );

            ejecutar(
                    () -> {
                        asientoDAO.actualizar(asiento);
                        return true;
                    },
                    resultado -> {
                        limpiar();
                        cargarDatos("Asiento actualizado.");
                    },
                    "No se pudo actualizar. Revisa si existe "
                            + "un asiento duplicado, una reserva "
                            + "activa o un historial de boletos."
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
            mostrarError("Selecciona un asiento.");
            return;
        }

        int id = seleccionado.getIdAsiento();

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmacion.setTitle("Eliminar asiento");

        confirmacion.setHeaderText(
                "¿Eliminar el asiento "
                        + seleccionado.getFila()
                        + "-" + seleccionado.getNumero() + "?"
        );

        confirmacion.setContentText(
                "Esta acción no se puede deshacer."
        );

        confirmacion.initOwner(
                tablaAsientos.getScene().getWindow()
        );

        if (confirmacion.showAndWait()
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        ejecutar(
                () -> {
                    asientoDAO.eliminar(id);
                    return true;
                },
                resultado -> {
                    limpiar();
                    cargarDatos("Asiento eliminado.");
                },
                "No se pudo eliminar. El asiento podría tener "
                        + "una reserva activa o boletos relacionados."
        );
    }

    @FXML
    private void limpiar() {
        if (ocupado) {
            return;
        }

        tablaAsientos.getSelectionModel().clearSelection();
        seleccionado = null;

        cmbZona.getSelectionModel().clearSelection();
        cmbZona.setValue(null);

        txtFila.clear();
        txtNumero.clear();
        lblMensaje.setText("");

        actualizarControles();
    }

    @FXML
    private void recargar() {
        if (!autorizar()) {
            return;
        }

        cargarDatos("Asientos cargados.");
    }

    private void cargarDatos(String mensaje) {
        ejecutar(
                () -> new Datos(
                        eventoDAO.listar(),
                        zonaDAO.listar(),
                        asientoDAO.listar()
                ),
                datos -> {
                    limpiar();
                    eventos.setAll(datos.eventos());
                    zonas.setAll(datos.zonas());
                    asientos.setAll(datos.asientos());

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

    private Asiento leerFormulario(int id) {
        ZonaLugar zona = cmbZona.getValue();
        String fila = txtFila.getText().trim();
        String numeroTexto = txtNumero.getText().trim();

        if (zona == null || fila.isEmpty()
                || numeroTexto.isEmpty()) {

            throw new IllegalArgumentException(
                    "Selecciona una zona y completa fila y número."
            );
        }

        if (fila.length() > 10) {
            throw new IllegalArgumentException(
                    "La fila admite hasta 10 caracteres."
            );
        }

        if (!numeroTexto.matches("[0-9]+")) {
            throw new IllegalArgumentException(
                    "El número debe ser un entero positivo."
            );
        }

        int numero;

        try {
            numero = Integer.parseInt(numeroTexto);
        } catch (NumberFormatException excepcion) {
            throw new IllegalArgumentException(
                    "El número debe estar entre 1 y 65535."
            );
        }

        if (numero < 1 || numero > 65535) {
            throw new IllegalArgumentException(
                    "El número debe estar entre 1 y 65535."
            );
        }

        int idZona = seleccionado == null
                ? zona.getIdZona()
                : seleccionado.getIdZona();

        return new Asiento(
                id,
                idZona,
                fila,
                numero,
                seleccionado == null
                        ? null
                        : seleccionado.getTokenReserva(),
                seleccionado == null
                        ? null
                        : seleccionado.getReservaHasta()
        );
    }

    private boolean autorizar() {
        if (ocupado) {
            return false;
        }

        try {
            ControlAcceso.exigirAcceso(Modulo.ASIENTOS);
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
                ControlAcceso.exigirAcceso(Modulo.ASIENTOS);
                return operacion.call();
            }
        };

        tarea.setOnSucceeded(event -> {
            ocupado = false;
            actualizarControles();

            if (!ControlAcceso.puedeAcceder(Modulo.ASIENTOS)) {
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

        Thread hilo = new Thread(tarea, "operacion-asientos");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void actualizarControles() {
        cmbZona.setDisable(
                ocupado || seleccionado != null
        );

        txtFila.setDisable(ocupado);
        txtNumero.setDisable(ocupado);
        tablaAsientos.setDisable(ocupado);

        btnGuardar.setDisable(
                ocupado || seleccionado != null
        );

        btnActualizar.setDisable(
                ocupado || seleccionado == null
        );

        btnEliminar.setDisable(
                ocupado || seleccionado == null
        );

        btnLimpiar.setDisable(ocupado);
        btnRecargar.setDisable(ocupado);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setStyle("-fx-text-fill: #b91c1c;");
        lblMensaje.setText(mensaje);
    }
}