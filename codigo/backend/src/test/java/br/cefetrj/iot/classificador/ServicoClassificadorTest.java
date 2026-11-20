package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.TipoSinal;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ServicoClassificadorTest {
    private static final String INSTANTE = "2026-11-19T14:30:05-03:00";

    private record Envio(String topico, String json, int qos, boolean retido) {}

    private final List<Envio> envios = new ArrayList<>();
    private final PublicadorLeituraClassificada capturar = (topico, json, qos, retido) ->
            envios.add(new Envio(topico, json, qos, retido));

    @ParameterizedTest(name = "{0} em {3} chega a {4}, QoS {5}")
    @CsvSource({
        "FREQUENCIA, 75,    , ROTINA,  frequencia/rotina,   0",
        "FREQUENCIA, 110,   , ATENCAO, frequencia/atencao,  1",
        "FREQUENCIA, 140,   , CRITICO, frequencia/critico,  2",
        "TEMPERATURA, 36.8, , ROTINA,  temperatura/rotina,  0",
        "TEMPERATURA, 38.2, , ATENCAO, temperatura/atencao, 1",
        "TEMPERATURA, 39.0, , CRITICO, temperatura/critico, 2",
        "SATURACAO, 98,     , ROTINA,  saturacao/rotina,     0",
        "SATURACAO, 93,     , ATENCAO, saturacao/atencao,    1",
        "SATURACAO, 86,     , CRITICO, saturacao/critico,    2",
        "PRESSAO, 115, 75,    ROTINA,  pressao/rotina,       0",
        "PRESSAO, 145, 75,    ATENCAO, pressao/atencao,      1",
        "PRESSAO, 185, 110,   CRITICO, pressao/critico,      2"
    })
    void roteiaEPreservaAOrigem(TipoSinal sinal, double medida, Double diastolica,
                                Prioridade esperada, String sufixo, int qos) {
        ServicoClassificador servico = new ServicoClassificador(capturar);
        Leitura bruta = sinal == TipoSinal.PRESSAO
                ? Leitura.pressao("leit-01", "PAC-101", "uti", "leito-01", medida, diastolica, INSTANTE)
                : Leitura.simples("leit-01", "PAC-101", "uti", "leito-01", sinal, medida, INSTANTE);
        entregar(servico, bruta);

        assertEquals(1, envios.size());
        Envio envio = envios.get(0);
        assertEquals("hospital/uti/leito-01/" + sufixo, envio.topico());
        assertEquals(qos, envio.qos());
        assertFalse(envio.retido());
        Leitura saida = ConversorJson.lerNoTopico(envio.topico(), envio.json(), Leitura.class);
        assertEquals(bruta.idLeitura(), saida.idLeitura());
        assertEquals(bruta.pacienteId(), saida.pacienteId());
        assertEquals(bruta.ala(), saida.ala());
        assertEquals(bruta.leito(), saida.leito());
        assertEquals(bruta.tipoSinal(), saida.tipoSinal());
        assertEquals(bruta.unidade(), saida.unidade());
        assertEquals(bruta.instanteColeta(), saida.instanteColeta());
        assertEquals(bruta.valor(), saida.valor());
        assertEquals(bruta.sistolica(), saida.sistolica());
        assertEquals(bruta.diastolica(), saida.diastolica());
        assertEquals(esperada, saida.prioridade());
        assertFalse(saida.motivo().isBlank());
    }

    @ParameterizedTest
    @CsvSource({
        "185, 110, CRITICO, Sistólica na faixa crítica da simulação",
        "145, 125, CRITICO, Diastólica na faixa crítica da simulação",
        "185, 125, CRITICO, Sistólica e diastólica na faixa crítica da simulação",
        "145, 95, ATENCAO, Sistólica e diastólica na faixa de atenção da simulação",
        "115, 75, ROTINA, Pressão arterial na faixa de rotina da simulação"
    })
    void motivoDaPressaoIdentificaAComponenteOuOEmpate(double s, double d, Prioridade p, String motivo) {
        var regra = new RegraPressao();
        var entrada = Leitura.pressao("pa", "PAC-101", "uti", "leito-01", s, d, INSTANTE);
        assertEquals(new ResultadoClassificacao(p, motivo), regra.classificar(entrada));
    }

    @Test
    void duplicataRenovaARecenciaEAExpulsaoPermiteNovaPublicacao() {
        var servico = new ServicoClassificador(capturar, 2);
        for (String id : List.of("a", "b", "a", "c", "a", "b")) {
            entregar(servico, frequencia(id));
        }
        List<String> ids = envios.stream()
                .map(e -> ConversorJson.lerNoTopico(e.topico(), e.json(), Leitura.class).idLeitura())
                .toList();
        assertEquals(List.of("a", "b", "c", "b"), ids);
    }

    @Test
    void envioFalhoPreservaOIdAnteriorEPermiteReenvioDoMesmoId() {
        AtomicInteger tentativas = new AtomicInteger();
        var servico = new ServicoClassificador((t, j, q, r) -> {
            if (tentativas.incrementAndGet() == 2) throw new IOException("Falha simulada.");
            capturar.publicar(t, j, q, r);
        }, 1);

        for (String id : List.of("a", "b", "a", "b", "b")) {
            entregar(servico, frequencia(id));
        }
        assertEquals(3, tentativas.get());
        assertEquals(2, envios.size());
        assertEquals("b", ConversorJson.deJson(envios.get(1).json(), Leitura.class).idLeitura());
    }

    @Test
    void rejeitaEntradasInvalidasESegueProcessando() {
        var servico = new ServicoClassificador(capturar);
        Leitura valida = frequencia("leitura-valida");
        String json = ConversorJson.paraJson(valida);
        JsonObject base = JsonParser.parseString(json).getAsJsonObject();
        JsonObject semId = base.deepCopy();
        semId.remove("idLeitura");
        JsonObject sinalInvalido = base.deepCopy();
        sinalInvalido.addProperty("tipoSinal", "GLICEMIA");
        JsonObject numeroEmTexto = base.deepCopy();
        numeroEmTexto.addProperty("valor", "80");

        servico.processar(valida.topico(), "{");
        servico.processar(valida.topico(), semId.toString());
        servico.processar(valida.topico(), sinalInvalido.toString());
        servico.processar(valida.topico(), numeroEmTexto.toString());
        servico.processar("sensores/uti/leito-02/frequencia", json);
        Leitura classificada = valida.classificada(Prioridade.ROTINA, "Exemplo");
        servico.processar(valida.topico(), ConversorJson.paraJson(classificada));
        entregar(servico, classificada); // Mesmo um tópico hospital/... que combine com o JSON é recusado.
        assertTrue(envios.isEmpty());

        entregar(servico, valida);
        assertEquals(1, envios.size());
    }

    @Test
    void duasEntregasSimultaneasDoMesmoIdGeramUmaPublicacao() throws Exception {
        AtomicInteger quantidade = new AtomicInteger();
        var servico = new ServicoClassificador((t, j, q, r) -> quantidade.incrementAndGet());
        Leitura leitura = frequencia("concorrente");
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Void> tarefa = () -> {
            inicio.await();
            entregar(servico, leitura);
            return null;
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> primeira = executor.submit(tarefa);
            Future<Void> segunda = executor.submit(tarefa);
            inicio.countDown();
            primeira.get(5, TimeUnit.SECONDS);
            segunda.get(5, TimeUnit.SECONDS);
            assertEquals(1, quantidade.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Leitura frequencia(String id) {
        return Leitura.simples(id, "PAC-101", "uti", "leito-01", TipoSinal.FREQUENCIA, 80, INSTANTE);
    }

    private static void entregar(ServicoClassificador servico, Leitura leitura) {
        servico.processar(leitura.topico(), ConversorJson.paraJson(leitura));
    }
}
