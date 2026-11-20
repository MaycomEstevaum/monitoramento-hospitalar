package br.cefetrj.iot.notificador.estado;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.EstadoCicloAlerta;

final class AlertaAberto implements EstadoAlerta {
    static final AlertaAberto INSTANCIA = new AlertaAberto();

    private AlertaAberto() {}

    @Override
    public EstadoCicloAlerta codigo() {
        return EstadoCicloAlerta.ABERTO;
    }

    @Override
    public boolean estaAtivo() {
        return true;
    }

    @Override
    public Alerta reconhecer(Alerta atual, String usuarioId, String instante) {
        return new Alerta(
                atual.idAlerta(), atual.idLeituraOrigem(), atual.pacienteId(), atual.ala(), atual.leito(),
                atual.tipoSinal(), atual.unidadeOrigem(), atual.instanteLeituraOrigem(),
                atual.valorOrigem(), atual.sistolicaOrigem(), atual.diastolicaOrigem(), atual.motivoOrigem(),
                EstadoCicloAlerta.RECONHECIDO, atual.versao() + 1, atual.abertoEm(),
                usuarioId, instante,
                null, null, null);
    }
}
