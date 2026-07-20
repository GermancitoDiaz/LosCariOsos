package horafix;

/**
 * Unidad léxica producida por el analizador léxico.
 *
 * El Lexer entrega cadenas con formato "TIPO,lexema,linea"; Interfaz las
 * convierte en instancias de esta clase para que el Parser trabaje con una
 * estructura tipada en lugar de cadenas crudas.
 *
 * Tipos posibles del campo {@code tipo}:
 * <pre>
 *   RESERVADA     — APROBADAS, REPROBADAS, CURSANDO, MAX_CREDITOS,
 *                   MATERIA, REGLA, CREDITOS
 *   IDENTIFICADOR — nombre de materia, regla, u otra secuencia alfanumérica
 *   NUMERO        — entero sin ceros al inicio (0 ó [1-9][0-9]*)
 *   DOS_PUNTOS    — ':'
 *   COMA          — ','
 *   GUION         — '-'
 *   ERROR_CHAR    — carácter inválido o número con ceros al inicio (ej: 007)
 * </pre>
 */
public class Token {

    public String tipo;
    public String lexema;
    public int linea;

    public Token(String tipo, String lexema, int linea) {
        this.tipo   = tipo;
        this.lexema = lexema;
        this.linea  = linea;
    }

    /**
     * Compara tipo y lexema contra una palabra reservada específica, ya que
     * dos tokens RESERVADA distintos (por ejemplo APROBADAS y MATERIA)
     * comparten tipo pero no lexema.
     */
    public boolean esReservada(String palabra) {
        return tipo.equals("RESERVADA") && lexema.equals(palabra);
    }

    @Override
    public String toString() {
        return tipo + "," + lexema + "," + linea;
    }
}
