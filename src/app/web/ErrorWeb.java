package app.web;

public class ErrorWeb extends Exception {

    private final int codigo;

    public ErrorWeb(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
