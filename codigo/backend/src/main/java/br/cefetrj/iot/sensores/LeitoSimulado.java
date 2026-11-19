package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.TipoSinal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LeitoSimulado {
    private final String ala;
    private final String leito;
    private final String pacienteId;
    private final Map<TipoSinal, ModoOperacao> modosPorSinal = new EnumMap<>(TipoSinal.class);

    public LeitoSimulado(String ala, String leito, String pacienteId) {
        if (ala == null || ala.isBlank() || leito == null || leito.isBlank() || pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("Informe ala, leito e pacienteId.");
        }
        this.ala = ala.strip();
        this.leito = leito.strip();
        this.pacienteId = pacienteId.strip();
        for (TipoSinal sinal : TipoSinal.values()) {
            modosPorSinal.put(sinal, ModoOperacao.ROTINA);
        }
    }

    public String ala() {
        return ala;
    }

    public String leito() {
        return leito;
    }

    public String pacienteId() {
        return pacienteId;
    }

    public String chaveUnica() {
        return ala + "/" + leito;
    }

    public synchronized ModoOperacao modoAtual(TipoSinal sinal) {
        Objects.requireNonNull(sinal, "O tipo de sinal não pode ser nulo.");
        return modosPorSinal.getOrDefault(sinal, ModoOperacao.ROTINA);
    }

    public synchronized void definirModo(TipoSinal sinal, ModoOperacao modo) {
        Objects.requireNonNull(sinal, "O tipo de sinal não pode ser nulo.");
        Objects.requireNonNull(modo, "O modo de operação não pode ser nulo.");
        modosPorSinal.put(sinal, modo);
    }

    public synchronized void definirModoTodos(ModoOperacao modo) {
        Objects.requireNonNull(modo, "O modo de operação não pode ser nulo.");
        for (TipoSinal sinal : TipoSinal.values()) {
            modosPorSinal.put(sinal, modo);
        }
    }

    public synchronized Map<TipoSinal, ModoOperacao> snapshotModos() {
        return new EnumMap<>(modosPorSinal);
    }

    public String resumoModos() {
        return snapshotModos().entrySet().stream()
                .map(entry -> entry.getKey().name() + "=" + entry.getValue().name())
                .collect(Collectors.joining(", "));
    }
}
