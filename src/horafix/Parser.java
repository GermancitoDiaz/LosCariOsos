package horafix;

import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * ANALIZADOR SINTÁCTICO — HoraFix
 *
 * ─── GRAMÁTICA FORMAL DEL LENGUAJE HoraFix ───────────────────────────────
 *
 * <pre>
 * programa        → sentencia*
 *
 * sentencia       → sent_aprobadas
 *                 | sent_reprobadas
 *                 | sent_cursando
 *                 | sent_max_creditos
 *                 | sent_materia
 *                 | sent_regla
 *                 | sent_seriacion
 *
 * sent_aprobadas   → APROBADAS   ':' lista_materias
 * sent_reprobadas  → REPROBADAS  ':' lista_materias
 * sent_cursando    → CURSANDO    ':' lista_materias
 * sent_max_creditos→ MAX_CREDITOS ':' NUMERO
 * sent_materia     → MATERIA IDENTIFICADOR NUMERO '-' NUMERO CREDITOS NUMERO
 * sent_regla       → REGLA IDENTIFICADOR
 * sent_seriacion   → SERIACION IDENTIFICADOR ':' lista_materias
 *
 * lista_materias   → IDENTIFICADOR (',' IDENTIFICADOR)*
 * </pre>
 *
 * ─── ESTRATEGIA DE RECUPERACIÓN DE ERRORES ───────────────────────────────
 *
 * Se usa el <b>Modo Pánico</b>: cuando una {@link ParseException} es lanzada,
 * el método {@link #recuperar()} avanza el cursor hasta la siguiente palabra
 * RESERVADA (inicio de sentencia), permitiendo continuar el análisis y
 * reportar múltiples errores en una sola pasada.
 *
 * Los tokens {@code ERROR_CHAR} generados por el Lexer son saltados
 * silenciosamente por {@link #skipErrorTokens()} — ya fueron reportados
 * en la fase léxica, por lo que el Parser no los re-registra.
 */
public class Parser {

    // ── Estado interno ────────────────────────────────────────────────────────

    /** Lista completa de tokens producida por el Lexer, incluyendo ERROR_CHAR. */
    private final List<Token> tokens;

    /** Cursor: índice del siguiente token a consumir. */
    private int pos;

    /** Errores acumulados durante el análisis. Vacía = programa correcto. */
    private final List<String> errores;

    /** Raíz del árbol sintáctico construido durante el análisis. */
    private DefaultMutableTreeNode raiz;

    // ── Constantes del lenguaje ───────────────────────────────────────────────

    /**
     * Palabras reservadas del lenguaje HoraFix.
     * Usadas por {@link #sugerirKeyword(String)} para detectar keywords mal
     * escritas (ej: "APROBDA" → sugiere "APROBADAS") mediante Levenshtein.
     */
    private static final String[] PALABRAS_RESERVADAS = {
        "APROBADAS", "REPROBADAS", "CURSANDO", "MAX_CREDITOS", "MATERIA", "REGLA", "CREDITOS", "SERIACION"
    };

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param tokens lista de tokens producida por el Lexer.
     *               Puede contener tokens ERROR_CHAR; el parser los saltará.
     */
    public Parser(List<Token> tokens) {
        this.tokens  = tokens;
        this.pos     = 0;
        this.errores = new ArrayList<>();
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Punto de entrada del parser. Analiza el programa completo, construye
     * el árbol sintáctico y devuelve la lista de errores encontrados.
     *
     * @return lista de mensajes de error; vacía si la sintaxis es correcta
     */
    public List<String> analizar() {
        raiz = new DefaultMutableTreeNode("programa");
        analizarPrograma();
        return errores;
    }

    /**
     * Devuelve el árbol sintáctico construido por la última llamada a {@link #analizar()}.
     * Puede contener nodos parciales si hubo errores y se usó recuperación.
     *
     * @return nodo raíz "programa" del árbol sintáctico
     */
    public DefaultMutableTreeNode getRaiz() {
        return raiz;
    }

    // REGLAS GRAMATICALES — un método privado por cada producción de la gramática

    /**
     * <b>programa → sentencia*</b>
     *
     * Bucle principal del parser. Procesa sentencias en secuencia hasta agotar
     * los tokens. Ante cada error lanza {@link ParseException}, la captura,
     * la guarda y llama a {@link #recuperar()} para sincronizarse con la
     * siguiente sentencia y continuar el análisis.
     */
    private void analizarPrograma() {
        while (pos < tokens.size()) {
            // Los ERROR_CHAR al inicio de sentencia ya fueron reportados por el Lexer;
            // el Parser los salta para no generar errores duplicados.
            skipErrorTokens();
            if (pos >= tokens.size()) break;
            try {
                DefaultMutableTreeNode nodo = analizarSentencia();
                if (nodo != null) raiz.add(nodo);
            } catch (ParseException e) {
                errores.add(e.getMessage());
                recuperar();
            }
        }
    }

    /**
     * <b>sentencia → sent_aprobadas | sent_reprobadas | sent_cursando
     *              | sent_max_creditos | sent_materia | sent_regla</b>
     *
     * Lee el token actual para determinar qué producción aplicar. Si no es
     * una palabra RESERVADA, intenta detectar si parece una keyword mal escrita
     * (Levenshtein) para ofrecer una sugerencia útil.
     *
     * @return nodo del árbol que representa la sentencia reconocida
     * @throws ParseException si el token no corresponde a ninguna sentencia válida
     */
    private DefaultMutableTreeNode analizarSentencia() throws ParseException {
        Token actual = tokenActual();

        if (!actual.tipo.equals("RESERVADA")) {
            if (actual.tipo.equals("IDENTIFICADOR")) {
                String sugerencia = sugerirKeyword(actual.lexema);
                if (sugerencia != null) {
                    throw new ParseException(
                        ErrorManager.keywordMalEscrita(actual.linea, actual.lexema, sugerencia)
                    );
                }
            }
            throw new ParseException(
                ErrorManager.instruccionDesconocida(actual.linea, actual.lexema)
            );
        }

        switch (actual.lexema) {
            case "APROBADAS":    return analizarListaEstatus("APROBADAS");
            case "REPROBADAS":   return analizarListaEstatus("REPROBADAS");
            case "CURSANDO":     return analizarListaEstatus("CURSANDO");
            case "MAX_CREDITOS": return analizarMaxCreditos();
            case "MATERIA":      return analizarMateria();
            case "REGLA":        return analizarRegla();
            case "SERIACION":    return analizarSeriacion();
            default:
                throw new ParseException(
                    ErrorManager.keywordInesperada(actual.linea, actual.lexema)
                );
        }
    }

    /**
     * <b>sent_aprobadas | sent_reprobadas | sent_cursando
     *    → KEYWORD ':' lista_materias</b>
     *
     * Las tres sentencias de estatus comparten la misma estructura;
     * se parametriza la keyword para reutilizar el código.
     *
     * @param keyword "APROBADAS", "REPROBADAS" o "CURSANDO"
     * @return nodo "sent_aprobadas" / "sent_reprobadas" / "sent_cursando"
     *         con la lista de materias como subnodo
     * @throws ParseException si falta ':' o la lista de materias es inválida
     */
    private DefaultMutableTreeNode analizarListaEstatus(String keyword) throws ParseException {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("sent_" + keyword.toLowerCase());
        consumir("RESERVADA", keyword);
        nodo.add(new DefaultMutableTreeNode(keyword));
        consumir("DOS_PUNTOS", ":");
        nodo.add(analizarListaMaterias());
        return nodo;
    }

    /**
     * <b>sent_max_creditos → MAX_CREDITOS ':' NUMERO</b>
     *
     * @return nodo "sent_max_creditos" con el valor numérico como hoja
     * @throws ParseException si falta ':' o el valor no es un número válido
     */
    private DefaultMutableTreeNode analizarMaxCreditos() throws ParseException {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("sent_max_creditos");
        consumir("RESERVADA", "MAX_CREDITOS");
        nodo.add(new DefaultMutableTreeNode("MAX_CREDITOS"));
        consumir("DOS_PUNTOS", ":");
        verificarEsNumero("el límite de créditos");
        String valor = tokens.get(pos).lexema;
        consumir("NUMERO", null);
        nodo.add(new DefaultMutableTreeNode("NUMERO → " + valor));
        return nodo;
    }

    /**
     * <b>sent_materia → MATERIA IDENTIFICADOR NUMERO '-' NUMERO CREDITOS NUMERO</b>
     *
     * Declara una materia con su nombre, rango horario y créditos. Los días de
     * clase ya no se declaran explícitamente: se calculan a partir de los
     * créditos, siempre a partir de Lunes.
     * Ejemplo válido: {@code MATERIA calculo 8-10 CREDITOS 4}
     *
     * Los créditos determinan cuántos días ocupa la materia en el simulador
     * de horario (3=L-Mi, 4=L-J, 5=L-V, 6=L-V + hora extra viernes).
     * La validación del rango 3–6 se hace en el análisis semántico (E-M09).
     *
     * @return nodo "sent_materia" con nombre, horario y créditos como hojas
     * @throws ParseException si algún token obligatorio falta o tiene tipo incorrecto
     */
    private DefaultMutableTreeNode analizarMateria() throws ParseException {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("sent_materia");
        consumir("RESERVADA", "MATERIA");
        nodo.add(new DefaultMutableTreeNode("MATERIA"));

        verificarEsIdentificador("el nombre de la materia");
        String nombre = pos < tokens.size() ? tokens.get(pos).lexema : "";
        consumir("IDENTIFICADOR", null);
        nodo.add(new DefaultMutableTreeNode("ID → " + nombre));

        verificarEsNumero("la hora de inicio");
        String desde = pos < tokens.size() ? tokens.get(pos).lexema : "";
        consumir("NUMERO", null);
        consumir("GUION", "-");
        verificarEsNumero("la hora de fin");
        String hasta = pos < tokens.size() ? tokens.get(pos).lexema : "";
        consumir("NUMERO", null);
        nodo.add(new DefaultMutableTreeNode("HORARIO → " + desde + "-" + hasta));

        consumir("RESERVADA", "CREDITOS");
        verificarEsNumero("el número de créditos (3–6)");
        String creds = pos < tokens.size() ? tokens.get(pos).lexema : "";
        consumir("NUMERO", null);
        nodo.add(new DefaultMutableTreeNode("CREDITOS → " + creds));

        return nodo;
    }

    /**
     * <b>sent_regla → REGLA IDENTIFICADOR</b>
     *
     * @return nodo "sent_regla" con el nombre de la regla como hoja
     * @throws ParseException si falta el identificador de la regla
     */
    private DefaultMutableTreeNode analizarRegla() throws ParseException {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("sent_regla");
        consumir("RESERVADA", "REGLA");
        nodo.add(new DefaultMutableTreeNode("REGLA"));
        verificarEsIdentificador("el nombre de la regla");
        String nombre = pos < tokens.size() ? tokens.get(pos).lexema : "";
        consumir("IDENTIFICADOR", null);
        nodo.add(new DefaultMutableTreeNode("ID → " + nombre));
        return nodo;
    }

    /**
     * <b>sent_seriacion → SERIACION IDENTIFICADOR ':' lista_materias</b>
     *
     * Declara los prerequisitos de una materia: el identificador antes de ':'
     * es la materia dependiente, y la lista_materias son sus prerequisitos.
     * Solo se valida en la fase semántica si se declaró {@code REGLA respetar_seriacion}
     * (ver {@link ErrorManager.CodigoSemantico#E_M14}).
     * Ejemplo: {@code SERIACION calculo2 : calculo1}
     *
     * @return nodo "sent_seriacion" con la materia dependiente y sus prerequisitos
     * @throws ParseException si falta el identificador, ':' o la lista de prerequisitos
     */
    private DefaultMutableTreeNode analizarSeriacion() throws ParseException {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("sent_seriacion");
        consumir("RESERVADA", "SERIACION");
        nodo.add(new DefaultMutableTreeNode("SERIACION"));
        verificarEsIdentificador("el nombre de la materia dependiente");
        String nombre = pos < tokens.size() ? tokens.get(pos).lexema : "";
        consumir("IDENTIFICADOR", null);
        nodo.add(new DefaultMutableTreeNode("ID → " + nombre));
        consumir("DOS_PUNTOS", ":");
        nodo.add(analizarListaMaterias());
        return nodo;
    }

    /**
     * <b>lista_materias → IDENTIFICADOR (',' IDENTIFICADOR)*</b>
     *
     * La lista debe tener al menos un identificador. Se detectan dos
     * errores específicos:
     * <ul>
     *   <li><b>E-S02</b> — lista vacía (nada después de ':')</li>
     *   <li><b>E-S03</b> — coma final sin identificador siguiente</li>
     * </ul>
     *
     * @return nodo "lista_materias" con cada identificador como hoja
     * @throws ParseException si la lista está vacía o hay una coma final sin nombre
     */
    private DefaultMutableTreeNode analizarListaMaterias() throws ParseException {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("lista_materias");

        if (pos >= tokens.size()) {
            int linea = pos > 0 ? tokens.get(pos - 1).linea : 1;
            throw new ParseException(
                ErrorManager.errorSintactico(linea, ErrorManager.CodigoSintactico.E_S02,
                    "la lista de materias está vacía — " +
                    "se esperaba al menos un nombre de materia después de ':'")
            );
        }
        verificarEsIdentificador("un nombre de materia (la lista no puede estar vacía)");

        // verificarEsIdentificador ya avanzó sobre ERROR_CHAR; pos apunta al token real.
        if (pos < tokens.size()) nodo.add(new DefaultMutableTreeNode(tokens.get(pos).lexema));
        consumir("IDENTIFICADOR", null);

        // Leer pares ', IDENTIFICADOR' mientras haya comas.
        // skipErrorTokens() antes del chequeo por si hay ERROR_CHAR entre elementos.
        while (pos < tokens.size()) {
            skipErrorTokens();
            if (pos >= tokens.size() || !tokenActual().tipo.equals("COMA")) break;
            consumir("COMA", ",");

            if (pos >= tokens.size()) {
                int linea = tokens.get(pos - 1).linea;
                throw new ParseException(
                    ErrorManager.errorSintactico(linea, ErrorManager.CodigoSintactico.E_S03,
                        "coma final sin nombre de materia — " +
                        "se esperaba un identificador después de la última coma")
                );
            }
            verificarEsIdentificador("un nombre de materia después de la coma");
            if (pos < tokens.size()) nodo.add(new DefaultMutableTreeNode(tokens.get(pos).lexema));
            consumir("IDENTIFICADOR", null);
        }

        return nodo;
    }

    // MÉTODOS AUXILIARES DEL PARSER

    /**
     * Consume el token actual si su tipo (y opcionalmente su lexema) coinciden
     * con los esperados. Avanza el cursor {@code pos} en 1.
     *
     * Antes de verificar, llama a {@link #skipErrorTokens()} para ignorar
     * cualquier ERROR_CHAR que el Lexer haya dejado en posición intermedia.
     *
     * @param tipoEsperado   tipo de token requerido (ej: "RESERVADA", "NUMERO")
     * @param lexemaEsperado lexema exacto requerido, o {@code null} si cualquier
     *                       lexema del tipo indicado es válido
     * @throws ParseException con código de catálogo si el token no coincide,
     *                        o con E-S04 si se llegó al final del archivo
     */
    private void consumir(String tipoEsperado, String lexemaEsperado) throws ParseException {
        skipErrorTokens();

        if (pos >= tokens.size()) {
            throw new ParseException(
                ErrorManager.sentenciaIncompleta(descripcionToken(tipoEsperado, lexemaEsperado))
            );
        }

        Token actual = tokens.get(pos);
        ErrorManager.CodigoSintactico codigo = codigoParaTipo(tipoEsperado, lexemaEsperado);

        if (!actual.tipo.equals(tipoEsperado)) {
            throw new ParseException(
                ErrorManager.tokenInesperado(actual.linea, codigo,
                    descripcionToken(tipoEsperado, lexemaEsperado), actual.lexema)
            );
        }

        if (lexemaEsperado != null && !actual.lexema.equals(lexemaEsperado)) {
            throw new ParseException(
                ErrorManager.tokenInesperado(actual.linea, codigo,
                    "'" + lexemaEsperado + "'", actual.lexema)
            );
        }

        pos++;
    }

    /**
     * <b>Recuperación de errores — Modo Pánico.</b>
     *
     * Avanza {@code pos} descartando tokens hasta encontrar una palabra
     * RESERVADA (que marca el inicio de la siguiente sentencia) o llegar al
     * final del archivo. Esto garantiza que el parser pueda reportar múltiples
     * errores en una sola pasada en lugar de detenerse en el primero.
     *
     * Garantiza avanzar al menos un token: si el cursor ya está sobre una
     * RESERVADA que no es un inicio de sentencia válido (ej. "CREDITOS" suelto
     * fuera de una declaración MATERIA), {@link #analizarSentencia()} vuelve a
     * lanzar sin consumir nada — sin este avance forzado, el parser quedaría
     * atascado repitiendo el mismo error indefinidamente.
     */
    private void recuperar() {
        int inicio = pos;
        while (pos < tokens.size() && !tokens.get(pos).tipo.equals("RESERVADA")) {
            pos++;
        }
        if (pos == inicio && pos < tokens.size()) pos++;
    }

    /**
     * Devuelve el token en la posición actual del cursor sin avanzarlo.
     *
     * @return token actual; el llamador debe garantizar que {@code pos < tokens.size()}
     */
    private Token tokenActual() {
        return tokens.get(pos);
    }

    /**
     * Avanza el cursor saltando todos los tokens {@code ERROR_CHAR} consecutivos.
     *
     * El Lexer ya los reportó; incluirlos en la lista de errores del Parser
     * generaría mensajes duplicados. Al saltarlos aquí, el análisis sintáctico
     * puede continuar sobre los tokens válidos restantes.
     */
    private void skipErrorTokens() {
        while (pos < tokens.size() && tokens.get(pos).tipo.startsWith("ERROR")) {
            pos++;
        }
    }

    /**
     * Elige el código de catálogo ({@link ErrorManager.CodigoSintactico}) más
     * específico para el par (tipo, lexema) esperado, de modo que {@link #consumir}
     * pueda incluir el código correcto en el mensaje sin conocer el contexto gramatical.
     *
     * <pre>
     *   ':'  → E-S07 (se esperaba ':' después de keyword)
     *   '-'  → E-S10 (se esperaba '-' entre horas)
     *   IDENTIFICADOR → E-S08
     *   NUMERO        → E-S09
     *   otro          → E-S05 (token inesperado genérico)
     * </pre>
     *
     * @param tipo   tipo de token esperado
     * @param lexema lexema esperado, o {@code null}
     * @return código de catálogo correspondiente
     */
    private ErrorManager.CodigoSintactico codigoParaTipo(String tipo, String lexema) {
        if (lexema != null) {
            if (lexema.equals(":")) return ErrorManager.CodigoSintactico.E_S07;
            if (lexema.equals("-")) return ErrorManager.CodigoSintactico.E_S10;
        }
        switch (tipo) {
            case "IDENTIFICADOR": return ErrorManager.CodigoSintactico.E_S08;
            case "NUMERO":        return ErrorManager.CodigoSintactico.E_S09;
            default:              return ErrorManager.CodigoSintactico.E_S05;
        }
    }

    /**
     * Convierte un par (tipo, lexema) en un texto legible para mensajes de error.
     * Si hay lexema concreto, lo usa; si no, devuelve una descripción del tipo.
     *
     * @param tipo   tipo de token esperado
     * @param lexema lexema esperado, o {@code null}
     * @return descripción legible (ej: "un número", "':'", "un día (...)")
     */
    private String descripcionToken(String tipo, String lexema) {
        if (lexema != null) return "'" + lexema + "'";
        switch (tipo) {
            case "IDENTIFICADOR": return "un nombre de materia (identificador)";
            case "NUMERO":        return "un número";
            case "DOS_PUNTOS":    return "':'";
            case "COMA":          return "','";
            case "GUION":         return "'-'";
            default:              return tipo;
        }
    }

    // VERIFICADORES SEMÁNTICOS — inspeccionan el tipo ANTES de consumir
    // Cada uno llama skipErrorTokens() para saltar ERROR_CHAR intermedios,
    // y lanza mensajes de error específicos según el tipo incorrecto encontrado.

    /**
     * Verifica que el token actual sea IDENTIFICADOR.
     *
     * Si es un tipo incorrecto conocido (NUMERO, RESERVADA), lanza un
     * mensaje E-S08 con una explicación específica del problema. Esto es más
     * útil que el mensaje genérico de {@link #consumir}.
     *
     * @param queSeEspera descripción contextual para incluir en el mensaje
     *                    (ej: "el nombre de la materia")
     * @throws ParseException E-S08 con descripción específica según el tipo encontrado
     */
    private void verificarEsIdentificador(String queSeEspera) throws ParseException {
        skipErrorTokens();
        if (pos >= tokens.size()) return;
        Token t = tokenActual();
        if (t.tipo.equals("IDENTIFICADOR")) return;
        if (t.tipo.equals("NUMERO")) {
            throw new ParseException(ErrorManager.errorSintactico(t.linea,
                ErrorManager.CodigoSintactico.E_S08,
                "se esperaba " + queSeEspera + ", pero '" + t.lexema +
                "' es un número — use un nombre en minúsculas como algebra, calculo1, fisica"));
        }
        if (t.tipo.equals("RESERVADA")) {
            throw new ParseException(ErrorManager.errorSintactico(t.linea,
                ErrorManager.CodigoSintactico.E_S08,
                "se esperaba " + queSeEspera + ", pero '" + t.lexema +
                "' es una instrucción del lenguaje — use un nombre en minúsculas"));
        }
        // Cualquier otro tipo: consumir() generará el mensaje genérico E-S05
    }

    /**
     * Verifica que el token actual sea NUMERO.
     *
     * Si es un IDENTIFICADOR donde se espera número (ej: "ocho" en lugar de "8"),
     * lanza un error léxico E-L04 con el contexto donde se esperaba el número.
     *
     * @param contexto descripción del valor esperado para el mensaje de error
     *                 (ej: "la hora de inicio", "el límite de créditos")
     * @throws ParseException E-L04 si hay texto donde se espera número
     */
    private void verificarEsNumero(String contexto) throws ParseException {
        skipErrorTokens();
        if (pos >= tokens.size()) return;
        Token t = tokenActual();
        if (t.tipo.equals("NUMERO")) return;
        if (t.tipo.equals("IDENTIFICADOR")) {
            throw new ParseException(
                ErrorManager.valorNoNumerico(t.linea, t.lexema, contexto)
            );
        }
        // Otro tipo: consumir() dará el mensaje genérico
    }

    // SUGERENCIAS POR DISTANCIA DE LEVENSHTEIN

    /**
     * Busca la keyword más parecida a {@code palabra} usando distancia de Levenshtein.
     *
     * El umbral de aceptación es {@code max(2, largo_keyword / 3)}: keywords
     * más largas toleran más diferencias antes de descartar la sugerencia.
     *
     * @param palabra token encontrado (se normaliza a mayúsculas internamente)
     * @return la keyword más cercana si la distancia es aceptable, {@code null} si no hay
     */
    private String sugerirKeyword(String palabra) {
        String upper = palabra.toUpperCase();
        String mejorSugerencia = null;
        int mejorDistancia = Integer.MAX_VALUE;
        for (String kw : PALABRAS_RESERVADAS) {
            int dist = levenshtein(upper, kw);
            int umbral = Math.max(2, kw.length() / 3);
            if (dist <= umbral && dist < mejorDistancia) {
                mejorDistancia = dist;
                mejorSugerencia = kw;
            }
        }
        return mejorSugerencia;
    }

    /**
     * Calcula la <b>distancia de Levenshtein</b> entre las cadenas {@code a} y {@code b}.
     *
     * La distancia de Levenshtein es el número mínimo de operaciones de edición
     * (inserción, eliminación o sustitución de un carácter) para transformar
     * {@code a} en {@code b}. Se usa para medir qué tan "parecida" es una palabra
     * escrita por el usuario a una keyword del lenguaje.
     *
     * Implementación con programación dinámica O(|a|·|b|) en tiempo y espacio.
     *
     * @param a primera cadena
     * @param b segunda cadena
     * @return número mínimo de ediciones para transformar {@code a} en {@code b}
     */
    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                                   Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    // EXCEPCIÓN INTERNA

    /**
     * Excepción checked interna usada para el flujo de control del parser.
     *
     * No sale al exterior: {@link #analizarPrograma()} la captura, guarda el
     * mensaje en {@link #errores} y activa la recuperación por Modo Pánico.
     */
    private static class ParseException extends Exception {
        public ParseException(String mensaje) {
            super(mensaje);
        }
    }
}