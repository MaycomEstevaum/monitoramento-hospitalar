package sensores;

public abstract class FabricaSensor {

	// Factory Method
	public abstract Sensor criaSensor(String leito, String paciente);

	public Sensor criarEAnunciar(String leito, String paciente) {

		// cria o sensor "generico" a partir do Factory Method
		Sensor s = criaSensor(leito, paciente);

		System.out.println("sensor criado: " + s.getSinal() + " do " + leito);

		return s;
	}
}

// pra cada sinal vital tem uma fabrica correspondente
