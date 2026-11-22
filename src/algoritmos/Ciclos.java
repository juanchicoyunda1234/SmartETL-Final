package algoritmos;

import estructuras.ListaSecuencial;
import estructuras.Nodo;
import estructuras.Pila;
import grafo.Grafo;
import grafo.Vertice;
import model.Aeropuerto;

public final class Ciclos {

    private Ciclos() {
    }

    public static ListaSecuencial<Aeropuerto> detectar(Grafo<Aeropuerto> grafo) {
        int corrida = Vertice.nuevaCorrida();
        for (Nodo<Vertice<Aeropuerto>> n = grafo.getVertices().primerNodo(); n != null; n = n.getSiguiente()) {
            if (!n.getDato().fueVisitado(corrida)) {
                n.getDato().visitar(corrida, 0, null);
                ListaSecuencial<Aeropuerto> ciclo = visitar(n.getDato(), corrida);
                if (!ciclo.estaVacia()) {
                    return ciclo;
                }
            }
        }
        return new ListaSecuencial<>();
    }

    private static ListaSecuencial<Aeropuerto> visitar(Vertice<Aeropuerto> actual, int corrida) {
        for (Nodo<Vertice<Aeropuerto>> n = actual.getAdyacentes().primerNodo(); n != null; n = n.getSiguiente()) {
            Vertice<Aeropuerto> vecino = n.getDato();
            if (!vecino.fueVisitado(corrida)) {
                vecino.visitar(corrida, actual.getNivel() + 1, actual);
                ListaSecuencial<Aeropuerto> ciclo = visitar(vecino, corrida);
                if (!ciclo.estaVacia()) {
                    return ciclo;
                }
            } else if (vecino.getEstado() == Vertice.GRIS && vecino != actual && vecino != actual.getPadre()) {
                return construirCiclo(actual, vecino);
            }
        }
        actual.setEstado(Vertice.NEGRO);
        return new ListaSecuencial<>();
    }

    private static ListaSecuencial<Aeropuerto> construirCiclo(Vertice<Aeropuerto> ultimo, Vertice<Aeropuerto> inicio) {
        Pila<Aeropuerto> pila = new Pila<>();
        pila.apilar(inicio.getDato());
        for (Vertice<Aeropuerto> v = ultimo; v != inicio; v = v.getPadre()) {
            pila.apilar(v.getDato());
        }
        pila.apilar(inicio.getDato());
        ListaSecuencial<Aeropuerto> ciclo = new ListaSecuencial<>();
        while (!pila.estaVacia()) {
            ciclo.insertarFinal(pila.desapilar());
        }
        return ciclo;
    }
}
