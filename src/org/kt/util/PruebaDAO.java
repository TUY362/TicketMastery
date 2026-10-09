package org.kt.util;

import org.kt.dao.impl.AsientoDAOImpl;
import org.kt.dao.impl.BoletoDAOImpl;
import org.kt.dao.impl.ClienteDAOImpl;
import org.kt.dao.impl.EventoDAOImpl;
import org.kt.dao.impl.UsuarioDAOImpl;
import org.kt.dao.impl.VentaTransaccionDAOImpl;
import org.kt.dao.impl.ZonaLugarDAOImpl;

public class PruebaDAO {

    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Eventos: "
                    + new EventoDAOImpl().listar().size());

            System.out.println("Zonas: "
                    + new ZonaLugarDAOImpl().listar().size());

            System.out.println("Asientos: "
                    + new AsientoDAOImpl().listar().size());

            System.out.println("Clientes: "
                    + new ClienteDAOImpl().listar().size());

            System.out.println("Usuarios: "
                    + new UsuarioDAOImpl().listar().size());

            System.out.println("Ventas: "
                    + new VentaTransaccionDAOImpl().listar().size());

            System.out.println("Boletos: "
                    + new BoletoDAOImpl().listar().size());

            System.out.println("Todas las consultas finalizaron correctamente.");
        } finally {
            Conexion.getInstancia().cerrarConexion();
        }
    }
}