package br.cefetrj.iot.notificador.estado;

import static br.cefetrj.iot.contrato.EstadoCicloAlerta.ABERTO;
import static br.cefetrj.iot.contrato.EstadoCicloAlerta.RECONHECIDO;
import static br.cefetrj.iot.contrato.EstadoCicloAlerta.RESOLVIDO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.EventoAlerta;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.TipoSinal;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ContextoAlertaTest {
    private static final String COLETA = "2026-11-20T10:00:00-03:00";
    private static final String ABERTURA = "2026-11-20T10:00:01-03:00";
    private static final String CIENCIA = "2026-11-20T10:02:00-03:00";
    private static final String RESOLUCAO = "2026-11-20T10:15:00-03:00";
    private static final String SEM_FUSO = "2026-11-20T10:30:00";
    private static final String MOTIVO = "Atendimento simulado concluído";

    private static Leitura frequencia(double valor) {
        return Leitura.simples("leit-001", "PAC-101", "uti", "leito-01", TipoSinal.FREQUENCIA, valor, COLETA);
    }

    private static Leitura critica() {
        return frequencia(142).classificada(Prioridade.CRITICO, "Frequência cardíaca na faixa crítica da simulação");
    }

    private static ContextoAlerta aberto() {
        return ContextoAlerta.abrir(critica(), "alt-001", ABERTURA);
    }

    private static ContextoAlerta reconhecido() {
        ContextoAlerta contexto = aberto();
        contexto.reconhecer("enf-01", CIENCIA);
        return contexto;
    }

    private static void conferirOrigem(Leitura origem, Alerta alerta) {
        assertEquals(origem.idLeitura(), alerta.idLeituraOrigem());
        assertEquals(origem.pacienteId(), alerta.pacienteId());
        assertEquals(origem.ala(), alerta.ala());
        assertEquals(origem.leito(), alerta.leito());
        assertEquals(origem.tipoSinal(), alerta.tipoSinal());
        assertEquals(origem.unidade(), alerta.unidadeOrigem());
        assertEquals(origem.instanteColeta(), alerta.instanteLeituraOrigem());
        assertEquals(origem.valor(), alerta.valorOrigem());
        assertEquals(origem.sistolica(), alerta.sistolicaOrigem());
        assertEquals(origem.diastolica(), alerta.diastolicaOrigem());
        assertEquals(origem.motivo(), alerta.motivoOrigem());
    }

    private static void conferirRejeicao(ContextoAlerta contexto, Class<? extends Throwable> tipo, Executable acao) {
        EventoAlerta antes = contexto.ultimoEvento();
        boolean estavaAtivo = contexto.estaAtivo();
        assertThrows(tipo, acao);
        assertSame(antes, contexto.ultimoEvento());
        assertSame(antes.atual(), contexto.alertaAtual());
        assertEquals(antes.atual().estado(), contexto.estadoAtual());
        assertEquals(estavaAtivo, contexto.estaAtivo());
    }

    @Test
    void abreFrequenciaCriticaEGeraEventoInicial() {
        Leitura origem = critica();
        ContextoAlerta contexto = ContextoAlerta.abrir(origem, "alt-001", ABERTURA);
        EventoAlerta evento = contexto.ultimoEvento();
        Alerta alerta = contexto.alertaAtual();

        assertNull(evento.estadoAnterior());
        assertSame(alerta, evento.atual());
        assertEquals(ABERTURA, evento.instanteEvento());
        conferirOrigem(origem, alerta);
        assertEquals("alt-001", alerta.idAlerta());
        assertEquals("alertas/uti/leito-01/alt-001/estado", alerta.topico());
        assertEquals(ABERTO, contexto.estadoAtual());
        assertEquals(ABERTO, alerta.estado());
        assertEquals(1, alerta.versao().intValue());
        assertEquals(ABERTURA, alerta.abertoEm());
        assertTrue(contexto.estaAtivo());
        assertNull(alerta.reconhecidoPor());
        assertNull(alerta.reconhecidoEm());
        assertNull(alerta.resolvidoPor());
        assertNull(alerta.resolvidoEm());
        assertNull(alerta.motivoResolucao());
    }

    @Test
    void preservaAsDuasMedidasDePressaoEmTodoOCiclo() {
        Leitura origem = Leitura.pressao("leit-002", "PAC-101", "uti", "leito-01", 185, 110, COLETA)
                .classificada(Prioridade.CRITICO, "Sistólica na faixa crítica da simulação");
        ContextoAlerta contexto = ContextoAlerta.abrir(origem, "alt-002", ABERTURA);

        conferirOrigem(origem, contexto.alertaAtual());
        conferirOrigem(origem, contexto.reconhecer("enf-01", CIENCIA).atual());
        conferirOrigem(origem, contexto.resolver("med-01", RESOLUCAO, MOTIVO).atual());
        assertNull(contexto.alertaAtual().valorOrigem());
        assertEquals("alt-002", contexto.alertaAtual().idAlerta());
    }

    @Test
    void recusaAberturaSemLeituraCriticaClassificada() {
        Leitura bruta = frequencia(142);
        Leitura rotina = frequencia(75).classificada(Prioridade.ROTINA, "Rotina simulada");
        Leitura atencao = frequencia(110).classificada(Prioridade.ATENCAO, "Atenção simulada");

        assertThrows(IllegalArgumentException.class, () -> ContextoAlerta.abrir(null, "alt-001", ABERTURA));
        assertThrows(IllegalArgumentException.class, () -> ContextoAlerta.abrir(bruta, "alt-001", ABERTURA));
        assertThrows(IllegalArgumentException.class, () -> ContextoAlerta.abrir(rotina, "alt-001", ABERTURA));
        assertThrows(IllegalArgumentException.class, () -> ContextoAlerta.abrir(atencao, "alt-001", ABERTURA));
    }

    @Test
    void validaIdentificadorEHorarioDeAberturaPeloContrato() {
        assertThrows(IllegalArgumentException.class, () -> ContextoAlerta.abrir(critica(), "alt/001", ABERTURA));
        assertThrows(IllegalArgumentException.class, () -> ContextoAlerta.abrir(critica(), "alt-001", SEM_FUSO));
    }

    @Test
    void reconhecimentoRegistraAutorEHoraEPreservaEventoDeAbertura() {
        ContextoAlerta contexto = aberto();
        EventoAlerta abertura = contexto.ultimoEvento();
        EventoAlerta evento = contexto.reconhecer("enf-01", CIENCIA);
        Alerta atual = evento.atual();

        assertSame(evento, contexto.ultimoEvento());
        assertSame(atual, contexto.alertaAtual());
        assertEquals(ABERTO, evento.estadoAnterior());
        assertEquals(CIENCIA, evento.instanteEvento());
        assertEquals(RECONHECIDO, contexto.estadoAtual());
        assertEquals(RECONHECIDO, atual.estado());
        assertTrue(contexto.estaAtivo());
        assertEquals(2, atual.versao().intValue());
        assertEquals("alt-001", atual.idAlerta());
        assertEquals(ABERTURA, atual.abertoEm());
        assertEquals("enf-01", atual.reconhecidoPor());
        assertEquals(CIENCIA, atual.reconhecidoEm());
        assertNull(atual.resolvidoPor());
        assertNull(atual.resolvidoEm());
        assertNull(atual.motivoResolucao());
        conferirOrigem(critica(), atual);

        assertEquals(ABERTO, abertura.atual().estado());
        assertEquals(1, abertura.atual().versao().intValue());
        assertNull(abertura.atual().reconhecidoEm());
    }

    @Test
    void resolucaoConservaReconhecimentoERegistraAutorHoraEMotivo() {
        ContextoAlerta contexto = reconhecido();
        EventoAlerta ciencia = contexto.ultimoEvento();
        EventoAlerta evento = contexto.resolver("med-01", RESOLUCAO, MOTIVO);
        Alerta atual = evento.atual();

        assertSame(evento, contexto.ultimoEvento());
        assertSame(atual, contexto.alertaAtual());
        assertEquals(RECONHECIDO, evento.estadoAnterior());
        assertEquals(RESOLUCAO, evento.instanteEvento());
        assertEquals(RESOLVIDO, contexto.estadoAtual());
        assertEquals(RESOLVIDO, atual.estado());
        assertFalse(contexto.estaAtivo());
        assertEquals(3, atual.versao().intValue());
        assertEquals("alt-001", atual.idAlerta());
        assertEquals(ABERTURA, atual.abertoEm());
        assertEquals("enf-01", atual.reconhecidoPor());
        assertEquals(CIENCIA, atual.reconhecidoEm());
        assertEquals("med-01", atual.resolvidoPor());
        assertEquals(RESOLUCAO, atual.resolvidoEm());
        assertEquals(MOTIVO, atual.motivoResolucao());
        conferirOrigem(critica(), atual);

        assertEquals(RECONHECIDO, ciencia.atual().estado());
        assertNull(ciencia.atual().motivoResolucao());
    }

    @Test
    void abertoRejeitaResolucaoDireta() {
        ContextoAlerta contexto = aberto();
        conferirRejeicao(contexto, TransicaoAlertaInvalidaException.class,
                () -> contexto.resolver("med-01", RESOLUCAO, MOTIVO));
    }

    @Test
    void reconhecidoRejeitaSegundoReconhecimento() {
        ContextoAlerta contexto = reconhecido();
        conferirRejeicao(contexto, TransicaoAlertaInvalidaException.class,
                () -> contexto.reconhecer("enf-02", CIENCIA));
    }

    @Test
    void resolvidoRejeitaAsDuasAcoes() {
        ContextoAlerta contexto = reconhecido();
        contexto.resolver("med-01", RESOLUCAO, MOTIVO);
        conferirRejeicao(contexto, TransicaoAlertaInvalidaException.class,
                () -> contexto.reconhecer("enf-02", CIENCIA));
        conferirRejeicao(contexto, TransicaoAlertaInvalidaException.class,
                () -> contexto.resolver("med-02", RESOLUCAO, "Outra tentativa"));
    }

    @Test
    void reconhecimentoComDadosInvalidosPermiteTentarNovamente() {
        ContextoAlerta contexto = aberto();
        conferirRejeicao(contexto, IllegalArgumentException.class, () -> contexto.reconhecer(" ", CIENCIA));
        conferirRejeicao(contexto, IllegalArgumentException.class, () -> contexto.reconhecer("enf-01", SEM_FUSO));
        assertEquals(2, contexto.reconhecer("enf-01", CIENCIA).atual().versao().intValue());
    }

    @Test
    void resolucaoComDadosInvalidosPermiteTentarNovamente() {
        ContextoAlerta contexto = reconhecido();
        conferirRejeicao(contexto, IllegalArgumentException.class, () -> contexto.resolver(" ", RESOLUCAO, MOTIVO));
        conferirRejeicao(contexto, IllegalArgumentException.class, () -> contexto.resolver("med-01", SEM_FUSO, MOTIVO));
        conferirRejeicao(contexto, IllegalArgumentException.class, () -> contexto.resolver("med-01", RESOLUCAO, null));
        conferirRejeicao(contexto, IllegalArgumentException.class, () -> contexto.resolver("med-01", RESOLUCAO, " "));
        assertEquals(3, contexto.resolver("med-01", RESOLUCAO, MOTIVO).atual().versao().intValue());
    }

    @Test
    void cadaAlertaMantemSeuProprioEstado() {
        ContextoAlerta primeiro = aberto();
        Leitura outra = Leitura.simples("leit-003", "PAC-102", "uti", "leito-02", TipoSinal.FREQUENCIA, 138, COLETA)
                .classificada(Prioridade.CRITICO, "Frequência cardíaca na faixa crítica da simulação");
        ContextoAlerta segundo = ContextoAlerta.abrir(outra, "alt-002", ABERTURA);

        primeiro.reconhecer("enf-01", CIENCIA);
        assertEquals(RECONHECIDO, primeiro.estadoAtual());
        assertEquals(ABERTO, segundo.estadoAtual());
        assertEquals(1, segundo.alertaAtual().versao().intValue());
        assertEquals("PAC-102", segundo.alertaAtual().pacienteId());
        assertNull(segundo.ultimoEvento().estadoAnterior());
    }

    @Test
    void doisReconhecimentosNoMesmoContextoAplicamUmaUnicaTransicao() throws Exception {
        ContextoAlerta contexto = aberto();
        CyclicBarrier inicio = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primeira = executor.submit(() -> tentarReconhecer(contexto, inicio, "enf-01"));
            Future<Boolean> segunda = executor.submit(() -> tentarReconhecer(contexto, inicio, "enf-02"));
            boolean aceitouPrimeira = primeira.get(5, TimeUnit.SECONDS);
            boolean aceitouSegunda = segunda.get(5, TimeUnit.SECONDS);
            assertNotEquals(aceitouPrimeira, aceitouSegunda);
            assertEquals(aceitouPrimeira ? "enf-01" : "enf-02", contexto.alertaAtual().reconhecidoPor());
            assertEquals(RECONHECIDO, contexto.estadoAtual());
            assertEquals(2, contexto.alertaAtual().versao().intValue());
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean tentarReconhecer(ContextoAlerta contexto, CyclicBarrier inicio, String usuario)
            throws Exception {
        inicio.await(5, TimeUnit.SECONDS);
        try {
            contexto.reconhecer(usuario, CIENCIA);
            return true;
        } catch (TransicaoAlertaInvalidaException esperada) {
            return false;
        }
    }
}
