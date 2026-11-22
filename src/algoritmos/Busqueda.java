package algoritmos;

import estructuras.ArbolBST;
import estructuras.Criterio;
import estructuras.Lista;
import estructuras.ListaSecuencial;
import estructuras.ResultadoBusqueda;

import java.util.Comparator;

public final class Busqueda {

    private Busqueda() {
    }

    public static <T> ResultadoBusqueda<T> secuencial(Lista<T> lista, Criterio<T> criterio) {
        return lista.buscar(criterio);
    }

    public static <T> ResultadoBusqueda<T> binaria(ListaSecuencial<T> ordenada, T clave, Comparator<T> comparador) {
        int izquierda = 0;
        int derecha = ordenada.tamanio() - 1;
        int comparaciones = 0;
        while (izquierda <= derecha) {
            int medio = (izquierda + derecha) / 2;
            T elemento = ordenada.obtener(medio);
            comparaciones++;
            int comparacion = comparador.compare(clave, elemento);
            if (comparacion == 0) {
                return new ResultadoBusqueda<>(elemento, medio, comparaciones);
            }
            if (comparacion < 0) {
                derecha = medio - 1;
            } else {
                izquierda = medio + 1;
            }
        }
        return new ResultadoBusqueda<>(null, -1, comparaciones);
    }

    public static <T> ResultadoBusqueda<T> porArbol(ArbolBST<T> arbol, T clave) {
        return arbol.buscar(clave);
    }
}
