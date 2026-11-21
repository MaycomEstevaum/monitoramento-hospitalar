package br.cefetrj.iot.contrato;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TopicosTest {
    @Test
    void montaCincoFamilias() {
        assertEquals("sensores/uti/leito-01/frequencia",
                Topicos.leituraBruta("UTI", "Leito 01", TipoSinal.FREQUENCIA));
        assertEquals("hospital/enfermaria/leito-03/pressao/critico",
                Topicos.leituraClassificada("Enfermaria", "leito-03", TipoSinal.PRESSAO, Prioridade.CRITICO));
        assertEquals("comandos/alertas/alt-001/reconhecer",
                Topicos.comandoAlerta("alt-001", ComandoAlerta.Acao.RECONHECER));
        assertEquals("alertas/uti/leito-02/alt-001/estado", Topicos.estadoAlerta("uti", "leito-02", "alt-001"));
        assertEquals("respostas/alertas/alt-001/cmd-99", Topicos.respostaComando("alt-001", "cmd-99"));
    }

    @Test
    void normalizaAlaELeito() {
        assertEquals("sensores/clinica-medica/leito-10a/saturacao",
                Topicos.leituraBruta("Clínica Médica", "Leito 10A", TipoSinal.SATURACAO));
    }

    @Test
    void associaPrioridadeAQos() {
        assertEquals(0, Prioridade.ROTINA.getQos());
        assertEquals(1, Prioridade.ATENCAO.getQos());
        assertEquals(2, Prioridade.CRITICO.getQos());
    }

    @Test
    void rejeitaCuringasVaziosEIdsNaoCanonicos() {
        for (String invalida : new String[] { "uti/+", "uti/#", "", "   " }) {
            assertThrows(IllegalArgumentException.class,
                    () -> Topicos.leituraBruta(invalida, "leito-01", TipoSinal.TEMPERATURA));
        }
        for (String invalido : new String[] { "ALT-001", "alt/1", "cmd+1", "a#b" }) {
            assertThrows(IllegalArgumentException.class, () -> Topicos.respostaComando(invalido, "cmd-99"));
        }
    }

    @Test
    void montaControleEEstadoDoSimulador() {
        assertEquals("comandos/simulador/uti/leito-01/frequencia",
                Topicos.comandoSimulador("UTI", "Leito 01", TipoSinal.FREQUENCIA));
        assertEquals("comandos/simulador/enfermaria/leito-03/todos",
                Topicos.comandoSimulador("Enfermaria", "leito-03", null));
        assertEquals("simulador/uti/leito-01/estado", Topicos.estadoSimulador("UTI", "Leito 01"));
        assertEquals("comandos/simulador/+/+/+", Topicos.COMANDOS_SIMULADOR);
        assertEquals("simulador/+/+/estado", Topicos.ESTADOS_SIMULADOR);
    }
}