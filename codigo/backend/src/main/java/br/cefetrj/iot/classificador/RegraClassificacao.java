package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.Leitura;

@FunctionalInterface
public interface RegraClassificacao {
    ResultadoClassificacao classificar(Leitura leitura);
}
