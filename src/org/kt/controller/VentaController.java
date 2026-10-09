package org.kt.controller;

import com.google.zxing.WriterException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kt.dao.AsientoDAO;
import org.kt.dao.BoletoDAO;
import org.kt.dao.ClienteDAO;
import org.kt.dao.EventoDAO;
import org.kt.dao.VentaTransaccionDAO;
import org.kt.dao.ZonaLugarDAO;
import org.kt.dao.impl.AsientoDAOImpl;
import org.kt.dao.impl.BoletoDAOImpl;
import org.kt.dao.impl.ClienteDAOImpl;
import org.kt.dao.impl.EventoDAOImpl;
import org.kt.dao.impl.VentaTransaccionDAOImpl;
import org.kt.dao.impl.ZonaLugarDAOImpl;
import org.kt.exceptions.DBException;
import org.kt.model.Asiento;
import org.kt.model.Boleto;
import org.kt.model.Cliente;
import org.kt.model.Evento;
import org.kt.model.VentaTransaccion;
import org.kt.model.ZonaLugar;
import org.kt.util.Conexion;
import org.kt.util.ControlAcceso;
import org.kt.util.ControlAcceso.Modulo;
import org.kt.util.QRUtil;
import org.kt.util.SessionContext;

public class VentaController {

    private static final Logger LOGGER =
            Logger.getLogger(VentaController.class.getName());

    @FXML
    private Label lblTaquillero;

    @FXML
    private ComboBox<Cliente> cmbCliente;

    @FXML
    private ComboBox<Evento> cmbEvento;

    @FXML
    private ComboBox<ZonaLugar> cmbZona;

    @FXML
    private ComboBox<Asiento> cmbAsiento;

    @FXML
    private Label lblPrecio;

    @FXML
    private Button btnReservar;

    @FXML
    private Button btnCancelar;

    @FXML
    private Button btnRecargar;

    @FXML
    private Label lblReserva;

    @FXML
    private TextField txtReferencia;

    @FXML
    private CheckBox chkPagoConfirmado;

    @FXML
    private Button btnConfirmar;

    @FXML
    private Label lblMensaje;

    @FXML
    private VBox panelBoleto;

    @FXML
    private Label lblDetalleBoleto;

    @FXML
    private TextField txtCodigoBoleto;

    @FXML
    private ImageView imgQR;

    @FXML
    private Button btnNuevaVenta;

    private final ClienteDAO clienteDAO = new ClienteDAOImpl();
    private final EventoDAO eventoDAO = new EventoDAOImpl();
    private final ZonaLugarDAO zonaDAO = new ZonaLugarDAOImpl();
    private final AsientoDAO asientoDAO = new AsientoDAOImpl();

    private final VentaTransaccionDAO ventaDAO =
            new VentaTransaccionDAOImpl();

    private final BoletoDAO boletoDAO = new BoletoDAOImpl();

    private List<ZonaLugar> zonas = List.of();
    private List<Asiento> asientos = List.of();
    private Set<Integer> vendidos = Set.of();

    private Reserva reserva;
    private Timeline reloj;
    private boolean ocupado;
    private boolean cargando;
    private boolean emitido;
    private int idTaquillero;

    private record Datos(
            List<Cliente> clientes,
            List<Evento> eventos,
            List<ZonaLugar> zonas,
            List<Asiento> asientos,
            List<Boleto> boletos) {
    }

    private record Reserva(
            int idVenta,
            Cliente cliente,
            Evento evento,
            ZonaLugar zona,
            Asiento asiento) {
    }

