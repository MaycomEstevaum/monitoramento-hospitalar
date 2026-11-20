package br.cefetrj.iot.classificador;

@FunctionalInterface
public interface PublicadorLeituraClassificada {
    void publicar(String topico, String json, int qos, boolean reter) throws Exception;
}
