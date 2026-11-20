package br.cefetrj.iot.notificador.estado;

public final class TransicaoAlertaInvalidaException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TransicaoAlertaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
