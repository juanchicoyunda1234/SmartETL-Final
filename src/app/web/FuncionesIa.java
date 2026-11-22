package app.web;

import algoritmos.BFS;
import algoritmos.DFS;
import app.Sesion;
import etl.DatosLimpios;
import grafo.GrafoPais;
import grafo.MapaPaises;
import grafo.Vertice;
import ia.AnalizadorIA;
import ia.GeneradorFlujogramaLocal;
import ia.GeneradorPrompt;
import ia.RegistroEventos;
import ia.ResultadoIA;
import ia.TrazasEstructuras;
import model.Aeropuerto;

public final class FuncionesIa {

    public static final String[] FUNCIONES = {"flujo_etl", "flujo_cola", "flujo_pila", "flujo_lista", "flujo_bst",
            "flujo_bfs", "flujo_dfs", "explicar_arbol", "explicar_grafo", "errores_etl"};

    private FuncionesIa() {
    }

    public static boolean existe(String funcion) {
        for (String conocida : FUNCIONES) {
            if (conocida.equals(funcion)) {
                return true;
            }
        }
        return false;
    }

    public static ResultadoIA ejecutar(String funcion, AnalizadorIA analizador, Sesion sesion) throws Exception {
        switch (funcion) {
            case "flujo_etl":
                return flujoEtl(analizador, sesion);
            case "flujo_cola":
                return flujoEstructura(analizador, TrazasEstructuras.cola(), "cola");
            case "flujo_pila":
                return flujoEstructura(analizador, TrazasEstructuras.pila(), "pila");
            case "flujo_lista":
                return flujoEstructura(analizador, TrazasEstructuras.lista(), "lista");
            case "flujo_bst":
                return flujoEstructura(analizador, TrazasEstructuras.bst(), "bst");
            case "flujo_bfs":
                return flujoRecorrido(analizador, sesion, true);
            case "flujo_dfs":
                return flujoRecorrido(analizador, sesion, false);
            case "explicar_arbol":
                return explicarArbol(analizador, sesion);
            case "explicar_grafo":
                return explicarGrafo(analizador, sesion);
            default:
                return erroresEtl(analizador, sesion);
        }
    }

    private static ResultadoIA flujoEtl(AnalizadorIA analizador, Sesion sesion) throws Exception {
        RegistroEventos registro = sesion.ultimoEtl().getRegistro();
        return analizador.diagrama("Flujograma ETL", "flujo_etl", "Flujograma del ETL (eventos reales)",
                GeneradorPrompt.flujogramaEtl(registro), GeneradorFlujogramaLocal.etl(registro),
                AnalizadorIA.esperadosEtl(registro), AnalizadorIA.conocidos(registro));
    }

    private static ResultadoIA flujoEstructura(AnalizadorIA analizador, RegistroEventos registro, String estructura)
            throws Exception {
        return analizador.diagrama("Flujograma " + estructura, "flujo_" + estructura, "Flujograma: " + registro.getTitulo(),
                GeneradorPrompt.flujogramaEstructura(registro), GeneradorFlujogramaLocal.estructura(registro),
                AnalizadorIA.esperadosEstructura(registro), AnalizadorIA.conocidos(registro));
    }

    private static ResultadoIA flujoRecorrido(AnalizadorIA analizador, Sesion sesion, boolean esBfs) throws Exception {
        RegistroEventos registro = esBfs ? sesion.getUltimoBfs() : sesion.getUltimoDfs();
        if (registro == null) {
            Vertice<Aeropuerto> uio = sesion.mapa().verticePorIata("UIO");
            registro = new RegistroEventos((esBfs ? "BFS" : "DFS") + " desde UIO");
            if (esBfs) {
                BFS.recorrer(uio, null, BFS.SIN_LIMITE, registro);
                sesion.registrarBfs(registro, 1, uio);
            } else {
                DFS.recorrer(uio, null, DFS.SIN_LIMITE, registro);
                sesion.registrarDfs(registro, 1, uio);
            }
        }
        int nivelDiagrama = esBfs ? sesion.getNivelDiagramaBfs() : sesion.getNivelDiagramaDfs();
        String nombre = esBfs ? "BFS" : "DFS";
        return analizador.diagrama("Flujograma " + nombre, "flujo_" + nombre.toLowerCase(),
                "Flujograma dinámico: " + registro.getTitulo() + " (detalle hasta nivel " + nivelDiagrama + ")",
                GeneradorPrompt.flujogramaRecorrido(registro, nivelDiagrama),
                GeneradorFlujogramaLocal.recorrido(registro, nivelDiagrama),
                AnalizadorIA.esperadosRecorrido(registro, nivelDiagrama), AnalizadorIA.conocidos(registro));
    }

    private static ResultadoIA explicarArbol(AnalizadorIA analizador, Sesion sesion) throws Exception {
        DatosLimpios datos = sesion.datosReales();
        int comparacionesLista = sesion.listaEnlazada().buscar(a -> a.getIata().equals("UIO")).getComparaciones();
        String hechos = GeneradorPrompt.datosArbol(datos.getArbolIata(), "UIO", comparacionesLista);
        return analizador.texto("Explicar árbol", GeneradorPrompt.explicacion(
                "cómo está formado este árbol binario de búsqueda, por qué tiene esa altura y qué muestra el camino de la búsqueda",
                hechos), hechos);
    }

    private static ResultadoIA explicarGrafo(AnalizadorIA analizador, Sesion sesion) throws Exception {
        MapaPaises mapa = sesion.mapa();
        GrafoPais pais = mapa.obtener(sesion.getUltimoPais());
        if (pais == null) {
            pais = mapa.obtener("Ecuador");
        }
        String hechos = GeneradorPrompt.datosGrafo(pais);
        return analizador.texto("Explicar grafo de " + pais.getNombre(), GeneradorPrompt.explicacion(
                "cómo está formado el grafo de rutas aéreas de " + pais.getNombre()
                        + ", qué significan sus vértices, aristas internas y rutas externas", hechos), hechos);
    }

    private static ResultadoIA erroresEtl(AnalizadorIA analizador, Sesion sesion) throws Exception {
        String hechos = GeneradorPrompt.datosErrores(sesion.ultimoEtl().getTransform());
        return analizador.texto("Analizar errores ETL", GeneradorPrompt.explicacion(
                "qué dicen estos errores del ETL sobre la calidad de los datos y por qué cada regla los detecta", hechos), hechos);
    }
}
