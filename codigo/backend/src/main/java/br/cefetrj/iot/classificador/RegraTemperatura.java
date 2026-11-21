package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.TipoSinal;
import java.util.Objects;

public final class RegraTemperatura implements RegraClassificacao {

    @Override
    public ResultadoClassificacao classificar(Leitura leitura) {
        Objects.requireNonNull(leitura, "leitura é obrigatória.");
        if (leitura.tipoSinal() != TipoSinal.TEMPERATURA || leitura.valor() == null) {
            throw new IllegalArgumentException("RegraTemperatura exige uma leitura de TEMPERATURA com valor numérico.");
        }

        double temperatura = leitura.valor();
        if (temperatura <= 35.0 || temperatura >= 39.0) {
            return new ResultadoClassificacao(
                    Prioridade.CRITICO,
                    "Temperatura corporal na faixa crítica");
        }
        if (temperatura >= 36.0 && temperatura <= 37.5) {
            return new ResultadoClassificacao(
                    Prioridade.ROTINA,
                    "Temperatura corporal na faixa de rotina");
        }
        return new ResultadoClassificacao(
                Prioridade.ATENCAO,
                "Temperatura corporal na faixa de atenção");
    }
}
