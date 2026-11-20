package br.cefetrj.iot.notificador.estado;

import br.cefetrj.iot.contrato.EstadoCicloAlerta;

final class AlertaResolvido implements EstadoAlerta {
    static final AlertaResolvido INSTANCIA = new AlertaResolvido();

    private AlertaResolvido() {}

    @Override
    public EstadoCicloAlerta codigo() {
        return EstadoCicloAlerta.RESOLVIDO;
    }

    @Override
    public boolean estaAtivo() {
        return false;
    }
}
