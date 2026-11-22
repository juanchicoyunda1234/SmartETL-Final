package pruebas;

import algoritmos.Busqueda;
import algoritmos.Comparadores;
import algoritmos.Ordenamiento;
import algoritmos.ResultadoOrdenamiento;
import estructuras.Cola;
import estructuras.ListaEnlazada;
import estructuras.ListaSecuencial;
import estructuras.Pila;
import estructuras.ResultadoBusqueda;
import etl.DatosLimpios;
import etl.ProcesoETL;
import etl.Transform;
import ia.RegistroEventos;
import model.Aeropuerto;
import model.TipoError;

import java.nio.file.Files;
import java.nio.file.Paths;

public class PruebasHito2 {

    private static int pasaron = 0;
    private static int fallaron = 0;

    public static void main(String[] args) throws Exception {
        pruebasCola();
        pruebasPila();
        pruebasValidaciones();
        ProcesoETL etl = pruebasEtlReal();
        pruebasLoad(etl);
        pruebasOrdenamiento(etl.getDatos());
        pruebasBusqueda(etl.getDatos());
        pruebasDemostracion();
        System.out.println();
        System.out.println("Pruebas que pasaron: " + pasaron + " | fallaron: " + fallaron);
        if (fallaron > 0) {
            System.exit(1);
        }
    }

    static void verificar(String nombre, boolean condicion) {
        if (condicion) {
            pasaron++;
            System.out.println("  OK   " + nombre);
        } else {
            fallaron++;
            System.out.println("  FALLA " + nombre);
        }
    }

    private static void pruebasCola() {
        System.out.println("Cola");
        Cola<String> cola = new Cola<>();
        verificar("nace vacía", cola.estaVacia() && cola.tamanio() == 0);
        cola.encolar("UIO");
        cola.encolar("GYE");
        cola.encolar("CUE");
        verificar("frente es el primero encolado", cola.frente().equals("UIO") && cola.tamanio() == 3);
        verificar("desencolar respeta FIFO", cola.desencolar().equals("UIO") && cola.desencolar().equals("GYE"));
        verificar("mostrar", cola.mostrar().equals("[CUE]"));
        cola.desencolar();
        verificar("desencolar vacía lanza IllegalStateException", lanzaEstado(cola::desencolar));
        verificar("frente de vacía lanza IllegalStateException", lanzaEstado(cola::frente));
        cola.encolar("LIM");
        verificar("se puede reutilizar tras vaciarse", cola.frente().equals("LIM") && cola.tamanio() == 1);
        RegistroEventos registro = new RegistroEventos("cola");
        Cola<String> trazada = new Cola<>();
        trazada.setRegistro(registro);
        trazada.encolar("A");
        trazada.encolar("B");
        trazada.desencolar();
        verificar("registra encolar y desencolar", registro.contar("encolar") == 2 && registro.contar("desencolar") == 1
                && registro.ultimo("desencolar").estadoComoTexto().equals("[B]"));
    }

    private static void pruebasPila() {
        System.out.println("Pila");
        Pila<Integer> pila = new Pila<>();
        verificar("nace vacía", pila.estaVacia());
        pila.apilar(1);
        pila.apilar(2);
        pila.apilar(3);
        verificar("cima es el último apilado", pila.cima() == 3 && pila.tamanio() == 3);
        verificar("mostrar desde la cima", pila.mostrar().equals("[3, 2, 1]"));
        verificar("desapilar respeta LIFO", pila.desapilar() == 3 && pila.desapilar() == 2 && pila.desapilar() == 1);
        verificar("desapilar vacía lanza IllegalStateException", lanzaEstado(pila::desapilar));
        verificar("cima de vacía lanza IllegalStateException", lanzaEstado(pila::cima));
        Pila<Integer> muchos = new Pila<>();
        for (int i = 0; i < 20; i++) {
            muchos.apilar(i);
        }
        verificar("mostrar limita a 12 y resume el resto", muchos.mostrar().endsWith("8 y 8 más]"));
    }

