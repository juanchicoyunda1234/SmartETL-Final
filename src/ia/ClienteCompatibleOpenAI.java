package ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ClienteCompatibleOpenAI implements ProveedorIA {

    private final String url;
    private final String modelo;
    private final String clave;
    private final Duration espera;

    public ClienteCompatibleOpenAI(String url, String modelo, String clave, Duration espera) {
        this.url = url;
        this.modelo = modelo;
        this.clave = clave;
        this.espera = espera;
    }

    @Override
    public String enviar(String prompt) throws Exception {
        String cuerpo = "{\"model\":" + Json.cadena(modelo) + ",\"messages\":[{\"role\":\"user\",\"content\":"
                + Json.cadena(prompt) + "}]}";
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(url))
                .timeout(espera)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + clave)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> respuesta = HttpClient.newBuilder().connectTimeout(espera).build()
                .send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (respuesta.statusCode() != 200) {
            String mensaje = Json.valorDeCadena(respuesta.body(), "message", 0);
            if (mensaje == null) {
                String cuerpoError = respuesta.body().trim().replaceAll("\\s+", " ");
                mensaje = cuerpoError.length() > 160 ? cuerpoError.substring(0, 160) + "…" : cuerpoError;
            }
            throw new IOException("HTTP " + respuesta.statusCode() + (mensaje.isEmpty() ? "" : ": " + mensaje));
        }
        int mensaje = Json.posicion(respuesta.body(), "\"message\"\\s*:", 0);
        String texto = Json.valorDeCadena(respuesta.body(), "content", mensaje);
        if (texto == null) {
            String recibido = respuesta.body().trim().replaceAll("\\s+", " ");
            throw new IOException("la respuesta no tiene el formato chat/completions (revisa la URL): "
                    + (recibido.length() > 120 ? recibido.substring(0, 120) + "…" : recibido));
        }
        return texto;
    }
}
