package br.cefetrj.iot.contrato;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

final class Campos {
    private Campos() {
    }

    static void exigir(boolean condicao, String mensagem) {
        if (!condicao)
            throw new IllegalArgumentException(mensagem);
    }

    static void texto(String valor, String campo) {
        exigir(valor != null && !valor.isBlank(), campo + " é obrigatório.");
    }

    static void instante(String valor, String campo) {
        texto(valor, campo);
        try {
            OffsetDateTime.parse(valor);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(campo + " deve incluir data, hora e fuso.", e);
        }
    }
}