    private static boolean lanzaEstado(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalStateException ex) {
            return true;
        }
    }

    private static void pruebasValidaciones() {
        System.out.println("Reglas de Transform");
        verificar("IATA válido: 3 mayúsculas", Transform.esIataValido("UIO") && !Transform.esIataValido("DU9")
                && !Transform.esIataValido("\\N") && !Transform.esIataValido("uio") && !Transform.esIataValido("ABCD"));
        verificar("entero", Transform.esEntero("2688") && Transform.esEntero("-1") && !Transform.esEntero("A7")
                && !Transform.esEntero("") && !Transform.esEntero("\\N"));
        verificar("rango", Transform.estaEnRango("-0.12", 90) && !Transform.estaEnRango("95.5", 90)
                && !Transform.estaEnRango("abc", 180));
        verificar("limpio convierte \\N y vacío a Sin dato", Transform.limpio("\\N").equals("Sin dato")
                && Transform.limpio("  ").equals("Sin dato") && Transform.limpio(" Quito ").equals("Quito"));
    }

    private static ProcesoETL pruebasEtlReal() throws Exception {
        System.out.println("ETL completo sobre los datos reales");
        ProcesoETL etl = ProcesoETL.real();
        long inicio = System.nanoTime();
        DatosLimpios datos = etl.ejecutar();
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        Transform t = etl.getTransform();
        verificar("aeropuertos encolados 7698", etl.getEncolados(Transform.AEROPUERTOS) == 7698);
        verificar("aeropuertos: 7698 procesados, 0 errores", t.getProcesados(Transform.AEROPUERTOS) == 7698
                && t.getInvalidos(Transform.AEROPUERTOS) == 0);
        verificar("aeropuertos con IATA válido 6071 (BST)", datos.getArbolIata().tamanio() == 6071);
        verificar("aeropuertos sin IATA válido 1627", datos.getAeropuertos().contar(a -> a.getIata().equals("Sin dato")) == 1627);
        verificar("aeropuertos sin ciudad 49", datos.getAeropuertos().contar(a -> a.getCiudad().equals("Sin dato")) == 49);
        verificar("aerolíneas 6162 leídas y 0 errores", t.getProcesados(Transform.AEROLINEAS) == 6162
                && t.getInvalidos(Transform.AEROLINEAS) == 0);
        verificar("rutas leídas 67663", t.getProcesados(Transform.RUTAS) == 67663);
        verificar("rutas válidas 67074", datos.getRutas().tamanio() == 67074);
        verificar("rutas con error 589", t.getInvalidos(Transform.RUTAS) == 589);
        verificar("REFERENCIA_ROTA 473", t.getErrores(Transform.RUTAS, TipoError.REFERENCIA_ROTA) == 473);
        verificar("ID_NULO_NO_RECUPERABLE 116", t.getErrores(Transform.RUTAS, TipoError.ID_NULO_NO_RECUPERABLE) == 116);
        verificar("pila de errores con 589", t.getErrores().tamanio() == 589);
        boolean sinNulos = datos.getRutas().contar(r -> r.getIdOrigen().equals("\\N") || r.getIdDestino().equals("\\N")) == 0;
        verificar("las rutas válidas conservan los IDs resueltos", sinNulos);
        verificar("la lista por ID está ordenada", Ordenamiento.estaOrdenada(datos.getAeropuertosPorId(),
                Comparadores.AEROPUERTO_POR_ID));
        verificar("buscarPorId 2688 es Quito", datos.buscarPorId("2688").getIata().equals("UIO"));
        verificar("registro de eventos del ETL tiene lecturas y totales", etl.getRegistro().contar("leer") == 3
                && etl.getRegistro().contar("totales") == 3);
        System.out.println(String.format("  ETL completo en %.1f ms (Extract %.1f, Transform %.1f)", ms,
                etl.getMsExtract(), t.getMilisegundos()));
        return etl;
    }

    private static void pruebasLoad(ProcesoETL etl) throws Exception {
        System.out.println("Load");
        String[] archivos = etl.cargar("output");
        verificar("escribe 5 archivos", archivos.length == 5);
        verificar("airports_limpios.csv con 7698 líneas", Files.readAllLines(Paths.get("output/airports_limpios.csv")).size() == 7698);
        verificar("routes_limpias.csv con 67074 líneas", Files.readAllLines(Paths.get("output/routes_limpias.csv")).size() == 67074);
        verificar("errores_etl.txt con 589 errores y encabezado", Files.readAllLines(Paths.get("output/errores_etl.txt")).size() == 590);
        String reporte = new String(Files.readAllBytes(Paths.get("output/reporte_etl.txt")), "UTF-8");
        verificar("reporte con totales", reporte.contains("REFERENCIA_ROTA") && reporte.contains("67074"));
    }

    private static void pruebasOrdenamiento(DatosLimpios datos) {
        System.out.println("Ordenamiento");
        ListaEnlazada<Integer> numeros = new ListaEnlazada<>();
        int[] valores = {5, 1, 4, 2, 8};
        for (int v : valores) {
            numeros.insertarFinal(v);
        }
        ResultadoOrdenamiento<Integer> b = Ordenamiento.burbuja(numeros, Integer::compare);
        verificar("burbuja ordena 5 1 4 2 8", b.getLista().obtener(0) == 1 && b.getLista().obtener(4) == 8
                && Ordenamiento.estaOrdenada(b.getLista(), Integer::compare));
        verificar("burbuja no altera la original", numeros.obtener(0) == 5);
        verificar("burbuja cuenta 4 intercambios", b.getMovimientos() == 4);
        ResultadoOrdenamiento<Integer> i = Ordenamiento.insercion(numeros, Integer::compare);
        verificar("inserción ordena y cuenta 4 desplazamientos", Ordenamiento.estaOrdenada(i.getLista(), Integer::compare)
                && i.getMovimientos() == 4);
        ListaSecuencial<Integer> ordenada = new ListaSecuencial<>();
        for (int k = 0; k < 100; k++) {
            ordenada.insertarFinal(k);
        }
        verificar("inserción sobre lista ordenada hace n-1 comparaciones",
                Ordenamiento.insercion(ordenada, Integer::compare).getComparaciones() == 99);
        ResultadoOrdenamiento<Aeropuerto> porIata = Ordenamiento.insercion(datos.getAeropuertos(), Comparadores.AEROPUERTO_POR_IATA);
        verificar("aeropuertos por IATA con inserción quedan ordenados",
                Ordenamiento.estaOrdenada(porIata.getLista(), Comparadores.AEROPUERTO_POR_IATA));
        ResultadoOrdenamiento<Aeropuerto> porPais = Ordenamiento.burbuja(datos.getAeropuertos(), Comparadores.AEROPUERTO_POR_PAIS);
        verificar("aeropuertos por país con burbuja quedan ordenados",
                Ordenamiento.estaOrdenada(porPais.getLista(), Comparadores.AEROPUERTO_POR_PAIS));
        System.out.println(String.format("  Inserción IATA: %d comparaciones, %.1f ms | Burbuja país: %d comparaciones, %.1f ms",
                porIata.getComparaciones(), porIata.getMilisegundos(), porPais.getComparaciones(), porPais.getMilisegundos()));
    }

    private static void pruebasBusqueda(DatosLimpios datos) {
        System.out.println("Búsqueda");
        ResultadoBusqueda<Aeropuerto> secuencial = Busqueda.secuencial(datos.getAeropuertos(), a -> a.getIata().equals("UIO"));
        verificar("secuencial UIO: posición 2556 con 2556 comparaciones", secuencial.getIndice() + 1 == 2556
                && secuencial.getComparaciones() == 2556);
        ListaSecuencial<Aeropuerto> ordenada = Ordenamiento.insercion(datos.getAeropuertos(), Comparadores.AEROPUERTO_POR_IATA).getLista();
        ResultadoBusqueda<Aeropuerto> binaria = Busqueda.binaria(ordenada, Aeropuerto.conIata("UIO"), Comparadores.AEROPUERTO_POR_IATA);
        verificar("binaria UIO encuentra Quito con unas 13 comparaciones", binaria.encontrado()
                && binaria.getElemento().getCiudad().equals("Quito") && binaria.getComparaciones() <= 14);
        ResultadoBusqueda<Aeropuerto> ausente = Busqueda.binaria(ordenada, Aeropuerto.conIata("QQQ"), Comparadores.AEROPUERTO_POR_IATA);
        verificar("binaria de ausente devuelve -1", !ausente.encontrado() && ausente.getComparaciones() <= 14);
        ResultadoBusqueda<Aeropuerto> arbol = Busqueda.porArbol(datos.getArbolIata(), Aeropuerto.conIata("UIO"));
        verificar("BST UIO con 13 comparaciones", arbol.encontrado() && arbol.getComparaciones() == 13);
        System.out.println("  UIO: secuencial " + secuencial.getComparaciones() + ", binaria " + binaria.getComparaciones()
                + ", BST " + arbol.getComparaciones());
    }

    private static void pruebasDemostracion() throws Exception {
        System.out.println("Archivo de demostración sucio");
        ProcesoETL demo = ProcesoETL.demostracion();
        demo.ejecutar();
        Transform t = demo.getTransform();
        boolean todos = true;
        for (TipoError tipo : TipoError.values()) {
            if (t.getErroresDeTipo(tipo) == 0) {
                todos = false;
                System.out.println("    sin errores de tipo " + tipo);
            }
        }
        verificar("aparecen los 6 tipos de error en la pila", todos);
        verificar("aeropuertos demo: 15 leídos, 9 válidos", t.getProcesados(Transform.AEROPUERTOS) == 15
                && t.getValidos(Transform.AEROPUERTOS) == 9);
        verificar("IATA inválido pasa a Sin dato y es válido", demo.getDatos().buscarPorId("8").getIata().equals("Sin dato"));
        verificar("ruta con ID nulo de UIO se recupera", t.getValidos(Transform.RUTAS) == 4);
    }
}
