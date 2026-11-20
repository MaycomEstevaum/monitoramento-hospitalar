package sensores;

import comum.Topicos;
import prioridade.EstrategiaTemperatura;

public class SensorTemperatura extends Sensor {

	public SensorTemperatura(String leito, String paciente) {
		super(leito, paciente, new EstrategiaTemperatura());
	}

	@Override
	public String getSinal() {
		return Topicos.SINAL_TEMP;
	}

	@Override
	public String getUnidade() {
		return "\u00b0C";
	}

	@Override
	public int getCasas() {
		return 1;
	}

	@Override
	protected double valorNormal() {
		return 36.7;
	}

	@Override
	protected double valorCrise() {
		return 40.0;
	}

	@Override
	protected double ruido() {
		return 0.2;
	}
}
