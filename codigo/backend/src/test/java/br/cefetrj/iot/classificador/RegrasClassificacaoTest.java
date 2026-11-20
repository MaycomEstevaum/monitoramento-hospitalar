package br.cefetrj.iot.classificador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.TipoSinal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegrasClassificacaoTest {

    private static final String INSTANTE = "2026-11-19T14:30:05-03:00";

    private final RegraFrequencia regraFrequencia = new RegraFrequencia();
    private final RegraTemperatura regraTemperatura = new RegraTemperatura();
    private final RegraSaturacao regraSaturacao = new RegraSaturacao();
    private final RegraPressao regraPressao = new RegraPressao();

    @ParameterizedTest(name = "Frequência {0} bpm -> {1}")
    @CsvSource({
            "49.0, CRITICO",
            "50.0, ATENCAO",
            "51.0, ATENCAO",
            "59.0, ATENCAO",
            "60.0, ROTINA",
            "61.0, ROTINA",
            "99.0, ROTINA",
            "100.0, ROTINA",
            "101.0, ATENCAO",
            "119.0, ATENCAO",
            "120.0, ATENCAO",
            "49.99, CRITICO",
            "59.9, ATENCAO",
            "100.1, ATENCAO",
            "120.01, CRITICO",
            "121.0, CRITICO"
    })
    void deveClassificarLimitesFrequencia(double bpm, Prioridade esperada) {
        Leitura leitura = Leitura.simples("leit-freq", "PAC-101", "uti", "leito-01",
                TipoSinal.FREQUENCIA, bpm, INSTANTE);

        ResultadoClassificacao resultado = regraFrequencia.classificar(leitura);

        assertEquals(esperada, resultado.prioridade());
        assertNotNull(resultado.motivo());
        assertFalse(resultado.motivo().isBlank());
    }

    @ParameterizedTest(name = "Temperatura {0} °C -> {1}")
    @CsvSource({
            "34.9, CRITICO",
            "35.0, CRITICO",
            "35.1, ATENCAO",
            "35.9, ATENCAO",
            "36.0, ROTINA",
            "36.1, ROTINA",
            "37.4, ROTINA",
            "37.5, ROTINA",
            "37.6, ATENCAO",
            "38.9, ATENCAO",
            "39.0, CRITICO",
            "35.01, ATENCAO",
            "35.99, ATENCAO",
            "37.55, ATENCAO",
            "38.99, ATENCAO",
            "39.1, CRITICO"
    })
    void deveClassificarLimitesTemperatura(double celsius, Prioridade esperada) {
        Leitura leitura = Leitura.simples("leit-temp", "PAC-101", "uti", "leito-01",
                TipoSinal.TEMPERATURA, celsius, INSTANTE);

        ResultadoClassificacao resultado = regraTemperatura.classificar(leitura);

        assertEquals(esperada, resultado.prioridade());
        assertNotNull(resultado.motivo());
        assertFalse(resultado.motivo().isBlank());
    }

    @ParameterizedTest(name = "Saturação {0}% -> {1}")
    @CsvSource({
            "89.0, CRITICO",
            "90.0, ATENCAO",
            "91.0, ATENCAO",
            "94.0, ATENCAO",
            "95.0, ROTINA",
            "89.99, CRITICO",
            "94.5, ATENCAO",
            "96.0, ROTINA"
    })
    void deveClassificarLimitesSaturacao(double spo2, Prioridade esperada) {
        Leitura leitura = Leitura.simples("leit-sat", "PAC-101", "uti", "leito-01",
                TipoSinal.SATURACAO, spo2, INSTANTE);

        ResultadoClassificacao resultado = regraSaturacao.classificar(leitura);

        assertEquals(esperada, resultado.prioridade());
        assertNotNull(resultado.motivo());
        assertFalse(resultado.motivo().isBlank());
    }

    @ParameterizedTest(name = "Pressão {0}/{1} mmHg -> {2}")
    @CsvSource({
            // Limites da sistólica (diastólica fixa em rotina = 75)
            "79.0, 75.0, CRITICO",
            "80.0, 75.0, ATENCAO",
            "81.0, 75.0, ATENCAO",
            "89.0, 75.0, ATENCAO",
            "90.0, 75.0, ROTINA",
            "91.0, 75.0, ROTINA",
            "138.0, 75.0, ROTINA",
            "139.0, 75.0, ROTINA",
            "140.0, 75.0, ATENCAO",
            "179.0, 75.0, ATENCAO",
            "180.0, 75.0, CRITICO",
            "181.0, 75.0, CRITICO",
            "89.5, 75.0, ATENCAO",
            "139.5, 75.0, ATENCAO",
            "179.99, 75.0, ATENCAO",
            // Limites da diastólica (sistólica fixa em rotina = 115)
            "115.0, 49.0, CRITICO",
            "115.0, 50.0, ATENCAO",
            "115.0, 51.0, ATENCAO",
            "115.0, 59.0, ATENCAO",
            "115.0, 60.0, ROTINA",
            "115.0, 61.0, ROTINA",
            "115.0, 88.0, ROTINA",
            "115.0, 89.0, ROTINA",
            "115.0, 90.0, ATENCAO",
            "115.0, 119.0, ATENCAO",
            "115.0, 120.0, CRITICO",
            "115.0, 121.0, CRITICO",
            "115.0, 59.5, ATENCAO",
            "115.0, 89.5, ATENCAO",
            "115.0, 119.99, ATENCAO",
            // Prevalência de gravidade entre sistólica e diastólica
            "185.0, 110.0, CRITICO",
            "110.0, 125.0, CRITICO",
            "145.0, 75.0, ATENCAO",
            "110.0, 95.0, ATENCAO",
            "185.0, 125.0, CRITICO"
    })
    void deveClassificarLimitesEPrevalenciaPressao(double sistolica, double diastolica, Prioridade esperada) {
        Leitura leitura = Leitura.pressao("leit-pa", "PAC-101", "uti", "leito-01",
                sistolica, diastolica, INSTANTE);

        ResultadoClassificacao resultado = regraPressao.classificar(leitura);

        assertEquals(esperada, resultado.prioridade());
        assertNotNull(resultado.motivo());
        assertFalse(resultado.motivo().isBlank());
    }
}
