package app.web;

import algoritmos.BFS;
import algoritmos.Ciclos;
import algoritmos.DFS;
import algoritmos.Ordenamiento;
import algoritmos.ResultadoRecorrido;
import algoritmos.Rutas;
import estructuras.ListaSecuencial;
import estructuras.Nodo;
import grafo.Agrupacion;
import grafo.GrafoPais;
import grafo.MapaPaises;
import grafo.Vertice;
import ia.GeneradorFlujogramaLocal;
import ia.RegistroEventos;
import ia.VisorGrafo;
import ia.VisorHtml;
import model.Aeropuerto;

import java.util.function.Consumer;

public class ApiGrafo implements AreaApi {

    public static final int NODOS_DIBUJO = 160;
    public static final int VECINOS_DIBUJO = 40;

    private final ContextoWeb contexto;

    public ApiGrafo(ContextoWeb contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean atender(Peticion p) throws Exception {
        if (p.es("GET", "grafo", "paises")) {
            Respuesta.json(p.getIntercambio(), 200, paises());
            return true;
        }
        if (p.es("POST", "grafo", "construir")) {
            String[] nombres = p.json().textos("paises");
            if (nombres.length == 0) {
                throw new ErrorWeb(400, "Selecciona al menos un país de la lista");
            }
            MapaPaises mapa = mapa();
            TareaWeb tarea = contexto.getTareas().iniciar("Construir " + nombres.length + " grafos", conCandado(avisar -> construir(mapa, nombres, avisar)));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("POST", "grafo", "construir-todos")) {
            MapaPaises mapa = mapa();
            TareaWeb tarea = contexto.getTareas().iniciar("Construir todos los grafos", conCandado(avisar -> construirTodos(mapa, avisar)));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("GET", "grafo", "conexiones")) {
            contexto.getTareas().exigirLibre();
            MapaPaises mapa = mapa();
            String resultado;
            synchronized (contexto.getCandadoGrafo()) {
                resultado = conexiones(vertice(mapa, p.parametro("iata", "")));
            }
            Respuesta.json(p.getIntercambio(), 200, resultado);
            return true;
        }
        if (p.es("POST", "grafo", "recorrido")) {
            LectorJson json = p.json();
            boolean esBfs = !json.texto("algoritmo", "BFS").equals("DFS");
            int nivelMaximo = json.entero("nivelMaximo", BFS.SIN_LIMITE);
            int nivelDiagrama = Math.max(1, json.entero("nivelDiagrama", 1));
            boolean fronteras = json.booleano("fronteras", false);
            MapaPaises mapa = mapa();
            Vertice<Aeropuerto> origen;
            synchronized (contexto.getCandadoGrafo()) {
                origen = vertice(mapa, json.texto("origen", ""));
            }
            String nombre = esBfs ? "BFS" : "DFS";
            TareaWeb tarea = contexto.getTareas().iniciar(nombre + " desde " + origen.getEtiqueta(), conCandado(avisar -> {
                avisar.accept((esBfs ? "Recorriendo por niveles con la Cola propia" : "Recorriendo en profundidad con la Pila propia")
                        + (fronteras ? ", cruzando fronteras" : ", solo rutas internas"));
                RegistroEventos registro = new RegistroEventos(nombre + " desde " + origen.getEtiqueta());
                ResultadoRecorrido r = esBfs ? BFS.recorrer(origen, fronteras ? mapa : null, nivelMaximo, registro)
                        : DFS.recorrer(origen, fronteras ? mapa : null, nivelMaximo, registro);
                if (esBfs) {
                    contexto.getSesion().registrarBfs(registro, nivelDiagrama, origen);
                } else {
                    contexto.getSesion().registrarDfs(registro, nivelDiagrama, origen);
                }
                avisar.accept("Preparando " + Math.min(NODOS_DIBUJO, r.cantidad()) + " aeropuertos para dibujar");
                String resultado = recorrido(r, fronteras, registro.tamanio());
                contexto.getReporte().registrar(esBfs ? "bfs" : "dfs", resultado);
                return resultado;
            }));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("POST", "grafo", "ruta")) {
            contexto.getTareas().exigirLibre();
            LectorJson json = p.json();
            String resultado;
            synchronized (contexto.getCandadoGrafo()) {
                resultado = ruta(json.texto("desde", ""), json.texto("hasta", ""));
            }
            Respuesta.json(p.getIntercambio(), 200, resultado);
            return true;
        }
        if (p.es("POST", "grafo", "ciclo")) {
            contexto.getTareas().exigirLibre();
            String pais = p.json().texto("pais", "").trim();
            String resultado;
            synchronized (contexto.getCandadoGrafo()) {
                resultado = ciclo(pais);
            }
            Respuesta.json(p.getIntercambio(), 200, resultado);
            return true;
        }
        if (p.es("POST", "grafo", "diagrama-general")) {
            int nivelDiagrama = Math.max(1, p.json().entero("nivelDiagrama", 1));
            MapaPaises mapa = mapa();
            TareaWeb tarea = contexto.getTareas().iniciar("Diagrama general de países", conCandado(avisar -> diagramaGeneral(mapa, nivelDiagrama, avisar)));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("POST", "grafo", "visor")) {
            String origen = p.json().texto("origen", "UIO").trim().toUpperCase();
            MapaPaises mapa = mapa();
            TareaWeb tarea = contexto.getTareas().iniciar("Visor interactivo del grafo", conCandado(avisar -> {
                avisar.accept("Construyendo los grafos de todos los países");
                mapa.construirTodos();
                avisar.accept("Escribiendo el visor con vis-network");
                VisorGrafo.escribir(mapa, origen.isEmpty() ? "UIO" : origen);
                return new ObjetoJson().texto("html", "/diagramas/" + VisorGrafo.ARCHIVO).toString();
            }));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        return false;
    }

    private TrabajoWeb conCandado(TrabajoWeb trabajo) {
        return avisar -> {
            synchronized (contexto.getCandadoGrafo()) {
                return trabajo.hacer(avisar);
            }
        };
    }

    private MapaPaises mapa() throws Exception {
        if (!contexto.getSesion().tieneEtlReal()) {
            throw new ErrorWeb(409, "Primero prepara los datos: ejecuta el ETL con datos reales");
        }
        return contexto.getSesion().mapa();
    }

    private Vertice<Aeropuerto> vertice(MapaPaises mapa, String iata) throws ErrorWeb {
        String codigo = iata.trim().toUpperCase();
        if (codigo.isEmpty()) {
            throw new ErrorWeb(400, "Escribe un código IATA");
        }
        Vertice<Aeropuerto> v = mapa.verticePorIata(codigo);
        if (v == null) {
            throw new ErrorWeb(404, "No hay un aeropuerto con rutas y código IATA " + codigo);
        }
        return v;
    }

    private String paises() throws Exception {
        if (!contexto.getSesion().tieneEtlReal()) {
            return new ObjetoJson().booleano("datosListos", false).crudo("paises", new ArregloJson()).toString();
        }
        MapaPaises mapa = contexto.getSesion().mapa();
        ListaSecuencial<GrafoPais> ordenados = Ordenamiento.insercion(mapa.getPaises(), GrafoPais.POR_NOMBRE).getLista();
        ArregloJson lista = new ArregloJson();
        for (int i = 0; i < ordenados.tamanio(); i++) {
            GrafoPais pais = ordenados.obtener(i);
            lista.crudo(new ObjetoJson().texto("nombre", pais.getNombre()).booleano("construido", pais.estaConstruido())
                    .entero("vertices", pais.estaConstruido() ? pais.getVertices() : -1));
        }
        return new ObjetoJson().booleano("datosListos", true).crudo("paises", lista).toString();
    }

    private ObjetoJson resumenPais(GrafoPais pais) {
        return new ObjetoJson().texto("nombre", pais.getNombre()).entero("vertices", pais.getVertices())
                .entero("internas", pais.getRutasInternas()).entero("salidas", pais.getSalidasExterior())
                .entero("llegadas", pais.getLlegadasExterior()).decimal("ms", pais.getMilisegundos());
    }

    private String construir(MapaPaises mapa, String[] nombres, Consumer<String> avisar) throws ErrorWeb {
        ArregloJson lista = new ArregloJson();
        long vertices = 0;
        long aristas = 0;
        double ms = 0;
        for (String nombre : nombres) {
            avisar.accept("Construyendo el grafo de " + nombre);
            GrafoPais pais = mapa.obtener(nombre);
            if (pais == null) {
                throw new ErrorWeb(404, "No existe el país " + nombre);
            }
            lista.crudo(resumenPais(pais));
            vertices += pais.getVertices();
            aristas += pais.getRutasInternas();
            ms += pais.getMilisegundos();
        }
        contexto.getSesion().setUltimoPais(nombres[nombres.length - 1]);
        String resultado = new ObjetoJson().entero("cantidad", nombres.length).entero("vertices", vertices).entero("aristas", aristas)
                .decimal("ms", ms).crudo("paises", lista).toString();
        contexto.getReporte().registrar("construidos", resultado);
        return resultado;
    }

    private String construirTodos(MapaPaises mapa, Consumer<String> avisar) {
        avisar.accept("Construyendo los grafos de " + mapa.getPaises().tamanio() + " países");
        double ms = mapa.construirTodos();
        long vertices = 0;
        long aristas = 0;
        ListaSecuencial<GrafoPais> todos = mapa.getPaises();
        for (int i = 0; i < todos.tamanio(); i++) {
            vertices += todos.obtener(i).getVertices();
            aristas += todos.obtener(i).getRutasInternas();
        }
        String resultado = new ObjetoJson().entero("paises", mapa.paisesConVertices()).entero("enArbol", todos.tamanio())
                .entero("vertices", vertices).entero("aristas", aristas).decimal("ms", ms).toString();
        contexto.getReporte().registrar("grafos", resultado);
        return resultado;
    }

    private ObjetoJson aeropuerto(Aeropuerto a) {
        return new ObjetoJson().texto("iata", MapaPaises.etiqueta(a)).texto("nombre", a.getNombre())
                .texto("ciudad", a.getCiudad()).texto("pais", a.getPais());
    }

    private String conexiones(Vertice<Aeropuerto> v) {
        ArregloJson internas = new ArregloJson();
        int dibujadas = 0;
        for (Nodo<Vertice<Aeropuerto>> n = v.getAdyacentes().primerNodo(); n != null && dibujadas < VECINOS_DIBUJO; n = n.getSiguiente()) {
            internas.crudo(aeropuerto(n.getDato().getDato()));
            dibujadas++;
        }
        Agrupacion grupos = GrafoPais.externosPorPais(v);
        ArregloJson externos = new ArregloJson();
        for (int i = 0; i < grupos.tamanio(); i++) {
            externos.crudo(new ObjetoJson().texto("pais", grupos.clave(i)).entero("conteo", grupos.conteo(i)).texto("codigos", grupos.detalle(i)));
        }
        return new ObjetoJson().crudo("aeropuerto", aeropuerto(v.getDato()))
                .entero("totalInternas", v.getAdyacentes().tamanio()).crudo("internas", internas)
                .entero("totalExternas", v.getExternos().tamanio()).crudo("externos", externos)
                .entero("llegadas", v.getLlegadasExternas().tamanio()).toString();
    }

    private String recorrido(ResultadoRecorrido r, boolean fronteras, int eventos) {
        ArregloJson niveles = new ArregloJson();
        for (int nivel = 0; nivel <= r.nivelMaximo(); nivel++) {
            niveles.crudo(new ObjetoJson().entero("nivel", nivel).entero("cantidad", r.cantidadEnNivel(nivel))
                    .entero("paises", r.paisesEnNivel(nivel).tamanio()));
        }
        ArregloJson nodos = new ArregloJson();
        int mostrados = Math.min(NODOS_DIBUJO, r.cantidad());
        for (int i = 0; i < mostrados; i++) {
            Vertice<Aeropuerto> v = r.getVisitados().obtener(i);
            Aeropuerto a = v.getDato();
            nodos.crudo(new ObjetoJson().texto("id", v.getEtiqueta()).entero("nivel", v.getNivel())
                    .texto("padre", v.getPadre() == null ? null : v.getPadre().getEtiqueta())
                    .texto("nombre", a.getNombre()).texto("ciudad", a.getCiudad()).texto("pais", a.getPais()));
        }
        return new ObjetoJson().texto("algoritmo", r.getAlgoritmo()).texto("origen", r.getOrigen().getEtiqueta())
                .booleano("fronteras", fronteras).entero("cantidad", r.cantidad()).entero("nivelMaximo", r.nivelMaximo())
                .decimal("ms", r.getMilisegundos()).entero("eventos", eventos).entero("mostrados", mostrados)
                .crudo("niveles", niveles).crudo("nodos", nodos).toString();
    }

    private ArregloJson vecinos(Vertice<Aeropuerto> v, int limite) {
        ArregloJson codigos = new ArregloJson();
        int puestos = 0;
        for (Nodo<Vertice<Aeropuerto>> n = v == null ? null : v.getAdyacentes().primerNodo(); n != null && puestos < limite; n = n.getSiguiente()) {
            codigos.texto(n.getDato().getEtiqueta());
            puestos++;
        }
        return codigos;
    }

    private String ruta(String desde, String hasta) throws Exception {
        MapaPaises mapa = mapa();
        Vertice<Aeropuerto> origen = vertice(mapa, desde);
        Vertice<Aeropuerto> destino = vertice(mapa, hasta);
        ListaSecuencial<Aeropuerto> ruta = Rutas.buscar(origen, destino, mapa);
        ArregloJson escalas = new ArregloJson();
        for (int i = 0; i < ruta.tamanio(); i++) {
            escalas.crudo(aeropuerto(ruta.obtener(i)).crudo("vecinos", vecinos(mapa.vertice(ruta.obtener(i)), 6)));
        }
        String resultado = new ObjetoJson().texto("desde", origen.getEtiqueta()).texto("hasta", destino.getEtiqueta())
                .booleano("existe", !ruta.estaVacia()).entero("vuelos", Math.max(0, ruta.tamanio() - 1))
                .crudo("ruta", escalas).toString();
        contexto.getReporte().registrar("ruta:" + origen.getEtiqueta() + "-" + destino.getEtiqueta(), resultado);
        return resultado;
    }

    private String ciclo(String nombre) throws Exception {
        if (nombre.isEmpty()) {
            throw new ErrorWeb(400, "Escribe el nombre de un país");
        }
        MapaPaises mapa = mapa();
        GrafoPais pais = mapa.obtener(nombre);
        if (pais == null) {
            throw new ErrorWeb(404, "No existe el país " + nombre);
        }
        contexto.getSesion().setUltimoPais(pais.getNombre());
        ListaSecuencial<Aeropuerto> ciclo = Ciclos.detectar(pais.getGrafo());
        ArregloJson codigos = new ArregloJson();
        for (int i = 0; i < ciclo.tamanio(); i++) {
            codigos.crudo(aeropuerto(ciclo.obtener(i)));
        }
        String resultado = new ObjetoJson().texto("pais", pais.getNombre()).booleano("existe", !ciclo.estaVacia())
                .entero("longitud", Math.max(0, ciclo.tamanio() - 1)).crudo("ciclo", codigos).toString();
        contexto.getReporte().registrar("ciclo", resultado);
        return resultado;
    }

    private Vertice<Aeropuerto> origenPara(GrafoPais pais) {
        Vertice<Aeropuerto> ultimo = contexto.getSesion().getUltimoOrigen();
        if (ultimo != null && ultimo.getDato().getPais().equalsIgnoreCase(pais.getNombre())) {
            return ultimo;
        }
        Vertice<Aeropuerto> mayor = null;
        for (Nodo<Vertice<Aeropuerto>> n = pais.getGrafo().getVertices().primerNodo(); n != null; n = n.getSiguiente()) {
            if (mayor == null || n.getDato().getAdyacentes().tamanio() > mayor.getAdyacentes().tamanio()) {
                mayor = n.getDato();
            }
        }
        return mayor;
    }

    private String diagramaGeneral(MapaPaises mapa, int nivelDiagrama, Consumer<String> avisar) throws Exception {
        ListaSecuencial<GrafoPais> construidos = new ListaSecuencial<>();
        ListaSecuencial<GrafoPais> todos = mapa.construidos();
        for (int i = 0; i < todos.tamanio(); i++) {
            if (todos.obtener(i).getVertices() > 0) {
                construidos.insertarFinal(todos.obtener(i));
            }
        }
        if (construidos.estaVacia()) {
            throw new ErrorWeb(409, "Primero construye uno o más países");
        }
        avisar.accept("Escribiendo la página de cada uno de los " + construidos.tamanio() + " países");
        for (int i = 0; i < construidos.tamanio(); i++) {
            GrafoPais pais = construidos.obtener(i);
            Vertice<Aeropuerto> origen = origenPara(pais);
            VisorHtml.escribirPagina(VisorHtml.archivoPais(pais.getNombre()), "Grafo de " + pais.getNombre(),
                    GeneradorFlujogramaLocal.grafoPais(pais, origen, nivelDiagrama, construidos),
                    pais.resumen() + (pais.getVertices() > GeneradorFlujogramaLocal.LIMITE_VERTICES_COMPLETO
                            ? ". Se dibuja hasta el nivel " + nivelDiagrama + " desde " + origen : ""));
        }
        avisar.accept("Generando el diagrama general con Mermaid");
        VisorHtml.escribirDiagrama("general_paises", "Construcción de grafos por país",
                GeneradorFlujogramaLocal.generalPaises(construidos, 250), "Clic en un país para abrir su grafo");
        return new ObjetoJson().entero("paises", construidos.tamanio()).texto("html", "/diagramas/ver_general_paises.html").toString();
    }
}
