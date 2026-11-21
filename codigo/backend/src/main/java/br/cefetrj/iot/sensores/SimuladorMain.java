package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.TipoSinal;
import br.cefetrj.iot.contrato.Topicos;
import br.cefetrj.iot.mqtt.ClienteMqtt;
import br.cefetrj.iot.mqtt.ConfiguracaoMqtt;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SimuladorMain {
    private static final long INTERVALO_PUBLICACAO_MS = 1500L;

    public static void main(String[] args) throws Exception {
        ConfiguracaoMqtt config = ConfiguracaoMqtt.padrao("simulador");
        List<LeitoSimulado> leitos = criarLeitosPadrao();
        AtomicReference<ControladorSimulador> destinoControlador = new AtomicReference<>();

        try (ClienteMqtt clienteMqtt = new ClienteMqtt(config, (topico, json) -> {
            ControladorSimulador atual = destinoControlador.get();
            if (atual == null) {
                return;
            }
            if (topico.startsWith("comandos/simulador/")) {
                atual.processarComando(topico, json);
            } else if (topico.startsWith("alertas/") && topico.endsWith("/estado")) {
                atual.processarEstadoAlerta(topico, json);
            }
        })) {
            clienteMqtt.conectar();
            ControladorSimulador controlador = new ControladorSimulador(leitos, clienteMqtt::publicar);
            destinoControlador.set(controlador);
            clienteMqtt.assinar(Topicos.COMANDOS_SIMULADOR, 1);
            clienteMqtt.assinar(Topicos.ESTADOS, 1);
            controlador.publicarEstadosIniciais();
            System.out.println("Simulador conectado ao broker: " + config.brokerUri()
                    + " (clientId=" + config.clientId() + ")");
            System.out.println("Controle MQTT ativo em " + Topicos.COMANDOS_SIMULADOR + " (QoS 1)");
            System.out.println("Recuperação gradual (15 s) ativa via " + Topicos.ESTADOS + " (QoS 1)");

            List<PublicadorSensor> publicadores = List.of(
                    new PublicadorFrequencia(clienteMqtt),
                    new PublicadorTemperatura(clienteMqtt),
                    new PublicadorSaturacao(clienteMqtt),
                    new PublicadorPressao(clienteMqtt)
            );

            ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor();
            AtomicBoolean executando = new AtomicBoolean(true);
            try {
                agendador.scheduleAtFixedRate(
                        () -> publicarCiclo(leitos, publicadores, controlador, executando),
                        0L, INTERVALO_PUBLICACAO_MS, TimeUnit.MILLISECONDS);
                imprimirAjuda(leitos);
                executarConsole(leitos, controlador);
            } finally {
                destinoControlador.set(null);
                executando.set(false);
                encerrarPublicacoes(agendador);
            }
        }
        System.out.println("Simulador encerrado.");
    }

    private static void encerrarPublicacoes(ScheduledExecutorService agendador) {
        agendador.shutdown();
        boolean interrompido = false;
        // O envio em andamento termina (ou atinge o timeout) antes de fechar o cliente MQTT.
        while (!agendador.isTerminated()) {
            try {
                agendador.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrompido = true;
                agendador.shutdownNow();
            }
        }
        if (interrompido) Thread.currentThread().interrupt();
    }

    private static List<LeitoSimulado> criarLeitosPadrao() {
        return List.of(
                new LeitoSimulado("uti", "leito-01", "PAC-101"),
                new LeitoSimulado("uti", "leito-02", "PAC-102"),
                new LeitoSimulado("enfermaria", "leito-03", "PAC-201"),
                new LeitoSimulado("enfermaria", "leito-04", "PAC-202"),
                new LeitoSimulado("enfermaria", "leito-05", "PAC-203")
        );
    }

    private static void publicarCiclo(List<LeitoSimulado> leitos, List<PublicadorSensor> publicadores,
                                     ControladorSimulador controlador, AtomicBoolean executando) {
        for (LeitoSimulado leito : leitos) {
            if (!executando.get()) return;
            if (leito.concluirRecuperacoesVencidas()) {
                controlador.publicarEstado(leito);
            }
            for (PublicadorSensor publicador : publicadores) {
                if (!executando.get()) return;
                try {
                    publicador.publicar(leito);
                } catch (Exception e) {
                    System.err.println("Falha ao publicar leitura de " + leito.chaveUnica() + ": " + e.getMessage());
                }
            }
            if (leito.concluirRecuperacoesVencidas()) {
                controlador.publicarEstado(leito);
            }
        }
    }

    private static void executarConsole(List<LeitoSimulado> leitos, ControladorSimulador controlador)
            throws java.io.IOException {
        BufferedReader leitor = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String linha = leitor.readLine();
            if (linha == null) {
                break;
            }
            String entrada = linha.strip();
            if (entrada.isEmpty()) {
                continue;
            }

            String[] partes = entrada.split("\\s+");
            String comando = normalizar(partes[0]);

            switch (comando) {
                case "crise" -> processarAlteracaoModo(partes, leitos, ModoOperacao.CRISE, controlador);
                case "rotina" -> processarAlteracaoModo(partes, leitos, ModoOperacao.ROTINA, controlador);
                case "status" -> imprimirStatus(leitos);
                case "ajuda" -> imprimirAjuda(leitos);
                case "sair" -> {
                    return;
                }
                default -> System.out.println("Comando desconhecido. Digite 'ajuda' para listar os comandos.");
            }
        }
    }

    private static void processarAlteracaoModo(String[] partes, List<LeitoSimulado> leitos,
            ModoOperacao modo, ControladorSimulador controlador) {
        if (partes.length != 3) {
            System.out.println("Uso: " + modo.name().toLowerCase(Locale.ROOT) + " <leito> <sinal|todos>");
            return;
        }

        Optional<LeitoSimulado> leitoOpt = buscarLeito(leitos, partes[1]);
        if (leitoOpt.isEmpty()) {
            System.out.println("Leito não encontrado: " + partes[1] + ". Digite 'status' para ver os leitos.");
            return;
        }

        LeitoSimulado leito = leitoOpt.get();
        String alvoSinal = normalizar(partes[2]);

        if ("todos".equals(alvoSinal)) {
            leito.definirModoTodos(modo);
            controlador.publicarEstado(leito);
            System.out.println("Modo " + modo + " aplicado a todos os sinais de " + leito.chaveUnica() + " (" + leito.pacienteId() + ").");
            return;
        }

        Optional<TipoSinal> sinalOpt = interpretarSinal(alvoSinal);
        if (sinalOpt.isEmpty()) {
            System.out.println("Sinal inválido: " + partes[2] + ". Use: frequencia, temperatura, saturacao, pressao ou todos.");
            return;
        }

        TipoSinal sinal = sinalOpt.get();
        leito.definirModo(sinal, modo);
        controlador.publicarEstado(leito);
        System.out.println("Modo " + modo + " aplicado em " + leito.chaveUnica() + " (" + leito.pacienteId() + ") -> " + sinal.name());
    }

    private static Optional<LeitoSimulado> buscarLeito(List<LeitoSimulado> leitos, String texto) {
        String busca = normalizar(texto);
        return leitos.stream()
                .filter(l -> normalizar(l.leito()).equals(busca) || normalizar(l.chaveUnica()).equals(busca))
                .findFirst();
    }

    private static Optional<TipoSinal> interpretarSinal(String textoNormalizado) {
        return switch (textoNormalizado) {
            case "frequencia", "fc", "bpm" -> Optional.of(TipoSinal.FREQUENCIA);
            case "temperatura", "temp" -> Optional.of(TipoSinal.TEMPERATURA);
            case "saturacao", "spo2", "sat" -> Optional.of(TipoSinal.SATURACAO);
            case "pressao", "pa" -> Optional.of(TipoSinal.PRESSAO);
            default -> Optional.empty();
        };
    }

    private static void imprimirStatus(List<LeitoSimulado> leitos) {
        System.out.println("=== Status dos Leitos Simulados ===");
        for (LeitoSimulado leito : leitos) {
            System.out.printf(" - %-22s | %-8s | %s%n", leito.chaveUnica(), leito.pacienteId(), leito.resumoModos());
        }
    }

    private static void imprimirAjuda(List<LeitoSimulado> leitos) {
        System.out.println("Comandos disponíveis:");
        System.out.println("  crise <leito> <sinal|todos>   -> força valores críticos (ex.: crise leito-01 frequencia)");
        System.out.println("  rotina <leito> <sinal|todos>  -> retorna à faixa normal (ex.: rotina leito-01 frequencia)");
        System.out.println("  status                        -> exibe o modo atual de cada leito e sinal");
        System.out.println("  ajuda                         -> exibe esta ajuda");
        System.out.println("  sair                          -> encerra o simulador");
        imprimirStatus(leitos);
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return semAcento.replaceAll("\\p{M}", "");
    }
}
