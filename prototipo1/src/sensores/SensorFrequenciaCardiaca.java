package sensores;

import comum.Topicos;
import prioridade.EstrategiaFrequenciaCardiaca;

public class SensorFrequenciaCardiaca extends Sensor {

	public SensorFrequenciaCardiaca(String leito, String paciente) {
		super(leito, paciente, new EstrategiaFrequenciaCardiaca());
	}

	@Override
	public String getSinal() {
		return Topicos.SINAL_FC;
	}

	@Override
	public String getUnidade() {
		return "bpm";
	}

	@Override
	public int getCasas() {
		return 0;
	}

	@Override
	protected double valorNormal() {
		return 75;
	}

	@Override
	protected double valorCrise() {
		return 168;
	}

	@Override
	protected double ruido() {
		return 6;
	}
}
