package app;

import app.swing.VentanaPrincipal;
import app.web.ServidorWeb;
import etl.Proyecto;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws Exception {
        if (pideSwing(args)) {
            SwingUtilities.invokeLater(Main::iniciarSwing);
            return;
        }
        SwingUtilities.invokeAndWait(Main::prepararCarpeta);
        ServidorWeb servidor = ServidorWeb.iniciar(new Sesion());
        String direccion = servidor.getDireccion();
        System.out.println("SmartETL-DS web en " + direccion);
        System.out.println("Carpeta del proyecto: " + Proyecto.getBase());
        System.out.println("Para detener el servidor cierra esta consola o pulsa Ctrl+C.");
        abrirNavegador(direccion);
    }

    private static boolean pideSwing(String[] args) {
        for (String argumento : args) {
            if (argumento.equalsIgnoreCase("--swing")) {
                return true;
            }
        }
        return false;
    }

    private static void iniciarSwing() {
        prepararCarpeta();
        VentanaPrincipal ventana = new VentanaPrincipal();
        ventana.setTitle("SmartETL-DS  |  " + Proyecto.getBase());
        ventana.setVisible(true);
    }

    private static void prepararCarpeta() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
        }
        if (!Proyecto.detectar()) {
            pedirCarpeta();
        }
        avisarSiFaltanRecursos();
    }

    private static void abrirNavegador(String direccion) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(direccion));
                return;
            }
        } catch (Exception ignorada) {
        }
        System.out.println("No se pudo abrir el navegador. Abre a mano: " + direccion);
    }

    private static void pedirCarpeta() {
        JOptionPane.showMessageDialog(null, "No encontré la carpeta data con los archivos CSV.\n"
                        + "El programa se está ejecutando en: " + Paths.get("").toAbsolutePath() + "\n\n"
                        + "En la siguiente ventana selecciona la carpeta data\n"
                        + "(o la carpeta del proyecto SmartETL_DS-main, que la contiene).",
                "Ubicar los datos", JOptionPane.INFORMATION_MESSAGE);
        while (true) {
            JFileChooser selector = new JFileChooser(Paths.get("").toAbsolutePath().toFile());
            selector.setDialogTitle("Selecciona la carpeta data o la carpeta del proyecto");
            selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (selector.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(null, "No se eligió carpeta. El programa se abrirá, pero el ETL no "
                        + "encontrará los datos hasta que reinicies y elijas la carpeta.", "Sin datos",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (Proyecto.usarCarpeta(selector.getSelectedFile().toPath())) {
                return;
            }
            JOptionPane.showMessageDialog(null, "En " + selector.getSelectedFile() + " no está airports.csv.\n"
                    + "Elige la carpeta data (la que contiene airports.csv, routes.csv y airlines.csv).",
                    "Carpeta incorrecta", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void avisarSiFaltanRecursos() {
        if (Proyecto.esRaiz(Proyecto.getBase()) && !Proyecto.tieneRecursos()) {
            JOptionPane.showMessageDialog(null, "Los datos están en " + Proyecto.getBase() + ", pero ahí no está la "
                    + "carpeta recursos con mermaid.min.js.\nLos diagramas no se dibujarán sin internet.",
                    "Falta la carpeta recursos", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static String explicacionCarpeta() {
        return "Carpeta del proyecto en uso: " + Proyecto.getBase() + "\n\n"
                + "Debe ser la carpeta SmartETL_DS-main (la que contiene data, src y recursos).\n"
                + "Cierra el programa y vuelve a abrirlo: si no encuentra la carpeta solo,\n"
                + "te pedirá que selecciones la carpeta data.";
    }
}
