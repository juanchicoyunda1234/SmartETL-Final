package app.web;

import ia.Json;

public class ObjetoJson {

    private final StringBuilder texto = new StringBuilder("{");
    private boolean vacio = true;

    private ObjetoJson clave(String nombre) {
        if (!vacio) {
            texto.append(',');
        }
        vacio = false;
        texto.append(Json.cadena(nombre)).append(':');
        return this;
    }

    public ObjetoJson texto(String nombre, String valor) {
        clave(nombre).texto.append(valor == null ? "null" : Json.cadena(valor));
        return this;
    }

    public ObjetoJson entero(String nombre, long valor) {
        clave(nombre).texto.append(valor);
        return this;
    }

    public ObjetoJson decimal(String nombre, double valor) {
        clave(nombre).texto.append(Double.isFinite(valor) ? String.valueOf(Math.round(valor * 1000) / 1000.0) : "null");
        return this;
    }

    public ObjetoJson booleano(String nombre, boolean valor) {
        clave(nombre).texto.append(valor);
        return this;
    }

    public ObjetoJson crudo(String nombre, Object json) {
        clave(nombre).texto.append(json == null ? "null" : json.toString());
        return this;
    }

    @Override
    public String toString() {
        return texto + "}";
    }
}
