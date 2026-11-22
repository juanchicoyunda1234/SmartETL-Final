package app.web;

import algoritmos.Ordenamiento;
import estructuras.Lista;
import estructuras.ListaEnlazada;
import estructuras.ListaSecuencial;
import estructuras.Nodo;
import estructuras.ResultadoBusqueda;
import etl.Extract;
import etl.FuenteDatos;
import etl.ModoCarga;
import etl.OpcionesCarga;
import etl.Proyecto;
import etl.ResultadoExtraccion;
import model.Registro;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class ApiDatos implements AreaApi {

    public static final int FILAS_POR_PAGINA = 50;

    private final ContextoWeb contexto;
    private Lista<Registro> cargada;
    private Lista<Registro> mostrada;
    private FuenteDatos fuente;
    private String filtro = "";

    public ApiDatos(ContextoWeb contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean atender(Peticion p) throws Exception {
        if (p.es("GET", "datos", "fuentes")) {
            Respuesta.json(p.getIntercambio(), 200, fuentes());
            return true;
        }
        if (p.es("GET", "datos", "archivos")) {
            Respuesta.json(p.getIntercambio(), 200, archivos());
            return true;
        }
        if (p.es("POST", "datos", "cargar")) {
            cargar(p);
            return true;
        }
        if (p.es("GET", "datos", "pagina")) {
            Respuesta.json(p.getIntercambio(), 200, vista(p.parametroEntero("n", 1)));
            return true;
        }
        if (p.es("POST", "datos", "buscar")) {
            Respuesta.json(p.getIntercambio(), 200, buscar(p.json()));
            return true;
        }
        if (p.es("POST", "datos", "filtrar")) {
            Respuesta.json(p.getIntercambio(), 200, filtrar(p.json()));
            return true;
        }
        if (p.es("POST", "datos", "todos")) {
            Respuesta.json(p.getIntercambio(), 200, todos());
            return true;
        }
        return false;
    }

    private String fuentes() {
        ArregloJson lista = new ArregloJson();
        FuenteDatos[] todas = FuenteDatos.todas();
        for (int i = 0; i < todas.length; i++) {
            lista.crudo(new ObjetoJson().entero("indice", i).texto("nombre", todas[i].getNombre())
                    .texto("archivo", todas[i].getArchivoPorDefecto())
                    .crudo("encabezados", ArregloJson.deTextos(todas[i].getEncabezados()))
                    .entero("columnaBusqueda", todas[i].getColumnaBusquedaPorDefecto()));
        }
        return lista.toString();
    }

    private String archivos() {
        ArregloJson lista = new ArregloJson();
        agregarCsv(lista, "data");
        agregarCsv(lista, "data/demo");
        return lista.toString();
    }

    private void agregarCsv(ArregloJson lista, String carpeta) {
        File[] archivos = Proyecto.getBase().resolve(carpeta).toFile().listFiles((dir, nombre) -> nombre.toLowerCase().endsWith(".csv"));
        if (archivos == null) {
            return;
        }
        ListaSecuencial<String> nombres = new ListaSecuencial<>();
        for (File archivo : archivos) {
            nombres.insertarFinal(archivo.getName());
        }
        ListaSecuencial<String> ordenados = Ordenamiento.insercion(nombres, String::compareTo).getLista();
        for (int i = 0; i < ordenados.tamanio(); i++) {
            File archivo = new File(Proyecto.getBase().resolve(carpeta).toFile(), ordenados.obtener(i));
            lista.crudo(new ObjetoJson().texto("ruta", carpeta + "/" + archivo.getName()).texto("carpeta", carpeta)
                    .entero("bytes", archivo.length()));
        }
    }

    private void cargar(Peticion p) throws Exception {
        LectorJson json = p.json();
        FuenteDatos[] todas = FuenteDatos.todas();
        int indiceFuente = json.entero("fuente", 0);
        if (indiceFuente < 0 || indiceFuente >= todas.length) {
            errorCampo(p, "fuente", "Fuente de datos desconocida");
            return;
        }
        FuenteDatos elegida = todas[indiceFuente];
        String archivo = json.texto("archivo", elegida.getArchivoPorDefecto()).trim();
        if (archivo.isEmpty()) {
            errorCampo(p, "archivo", "Escribe o elige un archivo CSV");
            return;
        }
        OpcionesCarga opciones;
        String modo = json.texto("modo", "TODOS");
        try {
            opciones = opciones(json, modo);
        } catch (CampoInvalido ex) {
            errorCampo(p, ex.campo, ex.getMessage());
            return;
        }
        Lista<Registro> destino = json.texto("estructura", "enlazada").equals("secuencial")
                ? new ListaSecuencial<>() : new ListaEnlazada<>();
        ResultadoExtraccion<Registro> resultado;
        try {
            resultado = Extract.extraer(Proyecto.ruta(archivo), elegida.getCamposEsperados(), elegida.getMapeador(), destino, opciones);
        } catch (NoSuchFileException ex) {
            errorCampo(p, "archivo", "No existe el archivo " + archivo + " en " + Proyecto.getBase());
            return;
        }
        synchronized (this) {
            cargada = destino;
            mostrada = destino;
            fuente = elegida;
            filtro = "";
        }
        ObjetoJson respuesta = new ObjetoJson()
                .texto("archivo", Paths.get(archivo).getFileName().toString())
                .texto("modo", opciones.resumen())
                .texto("estructura", destino.descripcion())
                .decimal("ms", resultado.getMilisegundos())
                .entero("totalLineas", resultado.getTotalLineas())
                .entero("seleccionadas", resultado.getLineasSeleccionadas())
                .entero("cargados", resultado.getCargados())
                .entero("noLeidas", resultado.getNoLeidas())
                .crudo("vista", vista(1));
        Respuesta.json(p.getIntercambio(), 200, respuesta);
    }

    private OpcionesCarga opciones(LectorJson json, String modo) throws CampoInvalido {
        ModoCarga elegido;
        try {
            elegido = ModoCarga.valueOf(modo);
        } catch (IllegalArgumentException ex) {
            throw new CampoInvalido("modo", "Modo de carga desconocido");
        }
        switch (elegido) {
            case PRIMEROS:
                int primeros = numero(json, "cantidad", "N");
                return validar(() -> OpcionesCarga.primeros(primeros), "cantidad");
            case RANGO:
                int desde = numero(json, "desde", "Desde");
                int hasta = numero(json, "hasta", "Hasta");
                return validar(() -> OpcionesCarga.rango(desde, hasta), desde < 1 ? "desde" : "hasta");
            case ALEATORIO:
                int cantidad = numero(json, "cantidad", "N");
                String textoSemilla = json.texto("semilla", "").trim();
                Long semilla = null;
                if (!textoSemilla.isEmpty()) {
                    try {
                        semilla = Long.parseLong(textoSemilla);
                    } catch (NumberFormatException ex) {
                        throw new CampoInvalido("semilla", "La semilla debe ser un número entero o quedar vacía");
                    }
                }
                Long semillaFinal = semilla;
                return validar(() -> OpcionesCarga.aleatorio(cantidad, semillaFinal), "cantidad");
            default:
                return OpcionesCarga.todos();
        }
    }

    private interface Creador {
        OpcionesCarga crear();
    }

    private OpcionesCarga validar(Creador creador, String campo) throws CampoInvalido {
        try {
            return creador.crear();
        } catch (IllegalArgumentException ex) {
            throw new CampoInvalido(campo, ex.getMessage());
        }
    }

    private int numero(LectorJson json, String clave, String nombre) throws CampoInvalido {
        String texto = json.texto(clave, String.valueOf(json.entero(clave, Integer.MIN_VALUE))).trim();
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            throw new CampoInvalido(clave, "El campo " + nombre + " debe ser un número entero");
        }
    }

    private synchronized String vista(int pagina) throws ErrorWeb {
        exigirCarga();
        int total = mostrada.tamanio();
        int paginas = Math.max(1, (total + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
        int actual = Math.min(Math.max(1, pagina), paginas);
        int desde = (actual - 1) * FILAS_POR_PAGINA;
        int hasta = Math.min(total, desde + FILAS_POR_PAGINA);
        return new ObjetoJson()
                .texto("fuente", fuente.getNombre())
                .crudo("encabezados", ArregloJson.deTextos(fuente.getEncabezados()))
                .entero("columnaBusqueda", fuente.getColumnaBusquedaPorDefecto())
                .entero("total", total)
                .entero("totalCargados", cargada.tamanio())
                .texto("filtro", filtro)
                .entero("pagina", actual)
                .entero("paginas", paginas)
                .entero("desde", total == 0 ? 0 : desde + 1)
                .entero("hasta", hasta)
                .crudo("filas", filas(desde, hasta))
                .toString();
    }

    private ArregloJson filas(int desde, int hasta) {
        ArregloJson filas = new ArregloJson();
        if (mostrada instanceof ListaEnlazada) {
            Nodo<Registro> nodo = ((ListaEnlazada<Registro>) mostrada).primerNodo();
            for (int i = 0; i < desde && nodo != null; i++) {
                nodo = nodo.getSiguiente();
            }
            for (int i = desde; i < hasta && nodo != null; i++) {
                filas.crudo(fila(nodo.getDato()));
                nodo = nodo.getSiguiente();
            }
        } else {
            for (int i = desde; i < hasta; i++) {
                filas.crudo(fila(mostrada.obtener(i)));
            }
        }
        return filas;
    }

    private ArregloJson fila(Registro registro) {
        ArregloJson celdas = new ArregloJson();
        for (int c = 0; c < registro.numeroCampos(); c++) {
            celdas.texto(registro.valor(c));
        }
        return celdas;
    }

    private void exigirCarga() throws ErrorWeb {
        if (cargada == null) {
            throw new ErrorWeb(409, "Primero carga un archivo");
        }
    }

    private int columna(LectorJson json) throws ErrorWeb {
        int columna = json.entero("columna", -1);
        if (columna < 0 || columna >= fuente.getEncabezados().length) {
            throw new ErrorWeb(400, "Columna desconocida");
        }
        return columna;
    }

    private String valor(LectorJson json) throws ErrorWeb {
        String valor = json.texto("valor", "").trim();
        if (valor.isEmpty()) {
            throw new ErrorWeb(400, "Escribe el valor que quieres buscar");
        }
        return valor;
    }

    private synchronized String buscar(LectorJson json) throws ErrorWeb {
        exigirCarga();
        int columna = columna(json);
        String valor = valor(json);
        long inicio = System.nanoTime();
        ResultadoBusqueda<Registro> resultado = mostrada.buscar(r -> r.valor(columna).trim().equalsIgnoreCase(valor));
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        int pagina = resultado.encontrado() ? resultado.getIndice() / FILAS_POR_PAGINA + 1 : 1;
        return new ObjetoJson()
                .booleano("encontrado", resultado.encontrado())
                .entero("posicion", resultado.getIndice() + 1)
                .entero("comparaciones", resultado.getComparaciones())
                .decimal("ms", ms)
                .texto("columna", fuente.getEncabezados()[columna])
                .texto("valor", valor)
                .entero("filaEnPagina", resultado.encontrado() ? resultado.getIndice() % FILAS_POR_PAGINA : -1)
                .crudo("vista", vista(pagina))
                .toString();
    }

    private synchronized String filtrar(LectorJson json) throws ErrorWeb {
        exigirCarga();
        int columna = columna(json);
        String valor = valor(json);
        String minuscula = valor.toLowerCase();
        long inicio = System.nanoTime();
        Lista<Registro> coincidencias = cargada.filtrar(r -> r.valor(columna).toLowerCase().contains(minuscula));
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        mostrada = coincidencias;
        filtro = fuente.getEncabezados()[columna] + " contiene \"" + valor + "\"";
        return new ObjetoJson()
                .entero("coincidencias", coincidencias.tamanio())
                .entero("comparaciones", cargada.tamanio())
                .decimal("ms", ms)
                .crudo("vista", vista(1))
                .toString();
    }

    private synchronized String todos() throws ErrorWeb {
        exigirCarga();
        mostrada = cargada;
        filtro = "";
        return new ObjetoJson().crudo("vista", vista(1)).toString();
    }

    private void errorCampo(Peticion p, String campo, String mensaje) throws IOException {
        Respuesta.json(p.getIntercambio(), 400, new ObjetoJson().texto("error", mensaje).texto("campo", campo));
    }

    private static class CampoInvalido extends Exception {

        private final String campo;

        CampoInvalido(String campo, String mensaje) {
            super(mensaje);
            this.campo = campo;
        }
    }
}
