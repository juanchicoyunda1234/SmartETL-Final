package app.web;

import estructuras.ListaSecuencial;
import etl.DatosLimpios;
import etl.FuenteDatos;
import etl.ProcesoETL;
import etl.Proyecto;
import etl.Transform;
import model.ErrorETL;
import model.Registro;
import model.TipoError;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class ApiEtl implements AreaApi {

    public static final int FILAS_POR_PAGINA = 50;
    public static final int ERRORES_POR_PAGINA = 20;
    public static final int LINEAS_VISOR = 300;
    public static final String[] ARCHIVOS_OUTPUT = {"airports_limpios.csv", "routes_limpias.csv", "airlines_limpias.csv",
            "errores_etl.txt", "reporte_etl.txt"};

    private final ContextoWeb contexto;

    public ApiEtl(ContextoWeb contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean atender(Peticion p) throws Exception {
        if (p.es("POST", "etl", "ejecutar")) {
            boolean esReal = !p.json().texto("modo", "real").equals("demo");
            TareaWeb tarea = contexto.getTareas().iniciar(esReal ? "ETL con datos reales" : "ETL de demostración",
                    avisar -> ejecutar(esReal, avisar));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("POST", "etl", "cargar")) {
            ProcesoETL etl = ultimo();
            TareaWeb tarea = contexto.getTareas().iniciar("Load a output/", avisar -> cargar(etl, avisar));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("GET", "etl", "resultado")) {
            Respuesta.json(p.getIntercambio(), 200, resultado(ultimo()));
            return true;
        }
        if (p.es("GET", "etl", "validos")) {
            Respuesta.json(p.getIntercambio(), 200, validos(ultimo(), p.parametroEntero("fuente", 0), p.parametroEntero("pagina", 1)));
            return true;
        }
        if (p.es("GET", "etl", "errores")) {
            Respuesta.json(p.getIntercambio(), 200, errores(ultimo(), p.parametro("tipo", "TODOS"), p.parametroEntero("pagina", 1)));
            return true;
        }
        if (p.es("GET", "output", "*")) {
            Respuesta.json(p.getIntercambio(), 200, verOutput(p.parte(1)));
            return true;
        }
        return false;
    }

    private ProcesoETL ultimo() throws Exception {
        if (!contexto.getSesion().tieneEtl()) {
            throw new ErrorWeb(409, "Primero ejecuta el ETL");
        }
        return contexto.getSesion().ultimoEtl();
    }

    private String ejecutar(boolean esReal, Consumer<String> avisar) throws Exception {
        ProcesoETL etl = esReal ? ProcesoETL.real() : ProcesoETL.demostracion();
        etl.setAvisos(avisar);
        long inicio = System.nanoTime();
        etl.ejecutar();
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        contexto.getSesion().registrarEtl(etl, esReal);
        contexto.getReporte().registrarEtl(resumen(etl, esReal, ms));
        return resumen(etl, esReal, ms);
    }

    private String cargar(ProcesoETL etl, Consumer<String> avisar) throws Exception {
        etl.setAvisos(avisar);
        long inicio = System.nanoTime();
        etl.cargar("output");
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        return new ObjetoJson().entero("ms", ms).crudo("archivos", archivosEscritos(etl)).toString();
    }

    private ArregloJson archivosEscritos(ProcesoETL etl) throws Exception {
        ArregloJson archivos = new ArregloJson();
        for (String ruta : etl.getArchivosEscritos()) {
            Path archivo = Paths.get(ruta);
            archivos.crudo(new ObjetoJson().texto("nombre", archivo.getFileName().toString())
                    .entero("bytes", Files.exists(archivo) ? Files.size(archivo) : 0));
        }
        return archivos;
    }

    public static String resumen(ProcesoETL etl, boolean esReal, long ms) {
        Transform t = etl.getTransform();
        int procesados = 0;
        int validos = 0;
        ArregloJson fuentes = new ArregloJson();
        for (int f = 0; f < Transform.FUENTES.length; f++) {
            procesados += t.getProcesados(f);
            validos += t.getValidos(f);
            fuentes.crudo(new ObjetoJson().texto("fuente", Transform.FUENTES[f]).entero("procesados", t.getProcesados(f))
                    .entero("validos", t.getValidos(f)).entero("invalidos", t.getInvalidos(f)));
        }
        ArregloJson tipos = new ArregloJson();
        for (TipoError tipo : TipoError.values()) {
            tipos.crudo(new ObjetoJson().texto("tipo", tipo.name()).entero("total", t.getErroresDeTipo(tipo)));
        }
        return new ObjetoJson().texto("modo", esReal ? "REAL" : "DEMO").entero("procesados", procesados)
                .entero("validos", validos).entero("errores", t.getErrores().tamanio()).entero("ms", ms)
                .crudo("fuentes", fuentes).crudo("tipos", tipos).toString();
    }

    private String resultado(ProcesoETL etl) throws Exception {
        Transform t = etl.getTransform();
        int procesados = 0;
        int validos = 0;
        ArregloJson fuentes = new ArregloJson();
        for (int f = 0; f < Transform.FUENTES.length; f++) {
            procesados += t.getProcesados(f);
            validos += t.getValidos(f);
            fuentes.crudo(new ObjetoJson().entero("indice", f).texto("fuente", Transform.FUENTES[f])
                    .texto("archivo", Paths.get(etl.getArchivo(f)).getFileName().toString())
                    .entero("encolados", etl.getEncolados(f)).entero("procesados", t.getProcesados(f))
                    .entero("validos", t.getValidos(f)).entero("invalidos", t.getInvalidos(f))
                    .crudo("cola", ArregloJson.deTextos(etl.getVistaCola(f))));
        }
        ArregloJson tipos = new ArregloJson();
        for (TipoError tipo : TipoError.values()) {
            ArregloJson porFuente = new ArregloJson();
            for (int f = 0; f < Transform.FUENTES.length; f++) {
                porFuente.entero(t.getErrores(f, tipo));
            }
            tipos.crudo(new ObjetoJson().texto("tipo", tipo.name()).entero("total", t.getErroresDeTipo(tipo))
                    .crudo("porFuente", porFuente));
        }
        return new ObjetoJson()
                .texto("modo", contexto.getSesion().ultimoEtlEsReal() ? "REAL" : "DEMO")
                .entero("procesados", procesados)
                .entero("validos", validos)
                .entero("errores", t.getErrores().tamanio())
                .decimal("msExtract", etl.getMsExtract())
                .decimal("msTransform", t.getMilisegundos())
                .crudo("fuentes", fuentes)
                .crudo("tipos", tipos)
                .texto("reporte", etl.reporte())
                .crudo("archivos", archivosEscritos(etl))
                .toString();
    }

    private FuenteDatos fuenteDatos(int fuente) {
        if (fuente == Transform.RUTAS) {
            return FuenteDatos.RUTAS;
        }
        return fuente == Transform.AEROLINEAS ? FuenteDatos.AEROLINEAS : FuenteDatos.AEROPUERTOS;
    }

    private ListaSecuencial<? extends Registro> lista(DatosLimpios datos, int fuente) {
        if (fuente == Transform.RUTAS) {
            return datos.getRutas();
        }
        return fuente == Transform.AEROLINEAS ? datos.getAerolineas() : datos.getAeropuertos();
    }

    private String validos(ProcesoETL etl, int fuente, int pagina) throws ErrorWeb {
        if (fuente < 0 || fuente >= Transform.FUENTES.length) {
            throw new ErrorWeb(400, "Fuente desconocida");
        }
        ListaSecuencial<? extends Registro> lista = lista(etl.getDatos(), fuente);
        int total = lista.tamanio();
        int paginas = Math.max(1, (total + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
        int actual = Math.min(Math.max(1, pagina), paginas);
        int desde = (actual - 1) * FILAS_POR_PAGINA;
        int hasta = Math.min(total, desde + FILAS_POR_PAGINA);
        ArregloJson filas = new ArregloJson();
        for (int i = desde; i < hasta; i++) {
            Registro registro = lista.obtener(i);
            ArregloJson celdas = new ArregloJson();
            for (int c = 0; c < registro.numeroCampos(); c++) {
                celdas.texto(registro.valor(c));
            }
            filas.crudo(celdas);
        }
        return new ObjetoJson().entero("fuente", fuente).texto("nombre", Transform.FUENTES[fuente])
                .crudo("encabezados", ArregloJson.deTextos(fuenteDatos(fuente).getEncabezados()))
                .entero("total", total).entero("pagina", actual).entero("paginas", paginas)
                .entero("desde", total == 0 ? 0 : desde + 1).entero("hasta", hasta).crudo("filas", filas).toString();
    }

    private String errores(ProcesoETL etl, String tipo, int pagina) {
        ListaSecuencial<ErrorETL> pila = etl.getTransform().getErrores().elementos();
        ListaSecuencial<ErrorETL> filtrados = new ListaSecuencial<>();
        ListaSecuencial<Integer> posiciones = new ListaSecuencial<>();
        for (int i = 0; i < pila.tamanio(); i++) {
            if (tipo.equals("TODOS") || pila.obtener(i).getTipo().name().equals(tipo)) {
                filtrados.insertarFinal(pila.obtener(i));
                posiciones.insertarFinal(i + 1);
            }
        }
        int total = filtrados.tamanio();
        int paginas = Math.max(1, (total + ERRORES_POR_PAGINA - 1) / ERRORES_POR_PAGINA);
        int actual = Math.min(Math.max(1, pagina), paginas);
        int desde = (actual - 1) * ERRORES_POR_PAGINA;
        int hasta = Math.min(total, desde + ERRORES_POR_PAGINA);
        ArregloJson elementos = new ArregloJson();
        for (int i = desde; i < hasta; i++) {
            ErrorETL error = filtrados.obtener(i);
            elementos.crudo(new ObjetoJson().entero("posicion", posiciones.obtener(i)).texto("fuente", error.getFuente())
                    .entero("linea", error.getLinea()).texto("identificador", error.getIdentificador())
                    .texto("tipo", error.getTipo().name()).texto("motivo", error.getMotivo())
                    .texto("valorOriginal", error.getValorOriginal()));
        }
        return new ObjetoJson().texto("tipo", tipo).entero("totalPila", pila.tamanio()).entero("total", total)
                .entero("pagina", actual).entero("paginas", paginas).crudo("errores", elementos).toString();
    }

    private String verOutput(String nombre) throws Exception {
        boolean permitido = false;
        for (String conocido : ARCHIVOS_OUTPUT) {
            permitido = permitido || conocido.equals(nombre);
        }
        if (!permitido) {
            throw new ErrorWeb(404, "Solo se pueden ver los 5 archivos que escribe el Load");
        }
        Path archivo = Paths.get(Proyecto.ruta("output")).resolve(nombre);
        if (!Files.exists(archivo)) {
            throw new ErrorWeb(404, "Todavía no existe output/" + nombre + ": pulsa Cargar");
        }
        StringBuilder texto = new StringBuilder();
        int lineas = 0;
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (lineas < LINEAS_VISOR) {
                    texto.append(linea).append('\n');
                }
                lineas++;
            }
        }
        return new ObjetoJson().texto("nombre", nombre).entero("bytes", Files.size(archivo)).entero("totalLineas", lineas)
                .entero("mostradas", Math.min(lineas, LINEAS_VISOR)).texto("contenido", texto.toString()).toString();
    }
}
