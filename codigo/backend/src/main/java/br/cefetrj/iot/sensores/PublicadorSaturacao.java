package br.cefetrj.iot.sensores;

import br.cefetrj.iot.mqtt.ClienteMqtt;

public final class PublicadorSaturacao extends PublicadorSensor {
    public PublicadorSaturacao(ClienteMqtt clienteMqtt) {
        super(clienteMqtt);
    }

    public PublicadorSaturacao(DestinoPublicacao destino) {
        super(destino);
    }

    @Override
    public Sensor criarSensor() {
        return new SensorSaturacao();
    }
}
