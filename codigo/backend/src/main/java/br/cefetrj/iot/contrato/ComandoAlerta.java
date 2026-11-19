package br.cefetrj.iot.contrato;

public record ComandoAlerta(String idComando, String idAlerta, Acao acao, String usuarioId,
        String instanteComando, String motivoResolucao) implements Mensagem {
    public enum Acao {
        RECONHECER, RESOLVER
    }

    public ComandoAlerta {
        Topicos.id(idComando);
        Topicos.id(idAlerta);
        Campos.exigir(acao != null, "acao é obrigatória.");
        Campos.texto(usuarioId, "usuarioId");
        Campos.instante(instanteComando, "instanteComando");
        if (acao == Acao.RESOLVER)
            Campos.texto(motivoResolucao, "motivoResolucao");
        else
            Campos.exigir(motivoResolucao == null, "Só RESOLVER usa motivoResolucao.");
    }

    @Override
    public String topico() {
        return Topicos.comandoAlerta(idAlerta, acao);
    }
}