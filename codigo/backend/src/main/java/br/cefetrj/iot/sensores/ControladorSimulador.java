package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.ComandoSimulador;
import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.EstadoCicloAlerta;
import br.cefetrj.iot.contrato.EstadoSimulador;
import br.cefetrj.iot.contrato.TipoSinal;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Controla os modos dos leitos simulados a partir de comandos MQTT e publica o estado retido.
 */
public final class ControladorSimulador {
    private final List<LeitoSimulado> leitos;
    private final PublicadorSensor.DestinoPublicacao destino;
    private final Supplier<String> relogio;

    public ControladorSimulador(List<LeitoSimulado> leitos, PublicadorSensor.DestinoPublicacao destino) {
        this(leitos, destino, () -> OffsetDateTime.now().toString());
    }

    public ControladorSimulador(List<LeitoSimulado> leitos, PublicadorSensor.DestinoPublicacao destino,
            Supplier<String> relogio) {
        this.leitos = List.copyOf(Objects.requireNonNull(leitos, "A lista de leitos não pode ser nula."));
        this.destino = Objects.requireNonNull(destino, "O destino de publicação não pode ser nulo.");
        this.relogio = Objects.requireNonNull(relogio, "O relógio não pode ser nulo.");
    }

    public void publicarEstadosIniciais() {
        for (LeitoSimulado leito : leitos) {
            publicarEstado(leito);
        }
    }

    public synchronized void publicarEstado(LeitoSimulado leito) {
        Objects.requireNonNull(leito, "O leito não pode ser nulo.");
        Map<TipoSinal, ModoOperacao> modos = leito.snapshotModos();
        EstadoSimulador estado = new EstadoSimulador(
                leito.ala(),
                leito.leito(),
                leito.pacienteId(),
                paraCenario(modos.get(TipoSinal.FREQUENCIA)),
                paraCenario(modos.get(TipoSinal.TEMPERATURA)),
                paraCenario(modos.get(TipoSinal.SATURACAO)),
                paraCenario(modos.get(TipoSinal.PRESSAO)),
                relogio.get());
        try {
            destino.publicar(estado.topico(), ConversorJson.paraJson(estado), 1, true);
        } catch (Exception e) {
            System.err.println("[Simulador] Falha ao publicar estado de " + leito.chaveUnica() + ": " + e.getMessage());
        }
    }

    public synchronized void processarComando(String topico, String json) {
        ComandoSimulador comando;
        try {
            comando = ConversorJson.lerNoTopico(topico, json, ComandoSimulador.class);
        } catch (IllegalArgumentException e) {
            System.err.println("[Simulador] Comando descartado em " + topico + ": " + e.getMessage());
            return;
        }

        Optional<LeitoSimulado> leitoOpt = buscarLeito(comando.ala(), comando.leito());
        if (leitoOpt.isEmpty()) {
            System.err.println("[Simulador] Leito desconhecido no comando: " + comando.ala() + "/" + comando.leito());
            return;
        }

        LeitoSimulado leito = leitoOpt.get();
        ModoOperacao modo = paraModo(comando.cenario());
        if (comando.tipoSinal() == null) {
            leito.definirModoTodos(modo);
            System.out.println("[Simulador][MQTT] Modo " + modo + " aplicado a todos os sinais de "
                    + leito.chaveUnica() + " (" + leito.pacienteId() + ").");
        } else {
            leito.definirModo(comando.tipoSinal(), modo);
            System.out.println("[Simulador][MQTT] Modo " + modo + " aplicado em "
                    + leito.chaveUnica() + " (" + leito.pacienteId() + ") -> " + comando.tipoSinal().name());
        }
        publicarEstado(leito);
    }

    public synchronized void processarEstadoAlerta(String topico, String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        Alerta alerta;
        try {
            alerta = ConversorJson.lerNoTopico(topico, json, Alerta.class);
        } catch (IllegalArgumentException e) {
            System.err.println("[Simulador] Estado de alerta descartado em " + topico + ": " + e.getMessage());
            return;
        }
        if (alerta.estado() != EstadoCicloAlerta.RECONHECIDO) {
            return;
        }
        Optional<LeitoSimulado> leitoOpt = buscarLeito(alerta.ala(), alerta.leito());
        if (leitoOpt.isEmpty() || !leitoOpt.get().pacienteId().equals(alerta.pacienteId())) {
            return;
        }
        LeitoSimulado leito = leitoOpt.get();
        long instanteReconhecimentoMs = OffsetDateTime.parse(alerta.reconhecidoEm()).toInstant().toEpochMilli();
        if (leito.iniciarRecuperacaoGradual(alerta.tipoSinal(), instanteReconhecimentoMs)) {
            System.out.println("[Simulador][MQTT] Recuperação gradual de 15 s iniciada em "
                    + leito.chaveUnica() + " (" + leito.pacienteId() + ") -> " + alerta.tipoSinal().name());
        }
    }

    private Optional<LeitoSimulado> buscarLeito(String ala, String leito) {
        return leitos.stream()
                .filter(item -> normalizarSegmento(item.ala()).equals(ala)
                        && normalizarSegmento(item.leito()).equals(leito))
                .findFirst();
    }

    private static ModoOperacao paraModo(ComandoSimulador.Cenario cenario) {
        return cenario == ComandoSimulador.Cenario.CRISE ? ModoOperacao.CRISE : ModoOperacao.ROTINA;
    }

    private static ComandoSimulador.Cenario paraCenario(ModoOperacao modo) {
        return modo == ModoOperacao.CRISE ? ComandoSimulador.Cenario.CRISE : ComandoSimulador.Cenario.ROTINA;
    }

    private static String normalizarSegmento(String texto) {
        String semAcento = Normalizer.normalize(texto.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return semAcento.replaceAll("\\p{M}+", "").replaceAll("\\s+", "-");
    }
}
