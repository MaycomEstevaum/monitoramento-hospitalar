package br.cefetrj.iot.notificador;

import br.cefetrj.iot.contrato.EventoAlerta;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CentralAlertas {
    private final CopyOnWriteArrayList<ObservadorAlerta> observadores = new CopyOnWriteArrayList<>();

    public void registrar(ObservadorAlerta observador) {
        observadores.addIfAbsent(Objects.requireNonNull(observador, "observador é obrigatório"));
    }

    public void remover(ObservadorAlerta observador) {
        observadores.remove(observador);
    }

    public void notificar(EventoAlerta evento) {
        Objects.requireNonNull(evento, "evento é obrigatório");
        for (ObservadorAlerta observador : observadores) {
            try {
                observador.atualizar(evento);
            } catch (RuntimeException erro) {
                System.err.printf("Falha no observador %s para %s v%d: %s%n",
                        observador.getClass().getSimpleName(), evento.atual().idAlerta(),
                        evento.atual().versao(), erro.getMessage());
            }
        }
    }
}
