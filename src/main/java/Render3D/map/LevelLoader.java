package Render3D.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Cargador de niveles desde archivos de texto en recursos (carpeta
 * {@code levels/}). Cada nivel usa este formato:
 * <pre>
 *   # comentario
 *   name: Mazmorra 1    # título que muestra el juego
 *   map:
 *   ‣...fila con glifos...
 * </pre>
 * Las líneas en blanco y las que empiezan por '#' se ignoran. Tras la línea
 * {@code map:} todo lo que sigue son filas del mapa (glifos de
 * {@link MapConfig.SymbolType}); las filas se rellenan con espacios hasta el
 * ancho máximo para garantizar una grilla rectangular.
 *
 * El orden de juego es por NOMBRE DE ARCHIVO ({@code level_01.txt} primero,
 * comparación por números), no por una directiva interna. La directiva
 * {@code name:} solo decide el título mostrado. {@code symbolsLevel.txt} y
 * {@code symbols_readme.properties} son documentación del editor y NO se
 * cuentan como niveles.
 */
public final class LevelLoader {

    /** Nivel por defecto que se carga al iniciar la partida. */
    public static final String DEFAULT_LEVEL = "levels/level_01.txt";

    /** Tiempo por defecto de un nivel (1 minuto) si no declara {@code time:}. */
    public static final int DEFAULT_TIME_SECONDS = 60;

    private static final String LEVEL_DIR = "levels";
    private static final String SYMBOLS_FILE = "symbolsLevel.txt";

    /** Referencia a un nivel: su título, tiempo declarado y la ruta del recurso. */
    public static final class LevelInfo {
        public final String name;
        public final String path;
        public final int timeSeconds;

        LevelInfo(String name, String path, int timeSeconds) {
            this.name = name;
            this.path = path;
            this.timeSeconds = timeSeconds;
        }
    }

    private LevelLoader() {
    }

    /**
     * Enumera los niveles disponibles en {@code levels/} ordenados por nombre
     * de archivo con comparación natural ({@code level_02.txt} antes que
     * {@code level_10.txt}). Funciona en disco (dev) y empaquetado en jar.
     * Excluye los símbolos del editor ({@code symbolsLevel.txt}). Si no hay
     * ninguno, devuelve {@link #DEFAULT_LEVEL}.
     */
    public static LevelInfo[] listLevels() {
        List<String> paths = new ArrayList<>();
        Enumeration<URL> resources;
        try {
            resources = LevelLoader.class.getClassLoader().getResources(LEVEL_DIR);
        } catch (IOException e) {
            return new LevelInfo[] {new LevelInfo(getName(DEFAULT_LEVEL), DEFAULT_LEVEL, DEFAULT_TIME_SECONDS)};
        }
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if ("file".equals(url.getProtocol())) {
                listLevelsFromDirectory(url, paths);
            } else if ("jar".equals(url.getProtocol())) {
                listLevelsFromJar(url, paths);
            }
        }
        if (paths.isEmpty()) {
            paths.add(DEFAULT_LEVEL);
        }

