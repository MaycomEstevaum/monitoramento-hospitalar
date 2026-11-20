package sensores;

public class FabricaTemperatura extends FabricaSensor {

	// implementacao concreta do Factory Method
	@Override
	public Sensor criaSensor(String leito, String paciente) {
		return new SensorTemperatura(leito, paciente);
	}
}
