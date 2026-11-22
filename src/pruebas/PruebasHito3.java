package pruebas;

import algoritmos.Comparadores;
import estructuras.ArbolBST;
import estructuras.Lista;
import estructuras.ResultadoBusqueda;
import etl.DatosLimpios;
import etl.ProcesoETL;
import grafo.MapaPaises;
import ia.RegistroEventos;
import model.Aeropuerto;

public class PruebasHito3 {

    private static int pasaron = 0;
    private static int fallaron = 0;

    public static void main(String[] args) throws Exception {
        pruebasArbolPequenio();
        pruebasEliminar();
        DatosLimpios datos = ProcesoETL.real().ejecutar();
        pruebasArbolIata(datos);
        pruebasDesbalance(datos);
        pruebasArbolPaises(datos);
        System.out.println();
        System.out.println("Pruebas que pasaron: " + pasaron + " | fallaron: " + fallaron);
        if (fallaron > 0) {
            System.exit(1);
        }
    }

    private static void verificar(String nombre, boolean condicion) {
        if (condicion) {
            pasaron++;
            System.out.println("  OK   " + nombre);
        } else {
            fallaron++;
            System.out.println("  FALLA " + nombre);
        }
    }

    private static ArbolBST<Integer> arbolDeEjemplo() {
        ArbolBST<Integer> arbol = new ArbolBST<>(Integer::compare);
        int[] valores = {50, 30, 70, 20, 40, 60, 80, 35};
        for (int v : valores) {
            arbol.insertar(v);
        }
        return arbol;
    }

