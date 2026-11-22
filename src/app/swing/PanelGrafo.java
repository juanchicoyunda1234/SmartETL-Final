package app.swing;

import app.Sesion;

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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.nio.file.Path;

public class PanelGrafo extends JPanel {

    private final Sesion sesion;
    private final DefaultListModel<String> modeloPaises = new DefaultListModel<>();
    private final JList<String> lstPaises = new JList<>(modeloPaises);
    private final JTextField txtAeropuerto = new JTextField("UIO", 5);
    private final JTextField txtOrigen = new JTextField("UIO", 5);
    private final JTextField txtNivelMaximo = new JTextField("", 3);
    private final JTextField txtNivelDiagrama = new JTextField("1", 3);
    private final JCheckBox chkFronteras = new JCheckBox("Cruzar fronteras (rutas externas)");
    private final JTextField txtRutaOrigen = new JTextField("UIO", 5);
    private final JTextField txtRutaDestino = new JTextField("CUZ", 5);
    private final JTextField txtPaisCiclo = new JTextField("Ecuador", 10);
    private final JTextArea txtSalida = Tarea.areaTexto();
    private final JLabel lblEstado = new JLabel("Un grafo por país: aristas internas y rutas externas hacia otros países.");
    private boolean paisesCargados;

    public PanelGrafo(Sesion sesion) {
        this.sesion = sesion;
        setLayout(new BorderLayout(4, 4));
        add(construirIzquierda(), BorderLayout.WEST);
        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.add(construirControles(), BorderLayout.NORTH);
        centro.add(Tarea.conBorde(txtSalida, "Resultado"), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        add(lblEstado, BorderLayout.SOUTH);
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent e) {
                cargarPaises();
            }

            @Override
            public void ancestorRemoved(AncestorEvent e) {
            }

            @Override
            public void ancestorMoved(AncestorEvent e) {
            }
        });
    }

    private JPanel construirIzquierda() {
        JPanel izquierda = new JPanel(new BorderLayout(4, 4));
        JScrollPane lista = new JScrollPane(lstPaises);
        lista.setBorder(BorderFactory.createTitledBorder("Países (Ctrl+clic para varios)"));
        izquierda.add(lista, BorderLayout.CENTER);
        JPanel botones = new JPanel(new GridLayout(4, 1, 2, 2));
        botones.add(Tarea.boton("Construir seleccionados", this::construirSeleccionados));
        botones.add(Tarea.boton("Construir todos (con tiempo)", this::construirTodos));
        botones.add(Tarea.boton("Diagrama general con clic por país", this::diagramaGeneral));
        botones.add(Tarea.boton("Abrir visor interactivo", this::visor));
        izquierda.add(botones, BorderLayout.SOUTH);
        izquierda.setPreferredSize(new Dimension(260, 10));
        return izquierda;
    }

    private JPanel construirControles() {
        JPanel controles = new JPanel();
        controles.setLayout(new BoxLayout(controles, BoxLayout.Y_AXIS));
        JPanel conexiones = Tarea.fila("Conexiones directas");
        conexiones.add(new JLabel("Aeropuerto IATA:"));
        conexiones.add(txtAeropuerto);
        conexiones.add(Tarea.boton("Ver conexiones", this::conexiones));
        controles.add(conexiones);

        JPanel recorridos = Tarea.fila("Recorridos (BFS con la Cola propia, DFS con la Pila propia)");
        recorridos.add(new JLabel("Origen:"));
        recorridos.add(txtOrigen);
        recorridos.add(new JLabel("Nivel máximo (vacío = sin límite):"));
        recorridos.add(txtNivelMaximo);
        recorridos.add(new JLabel("Nivel del diagrama:"));
        recorridos.add(txtNivelDiagrama);
        recorridos.add(chkFronteras);
        recorridos.add(Tarea.boton("BFS", () -> recorrer(true)));
        recorridos.add(Tarea.boton("DFS", () -> recorrer(false)));
        controles.add(recorridos);

        JPanel ruta = Tarea.fila("Ruta más corta (en vuelos) y ciclos");
        ruta.add(new JLabel("De:"));
        ruta.add(txtRutaOrigen);
        ruta.add(new JLabel("a:"));
        ruta.add(txtRutaDestino);
        ruta.add(Tarea.boton("Buscar ruta", this::buscarRuta));
        ruta.add(new JLabel("   País:"));
        ruta.add(txtPaisCiclo);
        ruta.add(Tarea.boton("Detectar ciclo", this::detectarCiclo));
        controles.add(ruta);
        return controles;
    }

    private void cargarPaises() {
        if (paisesCargados) {
            return;
        }
        paisesCargados = true;
        Tarea.ejecutar(this, lblEstado, "Preparando el BST de países (ejecuta el ETL si hace falta)", () -> {
            MapaPaises mapa = sesion.mapa();
            return Ordenamiento.insercion(mapa.getPaises(), GrafoPais.POR_NOMBRE).getLista();
        }, paises -> {
            for (int i = 0; i < paises.tamanio(); i++) {
                modeloPaises.addElement(paises.obtener(i).getNombre());
            }
            lblEstado.setText(paises.tamanio() + " países en el BST de países. Elige uno o varios y pulsa Construir.");
        });
    }

    private void construirSeleccionados() {
        java.util.List<String> elegidos = lstPaises.getSelectedValuesList();
        if (elegidos.isEmpty()) {
            lblEstado.setText("Selecciona al menos un país de la lista.");
            return;
        }
        String[] nombres = elegidos.toArray(new String[0]);
        Tarea.ejecutar(this, lblEstado, "Construyendo " + nombres.length + " grafos", () -> {
            MapaPaises mapa = sesion.mapa();
            StringBuilder texto = new StringBuilder();
            for (String nombre : nombres) {
                GrafoPais pais = mapa.obtener(nombre);
                texto.append(String.format("%s (%.1f ms)%n", pais.resumen(), pais.getMilisegundos()));
            }
            sesion.setUltimoPais(nombres[nombres.length - 1]);
            return texto.toString();
        }, texto -> {
            txtSalida.setText(texto);
            lblEstado.setText("Grafos construidos. Un país ya construido no se vuelve a construir.");
        });
    }

    private void construirTodos() {
        Tarea.ejecutar(this, lblEstado, "Construyendo los grafos de todos los países", () -> {
            MapaPaises mapa = sesion.mapa();
            double ms = mapa.construirTodos();
            return String.format("Grafos construidos: %d países con al menos un vértice, de %d en el BST de países.%n"
                            + "Tiempo de esta construcción: %.1f ms (criterio: menos de 10 s).%n",
                    mapa.paisesConVertices(), mapa.getPaises().tamanio(), ms);
        }, texto -> {
            txtSalida.setText(texto);
            lblEstado.setText("Todos los países construidos.");
        });
    }

    private void diagramaGeneral() {
        int nivelDiagrama = Tarea.entero(txtNivelDiagrama, "Nivel del diagrama", 1);
        Tarea.ejecutar(this, lblEstado, "Generando el diagrama general y las páginas por país", () -> {
            MapaPaises mapa = sesion.mapa();
            ListaSecuencial<GrafoPais> construidos = new ListaSecuencial<>();
            ListaSecuencial<GrafoPais> todos = mapa.construidos();
            for (int i = 0; i < todos.tamanio(); i++) {
                if (todos.obtener(i).getVertices() > 0) {
                    construidos.insertarFinal(todos.obtener(i));
                }
            }
            if (construidos.estaVacia()) {
                throw new IllegalStateException("Primero construye uno o más países");
            }
            for (int i = 0; i < construidos.tamanio(); i++) {
                GrafoPais pais = construidos.obtener(i);
                Vertice<Aeropuerto> origen = origenPara(pais);
                VisorHtml.escribirPagina(VisorHtml.archivoPais(pais.getNombre()), "Grafo de " + pais.getNombre(),
                        GeneradorFlujogramaLocal.grafoPais(pais, origen, nivelDiagrama, construidos),
                        pais.resumen() + (pais.getVertices() > GeneradorFlujogramaLocal.LIMITE_VERTICES_COMPLETO
                                ? ". Se dibuja hasta el nivel " + nivelDiagrama + " desde " + origen : ""));
            }
            Path general = VisorHtml.escribirDiagrama("general_paises", "Construcción de grafos por país",
                    GeneradorFlujogramaLocal.generalPaises(construidos, 250), "Clic en un país para abrir su grafo");
            VisorHtml.abrir(general);
            return construidos.tamanio();
        }, cantidad -> lblEstado.setText("Diagrama general con " + cantidad + " países abierto en el navegador."));
    }

    private Vertice<Aeropuerto> origenPara(GrafoPais pais) {
        Vertice<Aeropuerto> ultimo = sesion.getUltimoOrigen();
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

    private Vertice<Aeropuerto> vertice(MapaPaises mapa, String iata) {
        Vertice<Aeropuerto> v = mapa.verticePorIata(iata);
        if (v == null) {
            throw new IllegalArgumentException("No hay un aeropuerto con rutas y código IATA " + iata);
        }
        return v;
    }

    private void conexiones() {
        String iata = txtAeropuerto.getText().trim().toUpperCase();
        Tarea.ejecutar(this, lblEstado, "Buscando conexiones de " + iata, () -> {
            Vertice<Aeropuerto> v = vertice(sesion.mapa(), iata);
            Aeropuerto a = v.getDato();
            StringBuilder texto = new StringBuilder(a.getIata() + " - " + a.getNombre() + " (" + a.getCiudad() + ", " + a.getPais() + ")\n\n");
            texto.append("Rutas internas (").append(v.getAdyacentes().tamanio()).append("): ");
            for (Nodo<Vertice<Aeropuerto>> n = v.getAdyacentes().primerNodo(); n != null; n = n.getSiguiente()) {
                texto.append(n.getDato().getEtiqueta()).append(n.getSiguiente() == null ? "" : ", ");
            }
            Agrupacion externos = GrafoPais.externosPorPais(v);
            texto.append("\n\nRutas externas (").append(v.getExternos().tamanio()).append("), agrupadas por país destino:\n");
            for (int i = 0; i < externos.tamanio(); i++) {
                texto.append(String.format("  %-28s %3d: %s%n", externos.clave(i), externos.conteo(i), externos.detalle(i)));
            }
            texto.append("\nLlegan desde el exterior: ").append(v.getLlegadasExternas().tamanio()).append(" rutas\n");
            return texto.toString();
        }, txtSalida::setText);
    }

    private void recorrer(boolean esBfs) {
        String iata = txtOrigen.getText().trim().toUpperCase();
        int nivelMaximo = Tarea.entero(txtNivelMaximo, "Nivel máximo", BFS.SIN_LIMITE);
        int nivelDiagrama = Tarea.entero(txtNivelDiagrama, "Nivel del diagrama", 1);
        boolean fronteras = chkFronteras.isSelected();
        String nombre = esBfs ? "BFS" : "DFS";
        Tarea.ejecutar(this, lblEstado, nombre + " desde " + iata, () -> {
            MapaPaises mapa = sesion.mapa();
            Vertice<Aeropuerto> origen = vertice(mapa, iata);
            RegistroEventos registro = new RegistroEventos(nombre + " desde " + iata);
            ResultadoRecorrido r = esBfs ? BFS.recorrer(origen, fronteras ? mapa : null, nivelMaximo, registro)
                    : DFS.recorrer(origen, fronteras ? mapa : null, nivelMaximo, registro);
            if (esBfs) {
                sesion.registrarBfs(registro, nivelDiagrama, origen);
            } else {
                sesion.registrarDfs(registro, nivelDiagrama, origen);
            }
            return describir(r, esBfs, fronteras, registro);
        }, texto -> {
            txtSalida.setText(texto);
            txtSalida.setCaretPosition(0);
            lblEstado.setText(nombre + " listo. Su registro de eventos queda disponible en la pestaña IA.");
        });
    }

    private String describir(ResultadoRecorrido r, boolean esBfs, boolean fronteras, RegistroEventos registro) {
        StringBuilder texto = new StringBuilder();
        texto.append(String.format("%s desde %s (%s): %d aeropuertos visitados, %s %d, %.1f ms, %d eventos registrados%n%n",
                r.getAlgoritmo(), r.getOrigen(), fronteras ? "cruzando fronteras" : "solo rutas internas", r.cantidad(),
                esBfs ? "niveles hasta" : "profundidad máxima", r.nivelMaximo(), r.getMilisegundos(), registro.tamanio()));
        if (esBfs) {
            for (int nivel = 0; nivel <= r.nivelMaximo(); nivel++) {
                Agrupacion paises = r.paisesEnNivel(nivel);
                texto.append(String.format("Nivel %d: %d aeropuertos en %d países%n", nivel, r.cantidadEnNivel(nivel), paises.tamanio()));
                for (int i = 0; i < Math.min(8, paises.tamanio()); i++) {
                    String detalle = paises.detalle(i);
                    texto.append("   ").append(paises.clave(i)).append(" (").append(paises.conteo(i)).append("): ")
                            .append(detalle.length() > 110 ? detalle.substring(0, 110) + "…" : detalle).append('\n');
                }
                if (paises.tamanio() > 8) {
                    texto.append("   y ").append(paises.tamanio() - 8).append(" países más\n");
                }
            }
        } else {
            texto.append("Orden de visita (primeros 200), con su profundidad:\n");
            for (int i = 0; i < Math.min(200, r.cantidad()); i++) {
                Vertice<Aeropuerto> v = r.getVisitados().obtener(i);
                texto.append(v.getEtiqueta()).append('(').append(v.getNivel()).append(')').append(i % 15 == 14 ? "\n" : " ");
            }
            texto.append('\n');
        }
        return texto.toString();
    }

    private void buscarRuta() {
        String desde = txtRutaOrigen.getText().trim().toUpperCase();
        String hasta = txtRutaDestino.getText().trim().toUpperCase();
        Tarea.ejecutar(this, lblEstado, "Buscando la ruta " + desde + " -> " + hasta, () -> {
            MapaPaises mapa = sesion.mapa();
            ListaSecuencial<Aeropuerto> ruta = Rutas.buscar(vertice(mapa, desde), vertice(mapa, hasta), mapa);
            if (ruta.estaVacia()) {
                return "No existe ruta de " + desde + " a " + hasta + ".";
            }
            StringBuilder texto = new StringBuilder("Ruta más corta de " + desde + " a " + hasta + ": "
                    + (ruta.tamanio() - 1) + " vuelos (BFS con registro de padre)\n\n");
            for (int i = 0; i < ruta.tamanio(); i++) {
                Aeropuerto a = ruta.obtener(i);
                texto.append(i == 0 ? "   " : "-> ").append(a.getIata()).append("  ").append(a.getNombre())
                        .append(" (").append(a.getCiudad()).append(", ").append(a.getPais()).append(")\n");
            }
            texto.append("\nPuede haber otras rutas con la misma cantidad de vuelos; BFS devuelve la primera que encuentra.\n");
            return texto.toString();
        }, txtSalida::setText);
    }

    private void detectarCiclo() {
        String pais = txtPaisCiclo.getText().trim();
        Tarea.ejecutar(this, lblEstado, "Buscando un ciclo en " + pais, () -> {
            MapaPaises mapa = sesion.mapa();
            GrafoPais grafo = mapa.obtener(pais);
            if (grafo == null) {
                throw new IllegalArgumentException("No existe el país " + pais);
            }
            sesion.setUltimoPais(grafo.getNombre());
            ListaSecuencial<Aeropuerto> ciclo = Ciclos.detectar(grafo.getGrafo());
            if (ciclo.estaVacia()) {
                return "No hay ciclos de longitud 3 o más en las rutas internas de " + grafo.getNombre() + ".";
            }
            StringBuilder texto = new StringBuilder("Ciclo de longitud " + (ciclo.tamanio() - 1) + " en " + grafo.getNombre()
                    + " (DFS con estados blanco, gris y negro; se ignoran las idas y vueltas A-B-A):\n\n");
            for (int i = 0; i < ciclo.tamanio(); i++) {
                texto.append(i == 0 ? "" : " -> ").append(ciclo.obtener(i).getIata());
            }
            return texto.append('\n').toString();
        }, txtSalida::setText);
    }

    private void visor() {
        String origen = txtOrigen.getText().trim().toUpperCase();
        Tarea.ejecutar(this, lblEstado, "Generando el visor interactivo (construye todos los países)", () -> {
            MapaPaises mapa = sesion.mapa();
            mapa.construirTodos();
            Path html = VisorGrafo.escribir(mapa, origen.isEmpty() ? "UIO" : origen);
            VisorHtml.abrir(html);
            return html;
        }, html -> lblEstado.setText("Visor abierto: " + html));
    }
}
