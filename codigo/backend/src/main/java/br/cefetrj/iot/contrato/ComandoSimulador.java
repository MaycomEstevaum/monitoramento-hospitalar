package br.cefetrj.iot.contrato;

public record ComandoSimulador(String idComando, String ala, String leito, TipoSinal tipoSinal,
        Cenario cenario, String instanteComando) implements Mensagem {
    public enum Cenario {
        ROTINA, CRISE
    }

    public ComandoSimulador {
        Topicos.id(idComando);
        ala = Topicos.segmento(ala);
        leito = Topicos.segmento(leito);
        Campos.exigir(cenario != null, "cenario é obrigatório.");
        Campos.instante(instanteComando, "instanteComando");
    }

    @Override
    public String topico() {
        return Topicos.comandoSimulador(ala, leito, tipoSinal);
    }
}
