package br.cefetrj.iot.notificador;

import br.cefetrj.iot.contrato.Alerta;
import br.cefetrj.iot.contrato.EventoAlerta;
import java.io.PrintStream;
import java.util.Objects;

public final class LogConsole implements ObservadorAlerta {

    private final PrintStream saida;

    public LogConsole() {
        this(System.out);
    }

    public LogConsole(PrintStream saida) {
        this.saida = Objects.requireNonNull(saida, "saida é obrigatória.");
    }

    @Override
    public void atualizar(EventoAlerta evento) {
        Objects.requireNonNull(evento, "evento é obrigatório.");
        Alerta alerta = evento.atual();
        if (evento.estadoAnterior() == null) {
            saida.printf(
                    "[Notificador][ABERTURA] alerta=%s | paciente=%s | local=%s/%s | sinal=%s | estado=%s | versao=%d | instante=%s%n",
                    alerta.idAlerta(),
                    alerta.pacienteId(),
                    alerta.ala(),
                    alerta.leito(),
                    alerta.tipoSinal().getSegmentoTopico(),
                    alerta.estado(),
                    alerta.versao(),
                    evento.instanteEvento()
            );
        } else {
            saida.printf(
                    "[Notificador][TRANSICAO] alerta=%s | paciente=%s | %s -> %s | versao=%d | instante=%s%n",
                    alerta.idAlerta(),
                    alerta.pacienteId(),
                    evento.estadoAnterior(),
                    alerta.estado(),
                    alerta.versao(),
                    evento.instanteEvento()
            );
        }
    }
}
