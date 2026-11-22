package ia;

import estructuras.ArbolBST;
import estructuras.Cola;
import estructuras.ListaEnlazada;
import estructuras.Pila;

public final class TrazasEstructuras {

    private TrazasEstructuras() {
    }

    public static RegistroEventos cola() {
        RegistroEventos registro = new RegistroEventos("Cola (encolar y desencolar aeropuertos)");
        Cola<String> cola = new Cola<>();
        cola.setRegistro(registro);
        cola.encolar("UIO");
        cola.encolar("GYE");
        cola.encolar("CUE");
        cola.desencolar();
        cola.encolar("LIM");
        cola.desencolar();
        return registro;
    }

    public static RegistroEventos pila() {
        RegistroEventos registro = new RegistroEventos("Pila (apilar y desapilar errores del ETL)");
        Pila<String> pila = new Pila<>();
        pila.setRegistro(registro);
        pila.apilar("REFERENCIA_ROTA");
        pila.apilar("ID_NULO_NO_RECUPERABLE");
        pila.apilar("DUPLICADO");
        pila.desapilar();
        pila.apilar("RANGO_INVALIDO");
        pila.desapilar();
        return registro;
    }

    public static RegistroEventos lista() {
        RegistroEventos registro = new RegistroEventos("Lista enlazada (insertar y buscar)");
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.setRegistro(registro);
        lista.insertarFinal("UIO");
        lista.insertarFinal("GYE");
        lista.insertarFinal("CUE");
        lista.insertarInicio("LIM");
        lista.buscar("CUE"::equals);
        lista.buscar("BOG"::equals);
        return registro;
    }

    public static RegistroEventos bst() {
        RegistroEventos registro = new RegistroEventos("BST por IATA (insertar, buscar y eliminar)");
        ArbolBST<String> arbol = new ArbolBST<>(String::compareTo);
        arbol.setRegistro(registro);
        String[] codigos = {"MEX", "GYE", "UIO", "BOG", "LIM", "SCL", "CUE"};
        for (String codigo : codigos) {
            arbol.insertar(codigo);
        }
        arbol.buscar("LIM");
        arbol.eliminar("GYE");
        arbol.buscar("CUE");
        return registro;
    }
}
