package org.kt.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public final class QRUtil {

    private QRUtil() {
    }

    public static WritableImage generar(String codigo)
            throws WriterException {

        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException(
                    "El código del boleto es obligatorio."
            );
        }

        Map<EncodeHintType, Object> opciones =
                new EnumMap<>(EncodeHintType.class);

        opciones.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        opciones.put(EncodeHintType.MARGIN, 4);

        opciones.put(
                EncodeHintType.ERROR_CORRECTION,
                ErrorCorrectionLevel.M
        );

        BitMatrix matriz = new QRCodeWriter().encode(
                codigo,
                BarcodeFormat.QR_CODE,
                256,
                256,
                opciones
        );

        WritableImage imagen = new WritableImage(
                matriz.getWidth(),
                matriz.getHeight()
        );

        PixelWriter escritor = imagen.getPixelWriter();

        for (int y = 0; y < matriz.getHeight(); y++) {
            for (int x = 0; x < matriz.getWidth(); x++) {
                escritor.setArgb(
                        x,
                        y,
                        matriz.get(x, y)
                                ? 0xFF000000
                                : 0xFFFFFFFF
                );
            }
        }

        return imagen;
    }
}