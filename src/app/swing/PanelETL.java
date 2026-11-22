package app.swing;

import app.Sesion;

import estructuras.ListaSecuencial;
import etl.DatosLimpios;
import etl.ProcesoETL;
import etl.Transform;
import model.ErrorETL;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Paths;

public class PanelETL extends JPanel {

    private static final int LIMITE_VALIDOS = 10;
    private static final int LIMITE_ERRORES = 60;

    private final Sesion sesion;
    private final JTextArea txtCola = Tarea.areaTexto();
    private final JTextArea txtValidos = Tarea.areaTexto();
    private final JTextArea txtErrores = Tarea.areaTexto();
    private final JTextArea txtContadores = Tarea.areaTexto();
    private final JLabel lblEstado = new JLabel("Pulsa Ejecutar ETL. Extract encola, Transform valida y separa en lista y pila, Load escribe output/.");
    private ProcesoETL actual;

    public PanelETL(Sesion sesion) {
        this.sesion = sesion;
        setLayout(new BorderLayout(4, 4));
        JPanel acciones = Tarea.fila("ETL: Extract -> Cola -> Transform -> Lista de válidos y Pila de errores -> Load");
        acciones.add(Tarea.boton("Ejecutar ETL (datos reales)", () -> ejecutar(ProcesoETL.real(), true)));
        acciones.add(Tarea.boton("Ejecutar demostración (archivos sucios de data/demo)", () -> ejecutar(ProcesoETL.demostracion(), false)));
        acciones.add(Tarea.boton("Cargar (Load a output/)", this::cargar));
        add(acciones, BorderLayout.NORTH);

        JPanel tres = new JPanel(new GridLayout(1, 3, 4, 4));
        tres.add(Tarea.conBorde(txtCola, "Cola de extracción (primeros " + ProcesoETL.VISTA_COLA + " por fuente, antes del Transform)"));
        tres.add(Tarea.conBorde(txtValidos, "Listas de válidos (primeros " + LIMITE_VALIDOS + ")"));
        tres.add(Tarea.conBorde(txtErrores, "Pila de errores (desde la cima, primeros " + LIMITE_ERRORES + ")"));
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tres, Tarea.conBorde(txtContadores, "Contadores"));
        division.setResizeWeight(0.62);
        add(division, BorderLayout.CENTER);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        add(lblEstado, BorderLayout.SOUTH);
    }

    private void ejecutar(ProcesoETL etl, boolean esReal) {
        Tarea.ejecutarConAvisos(this, lblEstado, "Preparando el ETL", avisar -> {
            etl.setAvisos(avisar);
            long inicio = System.nanoTime();
            etl.ejecutar();
            return (System.nanoTime() - inicio) / 1_000_000.0;
        }, ms -> {
            actual = etl;
            sesion.registrarEtl(etl, esReal);
            mostrar(etl);
            Transform t = etl.getTransform();
            lblEstado.setText(String.format("ETL %s en %.0f ms: %d válidos en listas, %d errores en la pila. Pulsa Cargar para escribir output/.",
                    esReal ? "real" : "de demostración", ms, etl.totalValidos(), t.getErrores().tamanio()));
        });
    }

    private void mostrar(ProcesoETL etl) {
        StringBuilder cola = new StringBuilder();
        for (int f = 0; f < Transform.FUENTES.length; f++) {
            cola.append("== ").append(Transform.FUENTES[f]).append(": ").append(etl.getEncolados(f))
                    .append(" encolados desde ").append(Paths.get(etl.getArchivo(f)).getFileName()).append('\n');
            String[] vista = etl.getVistaCola(f);
            for (int i = 0; i < vista.length; i++) {
                cola.append(i == 0 ? "frente -> " : "          ").append(vista[i]).append('\n');
            }
            cola.append('\n');
        }
        txtCola.setText(cola.toString());
        txtCola.setCaretPosition(0);

        DatosLimpios datos = etl.getDatos();
        StringBuilder validos = new StringBuilder();
        agregar(validos, "aeropuertos", datos.getAeropuertos());
        agregar(validos, "rutas", datos.getRutas());
        agregar(validos, "aerolíneas", datos.getAerolineas());
        txtValidos.setText(validos.toString());
        txtValidos.setCaretPosition(0);

        ListaSecuencial<ErrorETL> errores = etl.getTransform().getErrores().elementos();
        StringBuilder pila = new StringBuilder("Errores en la pila: " + errores.tamanio() + "\n\n");
        for (int i = 0; i < Math.min(LIMITE_ERRORES, errores.tamanio()); i++) {
            pila.append(i == 0 ? "cima -> " : "        ").append(errores.obtener(i).comoLinea()).append('\n');
        }
        txtErrores.setText(pila.toString());
        txtErrores.setCaretPosition(0);
        txtContadores.setText(etl.reporte());
        txtContadores.setCaretPosition(0);
    }

    private <T> void agregar(StringBuilder texto, String nombre, ListaSecuencial<T> lista) {
        texto.append("== ").append(nombre).append(": ").append(lista.tamanio()).append(" válidos\n");
        for (int i = 0; i < Math.min(LIMITE_VALIDOS, lista.tamanio()); i++) {
            texto.append("  ").append(lista.obtener(i)).append('\n');
        }
        texto.append('\n');
    }

    private void cargar() {
        if (actual == null) {
            lblEstado.setText("Primero ejecuta el ETL.");
            return;
        }
        ProcesoETL etl = actual;
        Tarea.ejecutarConAvisos(this, lblEstado, "Preparando el Load", avisar -> {
            etl.setAvisos(avisar);
            return etl.cargar("output");
        }, archivos -> {
            StringBuilder nombres = new StringBuilder();
            for (String archivo : archivos) {
                nombres.append(nombres.length() == 0 ? "" : ", ").append(Paths.get(archivo).getFileName());
            }
            lblEstado.setText("Load terminado. Archivos en output/: " + nombres);
            txtContadores.append("\nArchivos escritos en output/: " + nombres + "\n");
        });
    }
}
