package br.cefetrj.iot.notificador;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.ComandoAlerta;
import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.EventoAlerta;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Mensagem;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.ResultadoComando;
import br.cefetrj.iot.contrato.TipoSinal;
import br.cefetrj.iot.notificador.estado.ContextoAlerta;
import br.cefetrj.iot.notificador.estado.TransicaoAlertaInvalidaException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ProcessadorComandos implements AutoCloseable {
    private record Chave(String pacienteId, TipoSinal sinal) {}

    private final CentralAlertas central;
    private final PublicadorMensagem publicador;
    private final Supplier<String> relogio;
    private final ExecutorService fila = Executors.newSingleThreadExecutor(
            tarefa -> new Thread(tarefa, "notificador-fila"));

    // Estes três índices são acessados exclusivamente pela thread da fila.
    private final Map<String, ContextoAlerta> porId = new HashMap<>();
    private final Map<Chave, ContextoAlerta> ativos = new HashMap<>();
    private final Set<String> leiturasProcessadas = new HashSet<>();
    private volatile boolean encerrando;

    public ProcessadorComandos(CentralAlertas central, PublicadorMensagem publicador) {
        this(central, publicador, () -> OffsetDateTime.now().toString());
    }

    public ProcessadorComandos(CentralAlertas central, PublicadorMensagem publicador, Supplier<String> relogio) {
        this.central = Objects.requireNonNull(central);
        this.publicador = Objects.requireNonNull(publicador);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public Future<?> enfileirarLeituraCritica(String topico, String json) {
        return agendar(topico, () -> processarLeitura(topico, json));
    }

    public Future<?> enfileirarComando(String topico, String json) {
        return agendar(topico, () -> processarComando(topico, json));
    }

    private Future<?> agendar(String topico, Runnable tarefa) {
        return fila.submit(() -> {
            try {
                tarefa.run();
            } catch (RuntimeException erro) {
                System.err.printf("Falha ao processar %s: %s%n", topico, erro.getMessage());
                throw erro;
            }
        });
    }

    private void processarLeitura(String topico, String json) {
        Leitura leitura;
        try {
            leitura = ConversorJson.lerNoTopico(topico, json, Leitura.class);
        } catch (IllegalArgumentException erro) {
            System.err.printf("Leitura descartada em %s: %s%n", topico, erro.getMessage());
            return;
        }
        if (leitura.prioridade() != Prioridade.CRITICO || leiturasProcessadas.contains(leitura.idLeitura())) {
            return;
        }

        Chave chave = new Chave(leitura.pacienteId(), leitura.tipoSinal());
        if (ativos.containsKey(chave)) {
            leiturasProcessadas.add(leitura.idLeitura());
            return;
        }

        String id = "alt-" + UUID.randomUUID();
        ContextoAlerta contexto = ContextoAlerta.abrir(leitura, id, relogio.get());
        porId.put(id, contexto);
        ativos.put(chave, contexto);
        leiturasProcessadas.add(leitura.idLeitura());
        divulgar(contexto.ultimoEvento());
    }

    private void processarComando(String topico, String json) {
        ComandoAlerta comando;
        try {
            comando = ConversorJson.lerNoTopico(topico, json, ComandoAlerta.class);
        } catch (IllegalArgumentException erro) {
            System.err.printf("Comando descartado em %s: %s%n", topico, erro.getMessage());
            return;
        }

        ContextoAlerta contexto = porId.get(comando.idAlerta());
        if (contexto == null) {
            responder(comando, ResultadoComando.Resultado.REJEITADO, "ALERTA_INEXISTENTE", null);
            return;
        }

        EventoAlerta evento;
        String agora = relogio.get();
        try {
            evento = switch (comando.acao()) {
                case RECONHECER -> contexto.reconhecer(comando.usuarioId(), agora);
                case RESOLVER -> contexto.resolver(comando.usuarioId(), agora, comando.motivoResolucao());
            };
        } catch (TransicaoAlertaInvalidaException erro) {
            responder(comando, ResultadoComando.Resultado.REJEITADO, "TRANSICAO_INVALIDA", contexto.alertaAtual());
            return;
        }

        Alerta atual = evento.atual();
        if (!contexto.estaAtivo()) {
            ativos.remove(new Chave(atual.pacienteId(), atual.tipoSinal()), contexto);
        }
        divulgar(evento);
        responder(comando, ResultadoComando.Resultado.APLICADO, null, atual);
    }

    private void divulgar(EventoAlerta evento) {
        // Cada mudança aceita é observada uma vez neste processo, inclusive quando o envio precisa ser repetido.
        central.notificar(evento);
        publicar(evento.atual(), true);
    }

    private void responder(ComandoAlerta comando, ResultadoComando.Resultado resultado, String motivo, Alerta atual) {
        ResultadoComando resposta = new ResultadoComando(comando.idComando(), comando.idAlerta(), resultado, motivo,
                atual == null ? null : atual.estado(), atual == null ? null : atual.versao());
        publicar(resposta, false);
    }

    private void publicar(Mensagem mensagem, boolean reter) {
        String topico = mensagem.topico();
        String json = ConversorJson.paraJson(mensagem);
        try {
            while (true) {
                try {
                    publicador.publicar(topico, json, 1, reter);
                    return;
                } catch (InterruptedException interrupcao) {
                    throw interrupcao;
                } catch (RuntimeException erroDePrograma) {
                    throw erroDePrograma;
                } catch (Exception falhaDeEnvio) {
                    if (encerrando) {
                        cancelarPendentes();
                        throw new IllegalStateException("Encerramento com envio sem confirmação em " + topico,
                                falhaDeEnvio);
                    }
                    System.err.printf("Envio sem confirmação em %s. Repetindo em 1 s: %s%n",
                            topico, falhaDeEnvio.getMessage());
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio interrompido em " + topico, erro);
        }
    }

    @Override
    public void close() {
        encerrando = true;
        fila.shutdown();
        try {
            if (!fila.awaitTermination(15, TimeUnit.SECONDS)) {
                cancelarPendentes();
                if (!fila.awaitTermination(12, TimeUnit.SECONDS)) {
                    System.err.println("A tarefa em andamento ainda não terminou após a interrupção.");
                }
            }
        } catch (InterruptedException erro) {
            cancelarPendentes();
            Thread.currentThread().interrupt();
        }
    }

    private void cancelarPendentes() {
        var descartadas = fila.shutdownNow();
        for (Runnable tarefa : descartadas) {
            if (tarefa instanceof Future<?> futuro) {
                futuro.cancel(false);
            }
        }
        System.err.printf("Fila interrompida no encerramento. Tarefas pendentes canceladas: %d.%n", descartadas.size());
    }
}
