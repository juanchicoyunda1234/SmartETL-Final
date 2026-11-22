package app.web;

import estructuras.ListaEnlazada;
import estructuras.ListaSecuencial;
import estructuras.Nodo;

public class ReporteSesion {

    private String etl;
    private final ListaEnlazada<String> historialIa = new ListaEnlazada<>();
    private final ListaSecuencial<String> secciones = new ListaSecuencial<>();
    private final ListaSecuencial<String> valores = new ListaSecuencial<>();
    private int contadorIa;

    public synchronized void registrarEtl(String resumenJson) {
        etl = resumenJson;
    }

    public synchronized String getEtl() {
        return etl;
    }

    public synchronized void registrar(String seccion, String json) {
        for (int i = 0; i < secciones.tamanio(); i++) {
            if (secciones.obtener(i).equals(seccion)) {
                valores.reemplazar(i, json);
                return;
            }
        }
        secciones.insertarFinal(seccion);
        valores.insertarFinal(json);
    }

    public synchronized String obtener(String seccion) {
        for (int i = 0; i < secciones.tamanio(); i++) {
            if (secciones.obtener(i).equals(seccion)) {
                return valores.obtener(i);
            }
        }
        return null;
    }

    public synchronized String agregarIa(String resultadoJson) {
        contadorIa++;
        String id = "r" + contadorIa;
        historialIa.insertarFinal(new ObjetoJson().texto("id", id).crudo("resultado", resultadoJson).toString());
        return id;
    }

    public synchronized String historialIa() {
        ArregloJson lista = new ArregloJson();
        for (Nodo<String> n = historialIa.primerNodo(); n != null; n = n.getSiguiente()) {
            lista.crudo(n.getDato());
        }
        return lista.toString();
    }

    public synchronized ArregloJson todos(String prefijo) {
        ArregloJson lista = new ArregloJson();
        for (int i = 0; i < secciones.tamanio(); i++) {
            if (secciones.obtener(i).startsWith(prefijo)) {
                lista.crudo(valores.obtener(i));
            }
        }
        return lista;
    }

    public synchronized String reporte() {
        return new ObjetoJson()
                .entero("generado", System.currentTimeMillis())
                .crudo("etl", etl)
                .crudo("ordenamiento", obtener("ordenamiento"))
                .crudo("comparacionOrdenamiento", obtener("comparacionOrdenamiento"))
                .crudo("busqueda", obtener("busqueda"))
                .crudo("bst", obtener("bst"))
                .crudo("desbalance", obtener("desbalance"))
                .crudo("grafos", obtener("grafos"))
                .crudo("construidos", obtener("construidos"))
                .crudo("bfs", obtener("bfs"))
                .crudo("dfs", obtener("dfs"))
                .crudo("rutas", todos("ruta:"))
                .crudo("ciclo", obtener("ciclo"))
                .crudo("documentacion", obtener("documentacion"))
                .crudo("ia", historialIa())
                .toString();
    }
}
