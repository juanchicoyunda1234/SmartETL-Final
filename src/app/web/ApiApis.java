package app.web;

import ia.AlmacenApis;
import ia.ApiGuardada;
import ia.ConfiguracionIA;

import java.io.IOException;

public class ApiApis implements AreaApi {

    private final ContextoWeb contexto;
    private final ApiIa ia;

    public ApiApis(ContextoWeb contexto, ApiIa ia) {
        this.contexto = contexto;
        this.ia = ia;
    }

    @Override
    public boolean atender(Peticion p) throws Exception {
        if (p.es("GET", "apis")) {
            Respuesta.json(p.getIntercambio(), 200, lista());
            return true;
        }
        if (p.es("POST", "apis")) {
            guardar(p, p.json());
            return true;
        }
        if (p.es("DELETE", "apis", "*")) {
            String nombre = p.parte(1);
            if (!almacen().eliminar(nombre)) {
                throw new ErrorWeb(404, "No existe la API guardada " + nombre);
            }
            contexto.getSesion().avisarCambioApis();
            Respuesta.json(p.getIntercambio(), 200, lista());
            return true;
        }
        if (p.es("POST", "apis", "*", "usar")) {
            contexto.getTareas().exigirLibre();
            ApiGuardada api = almacen().buscar(p.parte(1));
            if (api == null) {
                throw new ErrorWeb(404, "No existe la API guardada " + p.parte(1));
            }
            ia.usarGuardada(api);
            Respuesta.json(p.getIntercambio(), 200, ia.configuracion());
            return true;
        }
        return false;
    }

    private AlmacenApis almacen() throws ErrorWeb {
        AlmacenApis almacen = contexto.getSesion().getApisGuardadas();
        if (almacen == null) {
            throw new ErrorWeb(500, contexto.getSesion().getErrorApis());
        }
        return almacen;
    }

    private String lista() {
        AlmacenApis almacen = contexto.getSesion().getApisGuardadas();
        ArregloJson apis = new ArregloJson();
        if (almacen != null) {
            for (int i = 0; i < almacen.getApis().tamanio(); i++) {
                ApiGuardada api = almacen.getApis().obtener(i);
                apis.crudo(new ObjetoJson().texto("nombre", api.getNombre()).texto("proveedor", api.getProveedor())
                        .texto("modelo", api.getModelo()).texto("url", api.getUrl()));
            }
        }
        return new ObjetoJson().texto("archivo", AlmacenApis.archivoPorDefecto().toString())
                .texto("error", almacen == null ? contexto.getSesion().getErrorApis() : "")
                .crudo("apis", apis).toString();
    }

    private void guardar(Peticion p, LectorJson json) throws Exception {
        String nombre = json.texto("nombre", "").trim();
        String proveedor = json.texto("proveedor", "Gemini");
        String modelo = json.texto("modelo", "").trim();
        String url = json.texto("url", "").trim();
        String clave = json.texto("clave", "").trim();
        String original = json.texto("original", "").trim();
        if (nombre.isEmpty()) {
            errorCampo(p, "nombre", "Escribe un nombre para la API");
            return;
        }
        if (nombre.contains("/")) {
            errorCampo(p, "nombre", "El nombre no puede tener el carácter /");
            return;
        }
        if (!ApiIa.esProveedor(proveedor)) {
            errorCampo(p, "proveedor", "Proveedor desconocido: " + proveedor);
            return;
        }
        if (modelo.isEmpty()) {
            errorCampo(p, "modelo", "Escribe el modelo, " + ConfiguracionIA.ayudaModelo(proveedor));
            return;
        }
        AlmacenApis almacen = almacen();
        if (clave.isEmpty()) {
            ApiGuardada anterior = almacen.buscar(original.isEmpty() ? nombre : original);
            if (anterior == null) {
                errorCampo(p, "clave", "Escribe la clave: " + ConfiguracionIA.ayudaClave(proveedor));
                return;
            }
            clave = anterior.getClave();
        }
        if (!original.isEmpty() && !original.equals(nombre) && almacen.buscar(nombre) != null) {
            errorCampo(p, "nombre", "Ya existe otra API llamada " + nombre);
            return;
        }
        almacen.guardar(new ApiGuardada(nombre, proveedor, modelo, url.isEmpty() ? ConfiguracionIA.urlDe(proveedor) : url, clave));
        if (!original.isEmpty() && !original.equals(nombre)) {
            almacen.eliminar(original);
        }
        contexto.getSesion().avisarCambioApis();
        Respuesta.json(p.getIntercambio(), 200, lista());
    }

    private void errorCampo(Peticion p, String campo, String mensaje) throws IOException {
        Respuesta.json(p.getIntercambio(), 400, new ObjetoJson().texto("error", mensaje).texto("campo", campo));
    }
}
