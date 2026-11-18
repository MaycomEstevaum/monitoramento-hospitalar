package br.cefetrj.iot.demo;

import java.nio.charset.StandardCharsets;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class AssinanteTesteMain {
    public static void main(String[] args) throws Exception {
        MqttClient cliente = new MqttClient(
                "tcp://localhost:1883", "demo-assinante",
                new MemoryPersistence());

        MqttConnectOptions opcoes = new MqttConnectOptions();
        opcoes.setAutomaticReconnect(true);
        cliente.connect(opcoes);
        try {
            cliente.subscribe("teste/#", 1, (topico, mensagem) -> {
                String texto = new String(
                        mensagem.getPayload(), StandardCharsets.UTF_8);
                System.out.println(topico + " -> " + texto);
            });
            System.out.println("Assinante pronto. Pressione Enter para sair.");
            System.in.read();
        } finally {
            if (cliente.isConnected()) {
                cliente.disconnect();
            }
            cliente.close();
        }
    }
}