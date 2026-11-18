package br.cefetrj.iot.demo;

import java.nio.charset.StandardCharsets;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class PublicadorTesteMain {
    public static void main(String[] args) throws Exception {
        String topico = "teste/uti/leito-03/frequencia-cardiaca";
        String json = "{\"pacienteId\":\"P003\","
                + "\"sinal\":\"frequencia-cardiaca\","
                + "\"valor\":128,\"unidade\":\"bpm\"}";

        MqttMessage mensagem = new MqttMessage(
                json.getBytes(StandardCharsets.UTF_8));
        mensagem.setQos(1);
        mensagem.setRetained(false);

        MqttClient cliente = new MqttClient(
                "tcp://localhost:1883", "demo-publicador",
                new MemoryPersistence());
        cliente.connect();
        try {
            cliente.publish(topico, mensagem);
            System.out.println("Mensagem publicada em " + topico);
        } finally {
            if (cliente.isConnected()) {
                cliente.disconnect();
            }
            cliente.close();
        }
    }
}