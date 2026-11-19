package br.cefetrj.iot.contrato;

public record Alerta(String idAlerta, String idLeituraOrigem, String pacienteId, String ala, String leito,
        TipoSinal tipoSinal, String unidadeOrigem, String instanteLeituraOrigem,
        Double valorOrigem, Double sistolicaOrigem, Double diastolicaOrigem, String motivoOrigem,
        EstadoCicloAlerta estado, Integer versao, String abertoEm,
        String reconhecidoPor, String reconhecidoEm, String resolvidoPor,
        String resolvidoEm, String motivoResolucao) implements Mensagem {
    public Alerta {
        Topicos.id(idAlerta);
        Campos.texto(idLeituraOrigem, "idLeituraOrigem");
        Campos.texto(pacienteId, "pacienteId");
        ala = Topicos.segmento(ala);
        leito = Topicos.segmento(leito);
        Campos.exigir(tipoSinal != null, "tipoSinal é obrigatório.");
        tipoSinal.validarMedida(unidadeOrigem, valorOrigem, sistolicaOrigem, diastolicaOrigem);
        Campos.instante(instanteLeituraOrigem, "instanteLeituraOrigem");
        Campos.texto(motivoOrigem, "motivoOrigem");
        Campos.exigir(estado != null && versao != null && versao >= 1, "Estado ou versão inválidos.");
        Campos.instante(abertoEm, "abertoEm");
        if (estado == EstadoCicloAlerta.ABERTO) {
            Campos.exigir(reconhecidoPor == null && reconhecidoEm == null, "Alerta aberto sem reconhecimento.");
        } else {
            Campos.texto(reconhecidoPor, "reconhecidoPor");
            Campos.instante(reconhecidoEm, "reconhecidoEm");
        }
        if (estado == EstadoCicloAlerta.RESOLVIDO) {
            Campos.texto(resolvidoPor, "resolvidoPor");
            Campos.instante(resolvidoEm, "resolvidoEm");
            Campos.texto(motivoResolucao, "motivoResolucao");
        } else {
            Campos.exigir(resolvidoPor == null && resolvidoEm == null && motivoResolucao == null,
                    "Dados de resolução só existem em RESOLVIDO.");
        }
    }

    @Override
    public String topico() {
        return Topicos.estadoAlerta(ala, leito, idAlerta);
    }
}