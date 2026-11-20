package br.cefetrj.iot.notificador.estado;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.EstadoCicloAlerta;
import br.cefetrj.iot.contrato.EventoAlerta;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import java.util.Map;

public final class ContextoAlerta {
    private static final Map<EstadoCicloAlerta, EstadoAlerta> ESTADOS = Map.of(
            EstadoCicloAlerta.ABERTO, AlertaAberto.INSTANCIA,
            EstadoCicloAlerta.RECONHECIDO, AlertaReconhecido.INSTANCIA,
            EstadoCicloAlerta.RESOLVIDO, AlertaResolvido.INSTANCIA);

    private EstadoAlerta estadoCorrente;
    private EventoAlerta ultimoEvento;

    private ContextoAlerta(Alerta aberto) {
        estadoCorrente = AlertaAberto.INSTANCIA;
        ultimoEvento = new EventoAlerta(null, aberto, aberto.abertoEm());
    }

    public static ContextoAlerta abrir(Leitura leituraCritica, String idAlerta, String instanteNotificador) {
        if (leituraCritica == null || leituraCritica.prioridade() != Prioridade.CRITICO) {
            throw new IllegalArgumentException("A abertura exige uma leitura classificada como CRITICO.");
        }
        Alerta aberto = new Alerta(
                idAlerta, leituraCritica.idLeitura(), leituraCritica.pacienteId(),
                leituraCritica.ala(), leituraCritica.leito(), leituraCritica.tipoSinal(),
                leituraCritica.unidade(), leituraCritica.instanteColeta(),
                leituraCritica.valor(), leituraCritica.sistolica(), leituraCritica.diastolica(), leituraCritica.motivo(),
                EstadoCicloAlerta.ABERTO, 1, instanteNotificador,
                null, null,
                null, null, null);
        return new ContextoAlerta(aberto);
    }

    public synchronized Alerta alertaAtual() {
        return ultimoEvento.atual();
    }

    public synchronized EstadoCicloAlerta estadoAtual() {
        return estadoCorrente.codigo();
    }

    public synchronized boolean estaAtivo() {
        return estadoCorrente.estaAtivo();
    }

    public synchronized EventoAlerta ultimoEvento() {
        return ultimoEvento;
    }

    public synchronized EventoAlerta reconhecer(String usuarioId, String instanteNotificador) {
        Alerta novo = estadoCorrente.reconhecer(ultimoEvento.atual(), usuarioId, instanteNotificador);
        return confirmar(novo, instanteNotificador);
    }

    public synchronized EventoAlerta resolver(String usuarioId, String instanteNotificador, String motivoResolucao) {
        Alerta novo = estadoCorrente.resolver(ultimoEvento.atual(), usuarioId, instanteNotificador, motivoResolucao);
        return confirmar(novo, instanteNotificador);
    }

    private EventoAlerta confirmar(Alerta novo, String instanteNotificador) {
        EstadoAlerta proximo = ESTADOS.get(novo.estado());
        if (proximo == null) {
            throw new IllegalStateException("Sem implementação para o estado " + novo.estado());
        }
        EventoAlerta evento = new EventoAlerta(estadoCorrente.codigo(), novo, instanteNotificador);
        estadoCorrente = proximo;
        ultimoEvento = evento;
        return evento;
    }
}
