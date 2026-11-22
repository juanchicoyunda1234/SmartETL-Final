package algoritmos;

import estructuras.Cola;
import estructuras.ListaSecuencial;
import grafo.MapaPaises;
import grafo.Vertice;
import ia.Evento;
import ia.RegistroEventos;
import model.Aeropuerto;

public final class BFS {

    public static final int SIN_LIMITE = -1;

    private BFS() {
    }

    public static ResultadoRecorrido recorrer(Vertice<Aeropuerto> origen, MapaPaises mapa, int nivelMaximo,
                                             RegistroEventos registro) {
        long inicio = System.nanoTime();
        int corrida = Vertice.nuevaCorrida();
        ListaSecuencial<Vertice<Aeropuerto>> visitados = new ListaSecuencial<>();
        Cola<Vertice<Aeropuerto>> cola = new Cola<>();
        String alcance = mapa == null ? "solo rutas internas" : "cruzando fronteras";
        registrar(registro, new Evento("inicio", "Inicio: BFS desde " + origen + " (" + alcance + ")",
                origen.getEtiqueta(), origen.getDato().getPais(), new String[0], new String[0], 0, 0));
        origen.visitar(corrida, 0, null);
        visitados.insertarFinal(origen);
        cola.encolar(origen);
        registrar(registro, new Evento("encolar", "Encolar " + origen, origen.getEtiqueta(), origen.getDato().getPais(),
                new String[0], cola.primeros(Evento.LIMITE_VISTA), cola.tamanio(), 0));
        int nivel = 0;
        int paso = 0;
        while (!cola.estaVacia()) {
            int enCapa = cola.tamanio();
            for (int k = 0; k < enCapa; k++) {
                Vertice<Aeropuerto> actual = cola.desencolar();
                paso++;
                ListaSecuencial<Vertice<Aeropuerto>> nuevos = new ListaSecuencial<>();
                if (nivelMaximo == SIN_LIMITE || nivel < nivelMaximo) {
                    ListaSecuencial<Vertice<Aeropuerto>> vecinos = Vecinos.de(actual, mapa);
                    for (int i = 0; i < vecinos.tamanio(); i++) {
                        Vertice<Aeropuerto> vecino = vecinos.obtener(i);
                        if (!vecino.fueVisitado(corrida)) {
                            vecino.visitar(corrida, nivel + 1, actual);
                            visitados.insertarFinal(vecino);
                            cola.encolar(vecino);
                            nuevos.insertarFinal(vecino);
                        }
                    }
                }
                registrar(registro, new Evento("desencolar", "Paso " + paso + ": desencolar " + actual,
                        actual.getEtiqueta(), actual.getDato().getPais(), Vecinos.etiquetas(nuevos),
                        cola.primeros(Evento.LIMITE_VISTA), cola.tamanio(), nivel));
            }
            registrar(registro, new Evento("fin_nivel", "Fin del nivel " + nivel + ": " + enCapa + " aeropuertos",
                    String.valueOf(enCapa), "", new String[0], new String[0], cola.tamanio(), nivel));
            nivel++;
        }
        registrar(registro, new Evento("fin", "Fin: " + visitados.tamanio() + " aeropuertos visitados",
                String.valueOf(visitados.tamanio()), "", new String[0], new String[0], 0, nivel - 1));
        return new ResultadoRecorrido("BFS", origen, visitados, (System.nanoTime() - inicio) / 1_000_000.0);
    }

    private static void registrar(RegistroEventos registro, Evento evento) {
        if (registro != null) {
            registro.agregar(evento);
        }
    }
}
