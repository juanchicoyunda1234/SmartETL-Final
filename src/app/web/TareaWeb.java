package app.web;

import estructuras.ListaSecuencial;

public class TareaWeb {

    private final String id;
    private final String nombre;
    private final long inicio = System.nanoTime();
    private final ListaSecuencial<String> avisos = new ListaSecuencial<>();
    private final ListaSecuencial<Long> momentos = new ListaSecuencial<>();
    private boolean terminada;
    private String resultado;
    private String error;
    private int codigoError;
    private long duracion;

    public TareaWeb(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public synchronized void avisar(String mensaje) {
        avisos.insertarFinal(mensaje);
        momentos.insertarFinal(milisegundos());
        notifyAll();
    }

    public synchronized void terminar(String resultadoJson) {
        resultado = resultadoJson;
        cerrar();
    }

    public synchronized void fallar(int codigo, String mensaje) {
        codigoError = codigo;
        error = mensaje;
        cerrar();
    }

    private void cerrar() {
        duracion = milisegundos();
        terminada = true;
        notifyAll();
    }

    public synchronized void esperarCambio(int avisosVistos, long esperaMaxima) throws InterruptedException {
        if (!terminada && avisos.tamanio() <= avisosVistos) {
            wait(esperaMaxima);
        }
    }

    private long milisegundos() {
        return (System.nanoTime() - inicio) / 1_000_000;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public synchronized boolean estaTerminada() {
        return terminada;
    }

    public synchronized int cantidadAvisos() {
        return avisos.tamanio();
    }

    public synchronized String aviso(int indice) {
        return avisos.obtener(indice);
    }

    public synchronized long momento(int indice) {
        return momentos.obtener(indice);
    }

    public synchronized String getResultado() {
        return resultado;
    }

    public synchronized String getError() {
        return error;
    }

    public synchronized int getCodigoError() {
        return codigoError;
    }

    public synchronized long getDuracion() {
        return terminada ? duracion : milisegundos();
    }

    public synchronized String comoJson() {
        return new ObjetoJson().texto("id", id).texto("nombre", nombre).booleano("terminada", terminada)
                .entero("ms", getDuracion()).entero("avisos", avisos.tamanio()).crudo("resultado", resultado)
                .texto("error", error).toString();
    }
}
