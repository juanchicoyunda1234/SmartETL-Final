package estructuras;

import ia.Evento;
import ia.RegistroEventos;

public class Cola<T> {

    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamanio;
    private RegistroEventos registro;

    public void setRegistro(RegistroEventos registro) {
        this.registro = registro;
    }

    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (fin == null) {
            frente = nuevo;
        } else {
            fin.setSiguiente(nuevo);
        }
        fin = nuevo;
        tamanio++;
        registrar("encolar", "Encolar " + dato, dato);
    }

    public T desencolar() {
        if (estaVacia()) {
            throw new IllegalStateException("La cola está vacía: no hay elementos para desencolar");
        }
        T dato = frente.getDato();
        frente = frente.getSiguiente();
        if (frente == null) {
            fin = null;
        }
        tamanio--;
        registrar("desencolar", "Desencolar " + dato, dato);
        return dato;
    }

    public T frente() {
        if (estaVacia()) {
            throw new IllegalStateException("La cola está vacía: no hay frente");
        }
        return frente.getDato();
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    public int tamanio() {
        return tamanio;
    }

    public String[] primeros(int cantidad) {
        String[] vista = new String[Math.min(cantidad, tamanio)];
        Nodo<T> actual = frente;
        for (int i = 0; i < vista.length; i++) {
            vista[i] = String.valueOf(actual.getDato());
            actual = actual.getSiguiente();
        }
        return vista;
    }

    public String mostrar() {
        return Evento.formatearEstado(primeros(Evento.LIMITE_VISTA), tamanio);
    }

    private void registrar(String tipo, String texto, T dato) {
        if (registro != null) {
            registro.agregar(new Evento(tipo, texto, String.valueOf(dato), "cola", new String[0],
                    primeros(Evento.LIMITE_VISTA), tamanio, -1));
        }
    }
}
