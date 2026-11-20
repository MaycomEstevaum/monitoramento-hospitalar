package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.Topicos;
import br.cefetrj.iot.mqtt.ClienteMqtt;
import br.cefetrj.iot.mqtt.ConfiguracaoMqtt;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class ClassificadorMain {
    private ClassificadorMain() {}

    public static void main(String[] args) throws Exception {
        ConfiguracaoMqtt config = ConfiguracaoMqtt.padrao("classificador");
        AtomicReference<ServicoClassificador> servico = new AtomicReference<>();

        try (ClienteMqtt cliente = new ClienteMqtt(config,
                (topico, json) -> servico.get().processar(topico, json))) {
            // A mesma conexão recebe e publica. O serviço fica pronto antes da conexão.
            servico.set(new ServicoClassificador(cliente::publicar));
            cliente.conectar();
            cliente.assinar(Topicos.LEITURAS_BRUTAS, 1);
            System.out.println("Classificador pronto em " + config.brokerUri()
                    + " | " + Topicos.LEITURAS_BRUTAS + " | QoS 1. Pressione Enter para encerrar.");
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        }
    }
}
