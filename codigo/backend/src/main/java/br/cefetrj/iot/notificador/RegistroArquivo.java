package br.cefetrj.iot.notificador;

import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.EventoAlerta;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class RegistroArquivo implements ObservadorAlerta {
    private final Path caminho;

    public RegistroArquivo() {
        this(Path.of("logs", "alertas.log"));
    }

    public RegistroArquivo(Path caminho) {
        this.caminho = Objects.requireNonNull(caminho, "caminho é obrigatório").toAbsolutePath().normalize();
    }

    public Path caminhoArquivo() {
        return caminho;
    }

    @Override
    public synchronized void atualizar(EventoAlerta evento) {
        String anterior = evento.estadoAnterior() == null ? "INICIO" : evento.estadoAnterior().name();
        String linha = "%s | %s->%s | v%d | %s%n".formatted(
                evento.instanteEvento(), anterior, evento.atual().estado(), evento.atual().versao(),
                ConversorJson.paraJson(evento.atual()));
        try {
            Path diretorio = caminho.getParent();
            if (diretorio != null) {
                Files.createDirectories(diretorio);
            }
            Files.writeString(caminho, linha, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException erro) {
            throw new UncheckedIOException("Falha ao gravar " + caminho, erro);
        }
    }
}
