package ia;

public class Mermaid {

    private final StringBuilder texto = new StringBuilder("flowchart TD\n");
    private int contador;
    private String ultimo;

    public static String limpiar(String etiqueta) {
        return etiqueta.replace("\"", "'").replace("#", "Nº").replace("\n", "<br/>");
    }

    public String nodo(String apertura, String etiqueta, String cierre) {
        String id = "N" + contador;
        contador++;
        texto.append("    ").append(id).append(apertura).append('"').append(limpiar(etiqueta)).append('"')
                .append(cierre).append('\n');
        return id;
    }

    public String inicio(String etiqueta) {
        return encadenar(nodo("([", etiqueta, "])"));
    }

    public String caja(String etiqueta) {
        return encadenar(nodo("[", etiqueta, "]"));
    }

    public String decision(String etiqueta) {
        return encadenar(nodo("{", etiqueta, "}"));
    }

    public String fin(String etiqueta, String rotulo) {
        String id = nodo("([", etiqueta, "])");
        enlazar(ultimo, id, rotulo);
        ultimo = id;
        return id;
    }

    public String encadenar(String id) {
        if (ultimo != null) {
            enlazar(ultimo, id, null);
        }
        ultimo = id;
        return id;
    }

    public void enlazar(String desde, String hasta, String rotulo) {
        texto.append("    ").append(desde);
        if (rotulo == null || rotulo.isEmpty()) {
            texto.append(" --> ");
        } else {
            texto.append(" -->|\"").append(limpiar(rotulo)).append("\"| ");
        }
        texto.append(hasta).append('\n');
    }

    public void linea(String linea) {
        texto.append("    ").append(linea).append('\n');
    }

    public void clic(String id, String archivo) {
        texto.append("    click ").append(id).append(" \"").append(archivo).append("\" \"Abrir detalle\"\n");
    }

    public void setUltimo(String id) {
        this.ultimo = id;
    }

    public String getUltimo() {
        return ultimo;
    }

    @Override
    public String toString() {
        return texto.toString();
    }
}
