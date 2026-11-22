package grafo;

import estructuras.ListaEnlazada;

public class Vertice<T> {

    public static final int GRIS = 1;
    public static final int NEGRO = 2;

    private static int ultimaCorrida = 0;

    private final T dato;
    private final String etiqueta;
    private final ListaEnlazada<Vertice<T>> adyacentes = new ListaEnlazada<>();
    private final ListaEnlazada<T> externos = new ListaEnlazada<>();
    private final ListaEnlazada<T> llegadasExternas = new ListaEnlazada<>();
    private int marca;
    private int nivel;
    private int estado;
    private Vertice<T> padre;

    public Vertice(T dato, String etiqueta) {
        this.dato = dato;
        this.etiqueta = etiqueta;
    }

    public static int nuevaCorrida() {
        ultimaCorrida++;
        return ultimaCorrida;
    }

    public boolean fueVisitado(int corrida) {
        return marca == corrida;
    }

    public void visitar(int corrida, int nivel, Vertice<T> padre) {
        this.marca = corrida;
        this.nivel = nivel;
        this.padre = padre;
        this.estado = GRIS;
    }

    public boolean agregarAdyacente(Vertice<T> destino) {
        if (adyacentes.contiene(destino)) {
            return false;
        }
        adyacentes.insertarFinal(destino);
        return true;
    }

    public boolean agregarExterno(T destino) {
        if (externos.contiene(destino)) {
            return false;
        }
        externos.insertarFinal(destino);
        return true;
    }

    public boolean agregarLlegadaExterna(T origen) {
        if (llegadasExternas.contiene(origen)) {
            return false;
        }
        llegadasExternas.insertarFinal(origen);
        return true;
    }

    public T getDato() {
        return dato;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public ListaEnlazada<Vertice<T>> getAdyacentes() {
        return adyacentes;
    }

    public ListaEnlazada<T> getExternos() {
        return externos;
    }

    public ListaEnlazada<T> getLlegadasExternas() {
        return llegadasExternas;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }

    public Vertice<T> getPadre() {
        return padre;
    }

    public void setPadre(Vertice<T> padre) {
        this.padre = padre;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
