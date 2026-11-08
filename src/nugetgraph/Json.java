package nugetgraph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private static final Map<Character, Character> ESCAPES = Map.of(
            '"', '"',
            '\\', '\\',
            '/', '/',
            'b', '\b',
            'f', '\f',
            'n', '\n',
            'r', '\r',
            't', '\t');
    private final String text;
    private int position;

    private Json(String text) { this.text = text; }

    public static Object parse(String text) {
        Json reader = new Json(text);
        Object value = reader.value();
        reader.whitespace();
        if (reader.position != text.length()) { throw reader.error("trailing JSON data"); }
        return value;
    }

    private Object value() {
        whitespace();
        if (position >= text.length()) { throw error("missing value"); }
        return switch (text.charAt(position)) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        position++;
        Map<String, Object> result = new LinkedHashMap<>();
        if (take('}')) { return result; }
        do {
            whitespace();
            if (position >= text.length() || text.charAt(position) != '"') { throw error("expected key"); }
            String key = string();
            if (!take(':')) { throw error("expected colon"); }
            if (result.containsKey(key)) { throw error("duplicate JSON key"); }
            result.put(key, value());
        } while (take(','));
        if (!take('}')) { throw error("expected closing brace"); }
        return result;
    }

    private List<Object> array() {
        position++;
        List<Object> result = new ArrayList<>();
        if (take(']')) { return result; }
        do { result.add(value()); } while (take(','));
        if (!take(']')) { throw error("expected closing bracket"); }
        return result;
    }

    private String string() {
        position++;
        StringBuilder result = new StringBuilder();
        while (position < text.length()) {
            char current = text.charAt(position++);
            if (current == '"') { return result.toString(); }
            if (current < ' ') { throw error("control character in string"); }
            result.append(current == '\\' ? escaped() : current);
        }
        throw error("unclosed JSON string");
    }

    private char escaped() {
        char character = nextCharacter();
        if (character == 'u') { return unicode(); }
        Character decoded = ESCAPES.get(character);
        if (decoded == null) { throw error("invalid escape"); }
        return decoded;
    }

    private char nextCharacter() {
        if (position >= text.length()) { throw error("missing escape"); }
        return text.charAt(position++);
    }

    private char unicode() {
        int end = position + 4;
        if (end > text.length()) { throw error("short Unicode escape"); }
        String digits = text.substring(position, end);
        if (!digits.matches("[a-fA-F0-9]{4}")) { throw error("invalid Unicode escape"); }
        position = end;
        return (char) Integer.parseInt(digits, 16);
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, position)) { throw error("invalid literal"); }
        position += word.length();
        return value;
    }

    private java.math.BigDecimal number() {
        var matcher = java.util.regex.Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
                .matcher(text.substring(position));
        if (!matcher.lookingAt()) { throw error("invalid number"); }
        position += matcher.end();
        return new java.math.BigDecimal(matcher.group());
    }

    private boolean take(char character) {
        whitespace();
        if (position < text.length() && text.charAt(position) == character) { position++; return true; }
        return false;
    }

    private void whitespace() {
        while (position < text.length() && " \n\t\r".indexOf(text.charAt(position)) >= 0) { position++; }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at JSON offset " + position);
    }
}
