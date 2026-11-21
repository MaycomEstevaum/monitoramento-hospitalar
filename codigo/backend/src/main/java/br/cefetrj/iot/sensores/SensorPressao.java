package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.TipoSinal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SensorPressao implements Sensor {
    private static final double SISTOLICA_ROTINA_MIN = 105.0;
    private static final double SISTOLICA_ROTINA_MAX = 130.0;
    private static final double SISTOLICA_ROTINA_PASSO = 3.0;

    private static final double SISTOLICA_CRISE_MIN = 185.0;
    private static final double SISTOLICA_CRISE_MAX = 205.0;
    private static final double SISTOLICA_CRISE_PASSO = 4.0;

    private static final double DIASTOLICA_ROTINA_MIN = 65.0;
    private static final double DIASTOLICA_ROTINA_MAX = 82.0;
    private static final double DIASTOLICA_ROTINA_PASSO = 2.0;

    private static final double DIASTOLICA_CRISE_MIN = 122.0;
    private static final double DIASTOLICA_CRISE_MAX = 135.0;
    private static final double DIASTOLICA_CRISE_PASSO = 3.0;

    private final Map<String, Double> ultimaSistolicaPorLeito = new ConcurrentHashMap<>();
    private final Map<String, Double> ultimaDiastolicaPorLeito = new ConcurrentHashMap<>();

    @Override
    public TipoSinal getTipoSinal() {
        return TipoSinal.PRESSAO;
    }

    @Override
    public Leitura coletar(LeitoSimulado leito) {
        Objects.requireNonNull(leito, "O leito não pode ser nulo.");
        OptionalDouble progresso = leito.progressoRecuperacao(getTipoSinal());
        String chave = leito.chaveUnica();
        double sistolica;
        double diastolica;
        if (progresso.isPresent()) {
            double p = progresso.getAsDouble();
            sistolica = Math.round(195.0 - 53.0 * p);
            diastolica = Math.round(128.0 - 36.0 * p);
            ultimaSistolicaPorLeito.put(chave, sistolica);
            ultimaDiastolicaPorLeito.put(chave, diastolica);
        } else {
            ModoOperacao modo = leito.modoAtual(getTipoSinal());
            sistolica = (modo == ModoOperacao.CRISE)
                    ? calcularProximo(ultimaSistolicaPorLeito, chave, SISTOLICA_CRISE_MIN, SISTOLICA_CRISE_MAX, SISTOLICA_CRISE_PASSO)
                    : calcularProximo(ultimaSistolicaPorLeito, chave, SISTOLICA_ROTINA_MIN, SISTOLICA_ROTINA_MAX, SISTOLICA_ROTINA_PASSO);

            diastolica = (modo == ModoOperacao.CRISE)
                    ? calcularProximo(ultimaDiastolicaPorLeito, chave, DIASTOLICA_CRISE_MIN, DIASTOLICA_CRISE_MAX, DIASTOLICA_CRISE_PASSO)
                    : calcularProximo(ultimaDiastolicaPorLeito, chave, DIASTOLICA_ROTINA_MIN, DIASTOLICA_ROTINA_MAX, DIASTOLICA_ROTINA_PASSO);
        }

        return Leitura.pressao(
                UUID.randomUUID().toString(),
                leito.pacienteId(),
                leito.ala(),
                leito.leito(),
                sistolica,
                diastolica,
                OffsetDateTime.now().toString()
        );
    }

    private static double calcularProximo(Map<String, Double> historico, String chave, double min, double max, double passo) {
        double atual = historico.getOrDefault(chave, (min + max) / 2.0);
        if (atual < min || atual > max) {
            atual = (min + max) / 2.0;
        }
        double delta = ThreadLocalRandom.current().nextDouble(-passo, passo);
        double proximo = Math.round(Math.max(min, Math.min(max, atual + delta)));
        historico.put(chave, proximo);
        return proximo;
    }
}
