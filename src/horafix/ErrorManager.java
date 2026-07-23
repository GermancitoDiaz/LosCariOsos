package horafix;

/**
 * Punto único de generación de mensajes de error del compilador. El Lexer y
 * el Parser no arman texto de error por su cuenta: siempre pasan por aquí,
 * de modo que el formato y los códigos de catálogo se mantienen consistentes
 * en todo el proyecto. Los códigos deben coincidir con los que se muestran
 * en {@code Interfaz.crearCatalogoErrores()}.
 */
public final class ErrorManager {

    private ErrorManager() {}

    /**
     * Códigos de errores léxicos (E-L01 … E-L04).
     * E-L01 — Carácter(es) no válido(s) en el código fuente     (ej: @, #, $)
     * E-L02 — Número con ceros al inicio                         (ej: 007, 030)
     * E-L03 — Keyword mal escrita; sugerencia disponible         (ej: APROBDA)
     * E-L04 — Texto donde se espera un número entero             (ej: "ocho")
     */
    public enum CodigoLexico {
        E_L01("E-L01"),
        E_L02("E-L02"),
        E_L03("E-L03"),
        E_L04("E-L04");

        /** Código de catálogo tal como aparece en la interfaz (ej: "E-L01"). */
        public final String codigo;
        CodigoLexico(String c) { this.codigo = c; }
    }

    /**
     * Códigos de errores sintácticos (E-S01 … E-S10).
     *
     * E-S01 — Instrucción desconocida al inicio de sentencia
     * E-S02 — Lista de materias vacía después de ':'
     * E-S03 — Coma final sin identificador
     * E-S04 — Sentencia incompleta al final del archivo (EOF)
     * E-S05 — Token inesperado dentro de sentencia (genérico)
     * E-S06 — Keyword en posición incorrecta
     * E-S07 — Se esperaba ':' después de keyword
     * E-S08 — Se esperaba un identificador de materia
     * E-S09 — Se esperaba un número de hora válido
     * E-S10 — Se esperaba '-' para separar hora inicio y fin
     */
    public enum CodigoSintactico {
        E_S01("E-S01"),
        E_S02("E-S02"),
        E_S03("E-S03"),
        E_S04("E-S04"),
        E_S05("E-S05"),
        E_S06("E-S06"),
        E_S07("E-S07"),
        E_S08("E-S08"),
        E_S09("E-S09"),
        E_S10("E-S10");

        /** Código de catálogo tal como aparece en la interfaz (ej: "E-S07"). */
        public final String codigo;
        CodigoSintactico(String c) { this.codigo = c; }
    }

    // API GENÉRICA — para crear mensajes directamente con código y detalle

    public static String errorLexico(int linea, CodigoLexico codigo, String detalle) {
        return "Error léxico en línea " + linea + " [" + codigo.codigo + "]: " + detalle;
    }

    /**
     * Construye un mensaje de error sintáctico con número de línea y código de catálogo.
     *
     * @param linea   número de línea (base 1) donde ocurre el error
     * @param codigo  código de catálogo del error
     * @param detalle descripción específica del problema encontrado
     * @return mensaje formateado listo para mostrar en la interfaz
     */
    public static String errorSintactico(int linea, CodigoSintactico codigo, String detalle) {
        return "Error sintáctico en línea " + linea + " [" + codigo.codigo + "]: " + detalle;
    }

    /**
     * Construye un mensaje de error sintáctico <b>sin número de línea</b>.
     * Se usa cuando el error ocurre al llegar al fin de archivo (EOF), donde
     * ya no hay un token con número de línea disponible.
     *
     * @param codigo  código de catálogo del error
     * @param detalle descripción específica del problema
     * @return mensaje formateado
     */
    public static String errorSintacticoEOF(CodigoSintactico codigo, String detalle) {
        return "Error sintáctico [" + codigo.codigo + "]: " + detalle;
    }

