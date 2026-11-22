package ia;

import java.time.Duration;

public class ConfiguracionIA {

    public static final String GITHUB = "GitHub Models (Copilot)";
    public static final String[] PROVEEDORES = {"Gemini", "Claude", "OpenAI", "GLM", GITHUB};
    public static final String MODO_API = "API en vivo + respaldo local";
    public static final String MODO_LOCAL = "Solo local";
    public static final Duration ESPERA = Duration.ofSeconds(30);

    private String proveedor = "Gemini";
    private String modelo = "";
    private String url = "";
    private char[] clave = new char[0];
    private String modo = MODO_API;

    public static String urlDe(String proveedor) {
        switch (proveedor) {
            case "Claude":
                return "https://api.anthropic.com/v1/messages";
            case "OpenAI":
                return "https://api.openai.com/v1/chat/completions";
            case "GLM":
                return "https://api.z.ai/api/paas/v4/chat/completions";
            case GITHUB:
                return "https://models.github.ai/inference/chat/completions";
            default:
                return "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        }
    }

    public static String variableDeEntorno(String proveedor) {
        switch (proveedor) {
            case "Claude":
                return "ANTHROPIC_API_KEY";
            case "OpenAI":
                return "OPENAI_API_KEY";
            case "GLM":
                return "GLM_API_KEY";
            case GITHUB:
                return "GITHUB_TOKEN";
            default:
                return "GEMINI_API_KEY";
        }
    }

    public static String claveDeEntorno(String proveedor) {
        String valor = System.getenv(variableDeEntorno(proveedor));
        return valor == null ? "" : valor.trim();
    }

    public static String ayudaModelo(String proveedor) {
        if (proveedor.equals(GITHUB)) {
            return "formato publicador/modelo, ej.: openai/gpt-4.1-mini";
        }
        return "nombre exacto según la documentación del proveedor";
    }

    public static String ayudaClave(String proveedor) {
        if (proveedor.equals(GITHUB)) {
            return "token personal de GitHub (PAT) con permiso models:read";
        }
        if (proveedor.equals("GLM")) {
            return "si la clave es de la plataforma de China, usa https://open.bigmodel.cn/api/paas/v4/chat/completions";
        }
        return "clave de API del proveedor";
    }

    public ProveedorIA crearProveedor() {
        String texto = new String(clave);
        String destino = getUrl();
        if (proveedor.equals("Claude")) {
            return new ClienteAnthropic(destino, modelo, texto, ESPERA);
        }
        return new ClienteCompatibleOpenAI(destino, modelo, texto, ESPERA);
    }

    public String getUrl() {
        return url.isEmpty() ? urlDe(proveedor) : url;
    }

    public void setUrl(String url) {
        this.url = url == null ? "" : url.trim();
    }

    public boolean tieneClave() {
        return clave.length > 0;
    }

    public boolean soloLocal() {
        return modo.equals(MODO_LOCAL);
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo == null ? "" : modelo.trim();
    }

    public void setClave(char[] clave) {
        this.clave = clave == null ? new char[0] : clave;
    }

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }
}
