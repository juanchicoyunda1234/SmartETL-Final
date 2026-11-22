package app.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Peticion {

    private static final int CUERPO_MAXIMO = 1_000_000;

    private final HttpExchange intercambio;
    private final String[] partes;
    private String cuerpo;

    public Peticion(HttpExchange intercambio) {
        this.intercambio = intercambio;
        String ruta = intercambio.getRequestURI().getPath();
        String limpia = ruta.startsWith("/api/") ? ruta.substring(5) : ruta;
        this.partes = limpia.isEmpty() ? new String[0] : limpia.split("/");
        for (int i = 0; i < partes.length; i++) {
            partes[i] = decodificar(partes[i]);
        }
    }

    public HttpExchange getIntercambio() {
        return intercambio;
    }

    public String metodo() {
        return intercambio.getRequestMethod().toUpperCase();
    }

    public boolean es(String metodo, String... ruta) {
        if (!metodo().equals(metodo) || partes.length != ruta.length) {
            return false;
        }
        for (int i = 0; i < ruta.length; i++) {
            if (!ruta[i].equals("*") && !ruta[i].equals(partes[i])) {
                return false;
            }
        }
        return true;
    }

    public String parte(int indice) {
        return indice < partes.length ? partes[indice] : "";
    }

    public String parametro(String nombre, String porDefecto) {
        String consulta = intercambio.getRequestURI().getRawQuery();
        if (consulta == null) {
            return porDefecto;
        }
        for (String par : consulta.split("&")) {
            int igual = par.indexOf('=');
            String clave = decodificar(igual < 0 ? par : par.substring(0, igual));
            if (clave.equals(nombre)) {
                return igual < 0 ? "" : decodificar(par.substring(igual + 1));
            }
        }
        return porDefecto;
    }

    public int parametroEntero(String nombre, int porDefecto) {
        try {
            return Integer.parseInt(parametro(nombre, String.valueOf(porDefecto)).trim());
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }

    public String cuerpo() throws IOException {
        if (cuerpo == null) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream entrada = intercambio.getRequestBody()) {
                byte[] bloque = new byte[8192];
                int leidos;
                while ((leidos = entrada.read(bloque)) > 0) {
                    bytes.write(bloque, 0, leidos);
                    if (bytes.size() > CUERPO_MAXIMO) {
                        throw new IOException("El cuerpo de la petición es demasiado grande");
                    }
                }
            }
            cuerpo = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
        return cuerpo;
    }

    public LectorJson json() throws IOException {
        return new LectorJson(cuerpo());
    }

    private static String decodificar(String texto) {
        try {
            return URLDecoder.decode(texto, "UTF-8");
        } catch (Exception ex) {
            return texto;
        }
    }
}
