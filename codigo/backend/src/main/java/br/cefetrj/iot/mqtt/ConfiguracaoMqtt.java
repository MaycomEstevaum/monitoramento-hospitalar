package br.cefetrj.iot.mqtt;

import java.util.UUID;

public record ConfiguracaoMqtt(String brokerUri, String clientId, int timeoutSegundos, int keepAliveSegundos) {
    public ConfiguracaoMqtt {
        if (brokerUri == null || brokerUri.isBlank() || clientId == null || clientId.isBlank())
            throw new IllegalArgumentException("Informe brokerUri e clientId.");
        if (timeoutSegundos <= 0 || keepAliveSegundos <= 0)
            throw new IllegalArgumentException("Os tempos de rede devem ser positivos.");
    }

    public static ConfiguracaoMqtt padrao(String modulo) {
        String uri = System.getProperty("mqtt.broker.uri");
        if (uri == null || uri.isBlank()) uri = System.getenv("MQTT_BROKER_URI");
        if (uri == null || uri.isBlank()) uri = "tcp://localhost:1883";
        return new ConfiguracaoMqtt(uri.strip(), modulo + "-" + UUID.randomUUID(), 10, 20);
    }
}
