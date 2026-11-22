package app.web;

import algoritmos.Busqueda;
import algoritmos.Comparadores;
import algoritmos.Ordenamiento;
import algoritmos.ResultadoOrdenamiento;
import estructuras.ArbolBST;
import estructuras.Lista;
import estructuras.ListaEnlazada;
import estructuras.ListaSecuencial;
import estructuras.NodoArbol;
import estructuras.ResultadoBusqueda;
import etl.DatosLimpios;
import model.Aeropuerto;

import java.util.Comparator;
import java.util.Random;

public class ApiEstructuras implements AreaApi {

    public static final int PRIMEROS = 15;
    public static final int NIVELES_DIBUJO = 4;

    private final ContextoWeb contexto;
    private ListaSecuencial<Aeropuerto> ordenadaPorIata;
    private ArbolBST<Aeropuerto> arbolTrabajo;

    public ApiEstructuras(ContextoWeb contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean atender(Peticion p) throws Exception {
        if (p.es("GET", "estructuras", "estado")) {
            Respuesta.json(p.getIntercambio(), 200, estado());
            return true;
        }
        if (p.es("POST", "estructuras", "ordenar")) {
            LectorJson json = p.json();
            String algoritmo = json.texto("algoritmo", "Burbuja");
            String campo = campo(json.texto("campo", "IATA"));
            DatosLimpios datos = datos();
            TareaWeb tarea = contexto.getTareas().iniciar("Ordenar con " + algoritmo + " por " + campo, avisar -> {
                avisar.accept("Ordenando una copia de " + datos.getAeropuertos().tamanio() + " aeropuertos con " + algoritmo);
                String resultado = ordenar(datos, algoritmo, campo);
                contexto.getReporte().registrar("ordenamiento", resultado);
                return resultado;
            });
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("POST", "estructuras", "comparar-ordenamientos")) {
            String campo = campo(p.json().texto("campo", "IATA"));
            DatosLimpios datos = datos();
            TareaWeb tarea = contexto.getTareas().iniciar("Burbuja vs inserción por " + campo, avisar -> {
                avisar.accept("Burbuja: ordenando " + datos.getAeropuertos().tamanio() + " aeropuertos por " + campo);
                String burbuja = ordenar(datos, "Burbuja", campo);
                avisar.accept("Inserción: ordenando los mismos aeropuertos por " + campo);
                String insercion = ordenar(datos, "Inserción", campo);
                String resultado = new ObjetoJson().texto("campo", campo).crudo("burbuja", burbuja).crudo("insercion", insercion).toString();
                contexto.getReporte().registrar("comparacionOrdenamiento", resultado);
                return resultado;
            });
            ApiGeneral.tareaIniciada(p.getIntercambio(), tarea);
            return true;
        }
        if (p.es("POST", "estructuras", "muestra")) {
            Respuesta.json(p.getIntercambio(), 200, muestra(p.json()));
            return true;
        }
        if (p.es("POST", "estructuras", "buscar")) {
            Respuesta.json(p.getIntercambio(), 200, buscar(p.json().texto("iata", "").trim().toUpperCase()));
            return true;
        }
        if (p.es("POST", "estructuras", "bst", "construir")) {
            Respuesta.json(p.getIntercambio(), 200, construir());
            return true;
        }
        if (p.es("POST", "estructuras", "bst", "*")) {
            Respuesta.json(p.getIntercambio(), 200, operar(p.parte(2), p.json().texto("iata", "").trim().toUpperCase()));
            return true;
        }
        if (p.es("GET", "estructuras", "bst", "recorrido")) {
            Respuesta.json(p.getIntercambio(), 200, recorrido(p.parametro("tipo", "Inorden"), p.parametroEntero("limite", 20)));
            return true;
        }
        if (p.es("GET", "estructuras", "bst", "medidas")) {
            Respuesta.json(p.getIntercambio(), 200, medidas());
            return true;
        }
        if (p.es("POST", "estructuras", "desbalance")) {
            Respuesta.json(p.getIntercambio(), 200, desbalance());
            return true;
        }
        return false;
    }

    private String campo(String texto) throws ErrorWeb {
        if (texto.equals("IATA") || texto.equals("Nombre")) {
            return texto;
        }
        if (texto.equals("País") || texto.equals("Pais")) {
            return "País";
        }
        throw new ErrorWeb(400, "Campo de orden desconocido: " + texto + " (usa IATA, Nombre o País)");
    }

    private DatosLimpios datos() throws Exception {
        if (!contexto.getSesion().tieneEtlReal()) {
            throw new ErrorWeb(409, "Primero prepara los datos: ejecuta el ETL con datos reales");
        }
        return contexto.getSesion().datosReales();
    }

    private synchronized String estado() {
        return new ObjetoJson().booleano("datosListos", contexto.getSesion().tieneEtlReal())
                .crudo("arbol", arbolTrabajo == null ? null : medida(arbolTrabajo)).toString();
    }

    private String descripcion(Aeropuerto aeropuerto) {
        if (aeropuerto.getNombre() == null || aeropuerto.getNombre().isEmpty()) {
            return aeropuerto.getIata() + " (clave insertada a mano, sin datos de aeropuerto)";
        }
        return aeropuerto.getNombre() + " (" + aeropuerto.getCiudad() + ")";
    }

    private ObjetoJson medida(ArbolBST<Aeropuerto> arbol) {
        return new ObjetoJson().entero("nodos", arbol.contarNodos()).entero("altura", arbol.altura());
    }

    private String ordenar(DatosLimpios datos, String algoritmo, String campo) {
        Comparator<Aeropuerto> comparador = Comparadores.porCampo(campo);
        ResultadoOrdenamiento<Aeropuerto> r = algoritmo.equals("Burbuja")
                ? Ordenamiento.burbuja(datos.getAeropuertos(), comparador)
                : Ordenamiento.insercion(datos.getAeropuertos(), comparador);
        if (campo.equals("IATA")) {
            synchronized (this) {
                ordenadaPorIata = r.getLista();
            }
        }
        ArregloJson primeros = new ArregloJson();
        for (int i = 0; i < Math.min(PRIMEROS, r.getLista().tamanio()); i++) {
            primeros.crudo(aeropuerto(r.getLista().obtener(i)));
        }
        return new ObjetoJson().texto("algoritmo", r.getAlgoritmo()).texto("campo", campo)
                .entero("elementos", r.getLista().tamanio()).entero("comparaciones", r.getComparaciones())
                .entero("movimientos", r.getMovimientos()).texto("nombreMovimientos", r.nombreMovimientos())
                .decimal("ms", r.getMilisegundos()).booleano("ordenada", Ordenamiento.estaOrdenada(r.getLista(), comparador))
                .crudo("primeros", primeros).toString();
    }

    private ObjetoJson aeropuerto(Aeropuerto a) {
        return new ObjetoJson().texto("id", a.getId()).texto("iata", a.getIata()).texto("nombre", a.getNombre())
                .texto("ciudad", a.getCiudad()).texto("pais", a.getPais());
    }

    private String muestra(LectorJson json) throws Exception {
        DatosLimpios datos = datos();
        int cantidad = json.entero("cantidad", 12) == 20 ? 20 : 12;
        String campo = campo(json.texto("campo", "IATA"));
        long semilla = json.entero("semilla", (int) (System.currentTimeMillis() % 100000));
        Lista<Aeropuerto> conIata = datos.getAeropuertos().filtrar(DatosLimpios::tieneIata);
        Random azar = new Random(semilla);
        boolean[] usados = new boolean[conIata.tamanio()];
        ListaSecuencial<Aeropuerto> muestra = new ListaSecuencial<>();
        while (muestra.tamanio() < cantidad) {
            int indice = azar.nextInt(conIata.tamanio());
            if (!usados[indice]) {
                usados[indice] = true;
                muestra.insertarFinal(conIata.obtener(indice));
            }
        }
        Comparator<Aeropuerto> comparador = Comparadores.porCampo(campo);
        ResultadoOrdenamiento<Aeropuerto> burbuja = Ordenamiento.burbuja(muestra, comparador);
        ResultadoOrdenamiento<Aeropuerto> insercion = Ordenamiento.insercion(muestra, comparador);
        ArregloJson elementos = new ArregloJson();
        for (int i = 0; i < muestra.tamanio(); i++) {
            elementos.crudo(aeropuerto(muestra.obtener(i)));
        }
        return new ObjetoJson().entero("semilla", semilla).texto("campo", campo).crudo("muestra", elementos)
                .crudo("java", new ObjetoJson()
                        .crudo("burbuja", new ObjetoJson().entero("comparaciones", burbuja.getComparaciones()).entero("movimientos", burbuja.getMovimientos()))
                        .crudo("insercion", new ObjetoJson().entero("comparaciones", insercion.getComparaciones()).entero("movimientos", insercion.getMovimientos())))
                .toString();
    }

    private ArregloJson camino(String[] pasos) {
        ArregloJson camino = new ArregloJson();
        for (String paso : pasos) {
            int parentesis = paso.lastIndexOf(" (");
            String codigo = parentesis < 0 ? paso : paso.substring(0, parentesis);
            String lado = parentesis < 0 ? "vacío" : paso.substring(parentesis + 2, paso.length() - 1);
            camino.crudo(new ObjetoJson().texto("codigo", codigo).texto("lado", lado));
        }
        return camino;
    }

    private String buscar(String iata) throws Exception {
        if (iata.isEmpty()) {
            throw new ErrorWeb(400, "Escribe un código IATA");
        }
        DatosLimpios datos = datos();
        ListaEnlazada<Aeropuerto> lista = contexto.getSesion().listaEnlazada();
        boolean ordenoAhora = false;
        ListaSecuencial<Aeropuerto> ordenada;
        synchronized (this) {
            if (ordenadaPorIata == null) {
                ordenadaPorIata = Ordenamiento.insercion(datos.getAeropuertos(), Comparadores.AEROPUERTO_POR_IATA).getLista();
                ordenoAhora = true;
            }
            ordenada = ordenadaPorIata;
        }
        long inicio = System.nanoTime();
        ResultadoBusqueda<Aeropuerto> secuencial = Busqueda.secuencial(lista, a -> a.getIata().equals(iata));
        double msSecuencial = (System.nanoTime() - inicio) / 1_000_000.0;
        inicio = System.nanoTime();
        ResultadoBusqueda<Aeropuerto> binaria = Busqueda.binaria(ordenada, Aeropuerto.conIata(iata), Comparadores.AEROPUERTO_POR_IATA);
        double msBinaria = (System.nanoTime() - inicio) / 1_000_000.0;
        inicio = System.nanoTime();
        ResultadoBusqueda<Aeropuerto> arbol = Busqueda.porArbol(datos.getArbolIata(), Aeropuerto.conIata(iata));
        double msArbol = (System.nanoTime() - inicio) / 1_000_000.0;
        String resultado = new ObjetoJson().texto("iata", iata).booleano("encontrado", arbol.encontrado())
                .crudo("aeropuerto", arbol.encontrado() ? aeropuerto(arbol.getElemento()) : null)
                .booleano("ordenoAhora", ordenoAhora).entero("elementos", lista.tamanio())
                .crudo("secuencial", new ObjetoJson().entero("comparaciones", secuencial.getComparaciones()).decimal("ms", msSecuencial).entero("posicion", secuencial.getIndice() + 1))
                .crudo("binaria", new ObjetoJson().entero("comparaciones", binaria.getComparaciones()).decimal("ms", msBinaria).entero("indice", binaria.getIndice()))
                .crudo("bst", new ObjetoJson().entero("comparaciones", arbol.getComparaciones()).decimal("ms", msArbol).entero("profundidad", arbol.getIndice()))
                .crudo("camino", camino(datos.getArbolIata().caminoHacia(Aeropuerto.conIata(iata))))
                .toString();
        contexto.getReporte().registrar("busqueda", resultado);
        return resultado;
    }

    private synchronized String construir() throws Exception {
        DatosLimpios datos = datos();
        long inicio = System.nanoTime();
        ArbolBST<Aeropuerto> arbol = new ArbolBST<>(Comparadores.AEROPUERTO_POR_IATA);
        arbol.setEtiquetador(Aeropuerto::getIata);
        for (int i = 0; i < datos.getAeropuertos().tamanio(); i++) {
            Aeropuerto a = datos.getAeropuertos().obtener(i);
            if (DatosLimpios.tieneIata(a)) {
                arbol.insertar(a);
            }
        }
        arbolTrabajo = arbol;
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        return new ObjetoJson().decimal("ms", ms).crudo("arbol", medida(arbol)).toString();
    }

    private ArbolBST<Aeropuerto> arbol() throws ErrorWeb {
        if (arbolTrabajo == null) {
            throw new ErrorWeb(409, "Primero pulsa Construir para crear la copia de trabajo del BST");
        }
        return arbolTrabajo;
    }

    private synchronized String operar(String operacion, String iata) throws Exception {
        if (iata.isEmpty()) {
            throw new ErrorWeb(400, "Escribe un código IATA");
        }
        ArbolBST<Aeropuerto> arbol = arbol();
        Aeropuerto clave = Aeropuerto.conIata(iata);
        ObjetoJson antes = medida(arbol);
        String mensaje;
        boolean exito;
        int comparaciones = -1;
        String[] pasos = arbol.caminoHacia(clave);
        if (operacion.equals("insertar")) {
            Aeropuerto existente = datos().buscarPorIata(iata);
            exito = arbol.insertar(existente != null ? existente : clave);
            mensaje = exito ? "Insertado " + iata + " como hoja al final del camino." : iata + " ya existe: insertar devuelve falso y no se duplica.";
            pasos = arbol.caminoHacia(clave);
        } else if (operacion.equals("buscar")) {
            ResultadoBusqueda<Aeropuerto> r = arbol.buscar(clave);
            exito = r.encontrado();
            comparaciones = r.getComparaciones();
            mensaje = exito ? "Encontrado a profundidad " + r.getIndice() + ": " + descripcion(r.getElemento())
                    : iata + " no existe en el árbol.";
        } else if (operacion.equals("eliminar")) {
            exito = arbol.eliminar(clave);
            mensaje = exito ? "Eliminado " + iata + ". El caso (hoja, un hijo o dos hijos) lo resuelve ArbolBST.eliminar."
                    : iata + " no existe en el árbol: no se elimina nada.";
        } else {
            throw new ErrorWeb(404, "Operación desconocida: " + operacion);
        }
        return new ObjetoJson().texto("operacion", operacion).texto("iata", iata).booleano("exito", exito)
                .texto("mensaje", mensaje).entero("comparaciones", comparaciones).crudo("camino", camino(pasos))
                .crudo("antes", antes).crudo("despues", medida(arbol)).toString();
    }

    private synchronized String recorrido(String tipo, int limite) throws ErrorWeb {
        ArbolBST<Aeropuerto> arbol = arbol();
        int tope = Math.max(1, Math.min(limite, 500));
        Lista<Aeropuerto> resultado = tipo.equals("Preorden") ? arbol.preOrden(tope)
                : tipo.equals("Postorden") ? arbol.postOrden(tope) : arbol.inOrden(tope);
        ArregloJson codigos = new ArregloJson();
        for (int i = 0; i < resultado.tamanio(); i++) {
            codigos.texto(resultado.obtener(i).getIata());
        }
        return new ObjetoJson().texto("tipo", tipo).entero("limite", tope).entero("nodos", arbol.contarNodos())
                .crudo("codigos", codigos).toString();
    }

    private synchronized String medidas() throws ErrorWeb {
        ArbolBST<Aeropuerto> arbol = arbol();
        String resultado = new ObjetoJson().entero("nodos", arbol.contarNodos()).entero("altura", arbol.altura())
                .decimal("promedio", arbol.promedioComparaciones())
                .entero("alturaIdeal", (int) Math.floor(Math.log(arbol.contarNodos()) / Math.log(2)))
                .crudo("niveles", nodoJson(arbol.getRaiz(), 0)).toString();
        contexto.getReporte().registrar("bst", resultado);
        return resultado;
    }

    private String nodoJson(NodoArbol<Aeropuerto> nodo, int nivel) {
        if (nodo == null) {
            return "null";
        }
        if (nivel >= NIVELES_DIBUJO) {
            return new ObjetoJson().booleano("oculto", true).toString();
        }
        return new ObjetoJson().texto("c", nodo.getDato().getIata())
                .crudo("i", nodoJson(nodo.getIzquierdo(), nivel + 1))
                .crudo("d", nodoJson(nodo.getDerecho(), nivel + 1)).toString();
    }

    private String desbalance() throws Exception {
        DatosLimpios datos = datos();
        ArbolBST<Aeropuerto> porId = new ArbolBST<>(Comparadores.AEROPUERTO_POR_ID);
        long inicio = System.nanoTime();
        for (int i = 0; i < 1500; i++) {
            porId.insertar(datos.getAeropuertos().obtener(i));
        }
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        ResultadoBusqueda<Aeropuerto> ultimo = porId.buscar(datos.getAeropuertos().obtener(1499));
        ArregloJson cadena = new ArregloJson();
        NodoArbol<Aeropuerto> nodo = porId.getRaiz();
        for (int i = 0; i < 6 && nodo != null; i++) {
            cadena.texto(nodo.getDato().getId());
            nodo = nodo.getDerecho();
        }
        ArbolBST<Aeropuerto> iata = datos.getArbolIata();
        String resultado = new ObjetoJson()
                .crudo("porId", new ObjetoJson().entero("nodos", porId.contarNodos()).entero("altura", porId.altura())
                        .decimal("ms", ms).entero("comparacionesUltimo", ultimo.getComparaciones()).crudo("cadena", cadena)
                        .entero("alturaIdeal", (int) Math.floor(Math.log(1500) / Math.log(2))))
                .crudo("porIata", new ObjetoJson().entero("nodos", iata.contarNodos()).entero("altura", iata.altura())
                        .decimal("promedio", iata.promedioComparaciones())
                        .entero("alturaIdeal", (int) Math.floor(Math.log(iata.contarNodos()) / Math.log(2)))
                        .crudo("niveles", nodoJson(iata.getRaiz(), 1)))
                .toString();
        contexto.getReporte().registrar("desbalance", resultado);
        return resultado;
    }
}
