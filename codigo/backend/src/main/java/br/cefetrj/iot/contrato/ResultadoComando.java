package br.cefetrj.iot.contrato;

public record ResultadoComando(String idComando, String idAlerta, Resultado resultado, String motivo,
        EstadoCicloAlerta estado, Integer versao) implements Mensagem {
    public enum Resultado {
        APLICADO, REJEITADO
    }

    public ResultadoComando {
        Topicos.id(idComando);
        Topicos.id(idAlerta);
        Campos.exigir(resultado != null, "resultado é obrigatório.");
        if (resultado == Resultado.REJEITADO)
            Campos.texto(motivo, "motivo da rejeição");
        if (resultado == Resultado.REJEITADO && "ALERTA_INEXISTENTE".equals(motivo)) {
            Campos.exigir(estado == null && versao == null, "Alerta inexistente não tem estado ou versão.");
        } else {
            Campos.exigir(estado != null && versao != null && versao >= 1, "Informe o estado e a versão.");
        }
    }

    @Override
    public String topico() {
        return Topicos.respostaComando(idAlerta, idComando);
    }
}