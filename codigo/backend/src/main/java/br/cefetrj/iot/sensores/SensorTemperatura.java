package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.TipoSinal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SensorTemperatura implements Sensor {
    private static final double ROTINA_MIN = 36.2;
    private static final double ROTINA_MAX = 37.3;
    private static final double ROTINA_PASSO = 0.15;

    private static final double CRISE_MIN = 39.3;
    private static final double CRISE_MAX = 40.2;
    private static final double CRISE_PASSO = 0.2;

    private final Map<String, Double> ultimoValorPorLeito = new ConcurrentHashMap<>();

    @Override
    public TipoSinal getTipoSinal() {
        return TipoSinal.TEMPERATURA;
    }

    @Override
    public Leitura coletar(LeitoSimulado leito) {
        Objects.requireNonNull(leito, "O leito não pode ser nulo.");
        ModoOperacao modo = leito.modoAtual(getTipoSinal());
        double valor = calcularProximoValor(leito.chaveUnica(), modo);

        return Leitura.simples(
                UUID.randomUUID().toString(),
                leito.pacienteId(),
                leito.ala(),
                leito.leito(),
                getTipoSinal(),
                valor,
                OffsetDateTime.now().toString()
        );
    }

    private double calcularProximoValor(String chaveLeito, ModoOperacao modo) {
        double min = (modo == ModoOperacao.CRISE) ? CRISE_MIN : ROTINA_MIN;
        double max = (modo == ModoOperacao.CRISE) ? CRISE_MAX : ROTINA_MAX;
        double passo = (modo == ModoOperacao.CRISE) ? CRISE_PASSO : ROTINA_PASSO;

        double atual = ultimoValorPorLeito.getOrDefault(chaveLeito, (min + max) / 2.0);
        if (atual < min || atual > max) {
            atual = (min + max) / 2.0;
        }

        double delta = ThreadLocalRandom.current().nextDouble(-passo, passo);
        double limitado = limitar(atual + delta, min, max);
        double proximo = Math.round(limitado * 10.0) / 10.0;
        ultimoValorPorLeito.put(chaveLeito, proximo);
        return proximo;
    }

    private static double limitar(double valor, double min, double max) {
        return Math.max(min, Math.min(max, valor));
    }
}
