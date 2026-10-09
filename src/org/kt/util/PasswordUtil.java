package org.kt.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static boolean verificar(
            String password,
            String hashGuardado) throws GeneralSecurityException {

        if (password == null || hashGuardado == null) {
            return false;
        }

        String[] partes = hashGuardado.split("\\$", -1);

        if (partes.length != 4
                || !"pbkdf2_sha256".equals(partes[0])) {
            return false;
        }

        int iteraciones;
        byte[] salt;
        byte[] hashEsperado;

        try {
            iteraciones = Integer.parseInt(partes[1]);
            salt = Base64.getDecoder().decode(partes[2]);
            hashEsperado = Base64.getDecoder().decode(partes[3]);
        } catch (IllegalArgumentException excepcion) {
            return false;
        }

        if (iteraciones != 600000
                || salt.length != 16
                || hashEsperado.length != 32) {
            return false;
        }

        char[] caracteres = password.toCharArray();

        PBEKeySpec especificacion = new PBEKeySpec(
                caracteres,
                salt,
                iteraciones,
                256
        );

        Arrays.fill(caracteres, '\0');

        byte[] hashCalculado = null;

        try {
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
            );

            hashCalculado = fabrica.generateSecret(
                    especificacion
            ).getEncoded();

            return MessageDigest.isEqual(
                    hashEsperado,
                    hashCalculado
            );
        } finally {
            especificacion.clearPassword();

            if (hashCalculado != null) {
                Arrays.fill(hashCalculado, (byte) 0);
            }
        }
    }
}