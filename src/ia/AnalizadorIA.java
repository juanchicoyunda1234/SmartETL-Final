package ia;

import etl.Proyecto;
import estructuras.ListaSecuencial;
import estructuras.Nodo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalizadorIA {

    public static final String CARPETA_EVIDENCIAS = "evidencias_ia";

    private static final String[] PALABRAS_IGNORADAS = {"BFS", "DFS", "ETL", "BST", "CSV", "API", "FIN", "IDS", "LOAD"};

    private final ConfiguracionIA configuracion;
    private Consumer<String> avisar = mensaje -> {
    };

    public AnalizadorIA(ConfiguracionIA configuracion) {
        this.configuracion = configuracion;
    }

    public void setAvisos(Consumer<String> avisar) {
        this.avisar = avisar;
    }

    private String destino() {
        return configuracion.getProveedor() + " (" + configuracion.getModelo() + ")";
    }

    public ResultadoIA diagrama(String funcion, String nombreArchivo, String titulo, String prompt, String mermaidLocal,
                                String[] esperados, String[] conocidos) throws IOException {
        String mermaid = mermaidLocal;
        String crudo = "";
        boolean usoApi = false;
        String aviso;
        if (configuracion.soloLocal()) {
            avisar.accept("Generando el diagrama con el respaldo local desde el registro de eventos");
            aviso = "Modo solo local: el diagrama lo generó el programa a partir del registro de eventos.";
        } else {
            try {
                avisar.accept("Conectando con " + destino() + " y esperando el diagrama");
                crudo = llamarApi(prompt);
                avisar.accept("Respuesta recibida: limpiando el código Mermaid");
                String limpio = limpiarMermaid(crudo);
                if (limpio == null) {
                    throw new IOException("la respuesta no es un diagrama Mermaid que empiece con flowchart o graph");
                }
                mermaid = limpio;
                usoApi = true;
                aviso = "Respuesta de " + configuracion.getProveedor() + " (" + configuracion.getModelo() + ").";
            } catch (Exception ex) {
                avisar.accept("La API falló (" + ex.getMessage() + "): usando el respaldo local");
                aviso = "La API no respondió bien (" + ex.getMessage() + "). Se usó el respaldo local.";
            }
        }
        avisar.accept("Comprobando la coherencia con el registro de eventos");
        String coherencia = coherencia(mermaid, esperados, conocidos);
        avisar.accept("Generando la página HTML con zoom");
        Path html = VisorHtml.escribirDiagrama(nombreArchivo, titulo, mermaid,
                (usoApi ? "Generado por IA" : "Generado por el respaldo local") + " desde el registro de eventos");
        if (!configuracion.soloLocal()) {
            avisar.accept("Guardando la evidencia en " + CARPETA_EVIDENCIAS);
        }
        Path evidencia = configuracion.soloLocal() ? null
                : guardarEvidencia(funcion, prompt, crudo, mermaid, usoApi, aviso + "\n" + coherencia);
        return new ResultadoIA(funcion, mermaid, true, usoApi, aviso, coherencia, html, evidencia);
    }

    public ResultadoIA texto(String funcion, String prompt, String textoLocal) throws IOException {
        String texto = "Respaldo local (sin IA). Datos reales usados:\n\n" + textoLocal;
        String crudo = "";
        boolean usoApi = false;
        String aviso;
        if (configuracion.soloLocal()) {
            avisar.accept("Preparando los datos reales (modo solo local)");
            aviso = "Modo solo local: se muestran los datos reales que se enviarían a la IA.";
        } else {
            try {
                avisar.accept("Conectando con " + destino() + " y esperando la explicación");
                crudo = llamarApi(prompt);
                avisar.accept("Respuesta recibida");
                if (crudo.trim().isEmpty()) {
                    throw new IOException("respuesta vacía");
                }
                texto = crudo.trim();
                usoApi = true;
                aviso = "Respuesta de " + configuracion.getProveedor() + " (" + configuracion.getModelo() + ").";
            } catch (Exception ex) {
                avisar.accept("La API falló (" + ex.getMessage() + "): usando el respaldo local");
                aviso = "La API no respondió bien (" + ex.getMessage() + "). Se usó el respaldo local.";
            }
            avisar.accept("Guardando la evidencia en " + CARPETA_EVIDENCIAS);
        }
        Path evidencia = configuracion.soloLocal() ? null
                : guardarEvidencia(funcion, prompt, crudo, texto, usoApi, aviso);
        return new ResultadoIA(funcion, texto, false, usoApi, aviso, "", null, evidencia);
    }

    public String validarConexion() throws Exception {
        String respuesta = llamarApi("Responde únicamente con la palabra OK.");
        if (respuesta == null || respuesta.trim().isEmpty()) {
            throw new IOException("la API respondió vacío");
        }
        return respuesta.trim();
    }

    private String llamarApi(String prompt) throws Exception {
        if (!configuracion.tieneClave()) {
            throw new IOException("no hay clave de API");
        }
        if (configuracion.getModelo().isEmpty()) {
            throw new IOException("falta escribir el nombre del modelo");
        }
        return configuracion.crearProveedor().enviar(prompt);
    }

    public static String limpiarMermaid(String respuesta) {
        if (respuesta == null) {
            return null;
        }
        String texto = respuesta.trim();
        int cerca = texto.indexOf("```");
        if (cerca >= 0) {
            int inicioLinea = texto.indexOf('\n', cerca);
            int cierre = inicioLinea < 0 ? -1 : texto.indexOf("```", inicioLinea);
            if (inicioLinea < 0) {
                return null;
            }
            texto = cierre < 0 ? texto.substring(inicioLinea + 1) : texto.substring(inicioLinea + 1, cierre);
        }
        StringBuilder limpio = new StringBuilder();
        for (String linea : texto.split("\n")) {
            String sinEspacios = linea.replaceAll("\\s+$", "");
            if (!sinEspacios.isEmpty()) {
                limpio.append(sinEspacios).append('\n');
            }
        }
        String resultado = limpio.toString().trim();
        if (resultado.isEmpty() || !(resultado.startsWith("flowchart") || resultado.startsWith("graph"))) {
            return null;
        }
        return resultado + "\n";
    }

    public static String coherencia(String mermaid, String[] esperados, String[] conocidos) {
        String normal = sinSeparadoresDeMiles(mermaid);
        ListaSecuencial<String> faltan = new ListaSecuencial<>();
        for (String esperado : esperados) {
            if (!esperado.isEmpty() && !contienePalabra(normal, esperado) && faltan.contar(esperado::equals) == 0) {
                faltan.insertarFinal(esperado);
            }
        }
        ListaSecuencial<String> sobran = new ListaSecuencial<>();
        Matcher m = Pattern.compile("(?<![A-Za-z0-9_])[A-Z]{3}(?![A-Za-z0-9_])").matcher(mermaid);
        while (m.find()) {
            String codigo = m.group();
            if (!esta(conocidos, codigo) && !esta(PALABRAS_IGNORADAS, codigo) && sobran.contar(codigo::equals) == 0) {
                sobran.insertarFinal(codigo);
            }
        }
        if (faltan.estaVacia() && sobran.estaVacia()) {
            return "Coherencia: el diagrama contiene todos los pasos esperados del registro y no agrega aeropuertos.";
        }
        return "Coherencia: faltan: " + (faltan.estaVacia() ? "ninguno" : unir(faltan))
                + " | sobran: " + (sobran.estaVacia() ? "ninguno" : unir(sobran));
    }

    private static String sinSeparadoresDeMiles(String texto) {
        String anterior;
        String actual = texto;
        do {
            anterior = actual;
            actual = actual.replaceAll("(\\d)[.,](\\d{3})(?!\\d)", "$1$2");
        } while (!actual.equals(anterior));
        return actual;
    }

    private static boolean contienePalabra(String texto, String palabra) {
        return Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(palabra) + "(?![A-Za-z0-9])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(texto).find();
    }

    private static boolean esta(String[] lista, String valor) {
        for (String elemento : lista) {
            if (elemento.equals(valor)) {
                return true;
            }
        }
        return false;
    }

    private static String unir(ListaSecuencial<String> lista) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < lista.tamanio(); i++) {
            texto.append(i == 0 ? "" : ", ").append(lista.obtener(i));
        }
        return texto.toString();
    }

    public static String[] esperadosRecorrido(RegistroEventos registro, int nivelDiagrama) {
        String verbo = registro.contar("desencolar") > 0 ? "desencolar" : "desapilar";
        ListaSecuencial<String> esperados = new ListaSecuencial<>();
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            Evento e = n.getDato();
            if (e.getTipo().equals(verbo) && e.getNivel() <= nivelDiagrama) {
                esperados.insertarFinal(e.getSujeto());
            }
        }
        return comoArreglo(esperados);
    }

    public static String[] esperadosEstructura(RegistroEventos registro) {
        ListaSecuencial<String> esperados = new ListaSecuencial<>();
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            if (!n.getDato().getSujeto().isEmpty()) {
                esperados.insertarFinal(n.getDato().getSujeto());
            }
        }
        return comoArreglo(esperados);
    }

    public static String[] esperadosEtl(RegistroEventos registro) {
        ListaSecuencial<String> esperados = new ListaSecuencial<>();
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            Evento e = n.getDato();
            if (e.getTipo().equals("totales")) {
                esperados.insertarFinal(e.getElementos()[0]);
                esperados.insertarFinal(e.getElementos()[1]);
                esperados.insertarFinal(e.getElementos()[2]);
            } else if (e.getTipo().equals("leer")) {
                esperados.insertarFinal(e.getSujeto().replace(".csv", ""));
            }
        }
        return comoArreglo(esperados);
    }

    public static String[] conocidos(RegistroEventos registro) {
        ListaSecuencial<String> conocidos = new ListaSecuencial<>();
        for (Nodo<Evento> n = registro.getEventos().primerNodo(); n != null; n = n.getSiguiente()) {
            Evento e = n.getDato();
            conocidos.insertarFinal(e.getSujeto());
            Matcher codigos = Pattern.compile("[A-Z]{3}").matcher(e.getTexto());
            while (codigos.find()) {
                conocidos.insertarFinal(codigos.group());
            }
            for (String elemento : e.getElementos()) {
                conocidos.insertarFinal(elemento.replaceAll(" \\(.*\\)$", ""));
            }
            for (String elemento : e.getEstado()) {
                conocidos.insertarFinal(elemento);
            }
        }
        return comoArreglo(conocidos);
    }

    private static String[] comoArreglo(ListaSecuencial<String> lista) {
        String[] arreglo = new String[lista.tamanio()];
        for (int i = 0; i < arreglo.length; i++) {
            arreglo[i] = lista.obtener(i);
        }
        return arreglo;
    }

    private Path guardarEvidencia(String funcion, String prompt, String crudo, String resultado, boolean usoApi,
                                  String notas) throws IOException {
        Path carpeta = Paths.get(Proyecto.ruta(CARPETA_EVIDENCIAS));
        Files.createDirectories(carpeta);
        LocalDateTime ahora = LocalDateTime.now();
        String marca = ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        Path archivo = carpeta.resolve(marca + "_" + VisorHtml.normalizar(funcion) + ".md");
        String contenido = "# Evidencia de IA: " + funcion + "\n\n"
                + "- Fecha y hora: " + ahora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n"
                + "- Proveedor: " + configuracion.getProveedor() + "\n"
                + "- Modelo: " + (configuracion.getModelo().isEmpty() ? "(no indicado)" : configuracion.getModelo()) + "\n"
                + "- Se usó el respaldo local: " + (usoApi ? "no" : "sí") + "\n"
                + "- Notas: " + notas.replace("\n", " | ") + "\n\n"
                + "## Prompt enviado\n\n````text\n" + prompt + "\n````\n\n"
                + "## Respuesta cruda\n\n````text\n" + (crudo.isEmpty() ? "(sin respuesta de la API)" : crudo) + "\n````\n\n"
                + "## Resultado final\n\n" + (resultado.trim().startsWith("flowchart") || resultado.trim().startsWith("graph")
                ? "```mermaid\n" + resultado.trim() + "\n```\n" : resultado + "\n");
        Files.write(archivo, contenido.getBytes(StandardCharsets.UTF_8));
        return archivo;
    }
}
