package publicadores;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import comum.Hora;
import comum.Hospital;
import sensores.FabricaFrequenciaCardiaca;
import sensores.FabricaPressao;
import sensores.FabricaSaturacao;
import sensores.FabricaSensor;
import sensores.FabricaTemperatura;
import sensores.Sensor;

// cria um monitor pra cada leito e dispara as medicoes a cada segundo
public class SimuladorHospital {

	private List<MonitorLeito> monitores = new ArrayList<MonitorLeito>();
	private ScheduledExecutorService agendador;
	private Consumer<String> saida = new Consumer<String>() {
		@Override
		public void accept(String texto) {
			System.out.println(texto);
		}
	};

	// onde vao as mensagens do simulador, o console por padrao
	public void setSaida(Consumer<String> saida) {
		this.saida = saida;
	}

	public void iniciar() throws Exception {
		FabricaSensor[] fabricas = { new FabricaFrequenciaCardiaca(), new FabricaTemperatura(),
				new FabricaSaturacao(), new FabricaPressao() };

		for (int i = 0; i < Hospital.LEITOS.length; i++) {
			MonitorLeito m = new MonitorLeito(Hospital.LEITOS[i], Hospital.PACIENTES[i], fabricas, saida);
			m.conectar();
			monitores.add(m);
		}

		agendador = Executors.newSingleThreadScheduledExecutor();
		agendador.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				// se uma rodada falhar o agendador nao pode parar
				try {
					for (MonitorLeito m : monitores) {
						m.ciclo();
					}
				} catch (Exception e) {
					saida.accept("erro na rodada de medicoes: " + e.getMessage());
				}
			}
		}, 1, 1, TimeUnit.SECONDS);

		saida.accept(Hora.agora() + " simulador iniciado com " + monitores.size() + " leitos");
	}

	public void parar() {
		if (agendador != null) {
			agendador.shutdownNow();
		}
		for (MonitorLeito m : monitores) {
			m.desconectar();
		}
	}

	public boolean provocarCrise(String leito, String sinal) {
		Sensor s = buscar(leito, sinal);
		if (s == null) {
			return false;
		}
		s.provocarCrise();
		saida.accept(Hora.agora() + " crise provocada: " + leito + " " + sinal);
		return true;
	}

	public boolean normalizar(String leito, String sinal) {
		Sensor s = buscar(leito, sinal);
		if (s == null) {
			return false;
		}
		s.normalizar();
		saida.accept(Hora.agora() + " normalizado: " + leito + " " + sinal);
		return true;
	}

	public long getTotalPublicadas() {
		long total = 0;
		for (MonitorLeito m : monitores) {
			total += m.getTotalPublicadas();
		}
		return total;
	}

	private Sensor buscar(String leito, String sinal) {
		for (MonitorLeito m : monitores) {
			if (m.getLeito().equals(leito)) {
				return m.getSensor(sinal);
			}
		}
		return null;
	}
}
