package model;

public class ErrorETL {

    private final String fuente;
    private final int linea;
    private final String identificador;
    private final TipoError tipo;
    private final String motivo;
    private final String valorOriginal;

    public ErrorETL(String fuente, int linea, String identificador, TipoError tipo, String motivo, String valorOriginal) {
        this.fuente = fuente;
        this.linea = linea;
        this.identificador = identificador;
        this.tipo = tipo;
        this.motivo = motivo;
        this.valorOriginal = valorOriginal;
    }

    public String getFuente() {
        return fuente;
    }

    public int getLinea() {
        return linea;
    }

    public String getIdentificador() {
        return identificador;
    }

    public TipoError getTipo() {
        return tipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getValorOriginal() {
        return valorOriginal;
    }

    public String comoLinea() {
        return fuente + " | línea " + linea + " | " + identificador + " | " + tipo + " | " + motivo
                + " | valor original: " + valorOriginal;
    }

    @Override
    public String toString() {
        return tipo + " en " + fuente + " línea " + linea + " (" + identificador + ")";
    }
}
