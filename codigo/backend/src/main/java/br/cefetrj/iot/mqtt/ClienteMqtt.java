package br.cefetrj.iot.mqtt;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public final class ClienteMqtt implements AutoCloseable {
    private final ConfiguracaoMqtt config;
    private final MqttAsyncClient cliente;
    private final ReceptorMensagemMqtt receptor;
    private final Map<String, Integer> assinaturas = new ConcurrentHashMap<>();
    private final ExecutorService mensagens = Executors.newSingleThreadExecutor();
    private volatile boolean encerrando;

    public ClienteMqtt(ConfiguracaoMqtt config, ReceptorMensagemMqtt receptor) throws MqttException {
        this.config = Objects.requireNonNull(config);
        this.receptor = Objects.requireNonNull(receptor);
        cliente = new MqttAsyncClient(config.brokerUri(), config.clientId(), new MemoryPersistence());
        cliente.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectionLost(Throwable causa) {
                if (!encerrando)
                    System.err.println("Conexão MQTT perdida. Aguardando reconexão.");
            }

            @Override
            public void connectComplete(boolean reconectou, String uri) {
                if (reconectou && !encerrando)
                    assinaturas.forEach(ClienteMqtt.this::reassinar);
            }

            @Override
            public void messageArrived(String topico, MqttMessage mensagem) {
                byte[] payload = mensagem.getPayload().clone();
                try {
                    mensagens.execute(() -> entregar(topico, payload));
                } catch (RejectedExecutionException e) {
                    if (!encerrando)
                        throw e;
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
    }

    public void conectar() throws MqttException {
        MqttConnectOptions opcoes = new MqttConnectOptions();
        opcoes.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        opcoes.setAutomaticReconnect(true);
        opcoes.setCleanSession(true);
        opcoes.setConnectionTimeout(config.timeoutSegundos());
        opcoes.setKeepAliveInterval(config.keepAliveSegundos());
        cliente.connect(opcoes).waitForCompletion();
    }

    public void assinar(String filtro, int qos) throws MqttException {
        MqttTopic.validate(filtro, true);
        if (qos < 0 || qos > 2)
            throw new IllegalArgumentException("QoS fora de 0 a 2.");
        assinaturas.put(filtro, qos);
        IMqttToken pedido = cliente.subscribe(filtro, qos);
        pedido.waitForCompletion(config.timeoutSegundos() * 1000L);
        conferirAssinatura(pedido, filtro, qos);
    }

    private static void conferirAssinatura(IMqttToken token, String filtro, int qos) {
        int[] concedidos = token.getGrantedQos();
        if (concedidos == null || concedidos.length != 1 || concedidos[0] != qos)
            throw new IllegalStateException("O broker não concedeu QoS " + qos + " para " + filtro);
    }

    private void reassinar(String filtro, int qos) {
        try {
            // O callback de reconexão não espera a resposta do broker na própria thread de
            // rede.
            cliente.subscribe(filtro, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    try {
                        conferirAssinatura(token, filtro, qos);
                        System.out.println("Assinatura restaurada: " + filtro);
                    } catch (IllegalStateException e) {
                        System.err.println(e.getMessage());
                    }
                }

                @Override
                public void onFailure(IMqttToken token, Throwable causa) {
                    System.err.println("Falha ao reassinar " + filtro + ": " + causa.getMessage());
                }
            });
        } catch (MqttException e) {
            System.err.println("Falha ao reassinar " + filtro + ": " + e.getMessage());
        }
    }

    private void entregar(String topico, byte[] payload) {
        try {
            String json = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(payload)).toString();
            receptor.receber(topico, json);
        } catch (Exception e) {
            System.err.println("Mensagem descartada em " + topico + ": " + e.getMessage());
        }
    }

    public void publicar(String topico, String json, int qos, boolean reter) throws MqttException {
        MqttTopic.validate(topico, false);
        cliente.publish(topico, json.getBytes(StandardCharsets.UTF_8), qos, reter)
                .waitForCompletion(config.timeoutSegundos() * 1000L);
    }

    @Override
    public void close() throws MqttException {
        encerrando = true;
        try {
            if (cliente.isConnected())
                cliente.disconnect().waitForCompletion();
        } finally {
            mensagens.shutdownNow();
            cliente.close();
        }
    }
}