    @FXML
    private void initialize() {
        ControlAcceso.exigirAcceso(Modulo.VENTAS);

        SessionContext sesion = SessionContext.getInstancia();
        idTaquillero = sesion.getIdUsuario();

        lblTaquillero.setText(
                "Taquillero: " + sesion.getNombreUsuario()
        );

        configurarCombo(cmbCliente, cliente ->
                cliente.getIdCliente() + " - " + cliente.getNombre());

        configurarCombo(cmbEvento, evento ->
                evento.getIdEvento() + " - " + evento.getNombre());

        configurarCombo(cmbZona, zona ->
                zona.getNombre() + " - Q "
                        + zona.getPrecio().toPlainString());

        configurarCombo(cmbAsiento, asiento ->
                asiento.getFila() + "-" + asiento.getNumero());

        cmbCliente.valueProperty().addListener(
                (observable, anterior, actual) -> actualizarControles()
        );

        cmbAsiento.valueProperty().addListener(
                (observable, anterior, actual) -> actualizarControles()
        );

        chkPagoConfirmado.selectedProperty().addListener(
                (observable, anterior, actual) -> actualizarControles()
        );

        lblReserva.sceneProperty().addListener(
                (observable, anterior, actual) -> {
                    if (actual == null) {
                        detenerReloj();
                    } else {
                        actualizarControles();
                    }
                }
        );

        recargar();
    }

