package algoritmos;

import estructuras.ListaSecuencial;
import grafo.Agrupacion;
import grafo.Vertice;
import model.Aeropuerto;

public class ResultadoRecorrido {

    private final String algoritmo;
    private final Vertice<Aeropuerto> origen;
    private final ListaSecuencial<Vertice<Aeropuerto>> visitados;
    private final double milisegundos;

    public ResultadoRecorrido(String algoritmo, Vertice<Aeropuerto> origen, ListaSecuencial<Vertice<Aeropuerto>> visitados,
                              double milisegundos) {
        this.algoritmo = algoritmo;
        this.origen = origen;
        this.visitados = visitados;
        this.milisegundos = milisegundos;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public Vertice<Aeropuerto> getOrigen() {
        return origen;
    }

    public ListaSecuencial<Vertice<Aeropuerto>> getVisitados() {
        return visitados;
    }

    public int cantidad() {
        return visitados.tamanio();
    }

    public double getMilisegundos() {
        return milisegundos;
    }

    public int nivelMaximo() {
        int maximo = 0;
        for (int i = 0; i < visitados.tamanio(); i++) {
            maximo = Math.max(maximo, visitados.obtener(i).getNivel());
        }
        return maximo;
    }

    public int cantidadEnNivel(int nivel) {
        return visitados.contar(v -> v.getNivel() == nivel);
    }

    public Agrupacion paisesEnNivel(int nivel) {
        Agrupacion grupos = new Agrupacion();
        for (int i = 0; i < visitados.tamanio(); i++) {
            Vertice<Aeropuerto> v = visitados.obtener(i);
            if (v.getNivel() == nivel) {
                grupos.agregar(v.getDato().getPais(), v.getEtiqueta());
            }
        }
        return grupos;
    }
}
