package br.cefetrj.iot.sensores;

import static org.junit.jupiter.api.Assertions.*;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.ComandoSimulador;
import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.EstadoCicloAlerta;
import br.cefetrj.iot.contrato.EstadoSimulador;
import br.cefetrj.iot.contrato.TipoSinal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ControladorSimuladorTest {
    private static final String INSTANTE = "2026-11-20T19:00:00-03:00";
    private static final String INSTANTE_REC_ANTIGO = "2026-11-20T19:00:05-03:00";
    private static final String INSTANTE_REC_ATUAL = "2026-11-20T19:00:15-03:00";

    private record Publicacao(String topico, String json, int qos, boolean retido) {}

    private final AtomicLong agoraMs = new AtomicLong(OffsetDateTime.parse(INSTANTE).toInstant().toEpochMilli());
    private final List<Publicacao> publicacoes = new ArrayList<>();
    private final LeitoSimulado leito1 = new LeitoSimulado("uti", "leito-01", "PAC-101", agoraMs::get);
    private final LeitoSimulado leito3 = new LeitoSimulado("enfermaria", "leito-03", "PAC-201", agoraMs::get);
    private final ControladorSimulador controlador = new ControladorSimulador(
            List.of(leito1, leito3),
            (topico, json, qos, retido) -> publicacoes.add(new Publicacao(topico, json, qos, retido)),
            () -> INSTANTE);

    @Test
    void publicaEstadosIniciaisRetidosParaTodosOsLeitos() {
        controlador.publicarEstadosIniciais();

        assertEquals(2, publicacoes.size());
        for (Publicacao p : publicacoes) {
            assertEquals(1, p.qos());
            assertTrue(p.retido());
        }
        EstadoSimulador primeiro = ConversorJson.lerNoTopico(
                publicacoes.get(0).topico(), publicacoes.get(0).json(), EstadoSimulador.class);
        assertEquals("PAC-101", primeiro.pacienteId());
        assertEquals(ComandoSimulador.Cenario.ROTINA, primeiro.frequencia());
        assertEquals(ComandoSimulador.Cenario.ROTINA, primeiro.temperatura());
        assertEquals(ComandoSimulador.Cenario.ROTINA, primeiro.saturacao());
        assertEquals(ComandoSimulador.Cenario.ROTINA, primeiro.pressao());
    }

    @Test
    void provocaCriseESimulaRecuperacaoImediataEmSinalUnico() {
        ComandoSimulador crise = new ComandoSimulador("sim-1", "uti", "leito-01",
                TipoSinal.FREQUENCIA, ComandoSimulador.Cenario.CRISE, INSTANTE);
        controlador.processarComando(crise.topico(), ConversorJson.paraJson(crise));

        assertEquals(ModoOperacao.CRISE, leito1.modoAtual(TipoSinal.FREQUENCIA));
        assertEquals(ModoOperacao.ROTINA, leito1.modoAtual(TipoSinal.PRESSAO));
        assertEquals(ModoOperacao.ROTINA, leito3.modoAtual(TipoSinal.FREQUENCIA));
        assertEquals(1, publicacoes.size());
        EstadoSimulador aposCrise = ConversorJson.lerNoTopico(
                publicacoes.get(0).topico(), publicacoes.get(0).json(), EstadoSimulador.class);
        assertEquals(ComandoSimulador.Cenario.CRISE, aposCrise.frequencia());
        assertEquals(ComandoSimulador.Cenario.ROTINA, aposCrise.pressao());

        ComandoSimulador rotina = new ComandoSimulador("sim-2", "uti", "leito-01",
                TipoSinal.FREQUENCIA, ComandoSimulador.Cenario.ROTINA, INSTANTE);
        controlador.processarComando(rotina.topico(), ConversorJson.paraJson(rotina));

        assertEquals(ModoOperacao.ROTINA, leito1.modoAtual(TipoSinal.FREQUENCIA));
        EstadoSimulador aposRotina = ConversorJson.lerNoTopico(
                publicacoes.get(1).topico(), publicacoes.get(1).json(), EstadoSimulador.class);
        assertEquals(ComandoSimulador.Cenario.ROTINA, aposRotina.frequencia());
    }

    @Test
    void aplicaTodosOsSinaisDoLeitoSemAfetarVizinho() {
        ComandoSimulador todosCrise = new ComandoSimulador("sim-3", "enfermaria", "leito-03",
                null, ComandoSimulador.Cenario.CRISE, INSTANTE);
        controlador.processarComando(todosCrise.topico(), ConversorJson.paraJson(todosCrise));

        for (TipoSinal sinal : TipoSinal.values()) {
            assertEquals(ModoOperacao.CRISE, leito3.modoAtual(sinal));
            assertEquals(ModoOperacao.ROTINA, leito1.modoAtual(sinal));
        }

        ComandoSimulador todosRotina = new ComandoSimulador("sim-4", "enfermaria", "leito-03",
                null, ComandoSimulador.Cenario.ROTINA, INSTANTE);
        controlador.processarComando(todosRotina.topico(), ConversorJson.paraJson(todosRotina));

        for (TipoSinal sinal : TipoSinal.values()) {
            assertEquals(ModoOperacao.ROTINA, leito3.modoAtual(sinal));
        }
    }

    @Test
    void descartaComandosInvalidosIncompativeisOuDeLeitoInexistente() {
        ComandoSimulador valido = new ComandoSimulador("sim-5", "uti", "leito-01",
                TipoSinal.SATURACAO, ComandoSimulador.Cenario.CRISE, INSTANTE);
        ComandoSimulador leitoDesconhecido = new ComandoSimulador("sim-6", "uti", "leito-99",
                TipoSinal.SATURACAO, ComandoSimulador.Cenario.CRISE, INSTANTE);

        controlador.processarComando("comandos/simulador/uti/leito-01/saturacao", "{");
        controlador.processarComando("comandos/simulador/uti/leito-02/saturacao", ConversorJson.paraJson(valido));
        controlador.processarComando(leitoDesconhecido.topico(), ConversorJson.paraJson(leitoDesconhecido));

        assertTrue(publicacoes.isEmpty());
        assertEquals(ModoOperacao.ROTINA, leito1.modoAtual(TipoSinal.SATURACAO));
    }

    @Test
    void alertaReconhecidoIniciaRecuperacaoGradualApenasNoSinalCorrespondente() {
        leito1.definirModo(TipoSinal.FREQUENCIA, ModoOperacao.CRISE);
        leito1.definirModo(TipoSinal.SATURACAO, ModoOperacao.CRISE);
        agoraMs.addAndGet(5_000L);

        Alerta reconhecido = new Alerta("alt-01", "leit-01", "PAC-101", "uti", "leito-01",
                TipoSinal.FREQUENCIA, "bpm", INSTANTE, 145.0, null, null, "Frequência cardíaca na faixa crítica",
                EstadoCicloAlerta.RECONHECIDO, 2, INSTANTE, "enf-01", INSTANTE_REC_ANTIGO, null, null, null);
        controlador.processarEstadoAlerta(reconhecido.topico(), ConversorJson.paraJson(reconhecido));

        assertTrue(leito1.emRecuperacao(TipoSinal.FREQUENCIA));
        assertFalse(leito1.emRecuperacao(TipoSinal.SATURACAO));
        assertFalse(leito3.emRecuperacao(TipoSinal.FREQUENCIA));
    }

    @Test
    void ignoraAlertaRetidoAntigoOuEmOutrosEstadosOuComPayloadInvalido() {
        agoraMs.addAndGet(10_000L);
        leito1.definirModo(TipoSinal.FREQUENCIA, ModoOperacao.CRISE);

        Alerta aberto = new Alerta("alt-02", "leit-02", "PAC-101", "uti", "leito-01",
                TipoSinal.FREQUENCIA, "bpm", INSTANTE, 145.0, null, null, "Frequência cardíaca na faixa crítica",
                EstadoCicloAlerta.ABERTO, 1, INSTANTE, null, null, null, null, null);
        Alerta resolvido = new Alerta("alt-03", "leit-03", "PAC-101", "uti", "leito-01",
                TipoSinal.FREQUENCIA, "bpm", INSTANTE, 145.0, null, null, "Frequência cardíaca na faixa crítica",
                EstadoCicloAlerta.RESOLVIDO, 3, INSTANTE, "enf-01", INSTANTE_REC_ATUAL, "med-01", INSTANTE_REC_ATUAL,
                "Normalizado");
        Alerta outroPaciente = new Alerta("alt-04", "leit-04", "PAC-999", "uti", "leito-01",
                TipoSinal.FREQUENCIA, "bpm", INSTANTE, 145.0, null, null, "Frequência cardíaca na faixa crítica",
                EstadoCicloAlerta.RECONHECIDO, 2, INSTANTE, "enf-01", INSTANTE_REC_ATUAL, null, null, null);
        Alerta retidoAntigo = new Alerta("alt-05", "leit-05", "PAC-101", "uti", "leito-01",
                TipoSinal.FREQUENCIA, "bpm", INSTANTE, 145.0, null, null, "Frequência cardíaca na faixa crítica",
                EstadoCicloAlerta.RECONHECIDO, 2, INSTANTE, "enf-01", INSTANTE_REC_ANTIGO, null, null, null);

        controlador.processarEstadoAlerta("alertas/uti/leito-01/alt-01/estado", "");
        controlador.processarEstadoAlerta("alertas/uti/leito-01/alt-01/estado", "{");
        controlador.processarEstadoAlerta(aberto.topico(), ConversorJson.paraJson(aberto));
        controlador.processarEstadoAlerta(resolvido.topico(), ConversorJson.paraJson(resolvido));
        controlador.processarEstadoAlerta(outroPaciente.topico(), ConversorJson.paraJson(outroPaciente));
        controlador.processarEstadoAlerta(retidoAntigo.topico(), ConversorJson.paraJson(retidoAntigo));

        assertFalse(leito1.emRecuperacao(TipoSinal.FREQUENCIA));
    }

    @Test
    void comandoManualRotinaCancelaRecuperacaoGradualEVoltaImediatamente() {
        leito1.definirModo(TipoSinal.FREQUENCIA, ModoOperacao.CRISE);
        agoraMs.addAndGet(15_000L);
        Alerta reconhecido = new Alerta("alt-06", "leit-06", "PAC-101", "uti", "leito-01",
                TipoSinal.FREQUENCIA, "bpm", INSTANTE, 145.0, null, null, "Frequência cardíaca na faixa crítica",
                EstadoCicloAlerta.RECONHECIDO, 2, INSTANTE, "enf-01", INSTANTE_REC_ATUAL, null, null, null);
        controlador.processarEstadoAlerta(reconhecido.topico(), ConversorJson.paraJson(reconhecido));
        assertTrue(leito1.emRecuperacao(TipoSinal.FREQUENCIA));

        ComandoSimulador rotina = new ComandoSimulador("sim-7", "uti", "leito-01",
                TipoSinal.FREQUENCIA, ComandoSimulador.Cenario.ROTINA, INSTANTE_REC_ATUAL);
        controlador.processarComando(rotina.topico(), ConversorJson.paraJson(rotina));

        assertFalse(leito1.emRecuperacao(TipoSinal.FREQUENCIA));
        assertEquals(ModoOperacao.ROTINA, leito1.modoAtual(TipoSinal.FREQUENCIA));
    }
}
