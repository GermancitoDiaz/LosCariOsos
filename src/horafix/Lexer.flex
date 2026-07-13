package horafix;

%%

/**
 * ANALIZADOR LÉXICO — HoraFix
 *
 * Generado por JFlex 1.9.1. Para regenerar Lexer.java después de modificar
 * este archivo ejecuta: jflex src/horafix/Lexer.flex
 *
 * Responsabilidad: leer el código fuente carácter a carácter y producir
 * una secuencia de tokens con formato "TIPO,lexema,linea".
 * Ese formato es parseado en Interfaz.java para construir objetos Token.
 *
 * Tipos de token que produce este lexer:
 *   RESERVADA   — palabras clave del lenguaje (APROBADAS, MATERIA, etc.)
 *   IDENTIFICADOR — nombre de materia o cualquier secuencia alfanumérica
 *                   que no sea keyword ni día
 *   NUMERO      — entero sin ceros al inicio (0 ó [1-9][0-9]*)
 *   DOS_PUNTOS  — símbolo ':'
 *   COMA        — símbolo ','
 *   GUION       — símbolo '-'
 *   ERROR_CHAR  — carácter(es) inválido(s) o número con ceros al inicio
 *
 * Precedencia de reglas en JFlex:
 *   1. Coincidencia más larga gana (maximal munch).
 *   2. Entre reglas de igual longitud, gana la que aparece primero en el archivo.
 *   Por eso las palabras reservadas se declaran ANTES de la regla genérica de
 *   identificadores de mayúsculas, y 0{DIGITO}+ se declara ANTES de {NUMERO}.
 */

/* ── Directivas JFlex ────────────────────────────────────────────────────── */
%class Lexer          /* nombre de la clase Java generada                    */
%public               /* modificador de acceso de la clase                   */
%type String          /* tipo de retorno de yylex()                          */
%line                 /* activa el contador de líneas (disponible como yyline) */

/* ══════════════════════════════════════════════════════════════════════════
 * SECCIÓN 1 — DEFINICIÓN DE MACROS (expresiones regulares reutilizables)
 *
 * Las macros no generan código por sí solas; son abreviaturas que se
 * expanden dentro de las reglas de la sección 2.
 * ══════════════════════════════════════════════════════════════════════════ */

LETRA_MIN = [a-z]           /* letra minúscula — los identificadores DEBEN empezar aquí */
LETRA_MAY = [A-Z]           /* letra mayúscula — usada para detectar posibles keywords   */
LETRA     = [a-zA-Z]        /* cualquier letra (usada en el cuerpo de identificadores)   */
DIGITO    = [0-9]           /* dígito decimal                                            */

/*
 * ID — identificador válido (nombres de materias, reglas, etc.)
 * Regla: debe INICIAR con letra minúscula; el resto puede ser letra, dígito o '_'.
 * Ejemplos válidos: algebra, calculo1, fisica_moderna
 * Ejemplos inválidos: Algebra, CALCULO (inician en mayúscula → regla de mayúsculas más abajo)
 */
ID = {LETRA_MIN}({LETRA}|{DIGITO}|"_")*

/*
 * NUMERO — entero positivo válido.
 * Acepta exactamente: el cero solo ('0') o cualquier entero que inicie con 1-9.
 * La regla de error 0{DIGITO}+ (más abajo) captura 007, 030, etc. antes de que
 * esta macro los acepte como dos tokens separados (0 + 07).
 */
NUMERO = 0|[1-9]{DIGITO}*

%%

/* ══════════════════════════════════════════════════════════════════════════
 * SECCIÓN 2 — REGLAS LÉXICAS
 *
 * Cada regla tiene la forma:  <patrón> { acción Java }
 * La acción devuelve un String "TIPO,lexema,linea" o no devuelve nada
 * (espacios en blanco).
 * yytext() → texto reconocido   yyline → índice de línea (base 0, por eso +1)
 * ══════════════════════════════════════════════════════════════════════════ */

