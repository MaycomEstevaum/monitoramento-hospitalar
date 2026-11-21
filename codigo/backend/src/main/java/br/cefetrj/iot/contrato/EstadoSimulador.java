package br.cefetrj.iot.contrato;

public record EstadoSimulador(String ala, String leito, String pacienteId,
        ComandoSimulador.Cenario frequencia, ComandoSimulador.Cenario temperatura,
        ComandoSimulador.Cenario saturacao, ComandoSimulador.Cenario pressao,
        String atualizadoEm) implements Mensagem {
    public EstadoSimulador {
        ala = Topicos.segmento(ala);
        leito = Topicos.segmento(leito);
        Campos.texto(pacienteId, "pacienteId");
        Campos.exigir(frequencia != null && temperatura != null && saturacao != null && pressao != null,
                "Todos os cenários dos sinais são obrigatórios.");
        Campos.instante(atualizadoEm, "atualizadoEm");
    }

    @Override
    public String topico() {
        return Topicos.estadoSimulador(ala, leito);
    }
}