        paths.sort(LevelLoader::naturalCompare);
        LevelInfo[] levels = new LevelInfo[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            levels[i] = new LevelInfo(getName(path), path, timeSeconds(path));
        }
        return levels;
    }

    /**
     * Tiempo del nivel declarado con la directiva {@code time: mm:ss} dentro
     * del archivo.
     *
     * @return los segundos de la primera directiva {@code time:} en formato
     *         {@code mm:ss}, o {@link #DEFAULT_TIME_SECONDS} si no la declara.
     */
    public static int timeSeconds(String resourcePath) {
        InputStream is = LevelLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            return DEFAULT_TIME_SECONDS;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("time:")) {
                    int seconds = parseMmSs(t.substring("time:".length()).trim());
                    if (seconds > 0) {
                        return seconds;
                    }
                }
            }
        } catch (IOException ignored) {
            // Recurso ilegible: se usa el tiempo por defecto.
        }
        return DEFAULT_TIME_SECONDS;
    }

    /** Parsea {@code mm:ss} a segundos; devuelve -1 si el formato no vale. */
    private static int parseMmSs(String value) {
        String[] parts = value.split(":");
        if (parts.length == 2) {
            try {
                int minutes = Integer.parseInt(parts[0].trim());
                int seconds = Integer.parseInt(parts[1].trim());
                if (minutes >= 0 && seconds >= 0) {
                    return minutes * 60 + seconds;
                }
            } catch (NumberFormatException ignored) {
                // Formato inválido.
            }
        }
        return -1;
    }

    /**
     * Título del nivel declarado con la directiva {@code name:} dentro del
     * archivo.
     *
     * @return el texto de la primera directiva {@code name:}, o el nombre del
     *         archivo si no la declara.
     */
    public static String getName(String resourcePath) {
        InputStream is = LevelLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            return getFileName(resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("name:")) {
                    String name = t.substring("name:".length()).trim();
                    return name.isEmpty() ? getFileName(resourcePath) : name;
                }
            }
        } catch (IOException ignored) {
            // Recurso ilegible: cae al nombre de archivo.
        }
        return getFileName(resourcePath);
    }

    /** Nombre de archivo (sin ruta), fallback para el título. */
    private static String getFileName(String resourcePath) {
        int slash = resourcePath.lastIndexOf('/');
        return slash < 0 ? resourcePath : resourcePath.substring(slash + 1);
    }

    /** Comparación natural: chunks numéricos como números ("level_2" < "level_10"). */
    private static int naturalCompare(String a, String b) {
        String sa = getFileName(a);
        String sb = getFileName(b);
        int i = 0, j = 0;
        while (i < sa.length() && j < sb.length()) {
            char ca = sa.charAt(i);
            char cb = sb.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int i2 = i;
                while (i2 < sa.length() && Character.isDigit(sa.charAt(i2))) i2++;
                int j2 = j;
                while (j2 < sb.length() && Character.isDigit(sb.charAt(j2))) j2++;
                int na = Integer.parseInt(sa.substring(i, i2));
                int nb = Integer.parseInt(sb.substring(j, j2));
                if (na != nb) {
                    return na - nb;
                }
                i = i2;
                j = j2;
            } else {
                if (ca != cb) {
                    return ca - cb;
                }
                i++;
                j++;
            }
        }
        return sa.length() - sb.length();
    }

    private static void listLevelsFromDirectory(URL url, List<String> out) {
        try (java.util.stream.Stream<Path> dirFiles = Files.list(Paths.get(url.toURI()))) {
            dirFiles.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(f -> f.endsWith(".txt"))
                    .filter(f -> !f.equalsIgnoreCase(SYMBOLS_FILE))
                    .forEach(f -> out.add(LEVEL_DIR + "/" + f));
        } catch (IOException | URISyntaxException ignored) {
            // No se pudo listar este directorio: se omite.
        }
    }

    private static void listLevelsFromJar(URL url, List<String> out) {
        try {
            String spec = url.getFile();
            int bang = spec.indexOf('!');
            if (bang < 0) {
                return;
            }
            String jarPath = spec.substring(0, bang);
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring("file:".length());
            }
            try (JarFile jar = new JarFile(jarPath)) {
                jar.stream()
                        .map(JarEntry::getName)
                        .filter(n -> n.startsWith(LEVEL_DIR + "/"))
                        .filter(n -> n.endsWith(".txt"))
                        .filter(n -> !n.endsWith("/" + SYMBOLS_FILE))
                        .forEach(out::add);
            }
        } catch (IOException | SecurityException ignored) {
            // JAR ilegible: se omite.
        }
    }

    /**
     * Lee un nivel de texto desde el classpath (recurso, no ruta de disco).
     *
     * @param resourcePath camino relativo a la raíz de recursos
     *                      (p. ej. {@code "levels/level_01.txt"}).
     * @return las filas del mapa (todo lo que va después de {@code map:}).
     * @throws IllegalArgumentException si el recurso no existe o está vacío.
     */
    public static String[] load(String resourcePath) {
        List<String> rows = new ArrayList<>();
        InputStream is = LevelLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Nivel no encontrado en recursos: " + resourcePath);
        }
        boolean inMap = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String t = line.trim();
                if (t.startsWith("#") || t.startsWith("name:") || t.startsWith("time:")) {
                    continue;
                }
                if (t.startsWith("map:")) {
                    inMap = true;
                    continue;
                }
                if (inMap) {
                    rows.add(stripTrailingWhitespace(line));
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error leyendo el nivel: " + resourcePath, e);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Nivel vacío (falta 'map:'): " + resourcePath);
        }
        return rows.toArray(new String[0]);
    }

    /** Por soporte de glifos anchos, se conservan los espacios en blanco entre
     *  aberturas; solo se limpia el final de cada fila. */
    private static String stripTrailingWhitespace(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }
}