/* ── PALABRAS RESERVADAS ────────────────────────────────────────────────────
 * Deben declararse ANTES de la regla genérica de mayúsculas para que JFlex
 * les dé prioridad al encontrar exactamente "APROBADAS", "MATERIA", etc.
 * Si estas reglas estuvieran después, "MATERIA" sería reconocida como
 * IDENTIFICADOR en lugar de RESERVADA.
 */
"APROBADAS" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

"REPROBADAS" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

"CURSANDO" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

"MATERIA" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

"REGLA" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

"MAX_CREDITOS" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

/*
 * CREDITOS — palabra reservada que indica los créditos de una materia.
 * Va siempre después del horario en la sentencia MATERIA:
 *   MATERIA nombre horaInicio-horaFin CREDITOS n
 * Valor válido: entero entre 3 y 6 (validado semánticamente). Los créditos
 * determinan automáticamente los días de clase (siempre desde Lunes), por lo
 * que ya no existe un token de día explícito en la gramática.
 * Declarada ANTES de la regla genérica de mayúsculas para tener prioridad.
 */
"CREDITOS" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

/* ── SEPARADORES ────────────────────────────────────────────────────────────
 * Tres símbolos con rol estructural en la gramática:
 *   ':'  separa la keyword de su lista (APROBADAS: mat1, mat2)
 *   ','  separa elementos en una lista de materias
 *   '-'  separa hora de inicio y fin en una declaración MATERIA
 */
":" {
    return "DOS_PUNTOS," + yytext() + "," + (yyline+1);
}

"," {
    return "COMA," + yytext() + "," + (yyline+1);
}

"-" {
    return "GUION," + yytext() + "," + (yyline+1);
}

/* ── ERROR LÉXICO: NÚMEROS CON CEROS AL INICIO ──────────────────────────────
 * Patrón: un '0' seguido de uno o más dígitos (ej: 007, 030, 001).
 * Se declara ANTES de {NUMERO} para aprovechar la regla de coincidencia
 * más larga: "007" (3 chars) gana sobre "0" (1 char) + resto.
 * El parser y la Interfaz reconocen ERROR_CHAR como E-L02.
 */
0{DIGITO}+ {
    return "ERROR_CHAR," + yytext() + "," + (yyline+1);
}

/* ── NÚMEROS ENTEROS VÁLIDOS ────────────────────────────────────────────── */
{NUMERO} {
    return "NUMERO," + yytext() + "," + (yyline+1);
}

/* ── IDENTIFICADORES VÁLIDOS (nombres de materias) ──────────────────────── */
{ID} {
    return "IDENTIFICADOR," + yytext() + "," + (yyline+1);
}

/* ── PALABRAS QUE INICIAN EN MAYÚSCULA Y NO SON KEYWORDS ────────────────────
 * Regla de captura para palabras como "Algebra", "APROBDA", "MaTERIA".
 * Se emiten como IDENTIFICADOR (no como error) para que el Parser pueda
 * aplicar Levenshtein y sugerir la keyword correcta si corresponde (E-L03).
 * Las keywords exactas declaradas arriba siempre ganan por precedencia.
 */
{LETRA_MAY}({LETRA}|{DIGITO}|"_")* {
    return "IDENTIFICADOR," + yytext() + "," + (yyline+1);
}

/* ── ESPACIOS EN BLANCO: IGNORAR ────────────────────────────────────────── */
[ \t\r\n]+ {
    /* espacios, tabuladores y saltos de línea no producen token */
}

/* ── ERROR LÉXICO: CARACTERES INVÁLIDOS ─────────────────────────────────────
 * Regla de último recurso (fallback): captura todo lo que ninguna regla
 * anterior reconoció. El '+' agrupa caracteres inválidos consecutivos en
 * un único token ERROR_CHAR en lugar de uno por carácter.
 * Ejemplos: "@", "#$%", "¡" → un solo ERROR_CHAR por grupo.
 * El parser y la Interfaz reconocen ERROR_CHAR como E-L01.
 */
[^a-zA-Z0-9 \t\r\n:,\-]+ {
    return "ERROR_CHAR," + yytext() + "," + (yyline+1);
}
