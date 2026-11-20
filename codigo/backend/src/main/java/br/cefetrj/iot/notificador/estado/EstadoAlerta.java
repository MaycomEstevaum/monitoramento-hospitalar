package br.cefetrj.iot.notificador.estado;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.EstadoCicloAlerta;

interface EstadoAlerta {
    EstadoCicloAlerta codigo();
    boolean estaAtivo();

    default Alerta reconhecer(Alerta atual, String usuarioId, String instante) {
        throw rejeitar(atual, "reconhecer");
    }

    default Alerta resolver(Alerta atual, String usuarioId, String instante, String motivoResolucao) {
        throw rejeitar(atual, "resolver");
    }

    private TransicaoAlertaInvalidaException rejeitar(Alerta atual, String acao) {
        return new TransicaoAlertaInvalidaException(
                "O alerta " + atual.idAlerta() + " em " + codigo() + " rejeita a ação " + acao + ".");
    }
}
