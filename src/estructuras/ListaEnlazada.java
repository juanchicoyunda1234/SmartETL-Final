package estructuras;

import ia.Evento;
import ia.RegistroEventos;

public class ListaEnlazada<T> implements Lista<T> {

    private Nodo<T> head;
    private Nodo<T> tail;
    private int tamanio;
    private RegistroEventos registro;

    public ListaEnlazada() {
        this.head = null;
        this.tail = null;
        this.tamanio = 0;
    }

    @Override
    public void insertarInicio(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        nuevo.setSiguiente(head);
        head = nuevo;
        if (tail == null) {
            tail = nuevo;
        }
        tamanio++;
        registrar("insertar", "Insertar al inicio " + dato + " (nuevo head)", String.valueOf(dato));
    }

    @Override
    public void insertarFinal(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (head == null) {
            head = nuevo;
            tail = nuevo;
        } else {
            tail.setSiguiente(nuevo);
            tail = nuevo;
        }
        tamanio++;
        registrar("insertar", "Insertar al final " + dato + " (nuevo tail, O(1))", String.valueOf(dato));
    }

    @Override
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamanio) {
            throw new IndexOutOfBoundsException("Indice " + indice + " fuera de rango, tamanio " + tamanio);
        }
        Nodo<T> actual = head;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    @Override
    public boolean estaVacia() {
        return tamanio == 0;
    }

    @Override
    public void limpiar() {
        head = null;
        tail = null;
        tamanio = 0;
    }

    @Override
    public ResultadoBusqueda<T> buscar(Criterio<T> criterio) {
        int comparaciones = 0;
        int indice = 0;
        Nodo<T> actual = head;
        while (actual != null) {
            comparaciones++;
            if (criterio.cumple(actual.getDato())) {
                registrar("buscar", "Buscar: encontrado " + actual.getDato() + " en la posición " + (indice + 1)
                        + " tras " + comparaciones + " comparaciones", String.valueOf(actual.getDato()));
                return new ResultadoBusqueda<>(actual.getDato(), indice, comparaciones);
            }
            actual = actual.getSiguiente();
            indice++;
        }
        registrar("buscar", "Buscar: no existe, se recorrieron " + comparaciones + " nodos", "");
        return new ResultadoBusqueda<>(null, -1, comparaciones);
    }

    @Override
    public Lista<T> filtrar(Criterio<T> criterio) {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        Nodo<T> actual = head;
        while (actual != null) {
            if (criterio.cumple(actual.getDato())) {
                resultado.insertarFinal(actual.getDato());
            }
            actual = actual.getSiguiente();
        }
        return resultado;
    }

    @Override
    public int contar(Criterio<T> criterio) {
        int total = 0;
        Nodo<T> actual = head;
        while (actual != null) {
            if (criterio.cumple(actual.getDato())) {
                total++;
            }
            actual = actual.getSiguiente();
        }
        return total;
    }

    @Override
    public String descripcion() {
        return "ListaEnlazada (head y tail)";
    }

    public Nodo<T> primerNodo() {
        return head;
    }

    public boolean contiene(T dato) {
        for (Nodo<T> actual = head; actual != null; actual = actual.getSiguiente()) {
            if (actual.getDato() == dato) {
                return true;
            }
        }
        return false;
    }

    public void setRegistro(RegistroEventos registro) {
        this.registro = registro;
    }

    private void registrar(String tipo, String texto, String sujeto) {
        if (registro == null) {
            return;
        }
        String[] vista = new String[Math.min(tamanio, Evento.LIMITE_VISTA)];
        Nodo<T> actual = head;
        for (int i = 0; i < vista.length; i++) {
            vista[i] = String.valueOf(actual.getDato());
            actual = actual.getSiguiente();
        }
        registro.agregar(new Evento(tipo, texto, sujeto, "lista", new String[0], vista, tamanio, -1));
    }
}
