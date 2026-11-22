package grafo;

import estructuras.ListaEnlazada;
import estructuras.Nodo;

public class Grafo<T> {

    private final ListaEnlazada<Vertice<T>> vertices = new ListaEnlazada<>();
    private int aristas;

    public Vertice<T> agregarVertice(T dato, String etiqueta) {
        Vertice<T> vertice = new Vertice<>(dato, etiqueta);
        vertices.insertarFinal(vertice);
        return vertice;
    }

    public boolean agregarArista(Vertice<T> origen, Vertice<T> destino) {
        if (origen.agregarAdyacente(destino)) {
            aristas++;
            return true;
        }
        return false;
    }

    public Vertice<T> buscarVertice(String etiqueta) {
        for (Nodo<Vertice<T>> n = vertices.primerNodo(); n != null; n = n.getSiguiente()) {
            if (n.getDato().getEtiqueta().equalsIgnoreCase(etiqueta)) {
                return n.getDato();
            }
        }
        return null;
    }

    public ListaEnlazada<Vertice<T>> getVertices() {
        return vertices;
    }

    public int numeroVertices() {
        return vertices.tamanio();
    }

    public int numeroAristas() {
        return aristas;
    }
}
