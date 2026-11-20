package br.cefetrj.iot.notificador;

import br.cefetrj.iot.contrato.Topicos;
import br.cefetrj.iot.mqtt.ClienteMqtt;
import br.cefetrj.iot.mqtt.ConfiguracaoMqtt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class NotificadorMain {
    private NotificadorMain() {}

    public static void main(String[] args) throws Exception {
        ConfiguracaoMqtt config = ConfiguracaoMqtt.padrao("notificador");
        CentralAlertas central = new CentralAlertas();
        RegistroArquivo arquivo = new RegistroArquivo();
        central.registrar(new LogConsole());
        central.registrar(arquivo);
        AtomicReference<ProcessadorComandos> destino = new AtomicReference<>();

        try (ClienteMqtt cliente = new ClienteMqtt(config, (topico, json) -> {
            ProcessadorComandos atual = destino.get();
            if (atual == null) {
                return;
            }
            if (topico.startsWith("hospital/")) {
                atual.enfileirarLeituraCritica(topico, json);
            } else if (topico.startsWith("comandos/alertas/")) {
                atual.enfileirarComando(topico, json);
            }
        });
             ProcessadorComandos processador = new ProcessadorComandos(central, cliente::publicar)) {
            destino.set(processador);
            try {
                cliente.conectar();
                cliente.assinar(Topicos.CRITICOS, 2);
                cliente.assinar(Topicos.COMANDOS, 1);
                System.out.printf("Notificador pronto em %s | %s (QoS 2) | %s (QoS 1)%n",
                        config.brokerUri(), Topicos.CRITICOS, Topicos.COMANDOS);
                System.out.println("Log: " + arquivo.caminhoArquivo());
                System.out.println("Pressione Enter para encerrar.");
                aguardarEncerramento();
            } finally {
                destino.set(null);
            }
        }
    }

    private static void aguardarEncerramento() throws IOException, InterruptedException {
        var entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        if (entrada.readLine() == null) {
            // Com a entrada fechada, o serviço segue ativo até o encerramento da JVM.
            var parada = new CountDownLatch(1);
            Thread gancho = new Thread(parada::countDown, "notificador-parada");
            Runtime.getRuntime().addShutdownHook(gancho);
            try {
                parada.await();
            } finally {
                try {
                    Runtime.getRuntime().removeShutdownHook(gancho);
                } catch (IllegalStateException emEncerramento) {
                    // A JVM já iniciou os ganchos de encerramento.
                }
            }
        }
    }
}
