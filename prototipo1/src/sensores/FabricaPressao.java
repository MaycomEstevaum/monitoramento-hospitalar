package sensores;

public class FabricaPressao extends FabricaSensor {

	// implementacao concreta do Factory Method
	@Override
	public Sensor criaSensor(String leito, String paciente) {
		return new SensorPressao(leito, paciente);
	}
}
