package horafix;

%%

/**
 * Especificación JFlex del analizador léxico de HoraFix.
 * Para regenerar Lexer.java tras modificar este archivo: jflex src/horafix/Lexer.flex
 *
 * Cada regla produce una cadena "TIPO,lexema,linea", que Interfaz.java
 * convierte en un objeto Token.
 *
 * JFlex resuelve ambigüedades por coincidencia más larga y, en caso de
 * empate, por orden de declaración. Por eso las palabras reservadas y el
 * patrón de ceros a la izquierda se declaran antes que las reglas genéricas
 * que, de lo contrario, las capturarían primero.
 */
%class Lexer
%public
%type String
%line

LETRA_MIN = [a-z]
LETRA_MAY = [A-Z]
LETRA     = [a-zA-Z]
DIGITO    = [0-9]

/* Identificador: debe iniciar en minúscula (algebra, calculo1, fisica_moderna). */
ID = {LETRA_MIN}({LETRA}|{DIGITO}|"_")*

/* Entero positivo sin ceros a la izquierda. */
NUMERO = 0|[1-9]{DIGITO}*

%%

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

"SERIACION" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

"MAX_CREDITOS" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

/*
 * Créditos de una materia; se declara al final de cada sentencia MATERIA
 * (MATERIA nombre horaInicio-horaFin CREDITOS n). El rango válido (3–6) se
 * valida en el análisis semántico. A partir de los créditos se calculan los
 * días de clase de la materia, siempre a partir de lunes.
 */
"CREDITOS" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

":" {
    return "DOS_PUNTOS," + yytext() + "," + (yyline+1);
}

"," {
    return "COMA," + yytext() + "," + (yyline+1);
}

"-" {
    return "GUION," + yytext() + "," + (yyline+1);
}

/* Número con ceros a la izquierda (007, 030): error léxico E-L02. */
0{DIGITO}+ {
    return "ERROR_CHAR," + yytext() + "," + (yyline+1);
}

{NUMERO} {
    return "NUMERO," + yytext() + "," + (yyline+1);
}

{ID} {
    return "IDENTIFICADOR," + yytext() + "," + (yyline+1);
}

/*
 * Palabras que inician en mayúscula pero no son ninguna keyword exacta
 * (Algebra, APROBDA, MaTERIA). Se emiten como IDENTIFICADOR en vez de error
 * para que el Parser pueda sugerir la keyword correcta por Levenshtein (E-L03).
 */
{LETRA_MAY}({LETRA}|{DIGITO}|"_")* {
    return "IDENTIFICADOR," + yytext() + "," + (yyline+1);
}

[ \t\r\n]+ { /* ignorar espacios en blanco */ }

/* Caracteres no reconocidos por ninguna regla anterior: error léxico E-L01. */
[^a-zA-Z0-9 \t\r\n:,\-]+ {
    return "ERROR_CHAR," + yytext() + "," + (yyline+1);
}
