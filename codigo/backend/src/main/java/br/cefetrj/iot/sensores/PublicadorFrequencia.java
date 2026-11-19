package br.cefetrj.iot.sensores;

import br.cefetrj.iot.mqtt.ClienteMqtt;

public final class PublicadorFrequencia extends PublicadorSensor {
    public PublicadorFrequencia(ClienteMqtt clienteMqtt) {
        super(clienteMqtt);
    }

    public PublicadorFrequencia(DestinoPublicacao destino) {
        super(destino);
    }

    @Override
    public Sensor criarSensor() {
        return new SensorFrequencia();
    }
}
