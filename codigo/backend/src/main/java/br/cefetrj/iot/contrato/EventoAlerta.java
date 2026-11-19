package br.cefetrj.iot.contrato;

public record EventoAlerta(EstadoCicloAlerta estadoAnterior, Alerta atual, String instanteEvento) {
    public EventoAlerta {
        Campos.exigir(atual != null, "O evento precisa do retrato do alerta.");
        Campos.instante(instanteEvento, "instanteEvento");
    }
}
