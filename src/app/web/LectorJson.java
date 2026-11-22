package app.web;

import estructuras.ListaSecuencial;
import ia.Json;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LectorJson {

    private final String json;

    public LectorJson(String json) {
        this.json = json == null ? "" : json;
    }

    public String texto(String clave, String porDefecto) {
        String valor = Json.valorDeCadena(json, clave, 0);
        return valor == null ? porDefecto : valor;
    }

    public int entero(String clave, int porDefecto) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(clave) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : porDefecto;
    }

    public boolean booleano(String clave, boolean porDefecto) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(clave) + "\"\\s*:\\s*(true|false)").matcher(json);
        return m.find() ? m.group(1).equals("true") : porDefecto;
    }

    public String[] textos(String clave) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(clave) + "\"\\s*:\\s*\\[").matcher(json);
        ListaSecuencial<String> valores = new ListaSecuencial<>();
        if (m.find()) {
            int i = m.end();
            while (i < json.length() && json.charAt(i) != ']') {
                if (json.charAt(i) == '"') {
                    StringBuilder valor = new StringBuilder();
                    i = leerCadena(i + 1, valor);
                    valores.insertarFinal(valor.toString());
                }
                i++;
            }
        }
        String[] resultado = new String[valores.tamanio()];
        for (int k = 0; k < resultado.length; k++) {
            resultado[k] = valores.obtener(k);
        }
        return resultado;
    }

    private int leerCadena(int inicio, StringBuilder destino) {
        int i = inicio;
        while (i < json.length() && json.charAt(i) != '"') {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char escape = json.charAt(i + 1);
                if (escape == 'u' && i + 5 < json.length()) {
                    destino.append((char) Integer.parseInt(json.substring(i + 2, i + 6), 16));
                    i += 6;
                    continue;
                }
                destino.append(escape == 'n' ? '\n' : escape == 't' ? '\t' : escape);
                i += 2;
                continue;
            }
            destino.append(c);
            i++;
        }
        return i;
    }
}
