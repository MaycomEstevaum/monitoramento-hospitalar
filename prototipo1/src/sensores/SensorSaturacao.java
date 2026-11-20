package sensores;

import comum.Topicos;
import prioridade.EstrategiaSaturacao;

public class SensorSaturacao extends Sensor {

	public SensorSaturacao(String leito, String paciente) {
		super(leito, paciente, new EstrategiaSaturacao());
	}

	@Override
	public String getSinal() {
		return Topicos.SINAL_SPO2;
	}

	@Override
	public String getUnidade() {
		return "%";
	}

	@Override
	public int getCasas() {
		return 0;
	}

	@Override
	protected double valorNormal() {
		return 97;
	}

	@Override
	protected double valorCrise() {
		return 82;
	}

	@Override
	protected double ruido() {
		return 1;
	}

	// saturacao nao passa de 100
	@Override
	protected double limitar(double valor) {
		return Math.min(100, valor);
	}
}
