package app.web;

import ia.Json;

public class ArregloJson {

    private final StringBuilder texto = new StringBuilder("[");
    private boolean vacio = true;

    private ArregloJson separar() {
        if (!vacio) {
            texto.append(',');
        }
        vacio = false;
        return this;
    }

    public ArregloJson texto(String valor) {
        separar().texto.append(valor == null ? "null" : Json.cadena(valor));
        return this;
    }

    public ArregloJson entero(long valor) {
        separar().texto.append(valor);
        return this;
    }

    public ArregloJson crudo(Object json) {
        separar().texto.append(json == null ? "null" : json.toString());
        return this;
    }

    public static ArregloJson deTextos(String[] valores) {
        ArregloJson arreglo = new ArregloJson();
        for (String valor : valores) {
            arreglo.texto(valor);
        }
        return arreglo;
    }

    @Override
    public String toString() {
        return texto + "]";
    }
}
