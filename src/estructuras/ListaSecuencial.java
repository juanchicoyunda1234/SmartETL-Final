package estructuras;

public class ListaSecuencial<T> implements Lista<T> {

    private static final int CAPACIDAD_INICIAL = 16;

    private Object[] datos;
    private int tope;

    public ListaSecuencial() {
        this.datos = new Object[CAPACIDAD_INICIAL];
        this.tope = 0;
    }

    private void asegurarCapacidad() {
        if (tope == datos.length) {
            Object[] ampliado = new Object[datos.length * 2];
            for (int i = 0; i < tope; i++) {
                ampliado[i] = datos[i];
            }
            datos = ampliado;
        }
    }

    @Override
    public void insertarInicio(T dato) {
        asegurarCapacidad();
        for (int i = tope; i > 0; i--) {
            datos[i] = datos[i - 1];
        }
        datos[0] = dato;
        tope++;
    }

    @Override
    public void insertarFinal(T dato) {
        asegurarCapacidad();
        datos[tope] = dato;
        tope++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T obtener(int indice) {
        if (indice < 0 || indice >= tope) {
            throw new IndexOutOfBoundsException("Indice " + indice + " fuera de rango, tamanio " + tope);
        }
        return (T) datos[indice];
    }

    @Override
    public int tamanio() {
        return tope;
    }

    @Override
    public boolean estaVacia() {
        return tope == 0;
    }

    @Override
    public void limpiar() {
        datos = new Object[CAPACIDAD_INICIAL];
        tope = 0;
    }

    public int capacidad() {
        return datos.length;
    }

    public void reemplazar(int indice, T dato) {
        if (indice < 0 || indice >= tope) {
            throw new IndexOutOfBoundsException("Indice " + indice + " fuera de rango, tamanio " + tope);
        }
        datos[indice] = dato;
    }

    public static <T> ListaSecuencial<T> copiaDe(Lista<T> origen) {
        ListaSecuencial<T> copia = new ListaSecuencial<>();
        if (origen instanceof ListaEnlazada) {
            for (Nodo<T> actual = ((ListaEnlazada<T>) origen).primerNodo(); actual != null; actual = actual.getSiguiente()) {
                copia.insertarFinal(actual.getDato());
            }
        } else {
            for (int i = 0; i < origen.tamanio(); i++) {
                copia.insertarFinal(origen.obtener(i));
            }
        }
        return copia;
    }

    @Override
    public ResultadoBusqueda<T> buscar(Criterio<T> criterio) {
        int comparaciones = 0;
        for (int i = 0; i < tope; i++) {
            comparaciones++;
            T dato = obtener(i);
            if (criterio.cumple(dato)) {
                return new ResultadoBusqueda<>(dato, i, comparaciones);
            }
        }
        return new ResultadoBusqueda<>(null, -1, comparaciones);
    }

    @Override
    public Lista<T> filtrar(Criterio<T> criterio) {
        ListaSecuencial<T> resultado = new ListaSecuencial<>();
        for (int i = 0; i < tope; i++) {
            T dato = obtener(i);
            if (criterio.cumple(dato)) {
                resultado.insertarFinal(dato);
            }
        }
        return resultado;
    }

    @Override
    public int contar(Criterio<T> criterio) {
        int total = 0;
        for (int i = 0; i < tope; i++) {
            if (criterio.cumple(obtener(i))) {
                total++;
            }
        }
        return total;
    }

    @Override
    public String descripcion() {
        return "ListaSecuencial (capacidad " + datos.length + ")";
    }
}
