package algoritmos;

import estructuras.ListaSecuencial;
import estructuras.Pila;
import grafo.MapaPaises;
import grafo.Vertice;
import ia.Evento;
import ia.RegistroEventos;
import model.Aeropuerto;

public final class DFS {

    public static final int SIN_LIMITE = -1;

    private DFS() {
    }

    public static ResultadoRecorrido recorrer(Vertice<Aeropuerto> origen, MapaPaises mapa, int profundidadMaxima,
                                             RegistroEventos registro) {
        long inicio = System.nanoTime();
        int corrida = Vertice.nuevaCorrida();
        ListaSecuencial<Vertice<Aeropuerto>> visitados = new ListaSecuencial<>();
        Pila<Vertice<Aeropuerto>> pila = new Pila<>();
        String alcance = mapa == null ? "solo rutas internas" : "cruzando fronteras";
        registrar(registro, new Evento("inicio", "Inicio: DFS desde " + origen + " (" + alcance + ")",
                origen.getEtiqueta(), origen.getDato().getPais(), new String[0], new String[0], 0, 0));
        origen.setNivel(0);
        origen.setPadre(null);
        pila.apilar(origen);
        registrar(registro, new Evento("apilar", "Apilar " + origen, origen.getEtiqueta(), origen.getDato().getPais(),
                new String[0], pila.primeros(Evento.LIMITE_VISTA), pila.tamanio(), 0));
        int paso = 0;
        while (!pila.estaVacia()) {
            Vertice<Aeropuerto> actual = pila.desapilar();
            if (actual.fueVisitado(corrida)) {
                continue;
            }
            actual.visitar(corrida, actual.getNivel(), actual.getPadre());
            visitados.insertarFinal(actual);
            paso++;
            ListaSecuencial<Vertice<Aeropuerto>> apilados = new ListaSecuencial<>();
            if (profundidadMaxima == SIN_LIMITE || actual.getNivel() < profundidadMaxima) {
                ListaSecuencial<Vertice<Aeropuerto>> vecinos = Vecinos.de(actual, mapa);
                for (int i = vecinos.tamanio() - 1; i >= 0; i--) {
                    Vertice<Aeropuerto> vecino = vecinos.obtener(i);
                    if (!vecino.fueVisitado(corrida)) {
                        vecino.setNivel(actual.getNivel() + 1);
                        vecino.setPadre(actual);
                        pila.apilar(vecino);
                        apilados.insertarFinal(vecino);
                    }
                }
            }
            registrar(registro, new Evento("desapilar", "Paso " + paso + ": desapilar " + actual,
                    actual.getEtiqueta(), actual.getDato().getPais(), Vecinos.etiquetas(apilados),
                    pila.primeros(Evento.LIMITE_VISTA), pila.tamanio(), actual.getNivel()));
        }
        registrar(registro, new Evento("fin", "Fin: " + visitados.tamanio() + " aeropuertos visitados",
                String.valueOf(visitados.tamanio()), "", new String[0], new String[0], 0, -1));
        return new ResultadoRecorrido("DFS", origen, visitados, (System.nanoTime() - inicio) / 1_000_000.0);
    }

    private static void registrar(RegistroEventos registro, Evento evento) {
        if (registro != null) {
            registro.agregar(evento);
        }
    }
}
