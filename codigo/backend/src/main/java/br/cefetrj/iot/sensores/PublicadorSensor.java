package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.mqtt.ClienteMqtt;
import java.util.Objects;

public abstract class PublicadorSensor {
    public static final int QOS_LEITURA = 1;
    public static final boolean RETER_LEITURA = false;

    @FunctionalInterface
    public interface DestinoPublicacao {
        void publicar(String topico, String json, int qos, boolean reter) throws Exception;
    }

    private final DestinoPublicacao destino;
    private Sensor sensor;

    protected PublicadorSensor(ClienteMqtt clienteMqtt) {
        this(Objects.requireNonNull(clienteMqtt, "O ClienteMqtt não pode ser nulo.")::publicar);
    }

    protected PublicadorSensor(DestinoPublicacao destino) {
        this.destino = Objects.requireNonNull(destino, "O destino de publicação não pode ser nulo.");
    }

    public abstract Sensor criarSensor();

    protected final Sensor obterSensor() {
        if (sensor == null) {
            sensor = Objects.requireNonNull(criarSensor(), "O método fábrica criarSensor() não pode retornar null.");
        }
        return sensor;
    }

    public final void publicar(LeitoSimulado leito) throws Exception {
        Objects.requireNonNull(leito, "O leito não pode ser nulo.");
        Leitura leitura = obterSensor().coletar(leito);
        String topico = leitura.topico();
        String payload = ConversorJson.paraJson(leitura);
        destino.publicar(topico, payload, QOS_LEITURA, RETER_LEITURA);
    }
}
