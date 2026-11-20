package br.cefetrj.iot.notificador.estado;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.EstadoCicloAlerta;

final class AlertaReconhecido implements EstadoAlerta {
    static final AlertaReconhecido INSTANCIA = new AlertaReconhecido();

    private AlertaReconhecido() {}

    @Override
    public EstadoCicloAlerta codigo() {
        return EstadoCicloAlerta.RECONHECIDO;
    }

    @Override
    public boolean estaAtivo() {
        return true;
    }

    @Override
    public Alerta resolver(Alerta atual, String usuarioId, String instante, String motivoResolucao) {
        return new Alerta(
                atual.idAlerta(), atual.idLeituraOrigem(), atual.pacienteId(), atual.ala(), atual.leito(),
                atual.tipoSinal(), atual.unidadeOrigem(), atual.instanteLeituraOrigem(),
                atual.valorOrigem(), atual.sistolicaOrigem(), atual.diastolicaOrigem(), atual.motivoOrigem(),
                EstadoCicloAlerta.RESOLVIDO, atual.versao() + 1, atual.abertoEm(),
                atual.reconhecidoPor(), atual.reconhecidoEm(),
                usuarioId, instante, motivoResolucao);
    }
}
