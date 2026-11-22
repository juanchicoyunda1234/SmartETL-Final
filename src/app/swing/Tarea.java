package app.swing;

import app.Main;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public final class Tarea {

    public interface Trabajo<T> {
        T hacer() throws Exception;
    }

    public interface TrabajoConAvisos<T> {
        T hacer(Consumer<String> avisar) throws Exception;
    }

    public interface AlTerminar<T> {
        void listo(T resultado) throws Exception;
    }

    private Tarea() {
    }

    public static <T> void ejecutar(Component panel, JLabel estado, String mensaje, Trabajo<T> trabajo,
                                    AlTerminar<T> alTerminar) {
        ejecutarConAvisos(panel, estado, mensaje, avisar -> trabajo.hacer(), alTerminar);
    }

    public static <T> void ejecutarConAvisos(Component panel, JLabel estado, String mensaje, TrabajoConAvisos<T> trabajo,
                                             AlTerminar<T> alTerminar) {
        Animacion animacion = new Animacion(estado, mensaje);
        animacion.iniciar();
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, String>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabajo.hacer(this::publish);
            }

            @Override
            protected void process(List<String> avisos) {
                animacion.setMensaje(avisos.get(avisos.size() - 1));
            }

            @Override
            protected void done() {
                animacion.detener();
                estado.setText("✔ Terminado en " + animacion.segundos() + " s");
                panel.setCursor(Cursor.getDefaultCursor());
                try {
                    alTerminar.listo(get());
                } catch (ExecutionException ex) {
                    mostrarError(panel, estado, ex.getCause());
                } catch (Exception ex) {
                    mostrarError(panel, estado, ex);
                }
            }
        }.execute();
    }

    private static void mostrarError(Component panel, JLabel estado, Throwable error) {
        String mensaje = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        if (error instanceof NoSuchFileException) {
            mensaje = "No se encontró el archivo " + error.getMessage() + ".\n\n" + Main.explicacionCarpeta();
        }
        estado.setText("Error: " + mensaje.split("\n")[0]);
        JOptionPane.showMessageDialog(panel, mensaje, "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    public static JPanel fila(String titulo) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        fila.setBorder(BorderFactory.createTitledBorder(titulo));
        return fila;
    }

    public static JTextArea areaTexto() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    public static JScrollPane conBorde(Component componente, String titulo) {
        JScrollPane panel = new JScrollPane(componente);
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        return panel;
    }

    public static JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    public static int entero(JTextField campo, String nombre, int siVacio) {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            return siVacio;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(campo, "El campo " + nombre + " debe ser un número entero. Se usa "
                    + (siVacio < 0 ? "sin límite" : String.valueOf(siVacio)) + ".", "Valor no válido", JOptionPane.WARNING_MESSAGE);
            return siVacio;
        }
    }
}