    private <T> void configurarCombo(
            ComboBox<T> combo,
            Function<T, String> descripcion) {

        combo.setConverter(new StringConverter<T>() {

            @Override
            public String toString(T objeto) {
                return objeto == null ? "" : descripcion.apply(objeto);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        });
    }

    @FXML
    private void recargar() {
        if (ocupado || reserva != null || emitido) {
            return;
        }

        ejecutar(
                () -> new Datos(
                        clienteDAO.listar(),
                        eventoDAO.listar(),
                        zonaDAO.listar(),
                        asientoDAO.listar(),
                        boletoDAO.listar()
                ),
                datos -> {
                    cargando = true;

                    try {
                        zonas = datos.zonas();
                        asientos = datos.asientos();

                        vendidos = datos.boletos().stream()
                                .filter(boleto ->
                                        !"ANULADO".equals(
                                                boleto.getEstado()))
                                .map(Boleto::getIdAsiento)
                                .collect(Collectors.toSet());

                        cmbCliente.setValue(null);
                        cmbEvento.setValue(null);
                        cmbZona.setValue(null);
                        cmbAsiento.setValue(null);

                        cmbCliente.getItems().setAll(datos.clientes());

                        cmbEvento.getItems().setAll(
                                datos.eventos().stream()
                                        .filter(Evento::isActivo)
                                        .filter(evento ->
                                                evento.getFechaHora()
                                                        .isAfter(ahora()))
                                        .toList()
                        );

                        cmbZona.getItems().clear();
                        cmbAsiento.getItems().clear();
                        lblPrecio.setText("Q 0.00");

                    } finally {
                        cargando = false;
                    }

                    mostrarMensaje(
                            "Disponibilidad actualizada.",
                            false
                    );
                },
                "No se pudo cargar la disponibilidad."
        );
    }

    @FXML
    private void seleccionarEvento() {
        if (cargando || ocupado || reserva != null || emitido) {
            return;
        }

        cargando = true;

        try {
            cmbZona.setValue(null);
            cmbAsiento.setValue(null);
            cmbZona.getItems().clear();
            cmbAsiento.getItems().clear();
            lblPrecio.setText("Q 0.00");

            Evento evento = cmbEvento.getValue();

            if (evento != null) {
                cmbZona.getItems().setAll(
                        zonas.stream()
                                .filter(zona ->
                                        zona.getIdEvento()
                                        == evento.getIdEvento())
                                .toList()
                );
            }

        } finally {
            cargando = false;
        }

        actualizarControles();
    }

    @FXML
    private void seleccionarZona() {
        if (cargando || ocupado || reserva != null || emitido) {
            return;
        }

        cmbAsiento.setValue(null);
        cmbAsiento.getItems().clear();

        ZonaLugar zona = cmbZona.getValue();

        if (zona == null) {
            lblPrecio.setText("Q 0.00");
        } else {
            lblPrecio.setText(
                    "Q " + zona.getPrecio().toPlainString()
            );

            cmbAsiento.getItems().setAll(
                    asientos.stream()
                            .filter(asiento ->
                                    asiento.getIdZona() == zona.getIdZona())
                            .filter(asiento ->
                                    !vendidos.contains(
                                            asiento.getIdAsiento()))
                            .filter(asiento ->
                                    asiento.getReservaHasta() == null
                                    || !asiento.getReservaHasta()
                                            .isAfter(ahora()))
                            .toList()
            );
        }

        actualizarControles();
    }

    @FXML
    private void reservar() {
        if (ocupado || reserva != null || emitido) {
            return;
        }

        Cliente cliente = cmbCliente.getValue();
        Evento evento = cmbEvento.getValue();
        ZonaLugar zona = cmbZona.getValue();
        Asiento asiento = cmbAsiento.getValue();

        if (cliente == null || evento == null
                || zona == null || asiento == null) {

            mostrarMensaje(
                    "Selecciona cliente, evento, zona y asiento.",
                    true
            );
            return;
        }

        ejecutar(
                () -> {
                    Asiento reservado = asientoDAO.reservar(
                            asiento.getIdAsiento()
                    );

                    try {
                        VentaTransaccion venta = new VentaTransaccion(
                                0,
                                cliente.getIdCliente(),
                                idTaquillero,
                                null,
                                "PENDIENTE",
                                null,
                                null
                        );

                        int idVenta = ventaDAO.insertar(venta);

                        return new Reserva(
                                idVenta,
                                cliente,
                                evento,
                                zona,
                                reservado
                        );

                    } catch (Exception excepcion) {
                        try {
                            asientoDAO.liberarReserva(
                                    reservado.getIdAsiento(),
                                    reservado.getTokenReserva()
                            );
                        } catch (Exception limpieza) {
                            excepcion.addSuppressed(limpieza);
                        }

                        throw excepcion;
                    }
                },
                resultado -> {
                    reserva = resultado;
                    txtReferencia.clear();
                    chkPagoConfirmado.setSelected(false);
                    iniciarReloj();

                    mostrarMensaje(
                            "Asiento reservado. Registra el pago recibido.",
                            false
                    );
                },
                "No se pudo reservar. Recarga la disponibilidad "
                        + "y selecciona un asiento disponible."
        );
    }

    @FXML
    private void confirmar() {
        if (ocupado || reserva == null || emitido) {
            return;
        }

        if (!reservaVigente()) {
            mostrarMensaje(
                    "La reserva venció. Cancélala y reserva nuevamente.",
                    true
            );
            return;
        }

        String referencia = txtReferencia.getText().trim();

        if (referencia.isEmpty() || referencia.length() > 100) {
            mostrarMensaje(
                    "La referencia debe contener entre 1 y 100 caracteres.",
                    true
            );
            return;
        }

        if (!chkPagoConfirmado.isSelected()) {
            mostrarMensaje(
                    "Confirma que el pago fue recibido.",
                    true
            );
            return;
        }

        Reserva actual = reserva;

        ejecutar(
                () -> emitirBoleto(actual, referencia),
                boleto -> {
                    detenerReloj();
                    reserva = null;
                    emitido = true;

                    lblReserva.setText("Venta completada");

                    lblPrecio.setText(
                            "Q " + boleto.getPrecioPagado().toPlainString()
                    );

                    lblDetalleBoleto.setText(
                            "Venta: " + actual.idVenta()
                            + "\nCliente: " + actual.cliente().getNombre()
                            + "\nEvento: " + actual.evento().getNombre()
                            + "\nZona: " + actual.zona().getNombre()
                            + "\nAsiento: " + actual.asiento().getFila()
                            + "-" + actual.asiento().getNumero()
                            + "\nTotal: Q "
                            + boleto.getPrecioPagado().toPlainString()
                    );

                    txtCodigoBoleto.setText(boleto.getCodigo());
                    panelBoleto.setVisible(true);
                    panelBoleto.setManaged(true);

                    mostrarQr(boleto.getCodigo());
                },
                "No se pudo completar la emisión. Revisa la referencia "
                        + "y la conexión. No registres otro pago."
        );
    }

    private Boleto emitirBoleto(
            Reserva actual,
            String referencia) throws DBException {

        Boleto existente = buscarBoletoPagado(actual.idVenta());

        if (existente != null) {
            return existente;
        }

        try {
            return boletoDAO.insertar(
                    actual.idVenta(),
                    actual.asiento().getIdAsiento(),
                    actual.asiento().getTokenReserva(),
                    referencia
            );

        } catch (DBException excepcion) {
            try {
                Boleto recuperado = buscarBoletoPagado(actual.idVenta());

                if (recuperado != null) {
                    return recuperado;
                }

            } catch (DBException consulta) {
                excepcion.addSuppressed(consulta);
            }

            throw excepcion;
        }
    }

    private Boleto buscarBoletoPagado(int idVenta) throws DBException {
        VentaTransaccion venta = ventaDAO.buscarPorId(idVenta)
                .orElseThrow(() ->
                        new DBException("La venta no existe."));

        if ("PENDIENTE".equals(venta.getEstado())) {
            return null;
        }

        if (!"PAGADA".equals(venta.getEstado())) {
            throw new DBException("La venta ya no está pendiente.");
        }

        return boletoDAO.listar().stream()
                .filter(boleto -> boleto.getIdVenta() == idVenta)
                .findFirst()
                .orElseThrow(() ->
                        new DBException(
                                "La venta está pagada, pero no se "
                                        + "pudo recuperar el boleto."
                        ));
    }

    private void mostrarQr(String codigo) {
        imgQR.setImage(null);

        try {
            imgQR.setImage(QRUtil.generar(codigo));

            mostrarMensaje(
                    "Boleto emitido correctamente. QR disponible.",
                    false
            );

        } catch (WriterException | IllegalArgumentException excepcion) {
            LOGGER.log(
                    Level.SEVERE,
                    "El boleto fue emitido, pero no se pudo generar el QR.",
                    excepcion
            );

            mostrarMensaje(
                    "El boleto ya fue emitido, pero no se pudo "
                            + "generar el QR. Usa el código alfanumérico. "
                            + "No repitas el cobro.",
                    true
            );
        }
    }

    @FXML
    private void cancelar() {
        if (ocupado || reserva == null) {
            return;
        }

        Reserva actual = reserva;

        ejecutar(
                () -> {
                    VentaTransaccion venta = ventaDAO
                            .buscarPorId(actual.idVenta())
                            .orElse(null);

                    if (venta != null) {
                        if (!"PENDIENTE".equals(venta.getEstado())) {
                            throw new DBException(
                                    "La venta ya fue procesada."
                            );
                        }

                        ventaDAO.eliminar(actual.idVenta());
                    }

                    Asiento asiento = asientoDAO.buscarPorId(
                            actual.asiento().getIdAsiento()
                    ).orElse(null);

                    if (asiento != null
                            && actual.asiento().getTokenReserva() != null
                            && actual.asiento().getTokenReserva().equals(
                                    asiento.getTokenReserva())) {

                        asientoDAO.liberarReserva(
                                asiento.getIdAsiento(),
                                actual.asiento().getTokenReserva()
                        );
                    }

                    return true;
                },
                resultado -> {
                    detenerReloj();
                    reserva = null;
                    limpiarPago();
                    lblReserva.setText("Sin reserva activa");
                    recargar();
                },
                "No se pudo cancelar. Revisa la conexión. "
                        + "Si la venta ya se pagó, no vuelvas a cobrar."
        );
    }

    @FXML
    private void nuevaVenta() {
        if (ocupado || reserva != null) {
            return;
        }

        emitido = false;
        limpiarPago();

        panelBoleto.setVisible(false);
        panelBoleto.setManaged(false);
        lblDetalleBoleto.setText("");
        txtCodigoBoleto.clear();
        imgQR.setImage(null);
        lblReserva.setText("Sin reserva activa");

        recargar();
    }

    private void limpiarPago() {
        txtReferencia.clear();
        chkPagoConfirmado.setSelected(false);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private boolean reservaVigente() {
        return reserva != null
                && reserva.asiento().getReservaHasta() != null
                && reserva.asiento().getReservaHasta().isAfter(ahora());
    }

    private void iniciarReloj() {
        detenerReloj();

        reloj = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> actualizarTiempo()
                )
        );

        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
        actualizarTiempo();
    }

