package UtilsRender;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector de JSON minimo para el subconjunto que devuelve un GLB.
 *
 * <p>No se aniade una dependencia por esto: el motor no usa ninguna y anadir
 * una solo para leer el manifiesto de un archivo de modelo no compensa. Se
 * soportan objetos, arrays, cadenas, numeros, booleanos y null, que es todo lo
 * que aparece en la parte {@code JSON} de un GLB.
 *
 * <p>Los valores se devuelven como {@link Map}, {@link List}, {@link String},
 * {@link Double}, {@link Boolean} o null, segun que datoJSON.
 */
public final class Json {

    private final String text;
    private int at;

    private Json(String text) {
        this.text = text;
    }

    /** Parsea un documento JSON. */
    public static Object parse(String text) {
        Json json = new Json(text);
        json.skipSpaces();
        Object value = json.readValue();
        json.skipSpaces();
        return value;
    }

    // ------------------------------------------------------------------
    // Acceso comodo
    // ------------------------------------------------------------------

    public static Map<String, Object> object(Object value) {
        return value instanceof Map ? castMap(value) : null;
    }

    public static List<Object> array(Object value) {
        return value instanceof List ? castList(value) : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object value) {
        return (List<Object>) value;
    }

    // ------------------------------------------------------------------
    // Lexer
    // ------------------------------------------------------------------

    private Object readValue() {
        char c = peek();
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                expect("true");
                return Boolean.TRUE;
            case 'f':
                expect("false");
                return Boolean.FALSE;
            case 'n':
                expect("null");
                return null;
            default:
                return readNumber();
        }
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        at++;                        // '{'
        skipSpaces();
        if (peek() == '}') {
            at++;
            return map;
        }
        while (true) {
            skipSpaces();
            String key = readString();
            skipSpaces();
            if (peek() != ':') {
                throw new IllegalArgumentException("Se esperaba ':' en " + at);
            }
            at++;
            skipSpaces();
            map.put(key, readValue());
            skipSpaces();
            char c = peek();
            at++;
            if (c == '}') {
                return map;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Se esperaba ',' o '}' en " + at);
            }
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        at++;                        // '['
        skipSpaces();
        if (peek() == ']') {
            at++;
            return list;
        }
        while (true) {
            skipSpaces();
            list.add(readValue());
            skipSpaces();
            char c = peek();
            at++;
            if (c == ']') {
                return list;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Se esperaba ',' o ']' en " + at);
            }
        }
    }

    private String readString() {
        if (peek() != '"') {
            throw new IllegalArgumentException("Se esperaba cadena en " + at);
        }
        at++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = text.charAt(at++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char escape = text.charAt(at++);
            switch (escape) {
                case '"':  sb.append('"');  break;
                case '\\': sb.append('\\'); break;
                case '/':  sb.append('/');  break;
                case 'b':  sb.append('\b'); break;
                case 'f':  sb.append('\f'); break;
                case 'n':  sb.append('\n'); break;
                case 'r':  sb.append('\r'); break;
                case 't':  sb.append('\t'); break;
                case 'u':
                    sb.append((char) Integer.parseInt(
                            text.substring(at, at + 4), 16));
                    at += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Escape desconocido: " + escape);
            }
        }
    }

    private Double readNumber() {
        int start = at;
        while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
            at++;
        }
        if (start == at) {
            throw new IllegalArgumentException("Numero vacio en " + start);
        }
        return Double.valueOf(text.substring(start, at));
    }

    private void expect(String word) {
        if (!text.startsWith(word, at)) {
            throw new IllegalArgumentException("Se esperaba " + word + " en " + at);
        }
        at += word.length();
    }

    private char peek() {
        return text.charAt(at);
    }

    private void skipSpaces() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
            at++;
        }
    }
}
