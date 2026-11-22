package algoritmos;

import estructuras.Lista;
import estructuras.ListaSecuencial;

import java.util.Comparator;

public final class Ordenamiento {

    private Ordenamiento() {
    }

    public static <T> ResultadoOrdenamiento<T> burbuja(Lista<T> original, Comparator<T> comparador) {
        long inicio = System.nanoTime();
        ListaSecuencial<T> lista = ListaSecuencial.copiaDe(original);
        int n = lista.tamanio();
        long comparaciones = 0;
        long intercambios = 0;
        for (int i = 0; i < n - 1; i++) {
            boolean huboIntercambio = false;
            for (int j = 0; j < n - 1 - i; j++) {
                comparaciones++;
                if (comparador.compare(lista.obtener(j), lista.obtener(j + 1)) > 0) {
                    T temporal = lista.obtener(j);
                    lista.reemplazar(j, lista.obtener(j + 1));
                    lista.reemplazar(j + 1, temporal);
                    intercambios++;
                    huboIntercambio = true;
                }
            }
            if (!huboIntercambio) {
                break;
            }
        }
        return new ResultadoOrdenamiento<>("Burbuja", lista, comparaciones, intercambios, milisegundosDesde(inicio));
    }

    public static <T> ResultadoOrdenamiento<T> insercion(Lista<T> original, Comparator<T> comparador) {
        long inicio = System.nanoTime();
        ListaSecuencial<T> lista = ListaSecuencial.copiaDe(original);
        long comparaciones = 0;
        long desplazamientos = 0;
        for (int i = 1; i < lista.tamanio(); i++) {
            T actual = lista.obtener(i);
            int j = i - 1;
            while (j >= 0) {
                comparaciones++;
                if (comparador.compare(lista.obtener(j), actual) <= 0) {
                    break;
                }
                lista.reemplazar(j + 1, lista.obtener(j));
                desplazamientos++;
                j--;
            }
            lista.reemplazar(j + 1, actual);
        }
        return new ResultadoOrdenamiento<>("Inserción", lista, comparaciones, desplazamientos, milisegundosDesde(inicio));
    }

    public static <T> boolean estaOrdenada(Lista<T> lista, Comparator<T> comparador) {
        for (int i = 1; i < lista.tamanio(); i++) {
            if (comparador.compare(lista.obtener(i - 1), lista.obtener(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static double milisegundosDesde(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000.0;
    }
}
