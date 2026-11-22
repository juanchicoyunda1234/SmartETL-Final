package ia;

public class Evento {

    public static final int LIMITE_VISTA = 12;

    private final String tipo;
    private final String texto;
    private final String sujeto;
    private final String grupo;
    private final String[] elementos;
    private final String[] estado;
    private final int tamanioEstado;
    private final int nivel;

    public Evento(String tipo, String texto) {
        this(tipo, texto, "", "", new String[0], new String[0], 0, -1);
    }

    public Evento(String tipo, String texto, String sujeto, String grupo, String[] elementos,
                  String[] estado, int tamanioEstado, int nivel) {
        this.tipo = tipo;
        this.texto = texto;
        this.sujeto = sujeto;
        this.grupo = grupo;
        this.elementos = elementos;
        this.estado = estado;
        this.tamanioEstado = tamanioEstado;
        this.nivel = nivel;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTexto() {
        return texto;
    }

    public String getSujeto() {
        return sujeto;
    }

    public String getGrupo() {
        return grupo;
    }

    public String[] getElementos() {
        return elementos;
    }

    public String[] getEstado() {
        return estado;
    }

    public int getTamanioEstado() {
        return tamanioEstado;
    }

    public int getNivel() {
        return nivel;
    }

    public String estadoComoTexto() {
        return formatearEstado(estado, tamanioEstado);
    }

    public static String formatearEstado(String[] primeros, int total) {
        return "[" + unir(primeros, total) + "]";
    }

    public static String unir(String[] primeros, int total) {
        StringBuilder texto = new StringBuilder();
        int mostrar = Math.min(primeros.length, LIMITE_VISTA);
        for (int i = 0; i < mostrar; i++) {
            if (i > 0) {
                texto.append(", ");
            }
            texto.append(primeros[i]);
        }
        if (total > mostrar) {
            texto.append(" y ").append(total - mostrar).append(" más");
        }
        return texto.toString();
    }

    public String nombreElementos() {
        switch (tipo) {
            case "desencolar":
                return "vecinos nuevos";
            case "desapilar":
                return "vecinos apilados";
            case "insertar":
            case "buscar":
                return "camino";
            case "totales":
                return "procesados, válidos, inválidos, por tipo";
            default:
                return "datos";
        }
    }

    @Override
    public String toString() {
        StringBuilder linea = new StringBuilder(tipo).append(" | ").append(texto);
        if (nivel >= 0) {
            linea.append(" | nivel ").append(nivel);
        }
        if (elementos.length > 0) {
            linea.append(" | ").append(nombreElementos()).append(": ").append(unir(elementos, elementos.length));
        }
        if (estado.length > 0) {
            linea.append(" | estado: ").append(estadoComoTexto());
        } else if (grupo.equals("bst")) {
            linea.append(" | nodos en el árbol: ").append(tamanioEstado);
        }
        return linea.toString();
    }
}
