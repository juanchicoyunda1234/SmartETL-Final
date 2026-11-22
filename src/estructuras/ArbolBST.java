package estructuras;

import ia.Evento;
import ia.RegistroEventos;

import java.util.Comparator;
import java.util.function.Function;

public class ArbolBST<T> {

    public static final int ALTURA_MAXIMA_RECURSIVA = 2000;

    private final Comparator<T> comparador;
    private NodoArbol<T> raiz;
    private int tamanio;
    private RegistroEventos registro;
    private Function<T, String> etiquetador = String::valueOf;

    public ArbolBST(Comparator<T> comparador) {
        this.comparador = comparador;
    }

    public void setRegistro(RegistroEventos registro) {
        this.registro = registro;
    }

    public void setEtiquetador(Function<T, String> etiquetador) {
        this.etiquetador = etiquetador;
    }

    public String etiqueta(T dato) {
        return etiquetador.apply(dato);
    }

    public NodoArbol<T> getRaiz() {
        return raiz;
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    public boolean insertar(T dato) {
        NodoArbol<T> nuevo = new NodoArbol<>(dato);
        if (raiz == null) {
            raiz = nuevo;
            tamanio++;
            registrar("insertar", "Insertar " + etiqueta(dato) + ": el árbol estaba vacío, queda como raíz", dato, new String[0]);
            return true;
        }
        NodoArbol<T> actual = raiz;
        while (true) {
            int comparacion = comparador.compare(dato, actual.getDato());
            if (comparacion == 0) {
                registrar("insertar", "Insertar " + etiqueta(dato) + ": ya existe, no se inserta", dato, caminoHacia(dato));
                return false;
            }
            if (comparacion < 0) {
                if (actual.getIzquierdo() == null) {
                    actual.setIzquierdo(nuevo);
                    break;
                }
                actual = actual.getIzquierdo();
            } else {
                if (actual.getDerecho() == null) {
                    actual.setDerecho(nuevo);
                    break;
                }
                actual = actual.getDerecho();
            }
        }
        tamanio++;
        if (registro != null) {
            String[] camino = caminoHacia(dato);
            registrar("insertar", "Insertar " + etiqueta(dato) + " como hoja tras " + (camino.length - 1) + " comparaciones", dato, camino);
        }
        return true;
    }

    public ResultadoBusqueda<T> buscar(T clave) {
        NodoArbol<T> actual = raiz;
        int comparaciones = 0;
        int profundidad = 0;
        while (actual != null) {
            comparaciones++;
            int comparacion = comparador.compare(clave, actual.getDato());
            if (comparacion == 0) {
                registrarBusqueda(clave, true, comparaciones);
                return new ResultadoBusqueda<>(actual.getDato(), profundidad, comparaciones);
            }
            actual = comparacion < 0 ? actual.getIzquierdo() : actual.getDerecho();
            profundidad++;
        }
        registrarBusqueda(clave, false, comparaciones);
        return new ResultadoBusqueda<>(null, -1, comparaciones);
    }

    public String[] caminoHacia(T clave) {
        ListaSecuencial<String> pasos = new ListaSecuencial<>();
        NodoArbol<T> actual = raiz;
        while (actual != null) {
            int comparacion = comparador.compare(clave, actual.getDato());
            if (comparacion == 0) {
                pasos.insertarFinal(etiqueta(actual.getDato()) + " (encontrado)");
                break;
            }
            String lado = comparacion < 0 ? "izquierda" : "derecha";
            pasos.insertarFinal(etiqueta(actual.getDato()) + " (" + lado + ")");
            actual = comparacion < 0 ? actual.getIzquierdo() : actual.getDerecho();
        }
        if (actual == null) {
            pasos.insertarFinal("vacío");
        }
        String[] camino = new String[pasos.tamanio()];
        for (int i = 0; i < camino.length; i++) {
            camino[i] = pasos.obtener(i);
        }
        return camino;
    }

    public boolean eliminar(T clave) {
        NodoArbol<T> padre = null;
        NodoArbol<T> actual = raiz;
        while (actual != null) {
            int comparacion = comparador.compare(clave, actual.getDato());
            if (comparacion == 0) {
                break;
            }
            padre = actual;
            actual = comparacion < 0 ? actual.getIzquierdo() : actual.getDerecho();
        }
        if (actual == null) {
            registrar("eliminar", "Eliminar " + etiqueta(clave) + ": no existe", clave, new String[0]);
            return false;
        }
        String caso = actual.esHoja() ? "hoja" : "un hijo";
        if (actual.getIzquierdo() != null && actual.getDerecho() != null) {
            NodoArbol<T> padreSucesor = actual;
            NodoArbol<T> sucesor = actual.getDerecho();
            while (sucesor.getIzquierdo() != null) {
                padreSucesor = sucesor;
                sucesor = sucesor.getIzquierdo();
            }
            caso = "dos hijos, se reemplaza por el sucesor inorden " + etiqueta(sucesor.getDato());
            actual.setDato(sucesor.getDato());
            padre = padreSucesor;
            actual = sucesor;
        }
        NodoArbol<T> hijo = actual.getIzquierdo() != null ? actual.getIzquierdo() : actual.getDerecho();
        if (padre == null) {
            raiz = hijo;
        } else if (padre.getIzquierdo() == actual) {
            padre.setIzquierdo(hijo);
        } else {
            padre.setDerecho(hijo);
        }
        tamanio--;
        registrar("eliminar", "Eliminar " + etiqueta(clave) + " (caso: " + caso + ")", clave, new String[0]);
        return true;
    }

    public int altura() {
        if (raiz == null) {
            return -1;
        }
        Cola<NodoArbol<T>> cola = new Cola<>();
        cola.encolar(raiz);
        int altura = -1;
        while (!cola.estaVacia()) {
            int enNivel = cola.tamanio();
            altura++;
            for (int i = 0; i < enNivel; i++) {
                NodoArbol<T> nodo = cola.desencolar();
                if (nodo.getIzquierdo() != null) {
                    cola.encolar(nodo.getIzquierdo());
                }
                if (nodo.getDerecho() != null) {
                    cola.encolar(nodo.getDerecho());
                }
            }
        }
        return altura;
    }

    public int contarNodos() {
        if (raiz == null) {
            return 0;
        }
        int total = 0;
        Pila<NodoArbol<T>> pila = new Pila<>();
        pila.apilar(raiz);
        while (!pila.estaVacia()) {
            NodoArbol<T> nodo = pila.desapilar();
            total++;
            if (nodo.getDerecho() != null) {
                pila.apilar(nodo.getDerecho());
            }
            if (nodo.getIzquierdo() != null) {
                pila.apilar(nodo.getIzquierdo());
            }
        }
        return total;
    }

    public double promedioComparaciones() {
        if (raiz == null) {
            return 0;
        }
        Cola<NodoArbol<T>> cola = new Cola<>();
        cola.encolar(raiz);
        long suma = 0;
        int nodos = 0;
        int nivel = 0;
        while (!cola.estaVacia()) {
            int enNivel = cola.tamanio();
            for (int i = 0; i < enNivel; i++) {
                NodoArbol<T> nodo = cola.desencolar();
                suma += nivel + 1;
                nodos++;
                if (nodo.getIzquierdo() != null) {
                    cola.encolar(nodo.getIzquierdo());
                }
                if (nodo.getDerecho() != null) {
                    cola.encolar(nodo.getDerecho());
                }
            }
            nivel++;
        }
        return (double) suma / nodos;
    }

    public String[] primerosNiveles(int niveles, int nodosPorNivel) {
        ListaSecuencial<String> lineas = new ListaSecuencial<>();
        if (raiz != null) {
            Cola<NodoArbol<T>> cola = new Cola<>();
            cola.encolar(raiz);
            int nivel = 0;
            while (!cola.estaVacia() && nivel < niveles) {
                int enNivel = cola.tamanio();
                String[] nombres = new String[enNivel];
                for (int i = 0; i < enNivel; i++) {
                    NodoArbol<T> nodo = cola.desencolar();
                    nombres[i] = etiqueta(nodo.getDato()) + hijosComoTexto(nodo);
                    if (nodo.getIzquierdo() != null) {
                        cola.encolar(nodo.getIzquierdo());
                    }
                    if (nodo.getDerecho() != null) {
                        cola.encolar(nodo.getDerecho());
                    }
                }
                lineas.insertarFinal("Nivel " + nivel + " (" + enNivel + " nodos): " + unir(nombres, nodosPorNivel));
                nivel++;
            }
        }
        String[] resultado = new String[lineas.tamanio()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = lineas.obtener(i);
        }
        return resultado;
    }

    private String hijosComoTexto(NodoArbol<T> nodo) {
        String izquierdo = nodo.getIzquierdo() == null ? "-" : etiqueta(nodo.getIzquierdo().getDato());
        String derecho = nodo.getDerecho() == null ? "-" : etiqueta(nodo.getDerecho().getDato());
        return " (izq " + izquierdo + ", der " + derecho + ")";
    }

    private String unir(String[] nombres, int limite) {
        StringBuilder texto = new StringBuilder();
        int mostrar = Math.min(nombres.length, limite);
        for (int i = 0; i < mostrar; i++) {
            if (i > 0) {
                texto.append("; ");
            }
            texto.append(nombres[i]);
        }
        if (nombres.length > mostrar) {
            texto.append(" y ").append(nombres.length - mostrar).append(" más");
        }
        return texto.toString();
    }

    public Lista<T> inOrden() {
        return inOrden(Integer.MAX_VALUE);
    }

    public Lista<T> preOrden() {
        return preOrden(Integer.MAX_VALUE);
    }

    public Lista<T> postOrden() {
        return postOrden(Integer.MAX_VALUE);
    }

    public Lista<T> inOrden(int limite) {
        verificarAlturaRecursiva();
        ListaSecuencial<T> resultado = new ListaSecuencial<>();
        inOrden(raiz, resultado, limite);
        return resultado;
    }

    public Lista<T> preOrden(int limite) {
        verificarAlturaRecursiva();
        ListaSecuencial<T> resultado = new ListaSecuencial<>();
        preOrden(raiz, resultado, limite);
        return resultado;
    }

    public Lista<T> postOrden(int limite) {
        verificarAlturaRecursiva();
        ListaSecuencial<T> resultado = new ListaSecuencial<>();
        postOrden(raiz, resultado, limite);
        return resultado;
    }

    private void inOrden(NodoArbol<T> nodo, ListaSecuencial<T> resultado, int limite) {
        if (nodo == null || resultado.tamanio() >= limite) {
            return;
        }
        inOrden(nodo.getIzquierdo(), resultado, limite);
        if (resultado.tamanio() < limite) {
            resultado.insertarFinal(nodo.getDato());
        }
        inOrden(nodo.getDerecho(), resultado, limite);
    }

    private void preOrden(NodoArbol<T> nodo, ListaSecuencial<T> resultado, int limite) {
        if (nodo == null || resultado.tamanio() >= limite) {
            return;
        }
        resultado.insertarFinal(nodo.getDato());
        preOrden(nodo.getIzquierdo(), resultado, limite);
        preOrden(nodo.getDerecho(), resultado, limite);
    }

    private void postOrden(NodoArbol<T> nodo, ListaSecuencial<T> resultado, int limite) {
        if (nodo == null || resultado.tamanio() >= limite) {
            return;
        }
        postOrden(nodo.getIzquierdo(), resultado, limite);
        postOrden(nodo.getDerecho(), resultado, limite);
        if (resultado.tamanio() < limite) {
            resultado.insertarFinal(nodo.getDato());
        }
    }

    private void verificarAlturaRecursiva() {
        if (altura() > ALTURA_MAXIMA_RECURSIVA) {
            throw new IllegalStateException("El árbol supera " + ALTURA_MAXIMA_RECURSIVA
                    + " niveles: no se recorre con recursión para no desbordar la pila de llamadas");
        }
    }

    private void registrarBusqueda(T clave, boolean encontrado, int comparaciones) {
        if (registro != null) {
            String resultado = encontrado ? "encontrado" : "no existe";
            registrar("buscar", "Buscar " + etiqueta(clave) + ": " + resultado + " tras " + comparaciones + " comparaciones",
                    clave, caminoHacia(clave));
        }
    }

    private void registrar(String tipo, String texto, T dato, String[] camino) {
        if (registro != null) {
            registro.agregar(new Evento(tipo, texto, etiqueta(dato), "bst", camino, new String[0], tamanio, -1));
        }
    }
}
