package ia;

import algoritmos.Ciclos;
import estructuras.ArbolBST;
import estructuras.ListaSecuencial;
import estructuras.Nodo;
import estructuras.ResultadoBusqueda;
import etl.Transform;
import grafo.Agrupacion;
import grafo.GrafoPais;
import grafo.Vertice;
import model.Aeropuerto;
import model.TipoError;

public final class GeneradorPrompt {

    public static final int LIMITE_EVENTOS = 250;

    private static final String REGLAS_DIAGRAMA = "Reglas obligatorias:\n"
            + "- Responde en español.\n"
            + "- Devuelve SOLO un bloque de código Mermaid (```mermaid ... ```), sin texto antes ni después.\n"
            + "- La primera línea del diagrama debe ser: flowchart TD\n"
            + "- Pon cada etiqueta de nodo entre comillas dobles, por ejemplo A[\"texto\"].\n"
            + "- Para saltos de línea dentro de una etiqueta usa <br/>.\n"
            + "- No inventes datos: usa solo los aeropuertos, números y pasos que aparecen en la entrada.\n"
            + "- Cada nodo debe corresponder a un paso real del registro de eventos.\n";

    private static final String EJEMPLO_BFS = "flowchart TD\n"
            + "    A([\"Inicio: BFS desde UIO\"]) --> B[\"Encolar UIO<br/>cola = [UIO]\"]\n"
            + "    B --> C[\"Paso 1: desencolar UIO<br/>vecinos nuevos: GYE, OCC, CUE<br/>cola = [GYE, OCC, CUE]\"]\n"
            + "    C --> D[\"Pasos 2 a 3: desencolar GYE, OCC<br/>ninguno aporta vecinos nuevos\"]\n"
            + "    D --> E[\"Nivel 2: 4 aeropuertos nuevos en 1 país\"]\n"
            + "    E --> F{\"¿Cola vacía?\"}\n"
            + "    F -->|\"Sí\"| G([\"Fin: 14 aeropuertos visitados\"])\n";

    private GeneradorPrompt() {
    }

