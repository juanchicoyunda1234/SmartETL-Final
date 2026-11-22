package pruebas;

import algoritmos.BFS;
import algoritmos.Ciclos;
import algoritmos.DFS;
import algoritmos.ResultadoRecorrido;
import algoritmos.Rutas;
import estructuras.ListaSecuencial;
import etl.DatosLimpios;
import etl.ProcesoETL;
import grafo.GrafoPais;
import grafo.MapaPaises;
import grafo.Vertice;
import ia.RegistroEventos;
import model.Aeropuerto;

public class PruebasHito4 {

    private static int pasaron = 0;
    private static int fallaron = 0;

    public static void main(String[] args) throws Exception {
        DatosLimpios datos = ProcesoETL.real().ejecutar();
        MapaPaises mapa = new MapaPaises(datos);
        pruebasPaises(mapa);
        pruebasRecorridos(mapa);
        pruebasTodos(mapa);
        System.out.println();
        System.out.println("Pruebas que pasaron: " + pasaron + " | fallaron: " + fallaron);
        if (fallaron > 0) {
            System.exit(1);
        }
    }

    private static void verificar(String nombre, boolean condicion) {
        if (condicion) {
            pasaron++;
            System.out.println("  OK   " + nombre);
        } else {
            fallaron++;
            System.out.println("  FALLA " + nombre);
        }
    }

    private static void verificarPais(MapaPaises mapa, String pais, int vertices, int internas, int salen, int llegan) {
        GrafoPais g = mapa.obtener(pais);
        verificar(pais + ": " + vertices + " vértices, " + internas + " internas, " + salen + " salen, " + llegan + " llegan",
                g.getVertices() == vertices && g.getRutasInternas() == internas && g.getSalidasExterior() == salen
                        && g.getLlegadasExterior() == llegan);
        if (g.getVertices() != vertices || g.getRutasInternas() != internas) {
            System.out.println("    obtenido: " + g.resumen());
        }
    }

    private static void pruebasPaises(MapaPaises mapa) {
        System.out.println("Grafos por país");
        verificarPais(mapa, "Ecuador", 14, 32, 26, 27);
        verificarPais(mapa, "Peru", 20, 54, 41, 41);
        verificarPais(mapa, "Colombia", 48, 144, 64, 64);
        verificarPais(mapa, "United States", 550, 5452, 1078, 1072);
        GrafoPais ecuador = mapa.obtener("ecuador");
        verificar("obtener no reconstruye un país ya construido", ecuador.getVertices() == 14);
        verificar("vértice UIO por IATA", mapa.verticePorIata("UIO").getDato().getCiudad().equals("Quito"));
        verificar("externos de Ecuador agrupados por país", ecuador.externosPorPais().tamanio() > 3);
    }

    private static void pruebasRecorridos(MapaPaises mapa) {
        System.out.println("Recorridos");
        Vertice<Aeropuerto> uio = mapa.verticePorIata("UIO");
        RegistroEventos registro = new RegistroEventos("BFS");
        ResultadoRecorrido interno = BFS.recorrer(uio, null, BFS.SIN_LIMITE, registro);
        verificar("BFS interno desde UIO visita 14 hasta nivel 2", interno.cantidad() == 14 && interno.nivelMaximo() == 2);
        verificar("BFS registra 14 pasos de desencolar", registro.contar("desencolar") == 14 && registro.contar("fin_nivel") == 3);
        ResultadoRecorrido nivelUno = BFS.recorrer(uio, null, 1, null);
        verificar("BFS con nivel máximo 1 no pasa del nivel 1", nivelUno.nivelMaximo() == 1
                && nivelUno.cantidad() == 1 + interno.cantidadEnNivel(1));
        ResultadoRecorrido dfs = DFS.recorrer(uio, null, DFS.SIN_LIMITE, new RegistroEventos("DFS"));
        verificar("DFS interno desde UIO también visita 14", dfs.cantidad() == 14);
        ResultadoRecorrido dfsLimitado = DFS.recorrer(uio, null, 1, null);
        verificar("DFS con profundidad 1", dfsLimitado.nivelMaximo() <= 1 && dfsLimitado.cantidad() == nivelUno.cantidad());
        ListaSecuencial<Aeropuerto> ruta = Rutas.buscar(uio, mapa.verticePorIata("CUZ"), mapa);
        verificar("UIO a CUZ en 2 vuelos", ruta.tamanio() == 3 && ruta.obtener(0).getIata().equals("UIO")
                && ruta.obtener(2).getIata().equals("CUZ"));
        ListaSecuencial<Aeropuerto> ciclo = Ciclos.detectar(mapa.obtener("Ecuador").getGrafo());
        verificar("ciclo de longitud 3 o más en Ecuador", ciclo.tamanio() >= 4
                && ciclo.obtener(0) == ciclo.obtener(ciclo.tamanio() - 1));
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < ciclo.tamanio(); i++) {
            texto.append(i == 0 ? "" : " -> ").append(ciclo.obtener(i).getIata());
        }
        System.out.println("  Ciclo: " + texto + " | Ruta UIO-CUZ vía " + ruta.obtener(1).getIata());
        ResultadoRecorrido mundo = BFS.recorrer(uio, mapa, BFS.SIN_LIMITE, new RegistroEventos("mundo"));
        verificar("BFS mundial desde UIO del orden de 3200 aeropuertos", mundo.cantidad() > 2500 && mundo.cantidad() < 4000);
        System.out.println("  BFS mundial: " + mundo.cantidad() + " aeropuertos, " + mundo.nivelMaximo() + " niveles, nivel 1: "
                + mundo.cantidadEnNivel(1) + ", nivel 2: " + mundo.cantidadEnNivel(2) + String.format(", %.1f ms", mundo.getMilisegundos()));
    }

    private static void pruebasTodos(MapaPaises mapa) {
        System.out.println("Construir todos");
        double ms = mapa.construirTodos();
        verificar("225 países con vértices", mapa.paisesConVertices() == 225);
        verificar("menos de 10 s", ms < 10000);
        int internas = 0;
        int externas = 0;
        ListaSecuencial<GrafoPais> paises = mapa.getPaises();
        for (int i = 0; i < paises.tamanio(); i++) {
            internas += paises.obtener(i).getRutasInternas();
            externas += paises.obtener(i).getSalidasExterior();
        }
        verificar("pares internos 17587", internas == 17587);
        verificar("pares externos 19523", externas == 19523);
        verificar("pares únicos 37110", internas + externas == 37110);
        System.out.println(String.format("  Construcción de todos: %.1f ms, internas %d, externas %d", ms, internas, externas));
    }
}
