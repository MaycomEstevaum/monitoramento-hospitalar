package br.cefetrj.iot.notificador;

import static br.cefetrj.iot.contrato.ComandoAlerta.Acao.RECONHECER;
import static br.cefetrj.iot.contrato.ComandoAlerta.Acao.RESOLVER;
import static br.cefetrj.iot.contrato.EstadoCicloAlerta.*;
import static br.cefetrj.iot.contrato.ResultadoComando.Resultado.*;
import static br.cefetrj.iot.contrato.TipoSinal.FREQUENCIA;
import static br.cefetrj.iot.contrato.TipoSinal.SATURACAO;
import static org.junit.jupiter.api.Assertions.*;

import br.cefetrj.iot.contrato.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessadorComandosTest {
    private static final String HORA_NOTIFICADOR = "2026-11-20T10:05:00-03:00";
    private static final String HORA_ORIGEM = "2026-11-20T10:00:00-03:00";
    private record Envio(String topico, String json, int qos, boolean reter) {}

    private final List<Envio> tentativas = new CopyOnWriteArrayList<>();
    private final List<Envio> entregues = new CopyOnWriteArrayList<>();
    private final List<EventoAlerta> eventos = new CopyOnWriteArrayList<>();
    private int falharNaTentativa;
    private ProcessadorComandos processador;

    @BeforeEach
    void iniciar() {
        var central = new CentralAlertas();
        central.registrar(eventos::add);
        processador = new ProcessadorComandos(central, (topico, json, qos, reter) -> {
            Envio envio = new Envio(topico, json, qos, reter);
            tentativas.add(envio);
            if (tentativas.size() == falharNaTentativa) throw new IOException("falha simulada");
            entregues.add(envio);
        }, () -> HORA_NOTIFICADOR);
    }

    @AfterEach
    void terminar() {
        processador.close();
    }

    private static Leitura critica(String id, String paciente, String leito, TipoSinal tipo) {
        double valor = tipo == FREQUENCIA ? 145 : 85;
        return Leitura.simples(id, paciente, "uti", leito, tipo, valor, HORA_ORIGEM)
                .classificada(Prioridade.CRITICO, "Fora da faixa da simulação");
    }

    private void receber(Leitura leitura) throws Exception {
        processador.enfileirarLeituraCritica(leitura.topico(), ConversorJson.paraJson(leitura)).get(5, TimeUnit.SECONDS);
    }

    private Alerta estado(int indice) {
        Envio envio = entregues.get(indice);
        assertEquals(1, envio.qos());
        assertTrue(envio.reter());
        return ConversorJson.lerNoTopico(envio.topico(), envio.json(), Alerta.class);
    }

    private ResultadoComando resposta(int indice) {
        Envio envio = entregues.get(indice);
        assertEquals(1, envio.qos());
        assertFalse(envio.reter());
        return ConversorJson.lerNoTopico(envio.topico(), envio.json(), ResultadoComando.class);
    }

    private Future<?> agendar(String id, String idComando, ComandoAlerta.Acao acao) {
        boolean resolucao = acao == RESOLVER;
        var comando = new ComandoAlerta(idComando, id, acao, resolucao ? "med-01" : "enf-01",
                HORA_ORIGEM, resolucao ? "Atendimento simulado concluído" : null);
        return processador.enfileirarComando(comando.topico(), ConversorJson.paraJson(comando));
    }

    private void agir(String id, String idComando, ComandoAlerta.Acao acao) throws Exception {
        agendar(id, idComando, acao).get(5, TimeUnit.SECONDS);
    }

    private String abrir() throws Exception {
        receber(critica("leit-1", "PAC-101", "leito-01", FREQUENCIA));
        return estado(0).idAlerta();
    }

    @Test
    void cicloPreservaOrigemPublicaEstadoAntesDaRespostaENotifica() throws Exception {
        String id = abrir();
        agir(id, "cmd-1", RECONHECER);
        agir(id, "cmd-2", RESOLVER);
        assertEquals(5, entregues.size());
        Alerta abertura = estado(0);
        Alerta ciencia = estado(1);
        Alerta fim = estado(3);
        assertEquals("alertas/uti/leito-01/" + id + "/estado", entregues.get(0).topico());
        assertEquals(ABERTO, abertura.estado());
        assertEquals(1, abertura.versao().intValue());
        assertEquals("leit-1", fim.idLeituraOrigem());
        assertEquals(HORA_ORIGEM, fim.instanteLeituraOrigem());
        assertEquals(HORA_NOTIFICADOR, fim.abertoEm());
        assertEquals("enf-01", fim.reconhecidoPor());
        assertEquals(HORA_NOTIFICADOR, fim.reconhecidoEm());
        assertEquals("med-01", fim.resolvidoPor());
        assertEquals(HORA_NOTIFICADOR, fim.resolvidoEm());
        assertEquals("Atendimento simulado concluído", fim.motivoResolucao());
        assertEquals(new ResultadoComando("cmd-1", id, APLICADO, null, RECONHECIDO, 2), resposta(2));
        assertEquals(new ResultadoComando("cmd-2", id, APLICADO, null, RESOLVIDO, 3), resposta(4));
        assertEquals(List.of(new EventoAlerta(null, abertura, HORA_NOTIFICADOR),
                new EventoAlerta(ABERTO, ciencia, HORA_NOTIFICADOR),
                new EventoAlerta(RECONHECIDO, fim, HORA_NOTIFICADOR)), eventos);
    }

    @Test
    void agrupaMesmoPacienteESinalEnquantoAbertoOuReconhecidoMesmoEmOutroLeito() throws Exception {
        String id = abrir();
        receber(critica("leit-2", "PAC-101", "leito-02", FREQUENCIA));
        assertEquals(1, entregues.size());
        agir(id, "cmd-1", RECONHECER);
        receber(critica("leit-3", "PAC-101", "leito-02", FREQUENCIA));
        assertEquals(3, entregues.size());
        assertEquals(2, eventos.size());
        assertEquals("leito-01", estado(1).leito());
    }

    @Test
    void separaPacientesNoMesmoLeitoESinaisDoMesmoPaciente() throws Exception {
        String primeiro = abrir();
        receber(critica("leit-2", "PAC-102", "leito-01", FREQUENCIA));
        receber(critica("leit-3", "PAC-101", "leito-01", SATURACAO));
        assertEquals(3, entregues.size());
        Alerta segundo = estado(1);
        Alerta terceiro = estado(2);
        assertNotEquals(primeiro, segundo.idAlerta());
        assertNotEquals(primeiro, terceiro.idAlerta());
        assertNotEquals(segundo.idAlerta(), terceiro.idAlerta());
        assertEquals("PAC-102", segundo.pacienteId());
        assertEquals(SATURACAO, terceiro.tipoSinal());
    }

    @Test
    void soLeituraIneditaAbreNovaOcorrenciaAposResolucao() throws Exception {
        Leitura origem = critica("leit-1", "PAC-101", "leito-01", FREQUENCIA);
        Leitura agrupada = critica("leit-2", "PAC-101", "leito-01", FREQUENCIA);
        receber(origem);
        String id = estado(0).idAlerta();
        receber(agrupada);
        agir(id, "cmd-1", RECONHECER);
        agir(id, "cmd-2", RESOLVER);
        receber(origem);
        receber(agrupada);
        assertEquals(5, entregues.size());
        receber(critica("leit-3", "PAC-101", "leito-01", FREQUENCIA));
        assertEquals(6, entregues.size());
        assertEquals(4, eventos.size());
        assertNotEquals(id, estado(5).idAlerta());
        assertEquals(ABERTO, estado(5).estado());
        assertEquals(1, estado(5).versao().intValue());
    }

    @Test
    void rejeicaoRespondeSemGerarNovoEstadoOuEvento() throws Exception {
        String id = abrir();
        agir("alt-inexistente", "cmd-0", RECONHECER);
        assertEquals(new ResultadoComando("cmd-0", "alt-inexistente", REJEITADO, "ALERTA_INEXISTENTE", null, null),
                resposta(1));
        agir(id, "cmd-1", RESOLVER);
        assertEquals(new ResultadoComando("cmd-1", id, REJEITADO, "TRANSICAO_INVALIDA", ABERTO, 1), resposta(2));
        agir(id, "cmd-2", RECONHECER);
        agir(id, "cmd-3", RESOLVER);
        agir(id, "cmd-4", RESOLVER);
        assertEquals(new ResultadoComando("cmd-4", id, REJEITADO, "TRANSICAO_INVALIDA", RESOLVIDO, 3), resposta(7));
        assertEquals(8, entregues.size());
        assertEquals(3, eventos.size());
    }

    @Test
    void descartaEntradasInvalidasEAtendeAProximaValida() throws Exception {
        Leitura valida = critica("leit-1", "PAC-101", "leito-01", FREQUENCIA);
        String json = ConversorJson.paraJson(valida);
        processador.enfileirarLeituraCritica(valida.topico(), "{").get(5, TimeUnit.SECONDS);
        processador.enfileirarLeituraCritica("hospital/uti/leito-02/frequencia/critico", json).get(5, TimeUnit.SECONDS);
        Leitura rotina = Leitura.simples("leit-1", "PAC-101", "uti", "leito-01", FREQUENCIA, 75, HORA_ORIGEM);
        receber(rotina);
        receber(rotina.classificada(Prioridade.ROTINA, "Rotina simulada"));
        processador.enfileirarComando("comandos/alertas/alt-1/resolver", "{}").get(5, TimeUnit.SECONDS);
        var cmd = new ComandoAlerta("cmd-1", "alt-1", RECONHECER, "enf-01", HORA_ORIGEM, null);
        processador.enfileirarComando("comandos/alertas/alt-1/resolver", ConversorJson.paraJson(cmd)).get(5, TimeUnit.SECONDS);
        assertTrue(entregues.isEmpty());
        assertTrue(eventos.isEmpty());
        receber(valida);
        assertEquals(1, entregues.size());
        assertEquals(ABERTO, estado(0).estado());
    }

    @Test
    void doisReconhecimentosNaFilaAplicamUmaUnicaTransicao() throws Exception {
        String id = abrir();
        Future<?> primeiro = agendar(id, "cmd-a", RECONHECER);
        Future<?> segundo = agendar(id, "cmd-b", RECONHECER);
        primeiro.get(5, TimeUnit.SECONDS);
        segundo.get(5, TimeUnit.SECONDS);
        assertEquals(new ResultadoComando("cmd-a", id, APLICADO, null, RECONHECIDO, 2), resposta(2));
        assertEquals(new ResultadoComando("cmd-b", id, REJEITADO, "TRANSICAO_INVALIDA", RECONHECIDO, 2), resposta(3));
        assertEquals(4, entregues.size());
        assertEquals(2, eventos.size());
    }

    @Test
    void repeteEnvioDeEstadoAntesDeResponderSemDuplicarOEvento() throws Exception {
        String id = abrir();
        falharNaTentativa = 2;
        agir(id, "cmd-1", RECONHECER);
        assertEquals(4, tentativas.size());
        assertEquals(tentativas.get(1), tentativas.get(2));
        assertEquals(RECONHECIDO, estado(1).estado());
        assertEquals(APLICADO, resposta(2).resultado());
        assertEquals(3, entregues.size());
        assertEquals(2, eventos.size());
    }

    @Test
    void repeteApenasARespostaQuandoSeuEnvioFalha() throws Exception {
        String id = abrir();
        falharNaTentativa = 3;
        agir(id, "cmd-1", RECONHECER);
        assertEquals(4, tentativas.size());
        assertEquals(tentativas.get(2), tentativas.get(3));
        assertEquals(APLICADO, resposta(2).resultado());
        assertEquals(3, entregues.size());
        assertEquals(2, eventos.size());
    }

    @Test
    void encerramentoDrenaTrabalhoAceitoERecusaNovasEntradas() throws Exception {
        Leitura valida = critica("leit-1", "PAC-101", "leito-01", FREQUENCIA);
        String json = ConversorJson.paraJson(valida);
        Future<?> recebido = processador.enfileirarLeituraCritica(valida.topico(), json);
        processador.close();
        recebido.get(5, TimeUnit.SECONDS);
        assertEquals(1, entregues.size());
        assertThrows(RejectedExecutionException.class, () -> processador.enfileirarLeituraCritica(valida.topico(), json));
    }

    @Test
    void encerrarDuranteFalhaDeEnvioCancelaEntradasPendentes() throws Exception {
        var inicioDoEnvio = new CountDownLatch(1);
        var central = new CentralAlertas();
        var recebidos = new CopyOnWriteArrayList<EventoAlerta>();
        central.registrar(recebidos::add);
        Future<?> emCurso;
        Future<?> pendente;
        try (var semBroker = new ProcessadorComandos(central, (topico, json, qos, reter) -> {
            inicioDoEnvio.countDown();
            throw new IOException("transporte indisponível");
        })) {
            Leitura primeira = critica("leit-a", "PAC-101", "leito-01", FREQUENCIA);
            Leitura segunda = critica("leit-b", "PAC-102", "leito-02", FREQUENCIA);
            emCurso = semBroker.enfileirarLeituraCritica(primeira.topico(), ConversorJson.paraJson(primeira));
            assertTrue(inicioDoEnvio.await(5, TimeUnit.SECONDS));
            pendente = semBroker.enfileirarLeituraCritica(segunda.topico(), ConversorJson.paraJson(segunda));
        }
        assertThrows(ExecutionException.class, () -> emCurso.get(5, TimeUnit.SECONDS));
        assertTrue(pendente.isCancelled());
        assertEquals(1, recebidos.size());
    }
}
