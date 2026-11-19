package br.cefetrj.iot.sensores;

import br.cefetrj.iot.mqtt.ClienteMqtt;

public final class PublicadorTemperatura extends PublicadorSensor {
    public PublicadorTemperatura(ClienteMqtt clienteMqtt) {
        super(clienteMqtt);
    }

    public PublicadorTemperatura(DestinoPublicacao destino) {
        super(destino);
    }

    @Override
    public Sensor criarSensor() {
        return new SensorTemperatura();
    }
}
