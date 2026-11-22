package ia;

import java.nio.file.Path;

public class ResultadoIA {

    private final String funcion;
    private final String contenido;
    private final boolean diagrama;
    private final boolean usoApi;
    private final String aviso;
    private final String coherencia;
    private final Path html;
    private final Path evidencia;

    public ResultadoIA(String funcion, String contenido, boolean diagrama, boolean usoApi, String aviso,
                       String coherencia, Path html, Path evidencia) {
        this.funcion = funcion;
        this.contenido = contenido;
        this.diagrama = diagrama;
        this.usoApi = usoApi;
        this.aviso = aviso;
        this.coherencia = coherencia;
        this.html = html;
        this.evidencia = evidencia;
    }

    public String getFuncion() {
        return funcion;
    }

    public String getContenido() {
        return contenido;
    }

    public boolean esDiagrama() {
        return diagrama;
    }

    public boolean usoApi() {
        return usoApi;
    }

    public String getAviso() {
        return aviso;
    }

    public String getCoherencia() {
        return coherencia;
    }

    public Path getHtml() {
        return html;
    }

    public Path getEvidencia() {
        return evidencia;
    }

    public String origen() {
        return usoApi ? "Generado por la API de IA" : "Generado por el respaldo local (sin IA)";
    }
}
