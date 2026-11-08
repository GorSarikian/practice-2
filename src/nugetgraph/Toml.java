package nugetgraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Toml {
    private static final int QUOTE_COUNT = 2;
    private Toml() { }

    public static Map<String, Object> read(Path path) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        int number = 0;
        for (String raw : Files.readAllLines(path)) {
            number++;
            String line = withoutComment(raw.replace("\uFEFF", "")).trim();
            if (line.isEmpty()) { continue; }
            int equals = line.indexOf('=');
            if (equals < 1) { throw error(number, "expected key = value"); }
            String key = line.substring(0, equals).trim();
            if (!key.matches("[A-Za-z0-9_-]+")) { throw error(number, "invalid bare key"); }
            Object value = scalar(line.substring(equals + 1).trim(), number);
            if (values.putIfAbsent(key, value) != null) { throw error(number, "duplicate key: " + key); }
        }
        return values;
    }

    private static String withoutComment(String raw) {
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (quote == '"' && current == '\\') { escaped = true; continue; }
            if (current == quote) { quote = 0; continue; }
            if (quote == 0 && isQuote(current)) { quote = current; continue; }
            if (quote == 0 && current == '#') { return raw.substring(0, i); }
        }
        return raw;
    }

    private static boolean isQuote(char character) { return character == '"' || character == '\''; }

    private static Object scalar(String value, int line) {
        if (value.startsWith("\"")) { return basicString(value, line); }
        if (value.matches("'[^'\\r\\n]*'")) { return value.substring(1, value.length() - 1); }
        if (value.equals("true") || value.equals("false")) { return Boolean.valueOf(value); }
        if (value.matches("[+-]?(0|[1-9](_?[0-9])*)")) {
            try { return Integer.valueOf(value.replace("_", "")); }
            catch (NumberFormatException exception) { throw error(line, "integer outside supported range"); }
        }
        throw error(line, "expected quoted string, boolean or integer");
    }

    private static String basicString(String value, int line) {
        if (value.length() < QUOTE_COUNT || !value.endsWith("\"")) { throw error(line, "unclosed string"); }
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < value.length() - 1; i++) {
            char current = value.charAt(i);
            validateCharacter(current, line);
            if (current != '\\') { result.append(current); continue; }
            i++;
            if (i >= value.length() - 1) { throw error(line, "unfinished escape"); }
            char escape = value.charAt(i);
            if (escape == 'u' || escape == 'U') {
                int count = escape == 'u' ? 4 : 8;
                result.appendCodePoint(unicode(value, i + 1, count, line));
                i += count;
            } else { result.append(escape(escape, line)); }
        }
        return result.toString();
    }

    private static void validateCharacter(char character, int line) {
        if (character == '"' || character < ' ') { throw error(line, "invalid string character"); }
    }

    private static char escape(char character, int line) {
        return switch (character) {
            case '"', '\\' -> character;
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'b' -> '\b';
            case 'f' -> '\f';
            default -> throw error(line, "unsupported escape");
        };
    }

    private static int unicode(String value, int start, int count, int line) {
        try {
            String hex = value.substring(start, start + count);
            if (!hex.matches("[0-9a-fA-F]+")) { throw error(line, "invalid Unicode escape"); }
            int code = Integer.parseInt(hex, 16);
            if (!Character.isValidCodePoint(code)
                    || (code >= Character.MIN_SURROGATE && code <= Character.MAX_SURROGATE)) {
                throw error(line, "invalid Unicode scalar");
            }
            return code;
        } catch (IndexOutOfBoundsException | NumberFormatException exception) {
            throw error(line, "invalid Unicode escape");
        }
    }

    private static IllegalArgumentException error(int line, String message) {
        return new IllegalArgumentException("TOML line " + line + ": " + message);
    }
}
