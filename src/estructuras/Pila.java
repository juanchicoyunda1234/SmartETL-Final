package estructuras;

import ia.Evento;
import ia.RegistroEventos;

public class Pila<T> {

    private Nodo<T> cima;
    private int tamanio;
    private RegistroEventos registro;

    public void setRegistro(RegistroEventos registro) {
        this.registro = registro;
    }

    public void apilar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(cima);
        cima = nuevo;
        tamanio++;
        registrar("apilar", "Apilar " + dato, dato);
    }

    public T desapilar() {
        if (estaVacia()) {
            throw new IllegalStateException("La pila está vacía: no hay elementos para desapilar");
        }
        T dato = cima.getDato();
        cima = cima.getSiguiente();
        tamanio--;
        registrar("desapilar", "Desapilar " + dato, dato);
        return dato;
    }

    public T cima() {
        if (estaVacia()) {
            throw new IllegalStateException("La pila está vacía: no hay cima");
        }
        return cima.getDato();
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    public int tamanio() {
        return tamanio;
    }

    public String[] primeros(int cantidad) {
        String[] vista = new String[Math.min(cantidad, tamanio)];
        Nodo<T> actual = cima;
        for (int i = 0; i < vista.length; i++) {
            vista[i] = String.valueOf(actual.getDato());
            actual = actual.getSiguiente();
        }
        return vista;
    }

    public ListaSecuencial<T> elementos() {
        ListaSecuencial<T> copia = new ListaSecuencial<>();
        for (Nodo<T> actual = cima; actual != null; actual = actual.getSiguiente()) {
            copia.insertarFinal(actual.getDato());
        }
        return copia;
    }

    public String mostrar() {
        return Evento.formatearEstado(primeros(Evento.LIMITE_VISTA), tamanio);
    }

    private void registrar(String tipo, String texto, T dato) {
        if (registro != null) {
            registro.agregar(new Evento(tipo, texto, String.valueOf(dato), "pila", new String[0],
                    primeros(Evento.LIMITE_VISTA), tamanio, -1));
        }
    }
}
