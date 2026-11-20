package br.cefetrj.iot.notificador;

import static org.junit.jupiter.api.Assertions.*;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.EventoAlerta;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Prioridade;
import br.cefetrj.iot.contrato.TipoSinal;
import br.cefetrj.iot.notificador.estado.ContextoAlerta;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CentralAlertasTest {
    private static final String AGORA = "2026-11-20T10:00:00-03:00";

    private static ContextoAlerta aberto() {
        Leitura leitura = Leitura.simples("leit-1", "PAC-101", "uti", "leito-01", TipoSinal.FREQUENCIA, 145, AGORA)
                .classificada(Prioridade.CRITICO, "Frequência na faixa crítica da simulação");
        return ContextoAlerta.abrir(leitura, "alt-001", AGORA);
    }

    @Test
    void registraUmaVezERemove() {
        CentralAlertas central = new CentralAlertas();
        List<EventoAlerta> a = new ArrayList<>();
        List<EventoAlerta> b = new ArrayList<>();
        ObservadorAlerta primeiro = a::add;
        ObservadorAlerta segundo = b::add;
        central.registrar(primeiro);
        central.registrar(primeiro);
        central.registrar(segundo);
        ContextoAlerta contexto = aberto();
        EventoAlerta abertura = contexto.ultimoEvento();
        central.notificar(abertura);
        central.remover(segundo);
        EventoAlerta ciencia = contexto.reconhecer("enf-01", AGORA);
        central.notificar(ciencia);
        assertEquals(List.of(abertura, ciencia), a);
        assertEquals(List.of(abertura), b);
    }

    @Test
    void registroConcorrenteDaMesmaInstanciaNotificaUmaVez() throws Exception {
        CentralAlertas central = new CentralAlertas();
        List<EventoAlerta> recebidos = new ArrayList<>();
        ObservadorAlerta observador = recebidos::add;
        var barreira = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> cadastrar = () -> {
                barreira.await(5, TimeUnit.SECONDS);
                central.registrar(observador);
                return null;
            };
            var primeira = executor.submit(cadastrar);
            var segunda = executor.submit(cadastrar);
            primeira.get(5, TimeUnit.SECONDS);
            segunda.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        EventoAlerta abertura = aberto().ultimoEvento();
        central.notificar(abertura);
        assertEquals(List.of(abertura), recebidos);
    }

    @Test
    void falhaNoArquivoPermiteAvisarProximoObservador(@TempDir Path dir) throws Exception {
        Path arquivoNoLugarDaPasta = Files.createFile(dir.resolve("ocupado"));
        CentralAlertas central = new CentralAlertas();
        central.registrar(new RegistroArquivo(arquivoNoLugarDaPasta.resolve("alertas.log")));
        List<EventoAlerta> recebidos = new ArrayList<>();
        central.registrar(recebidos::add);
        EventoAlerta abertura = aberto().ultimoEvento();
        central.notificar(abertura);
        assertEquals(List.of(abertura), recebidos);
    }

    @Test
    void consoleEArquivoRegistramTresEventosComAuditoria(@TempDir Path dir) throws Exception {
        Path log = dir.resolve("logs").resolve("alertas.log");
        var buffer = new ByteArrayOutputStream();
        CentralAlertas central = new CentralAlertas();
        try (var saida = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            central.registrar(new LogConsole(saida));
            central.registrar(new RegistroArquivo(log));
            ContextoAlerta contexto = aberto();
            var eventos = List.of(contexto.ultimoEvento(), contexto.reconhecer("enf-01", AGORA),
                    contexto.resolver("med-01", AGORA, "Reavaliação\nconcluída"));
            for (EventoAlerta evento : eventos) central.notificar(evento);
            List<String> linhas = Files.readAllLines(log, StandardCharsets.UTF_8);
            assertEquals(3, linhas.size());
            for (int i = 0; i < eventos.size(); i++) {
                var evento = eventos.get(i);
                var partes = linhas.get(i).split(" \\| ", 4);
                String anterior = evento.estadoAnterior() == null ? "INICIO" : evento.estadoAnterior().name();
                assertEquals(evento.instanteEvento(), partes[0]);
                assertEquals(anterior + "->" + evento.atual().estado(), partes[1]);
                assertEquals("v" + evento.atual().versao(), partes[2]);
                assertEquals(evento.atual(), ConversorJson.deJson(partes[3], Alerta.class));
            }
        }
        String texto = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("[ABERTURA] alerta=alt-001"));
        assertTrue(texto.contains("ABERTO -> RECONHECIDO"));
        assertTrue(texto.contains("RECONHECIDO -> RESOLVIDO"));
    }
}
