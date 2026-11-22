package pruebas;

import algoritmos.BFS;
import algoritmos.DFS;
import etl.ProcesoETL;
import grafo.MapaPaises;
import grafo.Vertice;
import ia.AlmacenApis;
import ia.AnalizadorIA;
import ia.ApiGuardada;
import ia.ClienteCompatibleOpenAI;
import ia.ConfiguracionIA;
import ia.Evento;
import ia.GeneradorFlujogramaLocal;
import ia.GeneradorPrompt;
import ia.Json;
import ia.RegistroEventos;
import ia.ResultadoIA;
import ia.TrazasEstructuras;
import ia.VisorHtml;
import model.Aeropuerto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PruebasIA {

    private static int pasaron = 0;
    private static int fallaron = 0;

    public static void main(String[] args) throws Exception {
        pruebasJson();
        pruebasLimpieza();
        pruebasCoherencia();
        pruebasEventosSinteticos();
        ProcesoETL etl = ProcesoETL.real();
        etl.ejecutar();
        MapaPaises mapa = new MapaPaises(etl.getDatos());
        pruebasBfsEcuador(mapa);
        pruebasEtlYEstructuras(etl);
        pruebasAnalizadorLocal(etl, mapa);
        pruebasProveedores();
        pruebasApisGuardadas();
        pruebasClaveNoSeFiltra();
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

    private static void pruebasProveedores() {
        System.out.println("Proveedores y URL editable");
        verificar("GitHub Models está en la lista", java.util.Arrays.asList(ConfiguracionIA.PROVEEDORES).contains(ConfiguracionIA.GITHUB));
        verificar("URL por defecto de GitHub Models",
                ConfiguracionIA.urlDe(ConfiguracionIA.GITHUB).equals("https://models.github.ai/inference/chat/completions"));
        verificar("URL por defecto de GLM es Z.ai internacional",
                ConfiguracionIA.urlDe("GLM").equals("https://api.z.ai/api/paas/v4/chat/completions"));
        ConfiguracionIA c = new ConfiguracionIA();
        c.setProveedor(ConfiguracionIA.GITHUB);
        verificar("sin URL escrita usa la de por defecto", c.getUrl().equals(ConfiguracionIA.urlDe(ConfiguracionIA.GITHUB)));
        c.setUrl("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        verificar("usa la URL escrita por el usuario", c.getUrl().startsWith("https://open.bigmodel.cn"));
        c.setClave("x".toCharArray());
        verificar("GitHub Models reutiliza ClienteCompatibleOpenAI", c.crearProveedor() instanceof ClienteCompatibleOpenAI);
        verificar("la ayuda del modelo de GitHub da el formato publicador/modelo",
                ConfiguracionIA.ayudaModelo(ConfiguracionIA.GITHUB).contains("openai/gpt-4.1-mini"));
    }

    private static void pruebasApisGuardadas() throws Exception {
        System.out.println("APIs guardadas");
        Path carpeta = Files.createTempDirectory("smartetl_apis");
        Path archivo = carpeta.resolve("sub").resolve("apis.properties");
        AlmacenApis almacen = new AlmacenApis(archivo);
        verificar("si el archivo no existe se crea vacío", Files.exists(archivo) && almacen.getApis().estaVacia());
        almacen.guardar(new ApiGuardada("api de prueba 1", "Claude", "claude-opus-5-5", "https://api.anthropic.com/v1/messages", "clave-1"));
        almacen.guardar(new ApiGuardada("con|barra", ConfiguracionIA.GITHUB, "openai/gpt-4.1-mini", "https://x|y", "a|b\\c"));
        verificar("guardar dos entradas", almacen.getApis().tamanio() == 2);
        verificar("el archivo usa el formato nombre|proveedor|modelo|url|clave", Files.readAllLines(archivo).get(0)
                .equals("api de prueba 1|Claude|claude-opus-5-5|https://api.anthropic.com/v1/messages|clave-1"));
        verificar("el | dentro de un valor se escapa", Files.readAllLines(archivo).get(1).startsWith("con\\|barra|"));
        almacen.guardar(new ApiGuardada("API DE PRUEBA 1", "Gemini", "otro-modelo", "", "clave-2"));
        verificar("guardar con el mismo nombre actualiza", almacen.getApis().tamanio() == 2
                && almacen.buscar("api de prueba 1").getProveedor().equals("Gemini"));
        AlmacenApis reabierto = new AlmacenApis(archivo);
        ApiGuardada barra = reabierto.buscar("con|barra");
        verificar("al reabrir se recargan las entradas", reabierto.getApis().tamanio() == 2);
        verificar("los | y \\ se recuperan intactos", barra != null && barra.getUrl().equals("https://x|y")
                && barra.getClave().equals("a|b\\c"));
        verificar("la descripción de la lista no muestra la clave", !barra.descripcion().contains("a|b")
                && barra.descripcion().contains("openai/gpt-4.1-mini"));
        verificar("eliminar", reabierto.eliminar("con|barra") && reabierto.getApis().tamanio() == 1
                && new AlmacenApis(archivo).getApis().tamanio() == 1);
        verificar("eliminar uno inexistente devuelve falso", !reabierto.eliminar("no existe"));
        verificar("el archivo por defecto está fuera del repositorio", AlmacenApis.archivoPorDefecto()
                .startsWith(Paths.get(System.getProperty("user.home"))) && AlmacenApis.archivoPorDefecto().toString().contains(".smartetl"));
        Files.walk(carpeta).sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }

    private static void pruebasClaveNoSeFiltra() throws Exception {
        System.out.println("La clave no se escribe en evidencias ni en el HTML");
        String clave = "CLAVE-SECRETA-PRUEBA-98765";
        Path evidencias = Paths.get(AnalizadorIA.CARPETA_EVIDENCIAS);
        Files.createDirectories(evidencias);
        String[] antes = evidencias.toFile().list();
        ConfiguracionIA c = new ConfiguracionIA();
        c.setProveedor(ConfiguracionIA.GITHUB);
        c.setModelo("openai/gpt-4.1-mini");
        c.setUrl("http://127.0.0.1:9/inference/chat/completions");
        c.setClave(clave.toCharArray());
        AnalizadorIA analizador = new AnalizadorIA(c);
        RegistroEventos cola = TrazasEstructuras.cola();
        ResultadoIA d = analizador.diagrama("Flujograma Cola", "prueba_clave", "Prueba clave", GeneradorPrompt.flujogramaEstructura(cola),
                GeneradorFlujogramaLocal.estructura(cola), AnalizadorIA.esperadosEstructura(cola), AnalizadorIA.conocidos(cola));
        ResultadoIA t = analizador.texto("Explicar prueba", "prompt", "datos");
        verificar("si la URL falla se usa el respaldo local", !d.usoApi() && !t.usoApi() && d.getAviso().contains("respaldo local"));
        boolean limpio = true;
        java.io.File[] archivos = evidencias.toFile().listFiles();
        for (java.io.File f : archivos) {
            if (new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).contains(clave)) {
                limpio = false;
            }
        }
        verificar("ningún archivo de evidencias_ia contiene la clave", limpio);
        verificar("el HTML del flujograma no contiene la clave",
                !new String(Files.readAllBytes(d.getHtml()), StandardCharsets.UTF_8).contains(clave));
        verificar("el aviso en pantalla no contiene la clave", !d.getAviso().contains(clave) && !t.getAviso().contains(clave));
        for (java.io.File f : archivos) {
            if (!java.util.Arrays.asList(antes).contains(f.getName())) {
                Files.delete(f.toPath());
            }
        }
        Files.deleteIfExists(Paths.get(VisorHtml.CARPETA, "prueba_clave.md"));
        Files.deleteIfExists(Paths.get(VisorHtml.CARPETA, "ver_prueba_clave.html"));
    }

    private static void pruebasJson() {
        System.out.println("JSON mínimo");
        verificar("escapa comillas, barra y salto", Json.cadena("a\"b\\c\nd").equals("\"a\\\"b\\\\c\\nd\""));
        String openai = "{\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"hola\\n\\\"mundo\\\" \\u00e1\"}}]}";
        int mensaje = Json.posicion(openai, "\"message\"\\s*:", 0);
        verificar("lee choices[0].message.content", "hola\n\"mundo\" á".equals(Json.valorDeCadena(openai, "content", mensaje)));
        String anthropic = "{\"content\":[{\"type\":\"thinking\",\"thinking\":\"\",\"signature\":\"x\"},"
                + "{\"type\":\"text\",\"text\":\"flowchart TD\\n A\"}],\"stop_reason\":\"end_turn\"}";
        int bloque = Json.posicion(anthropic, "\"type\"\\s*:\\s*\"text\"", 0);
        verificar("lee el bloque de texto saltando el de thinking",
                "flowchart TD\n A".equals(Json.valorDeCadena(anthropic, "text", bloque + 6)));
    }

    private static void pruebasLimpieza() {
        System.out.println("Limpieza de respuesta");
        String conCercas = "Aquí está:\n```mermaid\nflowchart TD\n    A[\"x\"] --> B[\"y\"]   \n\n```\nGracias";
        String limpio = AnalizadorIA.limpiarMermaid(conCercas);
        verificar("quita cercas, texto extra y espacios", limpio != null && limpio.startsWith("flowchart TD")
                && !limpio.contains("```") && !limpio.contains("Gracias") && limpio.contains("A[\"x\"] --> B[\"y\"]\n"));
        verificar("acepta graph", AnalizadorIA.limpiarMermaid("graph LR\nA-->B") != null);
        verificar("rechaza vacía", AnalizadorIA.limpiarMermaid("   ") == null);
        verificar("rechaza texto que no es diagrama", AnalizadorIA.limpiarMermaid("Lo siento, no puedo") == null);
        verificar("rechaza bloque que no empieza con flowchart", AnalizadorIA.limpiarMermaid("```\nsequenceDiagram\n```") == null);
    }

    private static void pruebasCoherencia() {
        System.out.println("Comprobación de coherencia");
        String bueno = "flowchart TD\n A[\"BFS desde UIO\"] --> B[\"GYE, CUE\"]";
        verificar("sin faltantes ni sobrantes", AnalizadorIA.coherencia(bueno, new String[]{"UIO", "GYE", "CUE"},
                new String[]{"UIO", "GYE", "CUE"}).contains("contiene todos"));
        String malo = "flowchart TD\n A[\"UIO\"] --> B[\"MIA\"]";
        String resultado = AnalizadorIA.coherencia(malo, new String[]{"UIO", "GYE"}, new String[]{"UIO", "GYE"});
        verificar("detecta faltante y sobrante", resultado.contains("faltan: GYE") && resultado.contains("sobran: MIA"));
        verificar("ignora separadores de miles", AnalizadorIA.coherencia("flowchart TD\n A[\"67.074 válidos\"]",
                new String[]{"67074"}, new String[0]).contains("contiene todos"));
    }

    private static void pruebasEventosSinteticos() {
        System.out.println("Generador local con eventos sintéticos");
        RegistroEventos r = new RegistroEventos("sintético");
        r.agregar(new Evento("inicio", "Inicio: BFS desde AAA (solo rutas internas)", "AAA", "X", new String[0], new String[0], 0, 0));
        r.agregar(new Evento("encolar", "Encolar AAA", "AAA", "X", new String[0], new String[]{"AAA"}, 1, 0));
        r.agregar(new Evento("desencolar", "Paso 1: desencolar AAA", "AAA", "X", new String[]{"BBB", "CCC"},
                new String[]{"BBB", "CCC"}, 2, 0));
        r.agregar(new Evento("desencolar", "Paso 2: desencolar BBB", "BBB", "X", new String[0], new String[]{"CCC"}, 1, 1));
        r.agregar(new Evento("desencolar", "Paso 3: desencolar CCC", "CCC", "X", new String[]{"DDD"}, new String[]{"DDD"}, 1, 1));
        r.agregar(new Evento("desencolar", "Paso 4: desencolar DDD", "DDD", "Y", new String[0], new String[0], 0, 2));
        r.agregar(new Evento("fin", "Fin: 4 aeropuertos visitados", "4", "", new String[0], new String[0], 0, 2));
        String m = GeneradorFlujogramaLocal.recorrido(r, 1);
        verificar("empieza con flowchart TD", m.startsWith("flowchart TD"));
        verificar("detalla el paso 1 con sus vecinos", m.contains("Paso 1: desencolar AAA<br/>vecinos nuevos: BBB, CCC"));
        verificar("un paso sin vecinos dice no aporta", m.contains("Paso 2: desencolar BBB<br/>no aporta vecinos nuevos"));
        verificar("resume el nivel 2", m.contains("Nivel 2: 1 aeropuertos nuevos en 1 país"));
        verificar("termina con cola vacía y fin", m.contains("¿Cola vacía?") && m.contains("Fin: 4 aeropuertos visitados"));
        String prompt = GeneradorPrompt.flujogramaRecorrido(r, 1);
        verificar("el prompt pide español, solo Mermaid, comillas y <br/>", prompt.contains("español")
                && prompt.contains("SOLO un bloque") && prompt.contains("comillas dobles") && prompt.contains("<br/>"));
        verificar("el prompt incluye los eventos reales y el resumen profundo", prompt.contains("Paso 3: desencolar CCC")
                && !prompt.contains("Paso 4: desencolar DDD") && prompt.contains("Nivel 2: 1 aeropuertos"));
    }

    private static void pruebasBfsEcuador(MapaPaises mapa) {
        System.out.println("Flujograma del BFS real en Ecuador");
        Vertice<Aeropuerto> uio = mapa.verticePorIata("UIO");
        RegistroEventos registro = new RegistroEventos("BFS desde UIO");
        BFS.recorrer(uio, null, BFS.SIN_LIMITE, registro);
        String nivel2 = GeneradorFlujogramaLocal.recorrido(registro, 2);
        verificar("nivel de diagrama 2: empieza en UIO y termina con 14", nivel2.contains("Inicio: BFS desde UIO")
                && nivel2.contains("Encolar UIO<br/>cola = [UIO]") && nivel2.contains("Fin: 14 aeropuertos visitados"));
        verificar("nivel de diagrama 2 detalla todos", nivel2.contains("Paso 2: desencolar GYE") && !nivel2.contains("Nivel 2:"));
        String nivel1 = GeneradorFlujogramaLocal.recorrido(registro, 1);
        verificar("nivel de diagrama 1 resume el nivel 2", nivel1.contains("Nivel 2: 4 aeropuertos nuevos en 1 país"));
        String coherencia = AnalizadorIA.coherencia(nivel1, AnalizadorIA.esperadosRecorrido(registro, 1),
                AnalizadorIA.conocidos(registro));
        verificar("el diagrama local es coherente con su registro", coherencia.contains("contiene todos"));
        System.out.println(nivel1);
        RegistroEventos dfs = new RegistroEventos("DFS desde UIO");
        DFS.recorrer(uio, null, DFS.SIN_LIMITE, dfs);
        String m = GeneradorFlujogramaLocal.recorrido(dfs, 1);
        verificar("flujograma DFS con pila", m.contains("pila = [") && m.contains("¿Pila vacía?")
                && m.contains("Fin: 14 aeropuertos visitados"));
    }

    private static void pruebasEtlYEstructuras(ProcesoETL etl) throws Exception {
        System.out.println("Flujogramas de ETL y estructuras");
        etl.cargar("output");
        String m = GeneradorFlujogramaLocal.etl(etl.getRegistro());
        verificar("ETL con las tres fuentes y sus cifras", m.contains("airports.csv") && m.contains("routes.csv")
                && m.contains("67074") && m.contains("589") && m.contains("LOAD"));
        verificar("ETL coherente", AnalizadorIA.coherencia(m, AnalizadorIA.esperadosEtl(etl.getRegistro()),
                AnalizadorIA.conocidos(etl.getRegistro())).contains("contiene todos"));
        String cola = GeneradorFlujogramaLocal.estructura(TrazasEstructuras.cola());
        verificar("traza de cola", cola.contains("Desencolar UIO<br/>cola = [GYE, CUE]"));
        String pila = GeneradorFlujogramaLocal.estructura(TrazasEstructuras.pila());
        verificar("traza de pila", pila.contains("Desapilar DUPLICADO<br/>pila = [ID_NULO_NO_RECUPERABLE, REFERENCIA_ROTA]"));
        String lista = GeneradorFlujogramaLocal.estructura(TrazasEstructuras.lista());
        verificar("traza de lista", lista.contains("Insertar al inicio LIM") && lista.contains("tras 4 comparaciones"));
        String bst = GeneradorFlujogramaLocal.estructura(TrazasEstructuras.bst());
        verificar("traza de BST con camino y eliminación", bst.contains("camino: MEX (izquierda) → GYE (derecha)")
                && bst.contains("dos hijos"));
    }

    private static void pruebasAnalizadorLocal(ProcesoETL etl, MapaPaises mapa) throws Exception {
        System.out.println("AnalizadorIA en modo local y con API sin clave");
        ConfiguracionIA local = new ConfiguracionIA();
        local.setModo(ConfiguracionIA.MODO_LOCAL);
        RegistroEventos cola = TrazasEstructuras.cola();
        ResultadoIA r = new AnalizadorIA(local).diagrama("Flujograma Cola", "prueba_cola", "Prueba cola",
                GeneradorPrompt.flujogramaEstructura(cola), GeneradorFlujogramaLocal.estructura(cola),
                AnalizadorIA.esperadosEstructura(cola), AnalizadorIA.conocidos(cola));
        verificar("modo local usa el respaldo y no guarda evidencia", !r.usoApi() && r.getEvidencia() == null);
        String html = new String(Files.readAllBytes(r.getHtml()), StandardCharsets.UTF_8);
        verificar("HTML con mermaid local, zoom, loose y código", html.contains("../recursos/mermaid.min.js")
                && html.contains("svgPanZoom") && html.contains("securityLevel:'loose'") && html.contains("textarea"));
        verificar("también escribe el .md", Files.exists(java.nio.file.Paths.get("diagramas/prueba_cola.md")));
        ConfiguracionIA sinClave = new ConfiguracionIA();
        sinClave.setModelo("modelo-de-prueba");
        ResultadoIA respaldo = new AnalizadorIA(sinClave).diagrama("Flujograma Cola", "prueba_cola", "Prueba cola",
                "prompt", GeneradorFlujogramaLocal.estructura(cola), new String[0], new String[0]);
        verificar("sin clave avisa y usa el respaldo local", !respaldo.usoApi() && respaldo.getAviso().contains("no hay clave"));
        String evidencia = new String(Files.readAllBytes(respaldo.getEvidencia()), StandardCharsets.UTF_8);
        verificar("la evidencia tiene prompt, respuesta y diagrama", evidencia.contains("## Prompt enviado")
                && evidencia.contains("## Respuesta cruda") && evidencia.contains("```mermaid"));
        Files.delete(respaldo.getEvidencia());
        Files.deleteIfExists(java.nio.file.Paths.get("diagramas/prueba_cola.md"));
        Files.deleteIfExists(java.nio.file.Paths.get("diagramas/ver_prueba_cola.html"));
        String datos = GeneradorPrompt.datosArbol(etl.getDatos().getArbolIata(), "UIO", 2556);
        verificar("datos del árbol con cifras reales", datos.contains("Nodos: 6071") && datos.contains("Altura (raíz sola = 0): 177")
                && datos.contains("13 comparaciones"));
        String grafo = GeneradorPrompt.datosGrafo(mapa.obtener("Ecuador"));
        verificar("datos del grafo de Ecuador", grafo.contains("14 aeropuertos") && grafo.contains("32 rutas internas"));
        String errores = GeneradorPrompt.datosErrores(etl.getTransform());
        verificar("errores solo agregados", errores.contains("REFERENCIA_ROTA: 473") && !errores.contains("línea ")
                && !errores.contains("UIO"));
        verificar("nombre de archivo normalizado", VisorHtml.archivoPais("Côte d'Ivoire").equals("grafo_Cote_d_Ivoire.html"));
    }
}