    // MENSAJES LÉXICOS PRE-ARMADOS
    // Cada método corresponde a un caso específico detectado por el Lexer
    // (ERROR_CHAR en Interfaz) o por el Parser (verificarXxx).

    /** E-L01: carácter o secuencia de caracteres no válidos (ej: {@code @}, {@code #$%}). */
    public static String charInvalido(int linea, String lexema) {
        return errorLexico(linea, CodigoLexico.E_L01,
                "'" + lexema + "': carácter(es) no válido(s)");
    }

    /** E-L02: número que inicia con ceros (ej: {@code 007}, {@code 030}). */
    public static String ceroAlInicio(int linea, String lexema) {
        return errorLexico(linea, CodigoLexico.E_L02,
                "'" + lexema + "': número con ceros al inicio");
    }

    public static String keywordMalEscrita(int linea, String encontrado, String sugerencia) {
        return errorLexico(linea, CodigoLexico.E_L03,
                "'" + encontrado + "' no es una instrucción reconocida" +
                (sugerencia != null ? " — ¿quiso escribir '" + sugerencia + "'?" : ""));
    }

    /**
     * E-L04: texto donde se esperaba un número entero (ej: "ocho" en lugar de 8).
     */
    public static String valorNoNumerico(int linea, String encontrado, String contexto) {
        return errorLexico(linea, CodigoLexico.E_L04,
                "'" + encontrado + "' no es un número válido para " + contexto +
                " — use un entero positivo (ej: 7, 8, 10, 13)");
    }

    // MENSAJES SINTÁCTICOS PRE-ARMADOS

    /** E-S01: el token al inicio de sentencia no es ninguna keyword conocida. */
    public static String instruccionDesconocida(int linea, String encontrado) {
        return errorSintactico(linea, CodigoSintactico.E_S01,
                "se esperaba una instrucción (APROBADAS, REPROBADAS, CURSANDO, " +
                "MAX_CREDITOS, MATERIA, REGLA, SERIACION) pero se encontró '" + encontrado + "'");
    }

    /** E-S06: keyword reconocida pero en una posición que la gramática no permite. */
    public static String keywordInesperada(int linea, String encontrado) {
        return errorSintactico(linea, CodigoSintactico.E_S06,
                "palabra reservada '" + encontrado + "' en posición incorrecta");
    }

    /**
     * E-S04: se llegó al final del archivo antes de completar una sentencia.
     */
    public static String sentenciaIncompleta(String esperado) {
        return errorSintacticoEOF(CodigoSintactico.E_S04,
                "sentencia incompleta al final del archivo — se esperaba " + esperado);
    }

    /**
     * Mensaje genérico para token de tipo o lexema incorrecto.
     */
    public static String tokenInesperado(int linea, CodigoSintactico codigo,
                                         String esperado, String encontrado) {
        return errorSintactico(linea, codigo,
                "se esperaba " + esperado + " pero se encontró '" + encontrado + "'");
    }

    /**
     * Códigos de errores semánticos (E-M01 … E-M13).
     *
     * E-M01 — Choque de horario entre dos materias
     * E-M02 — Materia reprobada sin horario asignado
     * E-M03 — Materia aprobada re-inscrita innecesariamente
     * E-M04 — Materia en CURSANDO sin horario declarado
     * E-M05 — Total de créditos supera MAX_CREDITOS
     * E-M06 — Horario fuera de rango académico válido (7:00–21:00)
     * E-M07 — Hora de fin menor o igual a la hora de inicio
     * E-M08 — Sin materias declaradas para generar el plan
     * E-M09 — Créditos de una materia fuera del rango válido (3–6)
     * E-M10 — Total de créditos insuficiente respecto a MAX_CREDITOS
     * E-M11 — MAX_CREDITOS fuera del rango institucional válido (20–35)
     * E-M12 — Materia con estado contradictorio (aparece en más de una lista)
     * E-M13 — Materia declarada más de una vez con MATERIA
     * E-M14 — Seriación incumplida (materia cursada sin aprobar su prerequisito),
     *         solo se valida si se declaró REGLA respetar_seriacion
     * E-M15 — REGLA con nombre desconocido, con sugerencia por distancia de Levenshtein
     */
    public enum CodigoSemantico {
        E_M01("E-M01"),
        E_M02("E-M02"),
        E_M03("E-M03"),
        E_M04("E-M04"),
        E_M05("E-M05"),
        E_M06("E-M06"),
        E_M07("E-M07"),
        E_M08("E-M08"),
        E_M09("E-M09"),
        E_M10("E-M10"),
        E_M11("E-M11"),
        E_M12("E-M12"),
        E_M13("E-M13"),
        E_M14("E-M14"),
        E_M15("E-M15");

