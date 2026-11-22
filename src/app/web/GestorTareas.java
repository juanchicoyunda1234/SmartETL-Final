package app.web;

import app.Main;
import estructuras.ListaEnlazada;
import estructuras.Nodo;

import java.nio.file.NoSuchFileException;

public class GestorTareas {

    private final ListaEnlazada<TareaWeb> tareas = new ListaEnlazada<>();
    private TareaWeb actual;
    private int contador;

    public synchronized TareaWeb iniciar(String nombre, TrabajoWeb trabajo) throws ErrorWeb {
        exigirLibre();
        contador++;
        TareaWeb tarea = new TareaWeb("t" + contador, nombre);
        actual = tarea;
        tareas.insertarFinal(tarea);
        Thread hilo = new Thread(() -> correr(tarea, trabajo), "smartetl-" + tarea.getId());
        hilo.setDaemon(true);
        hilo.start();
        return tarea;
    }

    public synchronized void exigirLibre() throws ErrorWeb {
        if (actual != null && !actual.estaTerminada()) {
            throw new ErrorWeb(409, "Ya hay una tarea en curso (" + actual.getNombre() + "). Espera a que termine.");
        }
    }

    public synchronized TareaWeb enCurso() {
        return actual != null && !actual.estaTerminada() ? actual : null;
    }

    public synchronized TareaWeb buscar(String id) {
        for (Nodo<TareaWeb> n = tareas.primerNodo(); n != null; n = n.getSiguiente()) {
            if (n.getDato().getId().equals(id)) {
                return n.getDato();
            }
        }
        return null;
    }

    private void correr(TareaWeb tarea, TrabajoWeb trabajo) {
        try {
            Object resultado = trabajo.hacer(tarea::avisar);
            tarea.terminar(resultado == null ? "null" : resultado.toString());
        } catch (ErrorWeb ex) {
            tarea.fallar(ex.getCodigo(), ex.getMessage());
        } catch (NoSuchFileException ex) {
            tarea.fallar(404, "No se encontró el archivo " + ex.getMessage() + ". " + Main.explicacionCarpeta());
        } catch (Exception ex) {
            tarea.fallar(500, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }
}
