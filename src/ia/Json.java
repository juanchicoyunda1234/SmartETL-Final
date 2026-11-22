package ia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Json {

    private Json() {
    }

    public static String cadena(String texto) {
        StringBuilder resultado = new StringBuilder("\"");
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"':
                    resultado.append("\\\"");
                    break;
                case '\\':
                    resultado.append("\\\\");
                    break;
                case '\n':
                    resultado.append("\\n");
                    break;
                case '\r':
                    resultado.append("\\r");
                    break;
                case '\t':
                    resultado.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) c));
                    } else {
                        resultado.append(c);
                    }
            }
        }
        return resultado.append('"').toString();
    }

    public static String valorDeCadena(String json, String clave, int desde) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(clave) + "\"\\s*:\\s*\"").matcher(json);
        if (!m.find(Math.max(0, desde))) {
            return null;
        }
        return leerCadena(json, m.end());
    }

    public static int posicion(String json, String patron, int desde) {
        Matcher m = Pattern.compile(patron).matcher(json);
        return m.find(Math.max(0, desde)) ? m.start() : -1;
    }

    private static String leerCadena(String json, int inicio) {
        StringBuilder resultado = new StringBuilder();
        for (int i = inicio; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                return resultado.toString();
            }
            if (c != '\\') {
                resultado.append(c);
                continue;
            }
            i++;
            char escape = json.charAt(i);
            switch (escape) {
                case 'n':
                    resultado.append('\n');
                    break;
                case 't':
                    resultado.append('\t');
                    break;
                case 'r':
                    resultado.append('\r');
                    break;
                case 'b':
                    resultado.append('\b');
                    break;
                case 'f':
                    resultado.append('\f');
                    break;
                case 'u':
                    resultado.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                    i += 4;
                    break;
                default:
                    resultado.append(escape);
            }
        }
        return resultado.toString();
    }
}
