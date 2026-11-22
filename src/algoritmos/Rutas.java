package algoritmos;

import estructuras.Cola;
import estructuras.ListaSecuencial;
import estructuras.Pila;
import grafo.MapaPaises;
import grafo.Vertice;
import model.Aeropuerto;

public final class Rutas {

    private Rutas() {
    }

    public static ListaSecuencial<Aeropuerto> buscar(Vertice<Aeropuerto> origen, Vertice<Aeropuerto> destino,
                                                     MapaPaises mapa) {
        int corrida = Vertice.nuevaCorrida();
        Cola<Vertice<Aeropuerto>> cola = new Cola<>();
        origen.visitar(corrida, 0, null);
        cola.encolar(origen);
        while (!cola.estaVacia()) {
            Vertice<Aeropuerto> actual = cola.desencolar();
            if (actual == destino) {
                return reconstruir(destino);
            }
            ListaSecuencial<Vertice<Aeropuerto>> vecinos = Vecinos.de(actual, mapa);
            for (int i = 0; i < vecinos.tamanio(); i++) {
                Vertice<Aeropuerto> vecino = vecinos.obtener(i);
                if (!vecino.fueVisitado(corrida)) {
                    vecino.visitar(corrida, actual.getNivel() + 1, actual);
                    cola.encolar(vecino);
                }
            }
        }
        return new ListaSecuencial<>();
    }

    private static ListaSecuencial<Aeropuerto> reconstruir(Vertice<Aeropuerto> destino) {
        Pila<Aeropuerto> pila = new Pila<>();
        for (Vertice<Aeropuerto> v = destino; v != null; v = v.getPadre()) {
            pila.apilar(v.getDato());
        }
        ListaSecuencial<Aeropuerto> camino = new ListaSecuencial<>();
        while (!pila.estaVacia()) {
            camino.insertarFinal(pila.desapilar());
        }
        return camino;
    }
}
