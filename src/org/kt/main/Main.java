package org.kt.main;

import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kt.util.SessionContext;

public class Main extends Application {

    @Override
    public void start(Stage ventana) throws Exception {
        URL archivo = Main.class.getResource(
                "/org/kt/view/Login.fxml"
        );

        if (archivo == null) {
            throw new IllegalStateException(
                    "No se encontró Login.fxml en org.kt.view."
            );
        }

        FXMLLoader loader = new FXMLLoader(archivo);
        Parent raiz = loader.load();

        Scene escena = new Scene(raiz, 480, 500);

        ventana.setTitle("TicketMastery");
        ventana.setScene(escena);
        ventana.setMinWidth(440);
        ventana.setMinHeight(500);
        ventana.centerOnScreen();
        ventana.show();
    }

    @Override
    public void stop() {
        SessionContext.getInstancia().cerrarSesion();
    }

    public static void main(String[] args) {
        launch(args);
    }
}