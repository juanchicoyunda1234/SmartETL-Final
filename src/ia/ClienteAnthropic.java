package ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ClienteAnthropic implements ProveedorIA {

    public static final String VERSION_API = "2026-10-21";

    private final String url;
    private final String modelo;
    private final String clave;
    private final Duration espera;

    public ClienteAnthropic(String url, String modelo, String clave, Duration espera) {
        this.url = url;
        this.modelo = modelo;
        this.clave = clave;
        this.espera = espera;
    }

    @Override
    public String enviar(String prompt) throws Exception {
        String cuerpo = "{\"model\":" + Json.cadena(modelo) + ",\"max_tokens\":16000,\"messages\":[{\"role\":\"user\",\"content\":"
                + Json.cadena(prompt) + "}]}";
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(url))
                .timeout(espera)
                .header("content-type", "application/json")
                .header("x-api-key", clave)
                .header("anthropic-version", VERSION_API)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> respuesta = HttpClient.newBuilder().connectTimeout(espera).build()
                .send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (respuesta.statusCode() != 200) {
            String mensaje = Json.valorDeCadena(respuesta.body(), "message", 0);
            throw new IOException("HTTP " + respuesta.statusCode() + (mensaje == null ? "" : ": " + mensaje));
        }
        int bloqueTexto = Json.posicion(respuesta.body(), "\"type\"\\s*:\\s*\"text\"", 0);
        if (bloqueTexto < 0) {
            throw new IOException("La respuesta no trae un bloque de texto (stop_reason: "
                    + Json.valorDeCadena(respuesta.body(), "stop_reason", 0) + ")");
        }
        String texto = Json.valorDeCadena(respuesta.body(), "text", bloqueTexto + 6);
        if (texto == null) {
            throw new IOException("No se pudo leer el texto de la respuesta");
        }
        return texto;
    }
}
