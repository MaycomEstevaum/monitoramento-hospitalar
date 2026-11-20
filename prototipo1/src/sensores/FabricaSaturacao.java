package sensores;

public class FabricaSaturacao extends FabricaSensor {

	// implementacao concreta do Factory Method
	@Override
	public Sensor criaSensor(String leito, String paciente) {
		return new SensorSaturacao(leito, paciente);
	}
}
