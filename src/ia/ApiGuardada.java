package ia;

public class ApiGuardada {

    private final String nombre;
    private final String proveedor;
    private final String modelo;
    private final String url;
    private final String clave;

    public ApiGuardada(String nombre, String proveedor, String modelo, String url, String clave) {
        this.nombre = nombre.trim();
        this.proveedor = proveedor;
        this.modelo = modelo.trim();
        this.url = url.trim();
        this.clave = clave;
    }

    public String getNombre() {
        return nombre;
    }

    public String getProveedor() {
        return proveedor;
    }

    public String getModelo() {
        return modelo;
    }

    public String getUrl() {
        return url;
    }

    public String getClave() {
        return clave;
    }

    public String descripcion() {
        return nombre + "  |  " + proveedor + "  |  " + (modelo.isEmpty() ? "(sin modelo)" : modelo);
    }

    @Override
    public String toString() {
        return descripcion();
    }
}
