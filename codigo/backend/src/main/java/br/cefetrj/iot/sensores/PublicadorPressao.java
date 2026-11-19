package br.cefetrj.iot.sensores;

import br.cefetrj.iot.mqtt.ClienteMqtt;

public final class PublicadorPressao extends PublicadorSensor {
    public PublicadorPressao(ClienteMqtt clienteMqtt) {
        super(clienteMqtt);
    }

    public PublicadorPressao(DestinoPublicacao destino) {
        super(destino);
    }

    @Override
    public Sensor criarSensor() {
        return new SensorPressao();
    }
}
