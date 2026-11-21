package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.TipoSinal;
import java.util.Objects;

public final class RegraSaturacao implements RegraClassificacao {

    @Override
    public ResultadoClassificacao classificar(Leitura leitura) {
        Objects.requireNonNull(leitura, "leitura é obrigatória.");
        if (leitura.tipoSinal() != TipoSinal.SATURACAO || leitura.valor() == null) {
            throw new IllegalArgumentException("RegraSaturacao exige uma leitura de SATURACAO com valor numérico.");
        }

        double saturacao = leitura.valor();
        if (saturacao < 90.0) {
            return new ResultadoClassificacao(
                    Prioridade.CRITICO,
                    "Saturação de oxigênio na faixa crítica");
        }
        if (saturacao >= 95.0) {
            return new ResultadoClassificacao(
                    Prioridade.ROTINA,
                    "Saturação de oxigênio na faixa de rotina");
        }
        return new ResultadoClassificacao(
                Prioridade.ATENCAO,
                "Saturação de oxigênio na faixa de atenção");
    }
}
