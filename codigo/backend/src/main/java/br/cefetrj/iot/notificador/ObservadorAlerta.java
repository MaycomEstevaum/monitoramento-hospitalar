package br.cefetrj.iot.notificador;

import br.cefetrj.iot.contrato.EventoAlerta;

@FunctionalInterface
public interface ObservadorAlerta {
    void atualizar(EventoAlerta evento);
}
