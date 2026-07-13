package horafix;

%%

%class Lexer
%public
%type String
%line

LETRA = [a-zA-Z]
DIGITO = [0-9]

ID = {LETRA}({LETRA}|{DIGITO}|"_")*

NUMERO = {DIGITO}+

HORA = {DIGITO}+":"{DIGITO}+

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

"MAX_CREDITOS" {
    return "RESERVADA," + yytext() + "," + (yyline+1);
}

"LUNES"|"MARTES"|"MIERCOLES"|"JUEVES"|"VIERNES" {
    return "DIA," + yytext() + "," + (yyline+1);
}

{HORA} {
    return "HORA," + yytext() + "," + (yyline+1);
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

{NUMERO} {
    return "NUMERO," + yytext() + "," + (yyline+1);
}

{ID} {
    return "IDENTIFICADOR," + yytext() + "," + (yyline+1);
}

[ \t\r\n]+ {
    /* Ignorar espacios */
}

. {
    return "ERROR," + yytext() + "," + (yyline+1);
}