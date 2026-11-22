package app.swing;

import javax.swing.JLabel;
import javax.swing.Timer;

public class Animacion {

    private static final String[] PUNTOS = {".  ", ".. ", "..."};

    private final JLabel etiqueta;
    private final Timer reloj;
    private String mensaje;
    private long inicio;
    private int paso;

    public Animacion(JLabel etiqueta, String mensaje) {
        this.etiqueta = etiqueta;
        this.mensaje = mensaje;
        this.reloj = new Timer(400, e -> pintar());
    }

    public void iniciar() {
        inicio = System.nanoTime();
        pintar();
        reloj.start();
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
        pintar();
    }

    public void detener() {
        reloj.stop();
    }

    public long segundos() {
        return (System.nanoTime() - inicio) / 1_000_000_000L;
    }

    private void pintar() {
        paso++;
        etiqueta.setText("⏳ " + mensaje + PUNTOS[paso % PUNTOS.length] + "   " + segundos() + " s");
    }
}
