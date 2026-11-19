package br.cefetrj.iot.mqtt;

@FunctionalInterface
public interface ReceptorMensagemMqtt {
    void receber(String topico, String json) throws Exception;
}
