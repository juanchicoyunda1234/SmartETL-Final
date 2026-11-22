package algoritmos;

import estructuras.ListaSecuencial;
import estructuras.Nodo;
import grafo.MapaPaises;
import grafo.Vertice;
import model.Aeropuerto;

public final class Vecinos {

    private Vecinos() {
    }

    public static ListaSecuencial<Vertice<Aeropuerto>> de(Vertice<Aeropuerto> vertice, MapaPaises mapa) {
        ListaSecuencial<Vertice<Aeropuerto>> vecinos = new ListaSecuencial<>();
        for (Nodo<Vertice<Aeropuerto>> n = vertice.getAdyacentes().primerNodo(); n != null; n = n.getSiguiente()) {
            vecinos.insertarFinal(n.getDato());
        }
        if (mapa != null) {
            for (Nodo<Aeropuerto> n = vertice.getExternos().primerNodo(); n != null; n = n.getSiguiente()) {
                Vertice<Aeropuerto> externo = mapa.vertice(n.getDato());
                if (externo != null) {
                    vecinos.insertarFinal(externo);
                }
            }
        }
        return vecinos;
    }

    public static String[] etiquetas(ListaSecuencial<Vertice<Aeropuerto>> vertices) {
        String[] etiquetas = new String[vertices.tamanio()];
        for (int i = 0; i < etiquetas.length; i++) {
            etiquetas[i] = vertices.obtener(i).getEtiqueta();
        }
        return etiquetas;
    }
}
