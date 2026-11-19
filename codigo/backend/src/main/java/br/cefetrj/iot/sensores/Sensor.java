package br.cefetrj.iot.sensores;

import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.TipoSinal;

public interface Sensor {
    TipoSinal getTipoSinal();

    Leitura coletar(LeitoSimulado leito);
}
