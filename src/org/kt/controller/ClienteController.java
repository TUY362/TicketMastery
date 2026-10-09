package org.kt.controller;

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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.kt.dao.ClienteDAO;
import org.kt.dao.impl.ClienteDAOImpl;
import org.kt.model.Cliente;
import org.kt.util.ControlAcceso;
import org.kt.util.ControlAcceso.Modulo;

public class ClienteController {

    private static final Logger LOGGER =
            Logger.getLogger(ClienteController.class.getName());

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtCorreo;

    @FXML
    private TextField txtTelefono;

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
    private TableView<Cliente> tablaClientes;

    @FXML
    private TableColumn<Cliente, Integer> colId;

    @FXML
    private TableColumn<Cliente, String> colNombre;

    @FXML
    private TableColumn<Cliente, String> colCorreo;

    @FXML
    private TableColumn<Cliente, String> colTelefono;

    private final ClienteDAO clienteDAO = new ClienteDAOImpl();

    private final ObservableList<Cliente> clientes =
            FXCollections.observableArrayList();

    private Cliente seleccionado;
    private boolean ocupado;

    @FXML
    private void initialize() {
        ControlAcceso.exigirAcceso(Modulo.CLIENTES);

        colId.setCellValueFactory(dato ->
                new ReadOnlyObjectWrapper<>(
                        dato.getValue().getIdCliente()
                ));

        colNombre.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getNombre()
                ));

        colCorreo.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getCorreo()
                ));

        colTelefono.setCellValueFactory(dato ->
                new ReadOnlyStringWrapper(
                        dato.getValue().getTelefono() == null
                                ? ""
                                : dato.getValue().getTelefono()
                ));

        tablaClientes.setItems(clientes);

        tablaClientes.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {
                    seleccionado = actual;

                    if (actual != null) {
                        txtNombre.setText(actual.getNombre());
                        txtCorreo.setText(actual.getCorreo());

                        txtTelefono.setText(
                                actual.getTelefono() == null
                                        ? ""
                                        : actual.getTelefono()
                        );
                    }

                    actualizarControles();
                });

        recargar();
    }

    @FXML
    private void guardar() {
        if (!autorizar()) {
            return;
        }

        try {
            Cliente cliente = leerFormulario(0);

            ejecutar(
                    () -> clienteDAO.insertar(cliente),
                    id -> {
                        limpiar();
                        cargarClientes(
                                "Cliente guardado con ID " + id + "."
                        );
                    },
                    "No se pudo guardar. Revisa la conexión "
                            + "y que el correo no esté registrado."
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
            mostrarError("Selecciona un cliente.");
            return;
        }

        try {
            Cliente cliente = leerFormulario(
                    seleccionado.getIdCliente()
            );

            ejecutar(
                    () -> {
                        clienteDAO.actualizar(cliente);
                        return true;
                    },
                    resultado -> {
                        limpiar();
                        cargarClientes("Cliente actualizado.");
                    },
                    "No se pudo actualizar. Revisa la conexión "
                            + "y que el correo no pertenezca "
                            + "a otro cliente."
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
            mostrarError("Selecciona un cliente.");
            return;
        }

        int id = seleccionado.getIdCliente();

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        confirmacion.setTitle("Eliminar cliente");

        confirmacion.setHeaderText(
                "¿Eliminar a " + seleccionado.getNombre() + "?"
        );

        confirmacion.setContentText(
                "Esta acción no se puede deshacer."
        );

        confirmacion.initOwner(
                tablaClientes.getScene().getWindow()
        );

        if (confirmacion.showAndWait()
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        ejecutar(
                () -> {
                    clienteDAO.eliminar(id);
                    return true;
                },
                resultado -> {
                    limpiar();
                    cargarClientes("Cliente eliminado.");
                },
                "No se pudo eliminar. El cliente podría "
                        + "tener ventas relacionadas."
        );
    }

    @FXML
    private void limpiar() {
        if (ocupado) {
            return;
        }

        tablaClientes.getSelectionModel().clearSelection();
        seleccionado = null;

        txtNombre.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        lblMensaje.setText("");

        actualizarControles();
    }

    @FXML
    private void recargar() {
        if (!autorizar()) {
            return;
        }

        cargarClientes("Clientes cargados.");
    }

    private void cargarClientes(String mensaje) {
        ejecutar(
                () -> clienteDAO.listar(),
                lista -> {
                    limpiar();
                    clientes.setAll(lista);

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

    private Cliente leerFormulario(int id) {
        String nombre = txtNombre.getText().trim();
        String correo = txtCorreo.getText().trim();
        String telefono = txtTelefono.getText().trim();

        if (nombre.isEmpty() || correo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Completa el nombre y el correo."
            );
        }

        if (nombre.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre admite hasta 100 caracteres."
            );
        }

        if (correo.length() > 150) {
            throw new IllegalArgumentException(
                    "El correo admite hasta 150 caracteres."
            );
        }

        if (!correo.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw new IllegalArgumentException(
                    "Escribe un correo válido, "
                            + "por ejemplo cliente@correo.com."
            );
        }

        if (telefono.length() > 25) {
            throw new IllegalArgumentException(
                    "El teléfono admite hasta 25 caracteres."
            );
        }

        if (!telefono.isEmpty()
                && (!telefono.matches("[0-9+() .-]+")
                || !telefono.matches(".*[0-9].*"))) {

            throw new IllegalArgumentException(
                    "El teléfono debe contener números. "
                            + "Puede incluir +, espacios, "
                            + "paréntesis, puntos y guiones."
            );
        }

        return new Cliente(
                id,
                nombre,
                correo,
                telefono.isEmpty() ? null : telefono
        );
    }

    private boolean autorizar() {
        if (ocupado) {
            return false;
        }

        try {
            ControlAcceso.exigirAcceso(Modulo.CLIENTES);
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
                ControlAcceso.exigirAcceso(Modulo.CLIENTES);
                return operacion.call();
            }
        };

        tarea.setOnSucceeded(event -> {
            ocupado = false;
            actualizarControles();

            if (!ControlAcceso.puedeAcceder(Modulo.CLIENTES)) {
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

        Thread hilo = new Thread(tarea, "operacion-clientes");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void actualizarControles() {
        txtNombre.setDisable(ocupado);
        txtCorreo.setDisable(ocupado);
        txtTelefono.setDisable(ocupado);
        tablaClientes.setDisable(ocupado);

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