        /** Código de catálogo tal como aparece en la interfaz (ej: "E-M01"). */
        public final String codigo;
        CodigoSemantico(String c) { this.codigo = c; }
    }

    public static String errorSemantico(int linea, CodigoSemantico codigo, String detalle) {
        return "Error semántico en línea " + linea + " [" + codigo.codigo + "]: " + detalle;
    }

    /**
     * Construye un mensaje de error semántico <b>sin número de línea</b>.
     * Se usa para problemas de alcance global que no corresponden a una sola
     * línea del código fuente (ej: ausencia total de declaraciones MATERIA).
     *
     * @param codigo  código de catálogo del error semántico
     * @param detalle descripción específica del problema
     * @return mensaje formateado
     */
    public static String errorSemanticoGlobal(CodigoSemantico codigo, String detalle) {
        return "Error semántico [" + codigo.codigo + "]: " + detalle;
    }

    // MENSAJES SEMÁNTICOS PRE-ARMADOS

    /** E-M01: dos materias con días en común (según créditos) y horas superpuestas. */
    public static String choqueHorario(String mat1, int lineaMat1, String mat2, int lineaMat2,
                                       String dia, int solapIni, int solapFin) {
        return errorSemantico(lineaMat1, CodigoSemantico.E_M01,
            "choque de horario — \"" + mat1 + "\" (línea " + lineaMat1 + ") y \"" + mat2 +
            "\" (línea " + lineaMat2 + ") se solapan en " + dia + " " + solapIni + ":00–" +
            solapFin + ":00. Cambia una de ellas a otro horario o ajusta los créditos.");
    }

    /** E-M02: materia reprobada sin declaración MATERIA. */
    public static String reprobadaSinHorario(String materia, int linea) {
        return errorSemantico(linea, CodigoSemantico.E_M02,
            "\"" + materia + "\" está en REPROBADAS pero no tiene horario " +
            "asignado — agrega una declaración MATERIA para programarla.");
    }

    /** E-M03: materia aprobada con horario asignado. */
    public static String aprobadaReInscrita(String materia, int linea) {
        return errorSemantico(linea, CodigoSemantico.E_M03,
            "\"" + materia + "\" ya está en APROBADAS — verifica si " +
            "realmente necesitas inscribirla de nuevo.");
    }

    /** E-M04: materia en CURSANDO sin declaración MATERIA. */
    public static String cursandoSinHorario(String materia, int linea) {
        return errorSemantico(linea, CodigoSemantico.E_M04,
            "\"" + materia + "\" está en CURSANDO pero no tiene horario " +
            "declarado con MATERIA.");
    }

    /** E-M05: suma de créditos supera MAX_CREDITOS. */
    public static String excesoCreditos(int total, int maximo, int lineaMaxCreditos) {
        return errorSemantico(lineaMaxCreditos, CodigoSemantico.E_M05,
            "el total de créditos (" + total + ") excede el límite " +
            "de MAX_CREDITOS (" + maximo + ") — revisa la cantidad de créditos " +
            "de tus materias porque se excede de los permitidos.");
    }

    /** E-M10: suma de créditos muy por debajo de MAX_CREDITOS (menos del 50%). */
    public static String creditosInsuficientes(int total, int maximo, int lineaMaxCreditos) {
        return errorSemantico(lineaMaxCreditos, CodigoSemantico.E_M10,
            "el total de créditos (" + total + ") es insuficiente frente al límite " +
            "de MAX_CREDITOS (" + maximo + ") — revisa la cantidad de créditos, " +
            "parece que te faltan materias por inscribir.");
    }

