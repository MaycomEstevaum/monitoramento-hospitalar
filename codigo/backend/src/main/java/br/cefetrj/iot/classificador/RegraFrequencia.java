package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.TipoSinal;
import java.util.Objects;

public final class RegraFrequencia implements RegraClassificacao {

    @Override
    public ResultadoClassificacao classificar(Leitura leitura) {
        Objects.requireNonNull(leitura, "leitura é obrigatória.");
        if (leitura.tipoSinal() != TipoSinal.FREQUENCIA || leitura.valor() == null) {
            throw new IllegalArgumentException("RegraFrequencia exige uma leitura de FREQUENCIA com valor numérico.");
        }

        double bpm = leitura.valor();
        if (bpm < 50.0 || bpm > 120.0) {
            return new ResultadoClassificacao(
                    Prioridade.CRITICO,
                    "Frequência cardíaca na faixa crítica");
        }
        if (bpm >= 60.0 && bpm <= 100.0) {
            return new ResultadoClassificacao(
                    Prioridade.ROTINA,
                    "Frequência cardíaca na faixa de rotina");
        }
        return new ResultadoClassificacao(
                Prioridade.ATENCAO,
                "Frequência cardíaca na faixa de atenção");
    }
}
