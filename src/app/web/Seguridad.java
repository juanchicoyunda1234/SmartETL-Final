package app.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public final class Seguridad {

    private static int puerto;

    private Seguridad() {
    }

    static void setPuerto(int puertoEnUso) {
        puerto = puertoEnUso;
    }

    public static boolean permitir(HttpExchange intercambio) throws IOException {
        String anfitrion = intercambio.getRequestHeaders().getFirst("Host");
        if (anfitrion == null || !(anfitrion.equals("127.0.0.1:" + puerto) || anfitrion.equals("localhost:" + puerto))) {
            Respuesta.error(intercambio, 403, "Solo se aceptan peticiones dirigidas a 127.0.0.1 o localhost");
            return false;
        }
        String metodo = intercambio.getRequestMethod().toUpperCase();
        boolean modifica = !(metodo.equals("GET") || metodo.equals("HEAD"));
        if (modifica && !"1".equals(intercambio.getRequestHeaders().getFirst("X-SmartETL"))) {
            Respuesta.error(intercambio, 403, "Petición rechazada: falta la cabecera X-SmartETL");
            return false;
        }
        return true;
    }
}
