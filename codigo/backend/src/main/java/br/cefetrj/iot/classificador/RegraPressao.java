package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.TipoSinal;
import java.util.Objects;

public final class RegraPressao implements RegraClassificacao {

    @Override
    public ResultadoClassificacao classificar(Leitura leitura) {
        Objects.requireNonNull(leitura, "leitura é obrigatória.");
        if (leitura.tipoSinal() != TipoSinal.PRESSAO
                || leitura.sistolica() == null
                || leitura.diastolica() == null) {
            throw new IllegalArgumentException(
                    "RegraPressao exige uma leitura de PRESSAO com sistólica e diastólica numéricas.");
        }

        ResultadoClassificacao sistolica = avaliarSistolica(leitura.sistolica());
        ResultadoClassificacao diastolica = avaliarDiastolica(leitura.diastolica());

        int grauSistolica = grau(sistolica.prioridade());
        int grauDiastolica = grau(diastolica.prioridade());
        if (grauSistolica > grauDiastolica) return sistolica;
        if (grauDiastolica > grauSistolica) return diastolica;

        String motivo = switch (sistolica.prioridade()) {
            case ROTINA -> "Pressão arterial na faixa de rotina da simulação";
            case ATENCAO -> "Sistólica e diastólica na faixa de atenção da simulação";
            case CRITICO -> "Sistólica e diastólica na faixa crítica da simulação";
        };
        return new ResultadoClassificacao(sistolica.prioridade(), motivo);
    }

    private static int grau(Prioridade prioridade) {
        return switch (prioridade) {
            case ROTINA -> 0;
            case ATENCAO -> 1;
            case CRITICO -> 2;
        };
    }

    private static ResultadoClassificacao avaliarSistolica(double sistolica) {
        if (sistolica < 80.0 || sistolica >= 180.0) {
            return new ResultadoClassificacao(
                    Prioridade.CRITICO,
                    "Sistólica na faixa crítica da simulação");
        }
        if (sistolica >= 90.0 && sistolica <= 139.0) {
            return new ResultadoClassificacao(
                    Prioridade.ROTINA,
                    "Sistólica na faixa de rotina da simulação");
        }
        return new ResultadoClassificacao(
                Prioridade.ATENCAO,
                "Sistólica na faixa de atenção da simulação");
    }

    private static ResultadoClassificacao avaliarDiastolica(double diastolica) {
        if (diastolica < 50.0 || diastolica >= 120.0) {
            return new ResultadoClassificacao(
                    Prioridade.CRITICO,
                    "Diastólica na faixa crítica da simulação");
        }
        if (diastolica >= 60.0 && diastolica <= 89.0) {
            return new ResultadoClassificacao(
                    Prioridade.ROTINA,
                    "Diastólica na faixa de rotina da simulação");
        }
        return new ResultadoClassificacao(
                Prioridade.ATENCAO,
                "Diastólica na faixa de atenção da simulação");
    }
}
