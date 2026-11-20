package sensores;

public class FabricaFrequenciaCardiaca extends FabricaSensor {

	// implementacao concreta do Factory Method
	@Override
	public Sensor criaSensor(String leito, String paciente) {
		return new SensorFrequenciaCardiaca(leito, paciente);
	}
}