    private static String texto(Lista<Integer> lista) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < lista.tamanio(); i++) {
            s.append(i == 0 ? "" : " ").append(lista.obtener(i));
        }
        return s.toString();
    }

    private static void pruebasArbolPequenio() {
        System.out.println("ArbolBST pequeño (50 30 70 20 40 60 80 35)");
        ArbolBST<Integer> vacio = new ArbolBST<>(Integer::compare);
        verificar("árbol vacío: altura -1 y 0 nodos", vacio.altura() == -1 && vacio.contarNodos() == 0);
        ArbolBST<Integer> arbol = arbolDeEjemplo();
        verificar("insertar repetido devuelve falso", !arbol.insertar(40) && arbol.contarNodos() == 8);
        verificar("inorden ordenado", texto(arbol.inOrden()).equals("20 30 35 40 50 60 70 80"));
        verificar("preorden", texto(arbol.preOrden()).equals("50 30 20 40 35 70 60 80"));
        verificar("postorden", texto(arbol.postOrden()).equals("20 35 40 30 60 80 70 50"));
        verificar("recorrido con límite", texto(arbol.inOrden(3)).equals("20 30 35"));
        verificar("altura 3", arbol.altura() == 3);
        ResultadoBusqueda<Integer> r = arbol.buscar(35);
        verificar("buscar 35: profundidad 3 y 4 comparaciones", r.getIndice() == 3 && r.getComparaciones() == 4);
        ResultadoBusqueda<Integer> raiz = arbol.buscar(50);
        verificar("buscar la raíz: profundidad 0 y 1 comparación", raiz.getIndice() == 0 && raiz.getComparaciones() == 1);
        verificar("buscar ausente da -1", arbol.buscar(99).getIndice() == -1);
        String[] camino = arbol.caminoHacia(35);
        verificar("camino de 35", camino.length == 4 && camino[0].equals("50 (izquierda)") && camino[2].equals("40 (izquierda)"));
        RegistroEventos registro = new RegistroEventos("bst");
        arbol.setRegistro(registro);
        arbol.buscar(60);
        verificar("la búsqueda registra su camino", registro.contar("buscar") == 1
                && registro.ultimo("buscar").getElementos().length == 3);
    }

    private static void pruebasEliminar() {
        System.out.println("Eliminar en BST");
        ArbolBST<Integer> arbol = arbolDeEjemplo();
        verificar("eliminar hoja 20", arbol.eliminar(20) && texto(arbol.inOrden()).equals("30 35 40 50 60 70 80"));
        verificar("eliminar con un hijo 40", arbol.eliminar(40) && texto(arbol.preOrden()).equals("50 30 35 70 60 80"));
        verificar("eliminar con dos hijos 50 (sucesor 60)", arbol.eliminar(50)
                && texto(arbol.preOrden()).equals("60 30 35 70 80"));
        verificar("eliminar ausente devuelve falso", !arbol.eliminar(99) && arbol.contarNodos() == 5);
        ArbolBST<Integer> uno = new ArbolBST<>(Integer::compare);
        uno.insertar(1);
        verificar("eliminar la única raíz deja el árbol vacío", uno.eliminar(1) && uno.altura() == -1);
    }

    private static void pruebasArbolIata(DatosLimpios datos) {
        System.out.println("BST global por IATA");
        ArbolBST<Aeropuerto> arbol = datos.getArbolIata();
        verificar("6071 nodos", arbol.contarNodos() == 6071);
        verificar("altura 177", arbol.altura() == 177);
        verificar("promedio de comparaciones 19,3", Math.abs(arbol.promedioComparaciones() - 19.3) < 0.05);
        ResultadoBusqueda<Aeropuerto> uio = arbol.buscar(Aeropuerto.conIata("UIO"));
        verificar("UIO con 13 comparaciones", uio.encontrado() && uio.getComparaciones() == 13);
        Lista<Aeropuerto> primeros = arbol.inOrden(5);
        verificar("inorden empieza en orden alfabético", primeros.tamanio() == 5
                && Comparadores.AEROPUERTO_POR_IATA.compare(primeros.obtener(0), primeros.obtener(1)) < 0);
        verificar("inorden completo tiene 6071", arbol.inOrden().tamanio() == 6071);
        verificar("primeros niveles para explicar", arbol.primerosNiveles(3, 8).length == 3);
        System.out.println(String.format("  Promedio de comparaciones: %.2f", arbol.promedioComparaciones()));
    }

    private static void pruebasDesbalance(DatosLimpios datos) {
        System.out.println("Desbalance: BST por ID ascendente");
        ArbolBST<Aeropuerto> porId = new ArbolBST<>(Comparadores.AEROPUERTO_POR_ID);
        for (int i = 0; i < 1500; i++) {
            porId.insertar(datos.getAeropuertos().obtener(i));
        }
        verificar("primeros 1500 por ID: altura 1499", porId.altura() == 1499);
        verificar("recorrido recursivo permitido bajo 2000 niveles", porId.inOrden(10).tamanio() == 10);
        ArbolBST<Aeropuerto> todos = new ArbolBST<>(Comparadores.AEROPUERTO_POR_ID);
        for (int i = 0; i < datos.getAeropuertos().tamanio(); i++) {
            todos.insertar(datos.getAeropuertos().obtener(i));
        }
        verificar("altura iterativa con 7698 por ID: 7697", todos.altura() == 7697);
        boolean protegido;
        try {
            todos.inOrden();
            protegido = false;
        } catch (IllegalStateException ex) {
            protegido = true;
        }
        verificar("no recorre con recursión más de 2000 niveles", protegido);
        verificar("búsqueda iterativa del último funciona", todos.buscar(datos.getAeropuertos().obtener(7697)).getComparaciones() == 7698);
    }

    private static void pruebasArbolPaises(DatosLimpios datos) {
        System.out.println("BST de países");
        MapaPaises mapa = new MapaPaises(datos);
        verificar("237 países", mapa.getArbol().contarNodos() == 237 && mapa.getPaises().tamanio() == 237);
        verificar("altura 15", mapa.getArbol().altura() == 15);
        verificar("busca sin distinguir mayúsculas", mapa.existe("ecuador") && mapa.existe("ECUADOR"));
    }
}