    public static String flujogramaRecorrido(RegistroEventos registro, int nivelDiagrama) {
        boolean esBfs = registro.contar("desencolar") > 0;
        String verbo = esBfs ? "desencolar" : "desapilar";
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un asistente que dibuja flujogramas de algoritmos para un curso de Estructuras de Datos.\n")
                .append("Genera el flujograma del recorrido ").append(esBfs ? "BFS (usa una cola)" : "DFS (usa una pila)")
                .append(" que se ejecutó realmente sobre el grafo de aeropuertos de OpenFlights.\n\n")
                .append(REGLAS_DIAGRAMA)
                .append("- Los pasos consecutivos sin vecinos nuevos se colapsan en una sola caja (\"Pasos a a b: ...\").\n")
                .append("- Las listas de más de 12 elementos se muestran con los primeros 12 y \"y N más\".\n")
                .append("- Detalla solo los pasos de nivel menor o igual a ").append(nivelDiagrama)
                .append("; los niveles más profundos van en una caja por nivel con el resumen dado.\n")
                .append("- Termina con la decisión ").append(esBfs ? "¿Cola vacía?" : "¿Pila vacía?")
                .append(" y el nodo de fin con el total de visitados.\n\n")
                .append("Ejemplo del formato esperado (otros datos):\n```mermaid\n").append(EJEMPLO_BFS).append("```\n\n")
                .append("Registro de eventos real (tipo | texto | nivel | vecinos | estado de la estructura):\n");
        int escritos = 0;
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null && escritos < LIMITE_EVENTOS; n = n.getSiguiente()) {
            Evento e = n.getDato();
            boolean profundo = e.getTipo().equals(verbo) && e.getNivel() > nivelDiagrama;
            if (!profundo && !e.getTipo().equals("fin_nivel")) {
                prompt.append(e).append('\n');
                escritos++;
            }
        }
        String[] resumen = GeneradorFlujogramaLocal.resumenNivelesProfundos(registro, nivelDiagrama);
        if (resumen.length > 0) {
            prompt.append("\nResumen calculado por el programa para los niveles profundos:\n");
            for (String linea : resumen) {
                prompt.append("- ").append(linea).append('\n');
            }
        }
        return prompt.toString();
    }

    public static String flujogramaEtl(RegistroEventos registro) {
        return "Eres un asistente que dibuja flujogramas para un curso de Estructuras de Datos.\n"
                + "Genera el flujograma del proceso ETL (Extract, Transform, Load) que se ejecutó realmente.\n"
                + "Extract lee cada CSV y encola los registros en una Cola; Transform desencola, valida y manda cada "
                + "registro a la Lista de válidos o a la Pila de errores; Load escribe los archivos limpios.\n\n"
                + REGLAS_DIAGRAMA
                + "- Para cada fuente dibuja la decisión ¿Registro válido? con las ramas Sí (lista) y No (pila) y las cifras reales.\n\n"
                + "Registro de eventos real:\n" + registro.comoTexto(LIMITE_EVENTOS);
    }

    public static String flujogramaEstructura(RegistroEventos registro) {
        return "Eres un asistente que dibuja flujogramas para un curso de Estructuras de Datos.\n"
                + "Genera el flujograma de esta traza real de operaciones sobre la estructura: " + registro.getTitulo()
                + ".\nCada operación es una caja con el estado resultante de la estructura (o el camino recorrido en el árbol).\n\n"
                + REGLAS_DIAGRAMA + "\nRegistro de eventos real:\n" + registro.comoTexto(LIMITE_EVENTOS);
    }

    public static String explicacion(String tema, String datos) {
        return "Eres profesor de Estructuras de Datos. Explica en español, en 2 a 4 párrafos cortos y claros para "
                + "estudiantes de tercer semestre, " + tema + ".\n"
                + "Usa solo los datos reales que siguen; no inventes cifras ni aeropuertos. No uses tablas.\n\n"
                + "Datos reales:\n" + datos;
    }

    public static String datosArbol(ArbolBST<Aeropuerto> arbol, String iataBuscado, int comparacionesLista) {
        StringBuilder datos = new StringBuilder("BST global de aeropuertos con clave IATA (comparación de texto).\n");
        datos.append("Nodos: ").append(arbol.contarNodos()).append('\n')
                .append("Altura (raíz sola = 0): ").append(arbol.altura()).append('\n')
                .append(String.format("Promedio de comparaciones de una búsqueda exitosa: %.2f%n", arbol.promedioComparaciones()))
                .append("Primeros niveles (nodo e hijos izquierdo y derecho):\n");
        for (String linea : arbol.primerosNiveles(4, 8)) {
            datos.append("  ").append(linea).append('\n');
        }
        ResultadoBusqueda<Aeropuerto> busqueda = arbol.buscar(Aeropuerto.conIata(iataBuscado));
        datos.append("Búsqueda de ").append(iataBuscado).append(": ")
                .append(busqueda.encontrado() ? "encontrado a profundidad " + busqueda.getIndice() : "no existe")
                .append(", ").append(busqueda.getComparaciones()).append(" comparaciones\n")
                .append("Camino: ").append(String.join(" -> ", arbol.caminoHacia(Aeropuerto.conIata(iataBuscado)))).append('\n');
        if (comparacionesLista > 0) {
            datos.append("La misma búsqueda en la lista de aeropuertos en orden del archivo costó ")
                    .append(comparacionesLista).append(" comparaciones.\n");
        }
        datos.append("Un árbol perfectamente balanceado con esos nodos tendría altura ")
                .append((int) Math.floor(Math.log(arbol.contarNodos()) / Math.log(2))).append(".\n");
        return datos.toString();
    }

    public static String datosGrafo(GrafoPais pais) {
        StringBuilder datos = new StringBuilder("Grafo dirigido de aeropuertos de ").append(pais.getNombre())
                .append(" (vértices = aeropuertos con al menos una ruta válida, aristas = rutas únicas origen-destino).\n");
        datos.append(pais.resumen()).append('\n').append("Aeropuertos con más rutas internas de salida:\n");
        ListaSecuencial<Vertice<Aeropuerto>> vertices = ListaSecuencial.copiaDe(pais.getGrafo().getVertices());
        ListaSecuencial<Vertice<Aeropuerto>> ordenados = algoritmos.Ordenamiento.insercion(vertices,
                (a, b) -> Integer.compare(b.getAdyacentes().tamanio(), a.getAdyacentes().tamanio())).getLista();
        for (int i = 0; i < Math.min(5, ordenados.tamanio()); i++) {
            Vertice<Aeropuerto> v = ordenados.obtener(i);
            datos.append("  ").append(v.getEtiqueta()).append(" (").append(v.getDato().getCiudad()).append("): ")
                    .append(v.getAdyacentes().tamanio()).append(" internas, ").append(v.getExternos().tamanio())
                    .append(" al exterior\n");
        }
        Agrupacion externos = pais.externosPorPais();
        datos.append("Rutas externas agrupadas por país destino:\n");
        for (int i = 0; i < Math.min(12, externos.tamanio()); i++) {
            datos.append("  ").append(externos.clave(i)).append(": ").append(externos.conteo(i)).append(" rutas\n");
        }
        if (externos.tamanio() > 12) {
            datos.append("  y ").append(externos.tamanio() - 12).append(" países más\n");
        }
        ListaSecuencial<Aeropuerto> ciclo = Ciclos.detectar(pais.getGrafo());
        if (!ciclo.estaVacia()) {
            datos.append("Ciclo encontrado con DFS: ");
            for (int i = 0; i < ciclo.tamanio(); i++) {
                datos.append(i == 0 ? "" : " -> ").append(ciclo.obtener(i).getIata());
            }
            datos.append('\n');
        }
        return datos.toString();
    }

    public static String datosErrores(Transform transform) {
        StringBuilder datos = new StringBuilder("Conteos agregados del ETL sobre OpenFlights (sin registros individuales).\n");
        for (int f = 0; f < Transform.FUENTES.length; f++) {
            datos.append(Transform.FUENTES[f]).append(": ").append(transform.getProcesados(f)).append(" procesados, ")
                    .append(transform.getValidos(f)).append(" válidos, ").append(transform.getInvalidos(f)).append(" inválidos\n");
        }
        datos.append("Errores por tipo:\n");
        for (TipoError tipo : TipoError.values()) {
            datos.append("  ").append(tipo).append(": ").append(transform.getErroresDeTipo(tipo)).append('\n');
        }
        datos.append("Reglas: REFERENCIA_ROTA = el ID de aeropuerto de la ruta no existe en airports; "
                + "ID_NULO_NO_RECUPERABLE = el ID era \\N y el código del aeropuerto no está en el BST por IATA; "
                + "los IDs nulos recuperables se resuelven buscando el código IATA en el BST.\n");
        return datos.toString();
    }
}
