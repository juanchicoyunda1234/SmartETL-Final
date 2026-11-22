package app.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class Respuesta {

    private Respuesta() {
    }

    public static void json(HttpExchange intercambio, int codigo, Object json) throws IOException {
        enviar(intercambio, codigo, "application/json; charset=utf-8", json.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void error(HttpExchange intercambio, int codigo, String mensaje) throws IOException {
        json(intercambio, codigo, new ObjetoJson().texto("error", mensaje == null ? "Error sin mensaje" : mensaje));
    }

    public static void enviar(HttpExchange intercambio, int codigo, String tipo, byte[] contenido) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        boolean sinCuerpo = intercambio.getRequestMethod().equalsIgnoreCase("HEAD");
        intercambio.sendResponseHeaders(codigo, sinCuerpo ? -1 : contenido.length);
        if (!sinCuerpo) {
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(contenido);
            }
        }
        intercambio.close();
    }
}