    private void actualizarTiempo() {
        if (reserva == null) {
            detenerReloj();
            return;
        }

        if (lblReserva.getScene() == null
                || lblReserva.getScene().getWindow() == null
                || !lblReserva.getScene().getWindow().isShowing()) {

            detenerReloj();
            return;
        }

        long segundos = Math.max(
                0,
                java.time.Duration.between(
                        ahora(),
                        reserva.asiento().getReservaHasta()
                ).getSeconds()
        );

        if (!reservaVigente()) {
            lblReserva.setText(
                    "Reserva vencida. Pulsa Cancelar reserva."
            );

            detenerReloj();
        } else {
            lblReserva.setText(
                    String.format(
                            "Tiempo restante: %02d:%02d",
                            segundos / 60,
                            segundos % 60
                    )
            );
        }

        actualizarControles();
    }

    private void detenerReloj() {
        if (reloj != null) {
            reloj.stop();
            reloj = null;
        }
    }

    private void comprobarAcceso() {
        ControlAcceso.exigirAcceso(Modulo.VENTAS);

        if (!Objects.equals(
                SessionContext.getInstancia().getIdUsuario(),
                idTaquillero)) {

            throw new SecurityException(
                    "La sesión cambió. Abre nuevamente el módulo."
            );
        }
    }

