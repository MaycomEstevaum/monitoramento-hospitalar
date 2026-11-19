package br.cefetrj.iot.contrato;

public enum TipoSinal {
    FREQUENCIA("frequencia", "bpm"), TEMPERATURA("temperatura", "°C"),
    SATURACAO("saturacao", "%"), PRESSAO("pressao", "mmHg");

    private final String segmento;
    private final String unidade;

    TipoSinal(String segmento, String unidade) {
        this.segmento = segmento;
        this.unidade = unidade;
    }

    public String getSegmentoTopico() {
        return segmento;
    }

    public String getUnidadePadrao() {
        return unidade;
    }

    public static TipoSinal fromSegmento(String segmento) {
        for (TipoSinal sinal : values()) {
            if (sinal.segmento.equals(segmento))
                return sinal;
        }
        throw new IllegalArgumentException("Sinal desconhecido: " + segmento);
    }

    void validarMedida(String unidadeRecebida, Double valor, Double sistolica, Double diastolica) {
        Campos.exigir(unidade.equals(unidadeRecebida), "Unidade esperada: " + unidade);
        if (this == PRESSAO) {
            Campos.exigir(valor == null && finito(sistolica) && finito(diastolica),
                    "Pressão exige sistólica e diastólica numéricas e valor nulo.");
        } else {
            Campos.exigir(finito(valor) && sistolica == null && diastolica == null,
                    "Este sinal exige valor numérico e campos de pressão nulos.");
        }
    }

    private static boolean finito(Double numero) {
        return numero != null && Double.isFinite(numero);
    }
}