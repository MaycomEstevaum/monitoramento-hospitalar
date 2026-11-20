package sensores;

import comum.Topicos;
import prioridade.EstrategiaPressao;

// mede a pressao sistolica
public class SensorPressao extends Sensor {

	public SensorPressao(String leito, String paciente) {
		super(leito, paciente, new EstrategiaPressao());
	}

	@Override
	public String getSinal() {
		return Topicos.SINAL_PA;
	}

	@Override
	public String getUnidade() {
		return "mmHg";
	}

	@Override
	public int getCasas() {
		return 0;
	}

	@Override
	protected double valorNormal() {
		return 120;
	}

	@Override
	protected double valorCrise() {
		return 185;
	}

	@Override
	protected double ruido() {
		return 6;
	}
}
