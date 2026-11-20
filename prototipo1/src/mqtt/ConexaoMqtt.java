package mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import comum.ConfigMqtt;

public class ConexaoMqtt {

	private String idCliente;
	private BlockingConnection conexao;

	// o id do cliente precisa ser unico no broker, por isso o numero sorteado
	public ConexaoMqtt(String prefixo) {
		this.idCliente = prefixo + "_" + new Random().nextInt(100000);
	}

	public void conectar() throws Exception {
		MQTT mqtt = new MQTT();
		mqtt.setHost(ConfigMqtt.HOST, ConfigMqtt.PORTA);
		mqtt.setClientId(idCliente);
		mqtt.setCleanSession(true);
		mqtt.setKeepAlive((short) 20);

		// se o broker nao estiver no ar falha logo, em vez de ficar tentando pra sempre
		mqtt.setConnectAttemptsMax(1);

		conexao = mqtt.blockingConnection();
		conexao.connect();
	}

	public void publicar(String topico, String mensagem, int qos) throws Exception {
		conexao.publish(topico, mensagem.getBytes(StandardCharsets.UTF_8), qosDe(qos), false);
	}

	// todos os filtros sao assinados com QoS 2, a mensagem chega com o QoS que o publicador usou
	public void assinar(String... filtros) throws Exception {
		Topic[] topicos = new Topic[filtros.length];
		for (int i = 0; i < filtros.length; i++) {
			topicos[i] = new Topic(filtros[i], QoS.EXACTLY_ONCE);
		}
		conexao.subscribe(topicos);
	}

	public void cancelar(String... filtros) throws Exception {
		conexao.unsubscribe(filtros);
	}

	// fica esperando a proxima mensagem
	public Recebida receber() throws Exception {
		Message m = conexao.receive();
		m.ack();
		return new Recebida(m.getTopic(), new String(m.getPayload(), StandardCharsets.UTF_8));
	}

	public void desconectar() {
		try {
			conexao.disconnect();
		} catch (Exception e) {
			// ja estava desconectado
		}
	}

	private QoS qosDe(int qos) {
		if (qos == 2) {
			return QoS.EXACTLY_ONCE;
		}
		if (qos == 1) {
			return QoS.AT_LEAST_ONCE;
		}
		return QoS.AT_MOST_ONCE;
	}

	public static class Recebida {

		private String topico;
		private String conteudo;

		public Recebida(String topico, String conteudo) {
			this.topico = topico;
			this.conteudo = conteudo;
		}

		public String getTopico() {
			return topico;
		}

		public String getConteudo() {
			return conteudo;
		}
	}
}
