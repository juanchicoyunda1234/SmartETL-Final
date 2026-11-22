package algoritmos;

import estructuras.ListaSecuencial;

public class ResultadoOrdenamiento<T> {

    private final String algoritmo;
    private final ListaSecuencial<T> lista;
    private final long comparaciones;
    private final long movimientos;
    private final double milisegundos;

    public ResultadoOrdenamiento(String algoritmo, ListaSecuencial<T> lista, long comparaciones, long movimientos,
                                 double milisegundos) {
        this.algoritmo = algoritmo;
        this.lista = lista;
        this.comparaciones = comparaciones;
        this.movimientos = movimientos;
        this.milisegundos = milisegundos;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public ListaSecuencial<T> getLista() {
        return lista;
    }

    public long getComparaciones() {
        return comparaciones;
    }

    public long getMovimientos() {
        return movimientos;
    }

    public double getMilisegundos() {
        return milisegundos;
    }

    public String nombreMovimientos() {
        return algoritmo.equals("Burbuja") ? "intercambios" : "desplazamientos";
    }
}
