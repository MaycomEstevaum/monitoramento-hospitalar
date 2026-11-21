package br.cefetrj.iot.contrato;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConversorJsonTest {
        private static final String HORA = "2026-11-19T14:30:05-03:00";

        private static Leitura pressao() {
                return Leitura.pressao("leit-001", "PAC-101", "uti", "leito-01", 185, 110, HORA);
        }

        private static <T extends Record & Mensagem> void verificarIdaEVolta(T mensagem, Class<T> tipo) {
                String json = ConversorJson.paraJson(mensagem);
                assertEquals(mensagem, ConversorJson.lerNoTopico(mensagem.topico(), json, tipo));
        }

        @Test
        void preservaOsTresSinaisDeUmValor() {
                verificarIdaEVolta(Leitura.simples("f-1", "PAC-101", "uti", "leito-01",
                                TipoSinal.FREQUENCIA, 75, HORA), Leitura.class);
                verificarIdaEVolta(Leitura.simples("t-1", "PAC-101", "uti", "leito-01",
                                TipoSinal.TEMPERATURA, 36.5, HORA), Leitura.class);
                verificarIdaEVolta(Leitura.simples("s-1", "PAC-101", "uti", "leito-01",
                                TipoSinal.SATURACAO, 97, HORA), Leitura.class);
        }

        @Test
        void preservaOsDoisValoresDePressao() {
                Leitura l = pressao();
                JsonObject objeto = JsonParser.parseString(ConversorJson.paraJson(l)).getAsJsonObject();
                assertTrue(objeto.get("valor").isJsonNull());
                assertEquals(185.0, objeto.get("sistolica").getAsDouble());
                assertEquals(110.0, objeto.get("diastolica").getAsDouble());
                verificarIdaEVolta(l, Leitura.class);
        }

        @Test
        void classificaSemPerderAOrigem() {
                Leitura bruta = pressao();
                Leitura l = bruta.classificada(Prioridade.CRITICO, "Regra da simulação");
                assertEquals(bruta.idLeitura(), l.idLeitura());
                assertEquals(bruta.instanteColeta(), l.instanteColeta());
                assertEquals(bruta.sistolica(), l.sistolica());
                assertEquals(bruta.diastolica(), l.diastolica());
                assertEquals("hospital/uti/leito-01/pressao/critico", l.topico());
                verificarIdaEVolta(l, Leitura.class);
                assertThrows(IllegalArgumentException.class,
                                () -> ConversorJson.lerNoTopico(bruta.topico(), ConversorJson.paraJson(l),
                                                Leitura.class));
        }

        @Test
        void preservaOrigemEHistoricoNoEstadoRetido() {
                Alerta aberto = new Alerta("alt-001", "leit-001", "PAC-101", "uti", "leito-01", TipoSinal.PRESSAO,
                                "mmHg", HORA, null, 185.0, 110.0, "Regra da simulação", EstadoCicloAlerta.ABERTO, 1,
                                HORA, null, null, null, null, null);
                verificarIdaEVolta(aberto, Alerta.class);
                Alerta resolvido = new Alerta("alt-001", "leit-001", "PAC-101", "uti", "leito-01", TipoSinal.PRESSAO,
                                "mmHg", HORA, null, 185.0, 110.0, "Regra da simulação", EstadoCicloAlerta.RESOLVIDO, 3,
                                HORA, "enf-01", "2026-11-19T14:31:00-03:00", "med-01", "2026-11-19T14:35:00-03:00",
                                "Simulação concluída");
                verificarIdaEVolta(resolvido, Alerta.class);
        }

        @Test
        void relacionaComandosERespostasPelosIds() {
                ComandoAlerta reconhecer = new ComandoAlerta("cmd-01", "alt-001", ComandoAlerta.Acao.RECONHECER,
                                "enf-01", HORA, null);
                ComandoAlerta resolver = new ComandoAlerta("cmd-02", "alt-001", ComandoAlerta.Acao.RESOLVER,
                                "med-01", HORA, "Simulação concluída");
                verificarIdaEVolta(reconhecer, ComandoAlerta.class);
                verificarIdaEVolta(resolver, ComandoAlerta.class);
                ResultadoComando resposta = new ResultadoComando(resolver.idComando(), resolver.idAlerta(),
                                ResultadoComando.Resultado.APLICADO, null, EstadoCicloAlerta.RESOLVIDO, 3);
                assertEquals("respostas/alertas/alt-001/cmd-02", resposta.topico());
                verificarIdaEVolta(resposta, ResultadoComando.class);
                verificarIdaEVolta(new ResultadoComando("cmd-03", "alt-999",
                                ResultadoComando.Resultado.REJEITADO, "ALERTA_INEXISTENTE", null, null),
                                ResultadoComando.class);
                assertThrows(IllegalArgumentException.class, () -> new ComandoAlerta(
                                "cmd-03", "alt-001", ComandoAlerta.Acao.RESOLVER, "med-01", HORA, " "));
        }

        @Test
        void rejeitaSintaxeTiposValoresAusentesEEnumsDesconhecidos() {
                String valido = ConversorJson.paraJson(pressao());
                for (String invalido : new String[] {
                                "{", "{}", "null", valido + "{}", valido.replace("\"PRESSAO\"", "'PRESSAO'"),
                                valido.replace("185.0", "\"185\""), valido.replace("110.0", "null"),
                                valido.replace("\"PRESSAO\"", "\"RESPIRACAO\""),
                                valido.replace("\"prioridade\":null", "\"prioridade\":\"URGENTE\""),
                                valido.replace("-03:00", "") }) {
                        assertThrows(IllegalArgumentException.class,
                                        () -> ConversorJson.deJson(invalido, Leitura.class));
                }
                assertThrows(IllegalArgumentException.class, () -> ConversorJson.lerNoTopico(
                                "sensores/uti/leito-02/pressao", valido, Leitura.class));
        }

        @Test
        void preservaComandosEEstadosDoSimulador() {
                ComandoSimulador unico = new ComandoSimulador("sim-01", "uti", "leito-01",
                                TipoSinal.PRESSAO, ComandoSimulador.Cenario.CRISE, HORA);
                ComandoSimulador todos = new ComandoSimulador("sim-02", "enfermaria", "leito-03",
                                null, ComandoSimulador.Cenario.ROTINA, HORA);
                EstadoSimulador estado = new EstadoSimulador("uti", "leito-01", "PAC-101",
                                ComandoSimulador.Cenario.ROTINA, ComandoSimulador.Cenario.ROTINA,
                                ComandoSimulador.Cenario.ROTINA, ComandoSimulador.Cenario.CRISE, HORA);

                assertEquals("comandos/simulador/uti/leito-01/pressao", unico.topico());
                assertEquals("comandos/simulador/enfermaria/leito-03/todos", todos.topico());
                assertEquals("simulador/uti/leito-01/estado", estado.topico());
                verificarIdaEVolta(unico, ComandoSimulador.class);
                verificarIdaEVolta(todos, ComandoSimulador.class);
                verificarIdaEVolta(estado, EstadoSimulador.class);

                assertThrows(IllegalArgumentException.class, () -> ConversorJson.lerNoTopico(
                                "comandos/simulador/uti/leito-02/pressao",
                                ConversorJson.paraJson(unico), ComandoSimulador.class));
        }
}