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

import org.joml.Vector3f;

/**
 * Cargador de niveles desde archivos de texto en recursos (carpeta
 * {@code levels/}). Cada nivel usa este formato:
 * <pre>
 *   # comentario
 *   name: Mazmorra 1    # título que muestra el juego
 *   time: 02:30
 *   ambient_light: dim      # dark | dim | normal | bright
 *   view_distance: normal   # low | normal | high
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
    private static final String LEVEL_TYPES_DIR = "level_types";
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
        int seconds = parseMmSs(valueOf(readLines(resourcePath), "time"));
        return seconds > 0 ? seconds : DEFAULT_TIME_SECONDS;
    }

    /** Parsea {@code mm:ss} a segundos; devuelve -1 si el formato no vale. */
    private static int parseMmSs(String value) {
        if (value == null) {
            return -1;
        }
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
     * archivo. Comparte el recorte de comentarios con {@link #loadLevel}, para
     * que el nombre del menú y el del nivel cargado nunca discrepen.
     *
     * @return el texto de la primera directiva {@code name:}, o el nombre del
     *         archivo si no la declara.
     */
    public static String getName(String resourcePath) {
        String name = valueOf(readLines(resourcePath), "name");
        return name == null ? getFileName(resourcePath) : name;
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
     * Atajo a {@link #loadLevel(String)} para quien solo quiere las filas.
     *
     * @param resourcePath camino relativo a la raíz de recursos
     *                      (p. ej. {@code "levels/level_01.txt"}).
     * @return las filas del mapa, ya rellenadas a rectángulo.
     * @throws IllegalArgumentException si el recurso no existe o está vacío.
     */
    public static String[] load(String resourcePath) {
        return loadLevel(resourcePath).toRows();
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

    // ---------------------------------------------------------------------
    // Parser por fases
    // ---------------------------------------------------------------------

    /**
     * Lee un nivel completo y lo resuelve en tres fases, en este orden:
     *
     * <ol>
     *   <li><b>Preset</b>: si el archivo declara {@code level_type: <nombre>}, se
     *       abre {@code level_types/<nombre>.txt} y se toman sus texturas de
     *       ambiente y sus colores de cielo. Sin esta linea el nivel usa el
     *       preset por defecto y sigue funcionando.</li>
     *   <li><b>Metadata</b>: {@code name:}, {@code time:}, {@code ambient_light:}
     *       y {@code view_distance:}. Las dos ultimas se traducen con
     *       {@link Atmosphere} y quedan como numeros.</li>
     *   <li><b>Mapa</b>: todo lo que sigue a {@code map:} pasa a
     *       {@code char[][]}, rellenando con espacios hasta un rectangulo.</li>
     * </ol>
     *
     * @param resourcePath ruta del recurso (p. ej. {@code levels/level_01.txt}).
     * @return el nivel resuelto, con metadata ya traducida.
     * @throws IllegalArgumentException si el recurso no existe o no trae mapa.
     */
    public static LevelData loadLevel(String resourcePath) {
        List<String> lines = readLines(resourcePath);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Nivel vacío: " + resourcePath);
        }

        // Fase 1: preset heredado de level_types/.
        LevelTypePreset preset = null;
        String presetName = valueOf(lines, "level_type");
        if (presetName != null) {
            preset = loadPreset(presetName);
        }
        if (preset == null) {
            preset = LevelTypePreset.defaultPreset("default");
        }

        // Fase 2: metadata propia del nivel.
        String name = valueOf(lines, "name");
        if (name == null || name.isEmpty()) {
            name = getFileName(resourcePath);
        }
        int time = parseMmSs(valueOf(lines, "time"));
        if (time <= 0) {
            time = DEFAULT_TIME_SECONDS;
        }
        float ambient = Atmosphere.ambientLight(valueOf(lines, "ambient_light"));
        float viewDistance = Atmosphere.viewDistance(valueOf(lines, "view_distance"));

        // Fase 3: matriz del mapa.
        char[][] map = toMatrix(rowsAfterMap(lines));

        return new LevelData(resourcePath, name, time, ambient, viewDistance, preset, map);
    }

    /**
     * Fase 1: carga {@code level_types/<nombre>.txt}. El preset admite
     * {@code floor_texture}, {@code ceiling_texture}, {@code sky_type},
     * {@code sky_speed}, {@code sky_horizon_color} y {@code sky_zenith_color};
     * cualquiera puede faltar y se hereda del preset por defecto. Los colores
     * se escriben como {@code #RRGGBB} o como tres floats separados por comas.
     */
    private static LevelTypePreset loadPreset(String presetName) {
        String path = LEVEL_TYPES_DIR + "/" + presetName + ".txt";
        List<String> lines = readLines(path);
        LevelTypePreset base = LevelTypePreset.defaultPreset(presetName);
        if (lines.isEmpty()) {
            System.err.println("[LevelLoader] Preset no encontrado, uso el por defecto: " + path);
            return base;
        }

        String floor = valueOf(lines, "floor_texture");
        String ceiling = valueOf(lines, "ceiling_texture");
        // Opcional: si el preset no la fija, el muro sigue siendo el global.
        String wall = valueOf(lines, "wall_texture");
        String skyType = valueOf(lines, "sky_type");
        if (skyType == null) {
            skyType = base.getSkyType();
        } else if (!LevelTypePreset.isKnownSkyType(skyType)) {
            // El shader solo implementa un perfil de cielo hoy. avisar es
            // mejor que ignorar un typo en silencio y pintar de otra cosa.
            System.err.println("[LevelLoader] sky_type desconocido en " + path + ": " + skyType
                    + " (se usa " + base.getSkyType() + ")");
            skyType = base.getSkyType();
        }
        float skySpeed = parseFloatOr(valueOf(lines, "sky_speed"), base.getSkySpeed());
        Vector3f horizon = parseColor(valueOf(lines, "sky_horizon_color"), base.getSkyHorizonColor());
        Vector3f zenith = parseColor(valueOf(lines, "sky_zenith_color"), base.getSkyZenithColor());

        return new LevelTypePreset(presetName, floor, ceiling, wall, skyType, skySpeed, horizon, zenith);
    }

    /** Lee todas las lineas de un recurso del classpath. */
    private static List<String> readLines(String resourcePath) {
        List<String> lines = new ArrayList<>();
        InputStream is = LevelLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Error leyendo el nivel: " + resourcePath, e);
        }
        return lines;
    }

    /**
     * Devuelve el valor de la primera directiva {@code clave: valor}, o
     * {@code null} si no aparece. Ignora comentarios y lineas en blanco, y
     * tambien el comentario que va pegado al valor
     * ({@code name: Sala 1  # el titulo}), que es parte del formato. Un
     * {@code #} pegado a una palabra se respeta, para que un nombre como
     * {@code Sala #3} no se corte.
     */
    private static String valueOf(List<String> lines, String key) {
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            if (t.startsWith(key + ":")) {
                String value = stripInlineComment(t.substring(key.length() + 1)).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Corta el comentario en linea de un valor ya recortado.
     *
     * <p>Regla: un {@code #} seguido de espacio abre comentario, salvo si es
     * el primer caracter del valor, porque ahi es un color hexadecimal
     * ({@code sky_horizon_color: #112233}) y no una nota. El precio de la
     * regla es que un nombre no puede llevar espacio antes del {@code #}
     * ({@code Sala#3} si, {@code Sala #3} no: se leeria como comentario).
     */
    private static String stripInlineComment(String value) {
        int start = 0;
        while (start < value.length() && Character.isWhitespace(value.charAt(start))) {
            start++;
        }
        if (start >= value.length() || value.charAt(start) == '#') {
            // Valor vacio, o empieza por '#': no hay comentario que cortar.
            return value;
        }
        for (int i = start; i < value.length(); i++) {
            if (value.charAt(i) == '#' && Character.isWhitespace(value.charAt(i - 1))) {
                return value.substring(0, i);
            }
        }
        return value;
    }

    /** Todas las lineas posteriores a {@code map:}, sin comentarios. */
    private static List<String> rowsAfterMap(List<String> lines) {
        List<String> rows = new ArrayList<>();
        boolean inMap = false;
        for (String line : lines) {
            String t = line.trim();
            if (!inMap) {
                if (t.startsWith("map:")) {
                    inMap = true;
                }
                continue;
            }
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            rows.add(stripTrailingWhitespace(line));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Nivel vacío (falta 'map:'): " + lines);
        }
        return rows;
    }

    /** Convierte filas en una matriz rectangular rellenando con espacios. */
    private static char[][] toMatrix(List<String> rows) {
        int width = 0;
        for (String row : rows) {
            width = Math.max(width, row.length());
        }
        char[][] map = new char[rows.size()][width];
        for (int r = 0; r < rows.size(); r++) {
            String row = rows.get(r);
            for (int c = 0; c < width; c++) {
                map[r][c] = c < row.length() ? row.charAt(c) : ' ';
            }
        }
        return map;
    }

    /** Acepta {@code #RRGGBB}, {@code RRGGBB} o {@code r,g,b} en 0..1. */
    private static Vector3f parseColor(String raw, Vector3f fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        try {
            if (value.contains(",")) {
                String[] parts = value.split(",");
                if (parts.length == 3) {
                    return new Vector3f(Float.parseFloat(parts[0].trim()),
                            Float.parseFloat(parts[1].trim()),
                            Float.parseFloat(parts[2].trim()));
                }
            }
            String hex = value.startsWith("#") ? value.substring(1) : value;
            if (hex.length() == 6) {
                return new Vector3f(
                        Integer.parseInt(hex.substring(0, 2), 16) / 255f,
                        Integer.parseInt(hex.substring(2, 4), 16) / 255f,
                        Integer.parseInt(hex.substring(4, 6), 16) / 255f);
            }
        } catch (NumberFormatException ignored) {
            // Formato no reconocido: se mantiene el color heredado.
        }
        System.err.println("[LevelLoader] Color no valido, uso el heredado: " + raw);
        return fallback;
    }

    private static float parseFloatOr(String raw, float fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}