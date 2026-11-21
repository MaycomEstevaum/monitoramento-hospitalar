package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.TipoSinal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

public final class LeitoSimulado {
    public static final long DURACAO_RECUPERACAO_MS = 15_000L;

    private final String ala;
    private final String leito;
    private final String pacienteId;
    private final LongSupplier relogioMs;
    private final Map<TipoSinal, ModoOperacao> modosPorSinal = new EnumMap<>(TipoSinal.class);
    private final Map<TipoSinal, Long> inicioCriseMs = new EnumMap<>(TipoSinal.class);
    private final Map<TipoSinal, Long> inicioRecuperacaoMs = new EnumMap<>(TipoSinal.class);
    private boolean recuperacaoRecemConcluida;

    public LeitoSimulado(String ala, String leito, String pacienteId) {
        this(ala, leito, pacienteId, System::currentTimeMillis);
    }

    public LeitoSimulado(String ala, String leito, String pacienteId, LongSupplier relogioMs) {
        if (ala == null || ala.isBlank() || leito == null || leito.isBlank() || pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("Informe ala, leito e pacienteId.");
        }
        this.ala = ala.strip();
        this.leito = leito.strip();
        this.pacienteId = pacienteId.strip();
        this.relogioMs = Objects.requireNonNull(relogioMs, "O relógio não pode ser nulo.");
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
        inicioRecuperacaoMs.remove(sinal);
        if (modo == ModoOperacao.CRISE) {
            inicioCriseMs.put(sinal, relogioMs.getAsLong());
        } else {
            inicioCriseMs.remove(sinal);
        }
    }

    public synchronized void definirModoTodos(ModoOperacao modo) {
        Objects.requireNonNull(modo, "O modo de operação não pode ser nulo.");
        for (TipoSinal sinal : TipoSinal.values()) {
            definirModo(sinal, modo);
        }
    }

    public synchronized boolean iniciarRecuperacaoGradual(TipoSinal sinal, long instanteReconhecimentoMs) {
        Objects.requireNonNull(sinal, "O tipo de sinal não pode ser nulo.");
        if (modosPorSinal.get(sinal) != ModoOperacao.CRISE || inicioRecuperacaoMs.containsKey(sinal)) {
            return false;
        }
        long inicioCrise = inicioCriseMs.getOrDefault(sinal, Long.MAX_VALUE);
        if (instanteReconhecimentoMs < inicioCrise) {
            return false;
        }
        inicioRecuperacaoMs.put(sinal, relogioMs.getAsLong());
        return true;
    }

    public synchronized OptionalDouble progressoRecuperacao(TipoSinal sinal) {
        Objects.requireNonNull(sinal, "O tipo de sinal não pode ser nulo.");
        Long inicio = inicioRecuperacaoMs.get(sinal);
        if (inicio == null) {
            return OptionalDouble.empty();
        }
        long decorrido = Math.max(0L, relogioMs.getAsLong() - inicio);
        if (decorrido >= DURACAO_RECUPERACAO_MS) {
            concluirRecuperacao(sinal);
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((double) decorrido / DURACAO_RECUPERACAO_MS);
    }

    public synchronized boolean concluirRecuperacoesVencidas() {
        long agora = relogioMs.getAsLong();
        for (TipoSinal sinal : TipoSinal.values()) {
            Long inicio = inicioRecuperacaoMs.get(sinal);
            if (inicio != null && agora - inicio >= DURACAO_RECUPERACAO_MS) {
                concluirRecuperacao(sinal);
            }
        }
        boolean mudou = recuperacaoRecemConcluida;
        recuperacaoRecemConcluida = false;
        return mudou;
    }

    public synchronized boolean emRecuperacao(TipoSinal sinal) {
        Objects.requireNonNull(sinal, "O tipo de sinal não pode ser nulo.");
        return inicioRecuperacaoMs.containsKey(sinal);
    }

    private void concluirRecuperacao(TipoSinal sinal) {
        modosPorSinal.put(sinal, ModoOperacao.ROTINA);
        inicioRecuperacaoMs.remove(sinal);
        inicioCriseMs.remove(sinal);
        recuperacaoRecemConcluida = true;
    }

    public synchronized Map<TipoSinal, ModoOperacao> snapshotModos() {
        return new EnumMap<>(modosPorSinal);
    }

    public synchronized String resumoModos() {
        return modosPorSinal.entrySet().stream()
                .map(entry -> {
                    String texto = entry.getValue().name();
                    if (inicioRecuperacaoMs.containsKey(entry.getKey())) {
                        texto += "(RECUPERANDO)";
                    }
                    return entry.getKey().name() + "=" + texto;
                })
                .collect(Collectors.joining(", "));
    }
}
