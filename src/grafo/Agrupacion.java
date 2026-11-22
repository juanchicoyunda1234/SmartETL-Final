package grafo;

import estructuras.ListaSecuencial;

public class Agrupacion {

    private final ListaSecuencial<String> claves = new ListaSecuencial<>();
    private final ListaSecuencial<Integer> conteos = new ListaSecuencial<>();
    private final ListaSecuencial<String> detalles = new ListaSecuencial<>();

    public void agregar(String clave, String detalle) {
        for (int i = 0; i < claves.tamanio(); i++) {
            if (claves.obtener(i).equals(clave)) {
                conteos.reemplazar(i, conteos.obtener(i) + 1);
                detalles.reemplazar(i, detalles.obtener(i) + ", " + detalle);
                return;
            }
        }
        claves.insertarFinal(clave);
        conteos.insertarFinal(1);
        detalles.insertarFinal(detalle);
    }

    public int tamanio() {
        return claves.tamanio();
    }

    public String clave(int indice) {
        return claves.obtener(indice);
    }

    public int conteo(int indice) {
        return conteos.obtener(indice);
    }

    public String detalle(int indice) {
        return detalles.obtener(indice);
    }
}
