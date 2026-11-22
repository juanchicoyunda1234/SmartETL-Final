package app.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import etl.Proyecto;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;

public class Estaticos implements HttpHandler {

    private static final String[] CARPETAS = {"web", "recursos", "diagramas"};

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        if (!Seguridad.permitir(intercambio)) {
            return;
        }
        String metodo = intercambio.getRequestMethod().toUpperCase();
        if (!metodo.equals("GET") && !metodo.equals("HEAD")) {
            Respuesta.error(intercambio, 405, "Solo se permite GET en los archivos");
            return;
        }
        String ruta = URLDecoder.decode(intercambio.getRequestURI().getPath(), "UTF-8");
        if (ruta.equals("/") || ruta.equals("/index.html")) {
            ruta = "/web/index.html";
        }
        Path archivo = resolver(ruta);
        if (archivo == null || !Files.isRegularFile(archivo)) {
            Respuesta.error(intercambio, 404, "No existe " + ruta);
            return;
        }
        Respuesta.enviar(intercambio, 200, tipo(archivo.getFileName().toString()), Files.readAllBytes(archivo));
    }

    public static Path resolver(String ruta) {
        for (String carpeta : CARPETAS) {
            String prefijo = "/" + carpeta + "/";
            if (ruta.startsWith(prefijo)) {
                Path base = Proyecto.getBase().resolve(carpeta).normalize();
                Path archivo = base.resolve(ruta.substring(prefijo.length())).normalize();
                return archivo.startsWith(base) ? archivo : null;
            }
        }
        return null;
    }

    public static String tipo(String nombre) {
        String minusculas = nombre.toLowerCase();
        if (minusculas.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (minusculas.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (minusculas.endsWith(".js") || minusculas.endsWith(".mjs")) {
            return "text/javascript; charset=utf-8";
        }
        if (minusculas.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (minusculas.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (minusculas.endsWith(".png")) {
            return "image/png";
        }
        if (minusculas.endsWith(".woff2")) {
            return "font/woff2";
        }
        return "text/plain; charset=utf-8";
    }
}
