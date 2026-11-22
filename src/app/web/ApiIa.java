package app.web;

import estructuras.ListaSecuencial;
import etl.Proyecto;
import ia.AnalizadorIA;
import ia.ApiGuardada;
import ia.ConfiguracionIA;
import ia.GeneradorPrompt;
import ia.ResultadoIA;
import ia.VisorHtml;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiIa implements AreaApi {

    public static final String[] NOMBRES = {"Flujograma ETL", "Flujograma Cola", "Flujograma Pila", "Flujograma Lista enlazada",
            "Flujograma BST", "Flujograma BFS", "Flujograma DFS", "Explicar árbol", "Explicar grafo", "Analizar errores ETL"};

    private static final Pattern ETIQUETA = Pattern.compile("[\\[({]+\"?([^\\[\\](){}\"]{2,160})\"?[\\])}]+");

    private final ContextoWeb contexto;

    public ApiIa(ContextoWeb contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean atender(Peticion p) throws Exception {
        if (p.es("GET", "ia", "config")) {
            Respuesta.json(p.getIntercambio(), 200, configuracion());
            return true;
        }
        if (p.es("POST", "ia", "config")) {
            contexto.getTareas().exigirLibre();
            LectorJson json = p.json();
            String proveedor = json.texto("proveedor", "Gemini");
            String modo = json.texto("modo", ConfiguracionIA.MODO_API);
            if (!esProveedor(proveedor)) {
                errorCampo(p, "proveedor", "Proveedor desconocido: " + proveedor);
                return true;
            }
            if (!modo.equals(ConfiguracionIA.MODO_API) && !modo.equals(ConfiguracionIA.MODO_LOCAL)) {
                errorCampo(p, "modo", "Modo desconocido: " + modo);
                return true;
            }
            aplicar(proveedor, json.texto("modelo", ""), json.texto("url", ""), modo, json.texto("clave", ""), json.booleano("conservarClave", false));
            Respuesta.json(p.getIntercambio(), 200, configuracion());
            return true;
        }
        if (p.es("POST", "ia", "validar")) {
            ConfiguracionIA configuracion = contexto.getSesion().getConfiguracion();
            String destino = configuracion.getProveedor() + " (" + configuracion.getModelo() + ")";
            TareaWeb tarea = contexto.getTareas().iniciar("Validar " + destino, avisar -> validar(configuracion, destino, avisar));
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("POST", "ia", "funcion")) {
            LectorJson json = p.json();
            String funcion = json.texto("funcion", "");
            boolean incluirResumen = json.booleano("incluirResumen", true);
            if (!FuncionesIa.existe(funcion)) {
                throw new ErrorWeb(400, "Función de IA desconocida: " + funcion);
            }
            TareaWeb tarea = contexto.getTareas().iniciar("IA: " + nombre(funcion), avisar -> {
                AnalizadorIA analizador = analizador(avisar);
                ResultadoIA resultado = ejecutar(funcion, analizador);
                String resumen = null;
                if (resultado.esDiagrama() && incluirResumen) {
                    avisar.accept("Resumen: pidiendo una explicación corta del diagrama");
                    ResultadoIA texto = analizador.texto(resultado.getFuncion() + " · resumen", GeneradorPrompt.explicacion(
                            "qué muestra este flujograma paso a paso, en lenguaje sencillo para un estudiante", resultado.getContenido()),
                            resumenLocal(resultado.getContenido()));
                    resumen = comoJson(funcion, texto, null);
                }
                String resultadoJson = comoJson(funcion, resultado, resumen);
                String id = contexto.getReporte().agregarIa(resultadoJson);
                return new ObjetoJson().texto("historial", id).crudo("resultado", resultadoJson).toString();
            });
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("POST", "ia", "documentacion")) {
            TareaWeb tarea = contexto.getTareas().iniciar("Generar documentación", this::documentacion);
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("GET", "ia", "historial")) {
            Respuesta.json(p.getIntercambio(), 200, contexto.getReporte().historialIa());
            return true;
        }
        if (p.es("GET", "evidencias", "*")) {
            Respuesta.json(p.getIntercambio(), 200, evidencia(p.parte(1)));
            return true;
        }
        return false;
    }

    private void errorCampo(Peticion p, String campo, String mensaje) throws IOException {
        Respuesta.json(p.getIntercambio(), 400, new ObjetoJson().texto("error", mensaje).texto("campo", campo));
    }

    public static boolean esProveedor(String proveedor) {
        for (String conocido : ConfiguracionIA.PROVEEDORES) {
            if (conocido.equals(proveedor)) {
                return true;
            }
        }
        return false;
    }

    public static String nombre(String funcion) {
        for (int i = 0; i < FuncionesIa.FUNCIONES.length; i++) {
            if (FuncionesIa.FUNCIONES[i].equals(funcion)) {
                return NOMBRES[i];
            }
        }
        return funcion;
    }

    private void aplicar(String proveedor, String modelo, String url, String modo, String clave, boolean conservarClave) {
        ConfiguracionIA configuracion = contexto.getSesion().getConfiguracion();
        String urlFinal = url.trim().isEmpty() ? ConfiguracionIA.urlDe(proveedor) : url.trim();
        boolean mismoProveedor = proveedor.equals(configuracion.getProveedor());
        boolean cambio = !mismoProveedor || !modelo.trim().equals(configuracion.getModelo()) || !urlFinal.equals(configuracion.getUrl());
        configuracion.setProveedor(proveedor);
        configuracion.setModelo(modelo);
        configuracion.setUrl(urlFinal);
        configuracion.setModo(modo);
        if (!clave.trim().isEmpty()) {
            configuracion.setClave(clave.trim().toCharArray());
            contexto.setOrigenClave("escrita");
            cambio = true;
        } else if (!(conservarClave && mismoProveedor && configuracion.tieneClave())) {
            String deEntorno = ConfiguracionIA.claveDeEntorno(proveedor);
            configuracion.setClave(deEntorno.toCharArray());
            contexto.setOrigenClave(deEntorno.isEmpty() ? "" : "entorno:" + ConfiguracionIA.variableDeEntorno(proveedor));
            cambio = true;
        }
        if (cambio) {
            contexto.setValidacion("sin_validar", "");
        }
    }

    public void usarGuardada(ApiGuardada api) {
        ConfiguracionIA configuracion = contexto.getSesion().getConfiguracion();
        configuracion.setProveedor(api.getProveedor());
        configuracion.setModelo(api.getModelo());
        configuracion.setUrl(api.getUrl().isEmpty() ? ConfiguracionIA.urlDe(api.getProveedor()) : api.getUrl());
        configuracion.setClave(api.getClave().toCharArray());
        configuracion.setModo(ConfiguracionIA.MODO_API);
        contexto.setOrigenClave("guardada:" + api.getNombre());
        contexto.setValidacion("sin_validar", "");
    }

    public String configuracion() {
        ConfiguracionIA configuracion = contexto.getSesion().getConfiguracion();
        ArregloJson proveedores = new ArregloJson();
        for (String proveedor : ConfiguracionIA.PROVEEDORES) {
            proveedores.crudo(new ObjetoJson().texto("nombre", proveedor).texto("url", ConfiguracionIA.urlDe(proveedor))
                    .texto("variable", ConfiguracionIA.variableDeEntorno(proveedor))
                    .booleano("enEntorno", !ConfiguracionIA.claveDeEntorno(proveedor).isEmpty())
                    .texto("ayudaModelo", ConfiguracionIA.ayudaModelo(proveedor))
                    .texto("ayudaClave", ConfiguracionIA.ayudaClave(proveedor)));
        }
        return new ObjetoJson().crudo("proveedores", proveedores)
                .crudo("modos", ArregloJson.deTextos(new String[]{ConfiguracionIA.MODO_API, ConfiguracionIA.MODO_LOCAL}))
                .crudo("actual", new ObjetoJson().texto("proveedor", configuracion.getProveedor())
                        .texto("modelo", configuracion.getModelo()).texto("url", configuracion.getUrl())
                        .texto("modo", configuracion.getModo()).booleano("tieneClave", configuracion.tieneClave())
                        .texto("origenClave", configuracion.tieneClave() ? contexto.getOrigenClave() : ""))
                .crudo("validacion", new ObjetoJson().texto("estado", contexto.getEstadoValidacion())
                        .texto("detalle", contexto.getDetalleValidacion()))
                .toString();
    }

    private String validar(ConfiguracionIA configuracion, String destino, Consumer<String> avisar) throws Exception {
        if (configuracion.getModelo().isEmpty() || !configuracion.tieneClave()) {
            String motivo = configuracion.getModelo().isEmpty() ? "falta escribir el modelo" : "falta la clave de API";
            contexto.setValidacion("fallo", motivo);
            throw new ErrorWeb(400, motivo);
        }
        contexto.setValidacion("validando", "");
        avisar.accept("Conectando con " + destino);
        long inicio = System.nanoTime();
        try {
            String respuesta = new AnalizadorIA(configuracion).validarConexion();
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            String corta = respuesta.length() > 30 ? respuesta.substring(0, 30) + "…" : respuesta;
            contexto.setValidacion("validada", destino + " respondió en " + ms + " ms: " + corta);
            return new ObjetoJson().texto("estado", "validada").entero("ms", ms).texto("respuesta", corta)
                    .texto("proveedor", configuracion.getProveedor()).texto("modelo", configuracion.getModelo()).toString();
        } catch (Exception ex) {
            String motivo = motivo(ex, configuracion.getUrl());
            contexto.setValidacion("fallo", motivo);
            throw new ErrorWeb(502, motivo);
        }
    }

    private String motivo(Exception ex, String url) {
        if (ex instanceof ConnectException) {
            return "no se pudo conectar con " + url;
        }
        if (ex instanceof HttpTimeoutException) {
            return "la API no respondió a tiempo";
        }
        return ex.getMessage() == null || ex.getMessage().isEmpty() ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private AnalizadorIA analizador(Consumer<String> avisar) {
        ConfiguracionIA configuracion = contexto.getSesion().getConfiguracion();
        if (!configuracion.tieneClave()) {
            String deEntorno = ConfiguracionIA.claveDeEntorno(configuracion.getProveedor());
            if (!deEntorno.isEmpty()) {
                configuracion.setClave(deEntorno.toCharArray());
                contexto.setOrigenClave("entorno:" + ConfiguracionIA.variableDeEntorno(configuracion.getProveedor()));
            }
        }
        AnalizadorIA analizador = new AnalizadorIA(configuracion);
        analizador.setAvisos(avisar);
        return analizador;
    }

    private ResultadoIA ejecutar(String funcion, AnalizadorIA analizador) throws Exception {
        synchronized (contexto.getCandadoGrafo()) {
            return FuncionesIa.ejecutar(funcion, analizador, contexto.getSesion());
        }
    }

    public static String resumenLocal(String mermaid) {
        ListaSecuencial<String> etiquetas = new ListaSecuencial<>();
        Matcher m = ETIQUETA.matcher(mermaid);
        while (m.find()) {
            String etiqueta = m.group(1).split("<br")[0].replace("&quot;", "").trim();
            etiqueta = etiqueta.length() > 48 ? etiqueta.substring(0, 47) + "…" : etiqueta;
            boolean repetida = false;
            for (int i = 0; i < etiquetas.tamanio(); i++) {
                repetida = repetida || etiquetas.obtener(i).equals(etiqueta);
            }
            if (!repetida && !etiqueta.isEmpty()) {
                etiquetas.insertarFinal(etiqueta);
            }
        }
        int flechas = mermaid.split("-->|-\\.->|==>", -1).length - 1;
        StringBuilder texto = new StringBuilder("El diagrama tiene " + etiquetas.tamanio() + " pasos y " + flechas + " flechas.");
        if (!etiquetas.estaVacia()) {
            texto.append(" Recorrido: ");
            for (int i = 0; i < Math.min(8, etiquetas.tamanio()); i++) {
                texto.append(i == 0 ? "" : " → ").append(etiquetas.obtener(i));
            }
            texto.append(etiquetas.tamanio() > 8 ? " → …" : "").append('.');
        }
        return texto.toString();
    }

    public String comoJson(String funcion, ResultadoIA r, String resumen) {
        ConfiguracionIA configuracion = contexto.getSesion().getConfiguracion();
        String coherencia = r.getCoherencia() == null ? "" : r.getCoherencia();
        return new ObjetoJson()
                .texto("funcion", funcion)
                .texto("titulo", r.getFuncion())
                .booleano("esDiagrama", r.esDiagrama())
                .texto("contenido", r.getContenido())
                .booleano("usoApi", r.usoApi())
                .texto("aviso", r.getAviso())
                .texto("coherencia", coherencia)
                .booleano("coherente", coherencia.startsWith("Coherencia: el diagrama contiene todos"))
                .texto("html", r.getHtml() == null ? null : "/diagramas/" + r.getHtml().getFileName())
                .texto("evidencia", r.getEvidencia() == null ? null : r.getEvidencia().getFileName().toString())
                .texto("proveedor", configuracion.getProveedor())
                .texto("modelo", configuracion.getModelo())
                .entero("momento", System.currentTimeMillis())
                .crudo("resumen", resumen)
                .toString();
    }

    public String comoJson(String funcion, ResultadoIA r) {
        return comoJson(funcion, r, null);
    }

    private String documentacion(Consumer<String> avisar) throws Exception {
        ListaSecuencial<ResultadoIA> resultados = new ListaSecuencial<>();
        ArregloJson resumen = new ArregloJson();
        int conApi = 0;
        for (int i = 0; i < FuncionesIa.FUNCIONES.length; i++) {
            String prefijo = "Documentación " + (i + 1) + " de 10 · " + NOMBRES[i] + ": ";
            avisar.accept(prefijo + "preparando datos");
            AnalizadorIA analizador = analizador(mensaje -> avisar.accept(prefijo + mensaje));
            ResultadoIA r = ejecutar(FuncionesIa.FUNCIONES[i], analizador);
            resultados.insertarFinal(r);
            String id = contexto.getReporte().agregarIa(comoJson(FuncionesIa.FUNCIONES[i], r));
            conApi += r.usoApi() ? 1 : 0;
            resumen.crudo(new ObjetoJson().texto("historial", id).texto("titulo", r.getFuncion()).booleano("usoApi", r.usoApi())
                    .booleano("esDiagrama", r.esDiagrama())
                    .booleano("coherente", r.getCoherencia() != null && r.getCoherencia().startsWith("Coherencia: el diagrama contiene todos")));
        }
        avisar.accept("Escribiendo diagramas/DOCUMENTACION.md");
        escribirDocumentacion(resultados);
        String resultado = new ObjetoJson().texto("archivo", "/diagramas/DOCUMENTACION.md").entero("conApi", conApi)
                .entero("total", resultados.tamanio()).crudo("funciones", resumen).toString();
        contexto.getReporte().registrar("documentacion", resultado);
        return resultado;
    }

    private void escribirDocumentacion(ListaSecuencial<ResultadoIA> resultados) throws IOException {
        StringBuilder md = new StringBuilder("# Documentación generada por SmartETL-DS\n\n");
        md.append("Generada el ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append(" a partir de ejecuciones reales. Cada diagrama tiene su página con zoom (`ver_*.html`).\n\n");
        for (int i = 0; i < resultados.tamanio(); i++) {
            ResultadoIA r = resultados.obtener(i);
            md.append("## ").append(r.getFuncion()).append("\n\n").append("*").append(r.origen()).append(".*");
            if (r.esDiagrama()) {
                md.append(" ").append(r.getCoherencia()).append(" Ver con zoom: [")
                        .append(r.getHtml().getFileName()).append("](").append(r.getHtml().getFileName()).append(")\n\n")
                        .append("```mermaid\n").append(r.getContenido().trim()).append("\n```\n\n");
            } else {
                md.append("\n\n").append(r.getContenido().trim()).append("\n\n");
            }
        }
        Path archivo = Paths.get(Proyecto.ruta(VisorHtml.CARPETA), "DOCUMENTACION.md");
        Files.createDirectories(archivo.getParent());
        Files.write(archivo, md.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String evidencia(String nombre) throws Exception {
        if (!nombre.matches("[A-Za-z0-9_.\\-]+\\.md")) {
            throw new ErrorWeb(400, "Nombre de evidencia no válido");
        }
        Path carpeta = Paths.get(Proyecto.ruta(AnalizadorIA.CARPETA_EVIDENCIAS)).normalize();
        Path archivo = carpeta.resolve(nombre).normalize();
        if (!archivo.startsWith(carpeta) || !Files.isRegularFile(archivo)) {
            throw new ErrorWeb(404, "No existe la evidencia " + nombre);
        }
        return new ObjetoJson().texto("nombre", nombre)
                .texto("contenido", new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8)).toString();
    }
}
