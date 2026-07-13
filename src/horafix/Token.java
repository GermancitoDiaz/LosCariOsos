package horafix;

/**
 * Representa una unidad léxica (token) producida por el Lexer.
 *
 * El Lexer devuelve Strings con formato "TIPO,lexema,linea".
 * Interfaz.java parsea ese String y construye objetos Token para pasarlos
 * al Parser, que trabaja con esta estructura en lugar de con Strings crudos.
 *
 * Tipos de token posibles (campo {@code tipo}):
 * <pre>
 *   RESERVADA     — palabra clave del lenguaje (APROBADAS, REPROBADAS,
 *                   CURSANDO, MAX_CREDITOS, MATERIA, REGLA, CREDITOS)
 *   IDENTIFICADOR — nombre de materia, regla, u otra secuencia alfanumérica
 *                   que no coincide con ninguna keyword
 *   NUMERO        — entero sin ceros al inicio (0 ó [1-9][0-9]*)
 *   DOS_PUNTOS    — símbolo ':'
 *   COMA          — símbolo ','
 *   GUION         — símbolo '-'
 *   ERROR_CHAR    — carácter inválido o número con ceros al inicio (ej: 007)
 * </pre>
 */
public class Token {

    /** Categoría léxica del token (ej: "RESERVADA", "NUMERO", "ERROR_CHAR"). */
    public String tipo;

    /** Texto exacto reconocido en el código fuente (ej: "MATERIA", "algebra1", "8"). */
    public String lexema;

    /** Número de línea (base 1) donde aparece el token en el código fuente. */
    public int linea;

    /**
     * Construye un token con los tres campos que lo identifican.
     *
     * @param tipo   categoría léxica del token
     * @param lexema texto exacto reconocido en la entrada
     * @param linea  número de línea donde aparece (base 1)
     */
    public Token(String tipo, String lexema, int linea) {
        this.tipo   = tipo;
        this.lexema = lexema;
        this.linea  = linea;
    }

    /**
     * Verifica si este token es una palabra reservada específica.
     *
     * Útil en el Parser y en la Simulación para distinguir, por ejemplo,
     * APROBADAS de MATERIA (ambas tienen tipo "RESERVADA" pero distinto lexema).
     *
     * @param palabra la keyword exacta a comparar, en mayúsculas (ej: "MATERIA")
     * @return {@code true} si el tipo es RESERVADA y el lexema coincide con {@code palabra}
     */
    public boolean esReservada(String palabra) {
        return tipo.equals("RESERVADA") && lexema.equals(palabra);
    }

    /**
     * Representación en el mismo formato que produce el Lexer: "TIPO,lexema,linea".
     * Útil para depuración.
     */
    @Override
    public String toString() {
        return tipo + "," + lexema + "," + linea;
    }
}
