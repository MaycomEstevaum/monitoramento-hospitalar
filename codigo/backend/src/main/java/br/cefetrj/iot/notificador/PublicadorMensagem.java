package br.cefetrj.iot.notificador;

@FunctionalInterface
public interface PublicadorMensagem {
    void publicar(String topico, String json, int qos, boolean reter) throws Exception;
}
