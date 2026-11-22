package app.web;

import app.Main;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

public class Api implements HttpHandler {

    private final AreaApi[] areas;

    public Api(AreaApi... areas) {
        this.areas = areas;
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            if (!Seguridad.permitir(intercambio)) {
                return;
            }
            Peticion peticion = new Peticion(intercambio);
            for (AreaApi area : areas) {
                if (area.atender(peticion)) {
                    return;
                }
            }
            Respuesta.error(intercambio, 404, "No existe la ruta " + peticion.metodo() + " " + intercambio.getRequestURI().getPath());
        } catch (ErrorWeb ex) {
            Respuesta.error(intercambio, ex.getCodigo(), ex.getMessage());
        } catch (NoSuchFileException ex) {
            Respuesta.error(intercambio, 404, "No se encontró el archivo " + ex.getMessage() + ". " + Main.explicacionCarpeta());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Respuesta.error(intercambio, 400, ex.getMessage());
        } catch (Exception ex) {
            Respuesta.error(intercambio, 500, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }
}
