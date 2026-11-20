package publicadores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import comum.Alerta;
import comum.Hora;
import comum.Leitura;
import comum.Prioridade;
import estados.AlertaSinal;
import estados.IEmissorAlerta;
import mqtt.ConexaoMqtt;
import prioridade.Classificacao;
import sensores.FabricaSensor;
import sensores.Sensor;

// monitor ao lado do leito, e o dispositivo que publica no broker
// tem uma conexao propria, os sensores do leito e um estado de alerta pra cada sinal
public class MonitorLeito implements IEmissorAlerta {

	private String leito;
	private String paciente;
	private ConexaoMqtt conexao;
	private List<Sensor> sensores = new ArrayList<Sensor>();
	private Map<String, AlertaSinal> alertas = new HashMap<String, AlertaSinal>();
	private AtomicLong totalPublicadas = new AtomicLong();
	private Consumer<String> saida;

	public MonitorLeito(String leito, String paciente, FabricaSensor[] fabricas, Consumer<String> saida) {
		this.leito = leito;
		this.paciente = paciente;
		this.saida = saida;
		this.conexao = new ConexaoMqtt("monitor_" + leito);

		// cada fabrica cria o sensor do seu sinal
		for (FabricaSensor f : fabricas) {
			Sensor s = f.criarEAnunciar(leito, paciente);
			sensores.add(s);
			alertas.put(s.getSinal(), new AlertaSinal(leito, paciente, s.getSinal(), this));
		}
	}

	public void conectar() throws Exception {
		conexao.conectar();
	}

	public void desconectar() {
		conexao.desconectar();
	}

	// uma rodada de medicoes, chamada pelo simulador a cada segundo
	public void ciclo() {
		for (Sensor s : sensores) {
			double valor = s.lerValor();
			Classificacao c = s.classificar(valor);

			Leitura leitura = new Leitura(leito, paciente, s.getSinal(), valor, s.getUnidade(), s.getCasas(),
					c.getPrioridade(), c.getCondicao(), Hora.agora());

			// a prioridade vai no topico e define o QoS
			publicar(leitura.getTopico(), leitura.paraJson(), c.getPrioridade().getQos());

			// o estado do sinal decide se precisa emitir alerta
			alertas.get(s.getSinal()).processar(c, leitura);
		}
	}

	@Override
	public void emitir(Alerta alerta) {
		publicar(alerta.getTopico(), alerta.paraJson(), Prioridade.CRITICO.getQos());
		saida.accept(Hora.agora() + " alerta " + alerta.getTipo() + ": " + alerta.getLeito() + " "
				+ alerta.getCondicao() + " (valor " + alerta.getValorTexto() + ")");
	}

	private void publicar(String topico, String mensagem, int qos) {
		try {
			conexao.publicar(topico, mensagem, qos);
			totalPublicadas.incrementAndGet();
		} catch (Exception e) {
			saida.accept("falha ao publicar em " + topico + ": " + e.getMessage());
		}
	}

	public Sensor getSensor(String sinal) {
		for (Sensor s : sensores) {
			if (s.getSinal().equals(sinal)) {
				return s;
			}
		}
		return null;
	}

	public String getLeito() {
		return leito;
	}

	public long getTotalPublicadas() {
		return totalPublicadas.get();
	}
}
