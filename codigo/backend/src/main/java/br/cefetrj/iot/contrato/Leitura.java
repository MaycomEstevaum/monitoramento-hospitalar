package br.cefetrj.iot.contrato;

public record Leitura(String idLeitura, String pacienteId, String ala, String leito, TipoSinal tipoSinal,
        String unidade, String instanteColeta, Double valor, Double sistolica,
        Double diastolica, Prioridade prioridade, String motivo) implements Mensagem {
    public Leitura {
        Campos.texto(idLeitura, "idLeitura");
        Campos.texto(pacienteId, "pacienteId");
        ala = Topicos.segmento(ala);
        leito = Topicos.segmento(leito);
        Campos.exigir(tipoSinal != null, "tipoSinal é obrigatório.");
        tipoSinal.validarMedida(unidade, valor, sistolica, diastolica);
        Campos.instante(instanteColeta, "instanteColeta");
        if (prioridade == null) {
            Campos.exigir(motivo == null, "Uma leitura bruta não tem motivo de classificação.");
        } else {
            Campos.texto(motivo, "motivo");
        }
    }

    public static Leitura simples(String id, String paciente, String ala, String leito,
            TipoSinal sinal, double valor, String instante) {
        Campos.exigir(sinal != null && sinal != TipoSinal.PRESSAO, "Escolha um dos três sinais simples.");
        return new Leitura(id, paciente, ala, leito, sinal, sinal.getUnidadePadrao(), instante,
                valor, null, null, null, null);
    }

    public static Leitura pressao(String id, String paciente, String ala, String leito,
            double sistolica, double diastolica, String instante) {
        return new Leitura(id, paciente, ala, leito, TipoSinal.PRESSAO,
                TipoSinal.PRESSAO.getUnidadePadrao(), instante, null, sistolica, diastolica, null, null);
    }

    public Leitura classificada(Prioridade novaPrioridade, String justificativa) {
        Campos.exigir(novaPrioridade != null, "Informe a prioridade da leitura classificada.");
        return new Leitura(idLeitura, pacienteId, ala, leito, tipoSinal, unidade, instanteColeta,
                valor, sistolica, diastolica, novaPrioridade, justificativa);
    }

    @Override
    public String topico() {
        return prioridade == null ? Topicos.leituraBruta(ala, leito, tipoSinal)
                : Topicos.leituraClassificada(ala, leito, tipoSinal, prioridade);
    }
}