    private <T> void ejecutar(
            Callable<T> operacion,
            Consumer<T> alCompletar,
            String mensajeError) {

        if (ocupado) {
            return;
        }

        try {
            comprobarAcceso();
        } catch (SecurityException excepcion) {
            mostrarMensaje(excepcion.getMessage(), true);
            return;
        }

        ocupado = true;
        actualizarControles();
        mostrarMensaje("Procesando...", false);

        Task<T> tarea = new Task<>() {

            @Override
            protected T call() throws Exception {
                Conexion gestor = Conexion.getInstancia();

                synchronized (gestor) {
                    synchronized (gestor.getConexion()) {
                        comprobarAcceso();
                        return operacion.call();
                    }
                }
            }
        };

        tarea.setOnSucceeded(event -> {
            ocupado = false;

            try {
                alCompletar.accept(tarea.getValue());
            } finally {
                actualizarControles();
            }
        });

        tarea.setOnFailed(event -> {
            ocupado = false;
            actualizarControles();
            mostrarMensaje(mensajeError, true);

            LOGGER.log(
                    Level.SEVERE,
                    mensajeError,
                    tarea.getException()
            );
        });

        Thread hilo = new Thread(tarea, "operacion-venta");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void actualizarControles() {
        boolean bloqueado = ocupado || reserva != null || emitido;

        cmbCliente.setDisable(bloqueado);
        cmbEvento.setDisable(bloqueado);

        cmbZona.setDisable(
                bloqueado || cmbEvento.getValue() == null
        );

        cmbAsiento.setDisable(
                bloqueado || cmbZona.getValue() == null
        );

        btnReservar.setDisable(
                bloqueado
                || cmbCliente.getValue() == null
                || cmbAsiento.getValue() == null
        );

        btnCancelar.setDisable(ocupado || reserva == null);
        btnRecargar.setDisable(bloqueado);

        boolean permitePago = !ocupado && reservaVigente();

        txtReferencia.setDisable(!permitePago);
        chkPagoConfirmado.setDisable(!permitePago);

        btnConfirmar.setDisable(
                !permitePago || !chkPagoConfirmado.isSelected()
        );

        btnNuevaVenta.setDisable(ocupado);

        if (lblReserva.getScene() != null
                && lblReserva.getScene().getRoot()
                instanceof BorderPane menu
                && menu.getLeft() != null) {

            menu.getLeft().setDisable(
                    ocupado || reserva != null
            );
        }
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setStyle(
                error
                        ? "-fx-text-fill: #b91c1c;"
                        : "-fx-text-fill: #166534;"
        );

        lblMensaje.setText(mensaje);
    }
}