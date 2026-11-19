package br.cefetrj.iot.contrato;

public enum Prioridade {
    ROTINA("rotina", 0), ATENCAO("atencao", 1), CRITICO("critico", 2);

    private final String segmento;
    private final int qos;

    Prioridade(String segmento, int qos) {
        this.segmento = segmento;
        this.qos = qos;
    }

    public String getSegmentoTopico() { return segmento; }
    public int getQos() { return qos; }
}
