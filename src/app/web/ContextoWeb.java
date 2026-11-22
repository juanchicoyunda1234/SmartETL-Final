package app.web;

import app.Sesion;

public class ContextoWeb {

    private final Sesion sesion;
    private final GestorTareas tareas = new GestorTareas();
    private final ReporteSesion reporte = new ReporteSesion();
    private int puerto;
    private String estadoValidacion = "sin_validar";
    private String detalleValidacion = "";
    private String origenClave = "";
    private final Object candadoGrafo = new Object();

    public ContextoWeb(Sesion sesion) {
        this.sesion = sesion;
    }

    public Sesion getSesion() {
        return sesion;
    }

    public GestorTareas getTareas() {
        return tareas;
    }

    public ReporteSesion getReporte() {
        return reporte;
    }

    public int getPuerto() {
        return puerto;
    }

    void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public synchronized String getEstadoValidacion() {
        return estadoValidacion;
    }

    public synchronized String getDetalleValidacion() {
        return detalleValidacion;
    }

    public synchronized void setValidacion(String estado, String detalle) {
        this.estadoValidacion = estado;
        this.detalleValidacion = detalle;
    }

    public synchronized String getOrigenClave() {
        return origenClave;
    }

    public synchronized void setOrigenClave(String origenClave) {
        this.origenClave = origenClave;
    }

    public Object getCandadoGrafo() {
        return candadoGrafo;
    }
}
