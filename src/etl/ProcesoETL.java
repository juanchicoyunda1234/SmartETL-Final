package etl;

import estructuras.Cola;
import ia.Evento;
import ia.RegistroEventos;
import model.Aerolinea;
import model.Aeropuerto;
import model.Ruta;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class ProcesoETL {

    public static final int VISTA_COLA = 20;

    private final String[] rutasArchivo;
    private final RegistroEventos registro = new RegistroEventos("ETL");
    private final String[][] vistasCola = new String[3][];
    private final int[] encolados = new int[3];
    private Transform transform;
    private DatosLimpios datos;
    private double msExtract;
    private String[] archivosEscritos = new String[0];
    private Consumer<String> avisar = mensaje -> {
    };

    public ProcesoETL(String aeropuertos, String rutas, String aerolineas) {
        this.rutasArchivo = new String[]{aeropuertos, rutas, aerolineas};
    }

    public static ProcesoETL real() {
        return new ProcesoETL("data/airports.csv", "data/routes.csv", "data/airlines.csv");
    }

    public static ProcesoETL demostracion() {
        return new ProcesoETL("data/demo/airports_sucio.csv", "data/demo/routes_sucio.csv", "data/airlines.csv");
    }

    public void setAvisos(Consumer<String> avisar) {
        this.avisar = avisar;
    }

    public DatosLimpios ejecutar() throws IOException {
        transform = new Transform(registro);
        registro.registrar("inicio", "Inicio del ETL: Extract a una Cola, Transform a Lista de válidos y Pila de errores");

        Cola<Aeropuerto> colaAeropuertos = new Cola<>();
        extraer(Transform.AEROPUERTOS, Aeropuerto.CAMPOS, colaAeropuertos, Aeropuerto::desdeCampos);
        avisar.accept("Transform de aeropuertos: validando " + colaAeropuertos.tamanio() + " registros de la cola");
        transform.transformarAeropuertos(colaAeropuertos);
        registro.registrar("indice", "Índices: BST global por IATA con " + transform.getDatos().getArbolIata().tamanio()
                + " nodos y lista de aeropuertos ordenada por ID para búsqueda binaria");

        Cola<Ruta> colaRutas = new Cola<>();
        extraer(Transform.RUTAS, Ruta.CAMPOS, colaRutas, Ruta::desdeCampos);
        avisar.accept("Transform de rutas: validando " + colaRutas.tamanio() + " registros y resolviendo IDs");
        transform.transformarRutas(colaRutas);

        Cola<Aerolinea> colaAerolineas = new Cola<>();
        extraer(Transform.AEROLINEAS, Aerolinea.CAMPOS, colaAerolineas, Aerolinea::desdeCampos);
        avisar.accept("Transform de aerolíneas: validando " + colaAerolineas.tamanio() + " registros");
        transform.transformarAerolineas(colaAerolineas);

        datos = transform.getDatos();
        registro.registrar("fin_transform", "Fin del Transform: " + totalValidos() + " válidos en listas y "
                + transform.getErrores().tamanio() + " errores en la pila");
        return datos;
    }

    private <T> void extraer(int fuente, int campos, Cola<T> cola, Mapeador<T> mapeador) throws IOException {
        avisar.accept("Extract: leyendo " + Paths.get(rutasArchivo[fuente]).getFileName() + " y encolando");
        long inicio = System.nanoTime();
        ResultadoExtraccion<T> resultado = Extract.extraerEnCola(Proyecto.ruta(rutasArchivo[fuente]), campos, mapeador, cola,
                OpcionesCarga.todos());
        msExtract += (System.nanoTime() - inicio) / 1_000_000.0;
        vistasCola[fuente] = cola.primeros(VISTA_COLA);
        encolados[fuente] = cola.tamanio();
        String archivo = Paths.get(rutasArchivo[fuente]).getFileName().toString();
        registro.agregar(new Evento("leer", "Extract lee " + archivo + ": " + resultado.getTotalLineas() + " líneas, "
                + resultado.getNoLeidas() + " no leídas", archivo, Transform.FUENTES[fuente], new String[0],
                new String[0], 0, fuente));
        registro.agregar(new Evento("encolar", cola.tamanio() + " registros de " + Transform.FUENTES[fuente]
                + " encolados en la cola de extracción", archivo, Transform.FUENTES[fuente], new String[0],
                new String[0], cola.tamanio(), fuente));
    }

    public String[] cargar(String carpeta) throws IOException {
        avisar.accept("Load: escribiendo los archivos limpios y los reportes en " + carpeta);
        archivosEscritos = Load.escribir(datos, transform, msExtract, transform.getMilisegundos(), Proyecto.ruta(carpeta));
        for (String archivo : archivosEscritos) {
            registro.registrar("escribir", "Load escribe " + Paths.get(archivo).getFileName());
        }
        registro.registrar("fin", "Fin del ETL");
        return archivosEscritos;
    }

    public int totalValidos() {
        return transform.getValidos(0) + transform.getValidos(1) + transform.getValidos(2);
    }

    public RegistroEventos getRegistro() {
        return registro;
    }

    public Transform getTransform() {
        return transform;
    }

    public DatosLimpios getDatos() {
        return datos;
    }

    public String[] getVistaCola(int fuente) {
        return vistasCola[fuente];
    }

    public int getEncolados(int fuente) {
        return encolados[fuente];
    }

    public String getArchivo(int fuente) {
        return rutasArchivo[fuente];
    }

    public double getMsExtract() {
        return msExtract;
    }

    public String[] getArchivosEscritos() {
        return archivosEscritos;
    }

    public String reporte() {
        return Load.textoReporte(transform, msExtract, transform.getMilisegundos(), 0);
    }
}
