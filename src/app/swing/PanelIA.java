package app.swing;

import app.Sesion;

import etl.Proyecto;
import algoritmos.BFS;
import algoritmos.DFS;
import estructuras.ListaSecuencial;
import etl.DatosLimpios;
import etl.ProcesoETL;
import grafo.GrafoPais;
import grafo.MapaPaises;
import grafo.Vertice;
import ia.AlmacenApis;
import ia.AnalizadorIA;
import ia.ApiGuardada;
import ia.ConfiguracionIA;
import ia.GeneradorFlujogramaLocal;
import ia.GeneradorPrompt;
import ia.RegistroEventos;
import ia.ResultadoIA;
import ia.TrazasEstructuras;
import ia.VisorHtml;
import model.Aeropuerto;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class PanelIA extends JPanel {

    private static final String VISTA_HTML = "HTML local con zoom (sin internet)";
    private static final String VISTA_IMAGEN = "Imagen dentro de la ventana (mermaid.ink, requiere internet)";

    private final Sesion sesion;
    private final JComboBox<String> cmbProveedor = new JComboBox<>(ConfiguracionIA.PROVEEDORES);
    private final JTextField txtModelo = new JTextField(16);
    private final JPasswordField txtClave = new JPasswordField(20);
    private final JComboBox<String> cmbModo = new JComboBox<>(new String[]{ConfiguracionIA.MODO_API, ConfiguracionIA.MODO_LOCAL});
    private final JComboBox<String> cmbVista = new JComboBox<>(new String[]{VISTA_HTML, VISTA_IMAGEN});
    private final JTextArea txtResultado = Tarea.areaTexto();
    private final JLabel lblOrigen = new JLabel(" ");
    private final JLabel lblAviso = new JLabel(" ");
    private final JLabel lblCoherencia = new JLabel(" ");
    private final JLabel lblEstado = new JLabel("Elige proveedor, escribe el modelo y la clave, o usa el modo Solo local.");
    private final JLabel lblApi = new JLabel();
    private final JTextField txtUrl = new JTextField(36);
    private final JLabel lblAyuda = new JLabel(" ");
    private final JComboBox<String> cmbGuardadas = new JComboBox<>();
    private boolean actualizandoGuardadas;
    private ResultadoIA ultimo;

    public PanelIA(Sesion sesion) {
        this.sesion = sesion;
        setLayout(new BorderLayout(4, 4));
        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));

        JPanel configuracion = Tarea.fila("Proveedor de IA (la clave vive en memoria; para guardarla usa la pestaña Guardar APIs)");
        configuracion.add(new JLabel("Proveedor:"));
        configuracion.add(cmbProveedor);
        configuracion.add(new JLabel("Modelo:"));
        txtModelo.setToolTipText("Nombre exacto del modelo según la documentación del proveedor");
        configuracion.add(txtModelo);
        configuracion.add(new JLabel("Clave:"));
        configuracion.add(txtClave);
        configuracion.add(new JLabel("Modo:"));
        configuracion.add(cmbModo);
        norte.add(configuracion);

        JPanel conexion = Tarea.fila("Conexión");
        conexion.add(new JLabel("API guardada:"));
        conexion.add(cmbGuardadas);
        conexion.add(new JLabel("URL:"));
        txtUrl.setToolTipText("Se rellena al elegir el proveedor; puedes cambiarla");
        conexion.add(txtUrl);
        lblAyuda.setForeground(new Color(90, 90, 90));
        conexion.add(lblAyuda);
        norte.add(conexion);

        JPanel validacion = Tarea.fila("Validar la API antes de usarla");
        validacion.add(Tarea.boton("Validar API", this::validarApi));
        lblApi.setOpaque(true);
        lblApi.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        validacion.add(lblApi);
        norte.add(validacion);

        JPanel vista = Tarea.fila("Visualización");
        vista.add(cmbVista);
        vista.add(Tarea.boton("Ver diagrama", this::verDiagrama));
        norte.add(vista);

        JPanel funciones = new JPanel(new GridLayout(2, 6, 4, 4));
        funciones.setBorder(BorderFactory.createTitledBorder("Funciones (los diagramas salen de eventos reales de la ejecución)"));
        funciones.add(Tarea.boton("Flujograma ETL", this::flujogramaEtl));
        funciones.add(Tarea.boton("Flujograma Cola", () -> flujogramaEstructura("cola")));
        funciones.add(Tarea.boton("Flujograma Pila", () -> flujogramaEstructura("pila")));
        funciones.add(Tarea.boton("Flujograma Lista enlazada", () -> flujogramaEstructura("lista")));
        funciones.add(Tarea.boton("Flujograma BST", () -> flujogramaEstructura("bst")));
        funciones.add(Tarea.boton("Flujograma BFS", () -> flujogramaRecorrido(true)));
        funciones.add(Tarea.boton("Flujograma DFS", () -> flujogramaRecorrido(false)));
        funciones.add(Tarea.boton("Explicar árbol", this::explicarArbol));
        funciones.add(Tarea.boton("Explicar grafo", this::explicarGrafo));
        funciones.add(Tarea.boton("Analizar errores ETL", this::analizarErrores));
        funciones.add(Tarea.boton("Generar documentación", this::documentacion));
        norte.add(funciones);
        add(norte, BorderLayout.NORTH);

        add(Tarea.conBorde(txtResultado, "Resultado: código Mermaid o explicación"), BorderLayout.CENTER);
        JPanel sur = new JPanel(new GridLayout(4, 1));
        sur.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        sur.add(lblOrigen);
        sur.add(lblAviso);
        sur.add(lblCoherencia);
        sur.add(lblEstado);
        add(sur, BorderLayout.SOUTH);
        cmbProveedor.addActionListener(e -> {
            cargarClaveDeEntorno();
            rellenarUrl();
        });
        cmbGuardadas.addActionListener(e -> elegirGuardada());
        DocumentListener alCambiar = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                marcarSinValidar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                marcarSinValidar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                marcarSinValidar();
            }
        };
        txtModelo.getDocument().addDocumentListener(alCambiar);
        txtClave.getDocument().addDocumentListener(alCambiar);
        txtUrl.getDocument().addDocumentListener(alCambiar);
        cargarClaveDeEntorno();
        rellenarUrl();
        sesion.setAlCambiarApis(this::actualizarGuardadas);
        actualizarGuardadas();
        marcarSinValidar();
    }

    private void rellenarUrl() {
        String proveedor = (String) cmbProveedor.getSelectedItem();
        txtUrl.setText(ConfiguracionIA.urlDe(proveedor));
        txtUrl.setCaretPosition(0);
        lblAyuda.setText("Modelo: " + ConfiguracionIA.ayudaModelo(proveedor) + ". Clave: " + ConfiguracionIA.ayudaClave(proveedor));
        txtModelo.setToolTipText(ConfiguracionIA.ayudaModelo(proveedor));
        txtClave.setToolTipText(ConfiguracionIA.ayudaClave(proveedor));
    }

    public void actualizarGuardadas() {
        actualizandoGuardadas = true;
        cmbGuardadas.removeAllItems();
        cmbGuardadas.addItem("(elige una)");
        AlmacenApis almacen = sesion.getApisGuardadas();
        if (almacen != null) {
            for (int i = 0; i < almacen.getApis().tamanio(); i++) {
                cmbGuardadas.addItem(almacen.getApis().obtener(i).getNombre());
            }
        }
        actualizandoGuardadas = false;
    }

    private void elegirGuardada() {
        if (actualizandoGuardadas || cmbGuardadas.getSelectedIndex() <= 0 || sesion.getApisGuardadas() == null) {
            return;
        }
        ApiGuardada api = sesion.getApisGuardadas().buscar((String) cmbGuardadas.getSelectedItem());
        if (api != null) {
            cargarApi(api);
        }
    }

    public void cargarApi(ApiGuardada api) {
        cmbProveedor.setSelectedItem(api.getProveedor());
        txtModelo.setText(api.getModelo());
        txtUrl.setText(api.getUrl().isEmpty() ? ConfiguracionIA.urlDe(api.getProveedor()) : api.getUrl());
        txtUrl.setCaretPosition(0);
        txtClave.setText(api.getClave());
        cmbModo.setSelectedItem(ConfiguracionIA.MODO_API);
        actualizandoGuardadas = true;
        cmbGuardadas.setSelectedItem(api.getNombre());
        actualizandoGuardadas = false;
        marcarSinValidar();
        lblEstado.setText("API guardada \"" + api.getNombre() + "\" cargada. Pulsa Validar API para comprobarla.");
    }

    private void marcarSinValidar() {
        lblApi.setText("API sin validar");
        lblApi.setBackground(new Color(236, 236, 236));
        lblApi.setForeground(Color.DARK_GRAY);
    }

    private void validarApi() {
        String proveedor = (String) cmbProveedor.getSelectedItem();
        String modelo = txtModelo.getText().trim();
        if (modelo.isEmpty() || txtClave.getPassword().length == 0) {
            marcarFallo("falta escribir el modelo o la clave");
            return;
        }
        AnalizadorIA analizador = analizador();
        lblApi.setBackground(new Color(255, 246, 204));
        lblApi.setForeground(Color.BLACK);
        Animacion animacion = new Animacion(lblApi, "Conectando con " + proveedor + " (" + modelo + ")");
        animacion.iniciar();
        new SwingWorker<String, Void>() {
            private long inicio;

            @Override
            protected String doInBackground() throws Exception {
                inicio = System.nanoTime();
                return analizador.validarConexion();
            }

            @Override
            protected void done() {
                animacion.detener();
                try {
                    String respuesta = get();
                    long ms = (System.nanoTime() - inicio) / 1_000_000;
                    lblApi.setText("✔ API validada: " + proveedor + " (" + modelo + ") respondió en " + ms + " ms: "
                            + (respuesta.length() > 30 ? respuesta.substring(0, 30) + "…" : respuesta));
                    lblApi.setBackground(new Color(214, 240, 214));
                    lblApi.setForeground(new Color(20, 100, 20));
                    lblEstado.setText("Listo: las funciones usarán " + proveedor + ". Si falla en una función, se usa el respaldo local.");
                } catch (ExecutionException ex) {
                    marcarFallo(ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    marcarFallo("validación interrumpida");
                }
            }
        }.execute();
    }

    private void marcarFallo(String motivo) {
        lblApi.setText("✖ La API no respondió: " + motivo);
        lblApi.setBackground(new Color(255, 228, 228));
        lblApi.setForeground(new Color(160, 30, 30));
        lblEstado.setText("Revisa proveedor, modelo y clave, o usa el modo Solo local: todo funciona sin API.");
    }

    private void cargarClaveDeEntorno() {
        String proveedor = (String) cmbProveedor.getSelectedItem();
        String clave = ConfiguracionIA.claveDeEntorno(proveedor);
        txtClave.setText(clave);
        lblEstado.setText(clave.isEmpty() ? "Sin clave en la variable " + ConfiguracionIA.variableDeEntorno(proveedor)
                + ": escríbela o usa Solo local." : "Clave leída de la variable " + ConfiguracionIA.variableDeEntorno(proveedor) + ".");
    }

    private AnalizadorIA analizador() {
        ConfiguracionIA configuracion = sesion.getConfiguracion();
        configuracion.setProveedor((String) cmbProveedor.getSelectedItem());
        configuracion.setModelo(txtModelo.getText());
        configuracion.setUrl(txtUrl.getText());
        configuracion.setClave(txtClave.getPassword());
        configuracion.setModo((String) cmbModo.getSelectedItem());
        return new AnalizadorIA(configuracion);
    }

    private void ejecutar(AnalizadorIA analizador, String mensaje, Tarea.Trabajo<ResultadoIA> trabajo) {
        Tarea.ejecutarConAvisos(this, lblEstado, mensaje, avisar -> {
            analizador.setAvisos(avisar);
            return trabajo.hacer();
        }, this::mostrar);
    }

    private void paso(AnalizadorIA analizador, Consumer<String> avisar, int numero, String nombre) {
        String prefijo = "Documentación " + numero + " de 10 · " + nombre + ": ";
        avisar.accept(prefijo + "preparando datos");
        analizador.setAvisos(mensaje -> avisar.accept(prefijo + mensaje));
    }

    private void mostrar(ResultadoIA r) {
        ultimo = r;
        txtResultado.setText(r.getContenido());
        txtResultado.setCaretPosition(0);
        lblOrigen.setText(r.getFuncion() + ": " + r.origen());
        lblAviso.setText(r.getAviso());
        lblCoherencia.setText(r.esDiagrama() ? r.getCoherencia() : " ");
        lblEstado.setText((r.getHtml() == null ? "" : "Diagrama en " + r.getHtml() + ". ")
                + (r.getEvidencia() == null ? "" : "Evidencia en " + r.getEvidencia() + "."));
    }

    private ResultadoIA diagramaEtl(AnalizadorIA analizador) throws Exception {
        RegistroEventos registro = sesion.ultimoEtl().getRegistro();
        return analizador.diagrama("Flujograma ETL", "flujo_etl", "Flujograma del ETL (eventos reales)",
                GeneradorPrompt.flujogramaEtl(registro), GeneradorFlujogramaLocal.etl(registro),
                AnalizadorIA.esperadosEtl(registro), AnalizadorIA.conocidos(registro));
    }

    private void flujogramaEtl() {
        AnalizadorIA analizador = analizador();
        ejecutar(analizador, "Generando el flujograma del ETL", () -> diagramaEtl(analizador));
    }

    private ResultadoIA diagramaEstructura(AnalizadorIA analizador, String estructura) throws Exception {
        RegistroEventos registro = estructura.equals("cola") ? TrazasEstructuras.cola()
                : estructura.equals("pila") ? TrazasEstructuras.pila()
                : estructura.equals("lista") ? TrazasEstructuras.lista() : TrazasEstructuras.bst();
        return analizador.diagrama("Flujograma " + estructura, "flujo_" + estructura, "Flujograma: " + registro.getTitulo(),
                GeneradorPrompt.flujogramaEstructura(registro), GeneradorFlujogramaLocal.estructura(registro),
                AnalizadorIA.esperadosEstructura(registro), AnalizadorIA.conocidos(registro));
    }

    private void flujogramaEstructura(String estructura) {
        AnalizadorIA analizador = analizador();
        ejecutar(analizador, "Ejecutando una traza real de " + estructura + " y generando su flujograma",
                () -> diagramaEstructura(analizador, estructura));
    }

    private ResultadoIA diagramaRecorrido(AnalizadorIA analizador, boolean esBfs) throws Exception {
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

    private void flujogramaRecorrido(boolean esBfs) {
        AnalizadorIA analizador = analizador();
        ejecutar(analizador, "Generando el flujograma del último " + (esBfs ? "BFS" : "DFS"), () -> diagramaRecorrido(analizador, esBfs));
    }

    private ResultadoIA textoArbol(AnalizadorIA analizador) throws Exception {
        DatosLimpios datos = sesion.datosReales();
        int comparacionesLista = sesion.listaEnlazada().buscar(a -> a.getIata().equals("UIO")).getComparaciones();
        String hechos = GeneradorPrompt.datosArbol(datos.getArbolIata(), "UIO", comparacionesLista);
        return analizador.texto("Explicar árbol", GeneradorPrompt.explicacion(
                "cómo está formado este árbol binario de búsqueda, por qué tiene esa altura y qué muestra el camino de la búsqueda",
                hechos), hechos);
    }

    private void explicarArbol() {
        AnalizadorIA analizador = analizador();
        ejecutar(analizador, "Pidiendo la explicación del BST", () -> textoArbol(analizador));
    }

    private ResultadoIA textoGrafo(AnalizadorIA analizador) throws Exception {
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

    private void explicarGrafo() {
        AnalizadorIA analizador = analizador();
        ejecutar(analizador, "Pidiendo la explicación del grafo de " + sesion.getUltimoPais(), () -> textoGrafo(analizador));
    }

    private ResultadoIA textoErrores(AnalizadorIA analizador) throws Exception {
        ProcesoETL etl = sesion.ultimoEtl();
        String hechos = GeneradorPrompt.datosErrores(etl.getTransform());
        return analizador.texto("Analizar errores ETL", GeneradorPrompt.explicacion(
                "qué dicen estos errores del ETL sobre la calidad de los datos y por qué cada regla los detecta", hechos), hechos);
    }

    private void analizarErrores() {
        AnalizadorIA analizador = analizador();
        ejecutar(analizador, "Analizando los errores del ETL (solo conteos agregados)", () -> textoErrores(analizador));
    }

    private void documentacion() {
        AnalizadorIA analizador = analizador();
        Tarea.ejecutarConAvisos(this, lblEstado, "Generando la documentación completa en diagramas/", avisar -> {
            ListaSecuencial<ResultadoIA> resultados = new ListaSecuencial<>();
            paso(analizador, avisar, 1, "Flujograma ETL");
            resultados.insertarFinal(diagramaEtl(analizador));
            String[] estructuras = {"cola", "pila", "lista", "bst"};
            for (int i = 0; i < estructuras.length; i++) {
                paso(analizador, avisar, i + 2, "Flujograma " + estructuras[i]);
                resultados.insertarFinal(diagramaEstructura(analizador, estructuras[i]));
            }
            paso(analizador, avisar, 6, "Flujograma BFS");
            resultados.insertarFinal(diagramaRecorrido(analizador, true));
            paso(analizador, avisar, 7, "Flujograma DFS");
            resultados.insertarFinal(diagramaRecorrido(analizador, false));
            paso(analizador, avisar, 8, "Explicar árbol");
            resultados.insertarFinal(textoArbol(analizador));
            paso(analizador, avisar, 9, "Explicar grafo");
            resultados.insertarFinal(textoGrafo(analizador));
            paso(analizador, avisar, 10, "Analizar errores");
            resultados.insertarFinal(textoErrores(analizador));
            avisar.accept("Escribiendo diagramas/DOCUMENTACION.md");
            Path indice = escribirDocumentacion(resultados);
            StringBuilder resumen = new StringBuilder("Documentación escrita en " + indice + "\n\n");
            int conApi = 0;
            for (int i = 0; i < resultados.tamanio(); i++) {
                ResultadoIA r = resultados.obtener(i);
                conApi += r.usoApi() ? 1 : 0;
                resumen.append("- ").append(r.getFuncion()).append(": ").append(r.origen())
                        .append(r.esDiagrama() ? " | " + r.getCoherencia() : "").append('\n');
            }
            return new ResultadoIA("Generar documentación", resumen.toString(), false, conApi > 0,
                    conApi + " de " + resultados.tamanio() + " funciones respondidas por la API.", "", null, null);
        }, this::mostrar);
    }

    private Path escribirDocumentacion(ListaSecuencial<ResultadoIA> resultados) throws IOException {
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
        return archivo;
    }

    private void verDiagrama() {
        if (ultimo == null || !ultimo.esDiagrama()) {
            lblEstado.setText("Primero genera un flujograma.");
            return;
        }
        ResultadoIA r = ultimo;
        if (VISTA_HTML.equals(cmbVista.getSelectedItem())) {
            Tarea.ejecutar(this, lblEstado, "Abriendo " + r.getHtml(), () -> {
                VisorHtml.abrir(r.getHtml());
                return r.getHtml();
            }, html -> lblEstado.setText("Abierto en el navegador: " + html));
            return;
        }
        Tarea.ejecutar(this, lblEstado, "Pidiendo la imagen a mermaid.ink", () -> pedirImagen(r.getContenido()), imagen -> {
            if (imagen == null) {
                ofrecerHtml(r, "mermaid.ink no devolvió una imagen.");
            } else {
                mostrarImagen(imagen, r.getFuncion());
                lblEstado.setText("Imagen de mermaid.ink mostrada en la ventana.");
            }
        });
    }

    private BufferedImage pedirImagen(String mermaid) {
        try {
            String codigo = Base64.getUrlEncoder().encodeToString(mermaid.getBytes(StandardCharsets.UTF_8));
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create("https://mermaid.ink/img/" + codigo + "?type=png"))
                    .timeout(ConfiguracionIA.ESPERA).GET().build();
            HttpResponse<byte[]> respuesta = HttpClient.newBuilder().connectTimeout(ConfiguracionIA.ESPERA).build()
                    .send(solicitud, HttpResponse.BodyHandlers.ofByteArray());
            if (respuesta.statusCode() != 200) {
                return null;
            }
            return ImageIO.read(new ByteArrayInputStream(respuesta.body()));
        } catch (Exception ex) {
            return null;
        }
    }

    private void ofrecerHtml(ResultadoIA r, String motivo) {
        int opcion = JOptionPane.showConfirmDialog(this, motivo + " (requiere internet).\n¿Abrir el HTML local con zoom?",
                "Imagen no disponible", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            try {
                VisorHtml.abrir(r.getHtml());
            } catch (IOException ex) {
                lblEstado.setText(ex.getMessage());
            }
        }
    }

    private void mostrarImagen(BufferedImage imagen, String titulo) {
        JDialog dialogo = new JDialog(SwingUtilities.getWindowAncestor(this), titulo);
        JLabel etiqueta = new JLabel(new ImageIcon(imagen));
        double[] escala = {1.0};
        Runnable aplicar = () -> {
            int ancho = Math.max(1, (int) (imagen.getWidth() * escala[0]));
            int alto = Math.max(1, (int) (imagen.getHeight() * escala[0]));
            etiqueta.setIcon(new ImageIcon(imagen.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH)));
        };
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(Tarea.boton("Acercar", () -> {
            escala[0] *= 1.25;
            aplicar.run();
        }));
        botones.add(Tarea.boton("Alejar", () -> {
            escala[0] /= 1.25;
            aplicar.run();
        }));
        botones.add(Tarea.boton("100 %", () -> {
            escala[0] = 1.0;
            aplicar.run();
        }));
        dialogo.add(botones, BorderLayout.NORTH);
        dialogo.add(new JScrollPane(etiqueta), BorderLayout.CENTER);
        dialogo.setSize(1000, 700);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }
}
