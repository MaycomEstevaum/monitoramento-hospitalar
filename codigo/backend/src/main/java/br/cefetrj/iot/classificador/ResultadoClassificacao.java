package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.Prioridade;
import java.util.Objects;

public record ResultadoClassificacao(Prioridade prioridade, String motivo) {
    public ResultadoClassificacao {
        Objects.requireNonNull(prioridade, "prioridade é obrigatória.");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("motivo é obrigatório.");
        }
    }
}
