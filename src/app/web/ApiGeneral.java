package app.web;

import com.sun.net.httpserver.HttpExchange;
import etl.Proyecto;
import ia.ConfiguracionIA;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ApiGeneral implements AreaApi {

    private static final long LATIDO = 15000;

    private final ContextoWeb contexto;

    public ApiGeneral(ContextoWeb contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean atender(Peticion p) throws Exception {
        if (p.es("GET", "estado")) {
            Respuesta.json(p.getIntercambio(), 200, estado());
            return true;
        }
        if (p.es("GET", "reporte")) {
            Respuesta.json(p.getIntercambio(), 200, contexto.getReporte().reporte());
            return true;
        }
        if (p.es("GET", "tareas", "*")) {
            Respuesta.json(p.getIntercambio(), 200, tarea(p.parte(1)).comoJson());
            return true;
        }
        if (p.es("GET", "tareas", "*", "eventos")) {
            transmitir(p.getIntercambio(), tarea(p.parte(1)));
            return true;
        }
        return false;
    }

    private TareaWeb tarea(String id) throws ErrorWeb {
        TareaWeb tarea = contexto.getTareas().buscar(id);
        if (tarea == null) {
            throw new ErrorWeb(404, "No existe la tarea " + id);
        }
        return tarea;
    }

    private String estado() {
        TareaWeb enCurso = contexto.getTareas().enCurso();
        ConfiguracionIA ia = contexto.getSesion().getConfiguracion();
        return new ObjetoJson()
                .texto("carpeta", Proyecto.getBase().toString())
                .entero("puerto", contexto.getPuerto())
                .booleano("datosEncontrados", Proyecto.esRaiz(Proyecto.getBase()))
                .booleano("recursos", Proyecto.tieneRecursos())
                .booleano("etlEjecutado", contexto.getSesion().tieneEtl())
                .booleano("etlReal", contexto.getSesion().ultimoEtlEsReal())
                .crudo("tareaEnCurso", enCurso == null ? null : new ObjetoJson().texto("id", enCurso.getId())
                        .texto("nombre", enCurso.getNombre()))
                .crudo("ia", new ObjetoJson().texto("proveedor", ia.getProveedor()).texto("modelo", ia.getModelo())
                        .texto("modo", ia.getModo()).booleano("tieneClave", ia.tieneClave())
                        .texto("validacion", contexto.getEstadoValidacion())
                        .texto("detalle", contexto.getDetalleValidacion()))
                .toString();
    }

    private void transmitir(HttpExchange intercambio, TareaWeb tarea) throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.sendResponseHeaders(200, 0);
        try (OutputStream salida = intercambio.getResponseBody()) {
            int enviados = 0;
            while (true) {
                while (enviados < tarea.cantidadAvisos()) {
                    escribir(salida, "aviso", new ObjetoJson().texto("mensaje", tarea.aviso(enviados))
                            .entero("ms", tarea.momento(enviados)).toString());
                    enviados++;
                }
                if (tarea.estaTerminada() && enviados >= tarea.cantidadAvisos()) {
                    if (tarea.getError() == null) {
                        escribir(salida, "resultado", new ObjetoJson().entero("ms", tarea.getDuracion())
                                .crudo("datos", tarea.getResultado()).toString());
                    } else {
                        escribir(salida, "fallo", new ObjetoJson().entero("codigo", tarea.getCodigoError())
                                .texto("error", tarea.getError()).toString());
                    }
                    return;
                }
                tarea.esperarCambio(enviados, LATIDO);
                if (enviados >= tarea.cantidadAvisos() && !tarea.estaTerminada()) {
                    salida.write(": latido\n\n".getBytes(StandardCharsets.UTF_8));
                    salida.flush();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ignorada) {
        } finally {
            intercambio.close();
        }
    }

    private void escribir(OutputStream salida, String evento, String datos) throws IOException {
        salida.write(("event: " + evento + "\ndata: " + datos + "\n\n").getBytes(StandardCharsets.UTF_8));
        salida.flush();
    }

    public static void tareaIniciada(HttpExchange intercambio, TareaWeb tarea) throws IOException {
        Respuesta.json(intercambio, 202, new ObjetoJson().texto("id", tarea.getId()).texto("nombre", tarea.getNombre())
                .texto("eventos", "/api/tareas/" + tarea.getId() + "/eventos"));
    }
}
