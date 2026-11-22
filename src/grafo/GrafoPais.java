package grafo;

import estructuras.Nodo;
import model.Aeropuerto;

import java.util.Comparator;

public class GrafoPais {

    public static final Comparator<GrafoPais> POR_NOMBRE = (a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre());

    private final String nombre;
    private final Grafo<Aeropuerto> grafo = new Grafo<>();
    private boolean construido;
    private int salidasExterior;
    private int llegadasExterior;
    private double milisegundos;

    public GrafoPais(String nombre) {
        this.nombre = nombre;
    }

    public static GrafoPais clave(String nombre) {
        return new GrafoPais(nombre);
    }

    void marcarConstruido(double milisegundos) {
        this.construido = true;
        this.milisegundos = milisegundos;
    }

    void contarSalida() {
        salidasExterior++;
    }

    void contarLlegada() {
        llegadasExterior++;
    }

    public Agrupacion externosPorPais() {
        Agrupacion grupos = new Agrupacion();
        for (Nodo<Vertice<Aeropuerto>> n = grafo.getVertices().primerNodo(); n != null; n = n.getSiguiente()) {
            for (Nodo<Aeropuerto> e = n.getDato().getExternos().primerNodo(); e != null; e = e.getSiguiente()) {
                grupos.agregar(e.getDato().getPais(), n.getDato().getEtiqueta() + "-" + e.getDato().getIata());
            }
        }
        return grupos;
    }

    public static Agrupacion externosPorPais(Vertice<Aeropuerto> vertice) {
        Agrupacion grupos = new Agrupacion();
        for (Nodo<Aeropuerto> e = vertice.getExternos().primerNodo(); e != null; e = e.getSiguiente()) {
            grupos.agregar(e.getDato().getPais(), e.getDato().getIata());
        }
        return grupos;
    }

    public String getNombre() {
        return nombre;
    }

    public Grafo<Aeropuerto> getGrafo() {
        return grafo;
    }

    public boolean estaConstruido() {
        return construido;
    }

    public int getVertices() {
        return grafo.numeroVertices();
    }

    public int getRutasInternas() {
        return grafo.numeroAristas();
    }

    public int getSalidasExterior() {
        return salidasExterior;
    }

    public int getLlegadasExterior() {
        return llegadasExterior;
    }

    public double getMilisegundos() {
        return milisegundos;
    }

    public String resumen() {
        return nombre + ": " + getVertices() + " aeropuertos, " + getRutasInternas() + " rutas internas, "
                + salidasExterior + " salen al exterior, " + llegadasExterior + " llegan del exterior";
    }

    @Override
    public String toString() {
        return nombre;
    }
}
