package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.TipoSinal;
import br.cefetrj.iot.contrato.Topicos;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicadorSensorTest {
    private record MensagemCapturada(String topico, String json, int qos, boolean reter) {}

    private final List<MensagemCapturada> capturadas = new ArrayList<>();
    private final PublicadorSensor.DestinoPublicacao destino = (topico, json, qos, reter) ->
            capturadas.add(new MensagemCapturada(topico, json, qos, reter));

    private final PublicadorFrequencia frequencia = new PublicadorFrequencia(destino);
    private final PublicadorTemperatura temperatura = new PublicadorTemperatura(destino);
    private final PublicadorSaturacao saturacao = new PublicadorSaturacao(destino);
    private final PublicadorPressao pressao = new PublicadorPressao(destino);
    private final List<PublicadorSensor> publicadores = List.of(frequencia, temperatura, saturacao, pressao);

    @Test
    void cadaPublicadorCriaOSensorCorrespondente() {
        assertInstanceOf(SensorFrequencia.class, frequencia.criarSensor());
        assertInstanceOf(SensorTemperatura.class, temperatura.criarSensor());
        assertInstanceOf(SensorSaturacao.class, saturacao.criarSensor());
        assertInstanceOf(SensorPressao.class, pressao.criarSensor());
    }

    @Test
    void quatroPublicacoesSeguemOContratoEAsFaixasDeRotina() throws Exception {
        LeitoSimulado leito = new LeitoSimulado("uti", "leito-01", "PAC-101");
        Map<TipoSinal, Leitura> primeira = publicarRodada(leito);
        conferirRotina(primeira);

        Map<TipoSinal, Leitura> segunda = publicarRodada(leito);
        conferirRotina(segunda);
        for (TipoSinal sinal : TipoSinal.values()) {
            assertNotEquals(primeira.get(sinal).idLeitura(), segunda.get(sinal).idLeitura());
        }
    }

    @Test
    void criseERetornoRespeitamLeitoESinal() throws Exception {
        LeitoSimulado alvo = new LeitoSimulado("uti", "leito-01", "PAC-101");
        LeitoSimulado vizinho = new LeitoSimulado("enfermaria", "leito-03", "PAC-201");
        conferirRotina(publicarRodada(alvo));

        alvo.definirModo(TipoSinal.FREQUENCIA, ModoOperacao.CRISE);
        Map<TipoSinal, Leitura> parcial = publicarRodada(alvo);
        faixa(parcial.get(TipoSinal.FREQUENCIA).valor(), 132, 148);
        conferirOutrosEmRotina(parcial);
        conferirRotina(publicarRodada(vizinho));

        alvo.definirModo(TipoSinal.FREQUENCIA, ModoOperacao.ROTINA);
        conferirRotina(publicarRodada(alvo));
        alvo.definirModoTodos(ModoOperacao.CRISE);
        Map<TipoSinal, Leitura> crise = publicarRodada(alvo);
        faixa(crise.get(TipoSinal.FREQUENCIA).valor(), 132, 148);
        faixa(crise.get(TipoSinal.TEMPERATURA).valor(), 39.3, 40.2);
        faixa(crise.get(TipoSinal.SATURACAO).valor(), 83, 88);
        faixa(crise.get(TipoSinal.PRESSAO).sistolica(), 185, 205);
        faixa(crise.get(TipoSinal.PRESSAO).diastolica(), 122, 135);
        conferirRotina(publicarRodada(vizinho));
        alvo.definirModoTodos(ModoOperacao.ROTINA);
        conferirRotina(publicarRodada(alvo));
    }

    private Map<TipoSinal, Leitura> publicarRodada(LeitoSimulado leito) throws Exception {
        capturadas.clear();
        for (PublicadorSensor publicador : publicadores) publicador.publicar(leito);
        assertEquals(4, capturadas.size());

        Map<TipoSinal, Leitura> leituras = new EnumMap<>(TipoSinal.class);
        for (MensagemCapturada m : capturadas) {
            assertEquals(1, m.qos());
            assertFalse(m.reter());
            Leitura l = ConversorJson.lerNoTopico(m.topico(), m.json(), Leitura.class);
            assertEquals(leito.pacienteId(), l.pacienteId());
            assertEquals(leito.ala(), l.ala());
            assertEquals(leito.leito(), l.leito());
            assertEquals(Topicos.leituraBruta(leito.ala(), leito.leito(), l.tipoSinal()), m.topico());
            assertEquals(l.tipoSinal().getUnidadePadrao(), l.unidade());
            assertNull(l.prioridade());
            assertNull(l.motivo());
            assertNull(leituras.put(l.tipoSinal(), l), "Sinal repetido na mesma rodada.");
        }
        assertNull(leituras.get(TipoSinal.PRESSAO).valor());
        return leituras;
    }

    private static void conferirRotina(Map<TipoSinal, Leitura> l) {
        faixa(l.get(TipoSinal.FREQUENCIA).valor(), 65, 95);
        conferirOutrosEmRotina(l);
    }

    private static void conferirOutrosEmRotina(Map<TipoSinal, Leitura> l) {
        faixa(l.get(TipoSinal.TEMPERATURA).valor(), 36.2, 37.3);
        faixa(l.get(TipoSinal.SATURACAO).valor(), 96, 99);
        faixa(l.get(TipoSinal.PRESSAO).sistolica(), 105, 130);
        faixa(l.get(TipoSinal.PRESSAO).diastolica(), 65, 82);
    }

    private static void faixa(Double valor, double minimo, double maximo) {
        assertNotNull(valor);
        assertTrue(valor >= minimo && valor <= maximo, () -> valor + " fora de " + minimo + " a " + maximo);
    }
}