    /** E-M11: MAX_CREDITOS declarado fuera del rango institucional válido (20–35). */
    public static String maxCreditosFueraDeRango(int valor, int linea) {
        return errorSemantico(linea, CodigoSemantico.E_M11,
            "MAX_CREDITOS (" + valor + ") está fuera del rango permitido " +
            "por la institución (20–35) — revisa el valor declarado.");
    }

    /** E-M06: hora fuera del rango académico válido (7–21). */
    public static String horarioFueraDeRango(String materia, int hora, int linea) {
        return errorSemantico(linea, CodigoSemantico.E_M06,
            "\"" + materia + "\" tiene una hora (" + hora +
            ":00) fuera del rango académico válido (7:00–21:00).");
    }

    /** E-M07: hora de fin menor o igual a hora de inicio. */
    public static String horaFinInvalida(String materia, int inicio, int fin, int linea) {
        return errorSemantico(linea, CodigoSemantico.E_M07,
            "\"" + materia + "\" tiene hora de fin (" + fin +
            ":00) menor o igual a la hora de inicio (" + inicio + ":00).");
    }

    /** E-M08: no hay ninguna declaración MATERIA en el programa (alcance global). */
    public static String sinMaterias() {
        return errorSemanticoGlobal(CodigoSemantico.E_M08,
            "no se encontraron declaraciones MATERIA — " +
            "agrega al menos una para generar el plan académico.");
    }

    /** E-M09: créditos de una materia fuera del rango válido (3–6). */
    public static String creditosFueraDeRango(String materia, int creditos, int linea) {
        return errorSemantico(linea, CodigoSemantico.E_M09,
            "\"" + materia + "\" tiene " + creditos +
            " créditos — el valor debe estar entre 3 y 6.");
    }

    /** E-M12: la misma materia aparece en más de una lista de estatus a la vez. */
    public static String estadoContradictorio(String materia, String estados) {
        return errorSemanticoGlobal(CodigoSemantico.E_M12,
            "\"" + materia + "\" aparece en más de un estado (" + estados +
            ") — una materia no puede tener estatus contradictorios al mismo tiempo.");
    }

    /** E-M13: la misma materia se declara más de una vez con MATERIA. */
    public static String materiaDuplicada(String materia, int primeraLinea, int lineaDuplicada) {
        return errorSemantico(lineaDuplicada, CodigoSemantico.E_M13,
            "\"" + materia + "\" ya fue declarada con MATERIA en la línea " + primeraLinea +
            " — elimina la declaración duplicada o usa un nombre distinto.");
    }

    /**
     * E-M14: la materia se está cursando (o tiene horario declarado) sin haber
     * aprobado uno de sus prerequisitos declarados con SERIACION.
     * Solo se reporta si el programa declaró {@code REGLA respetar_seriacion}.
     */
    public static String seriacionIncumplida(String materia, String prerequisito, int linea) {
        return errorSemantico(linea, CodigoSemantico.E_M14,
            "\"" + materia + "\" requiere haber aprobado \"" + prerequisito +
            "\" (declarado con SERIACION) antes de cursarla — agrega \"" + prerequisito +
            "\" a APROBADAS o quita \"" + materia + "\" de tu carga hasta aprobarlo.");
    }

    /** E-M15: REGLA con un nombre que no está en el catálogo de reglas conocidas. */
    public static String reglaDesconocida(int linea, String encontrada, String sugerencia) {
        return errorSemantico(linea, CodigoSemantico.E_M15,
            "\"" + encontrada + "\" no es una regla reconocida (no_choques, " +
            "no_reinscribir_aprobadas, respetar_seriacion)" +
            (sugerencia != null ? " — ¿quiso escribir \"" + sugerencia + "\"?" : ""));
    }
}