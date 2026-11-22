package ia;

import estructuras.ListaEnlazada;
import estructuras.Nodo;

public class RegistroEventos {

    private static final int LIMITE = 20000;

    private final String titulo;
    private final ListaEnlazada<Evento> eventos = new ListaEnlazada<>();
    private int descartados;

    public RegistroEventos(String titulo) {
        this.titulo = titulo;
    }

    public void agregar(Evento evento) {
        if (eventos.tamanio() < LIMITE) {
            eventos.insertarFinal(evento);
        } else {
            descartados++;
        }
    }

    public void registrar(String tipo, String texto) {
        agregar(new Evento(tipo, texto));
    }

    public String getTitulo() {
        return titulo;
    }

    public ListaEnlazada<Evento> getEventos() {
        return eventos;
    }

    public int tamanio() {
        return eventos.tamanio();
    }

    public int getDescartados() {
        return descartados;
    }

    public int contar(String tipo) {
        return eventos.contar(e -> e.getTipo().equals(tipo));
    }

    public Evento ultimo(String tipo) {
        Evento encontrado = null;
        for (Nodo<Evento> n = eventos.primerNodo(); n != null; n = n.getSiguiente()) {
            if (n.getDato().getTipo().equals(tipo)) {
                encontrado = n.getDato();
            }
        }
        return encontrado;
    }

    public String comoTexto(int limite) {
        StringBuilder texto = new StringBuilder();
        int escritos = 0;
        for (Nodo<Evento> n = eventos.primerNodo(); n != null && escritos < limite; n = n.getSiguiente()) {
            texto.append(escritos + 1).append(". ").append(n.getDato()).append('\n');
            escritos++;
        }
        if (eventos.tamanio() > escritos) {
            texto.append("(").append(eventos.tamanio() - escritos).append(" eventos más no incluidos)\n");
        }
        return texto.toString();
    }
}
