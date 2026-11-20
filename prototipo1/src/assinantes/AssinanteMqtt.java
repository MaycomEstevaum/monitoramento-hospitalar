package assinantes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import comum.Alerta;
import comum.Leitura;
import mqtt.ConexaoMqtt;
import mqtt.ConexaoMqtt.Recebida;

// Subject do Observer
// recebe as mensagens do broker e avisa os observadores, quem reage a elas e o observador
public class AssinanteMqtt {

	private ConexaoMqtt conexao;
	private List<IObservadorMensagens> observadores = new CopyOnWriteArrayList<IObservadorMensagens>();
	private volatile boolean rodando;

	public AssinanteMqtt(String nome) {
		this.conexao = new ConexaoMqtt(nome);
	}

	public void conectar() throws Exception {
		conexao.conectar();
	}

	public void assinar(String... filtros) throws Exception {
		conexao.assinar(filtros);
	}

	public void cancelar(String... filtros) throws Exception {
		conexao.cancelar(filtros);
	}

	public void adicionarObservador(IObservadorMensagens observador) {
		observadores.add(observador);
	}

	public void removerObservador(IObservadorMensagens observador) {
		observadores.remove(observador);
	}

	// comeca a escutar numa thread separada
	public void iniciar() {
		rodando = true;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (rodando) {
					try {
						Recebida r = conexao.receber();
						tratar(r);
					} catch (Exception e) {
						if (rodando) {
							System.out.println("conexao com o broker encerrada: " + e.getMessage());
						}
						break;
					}
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	public void parar() {
		rodando = false;
		conexao.desconectar();
	}

	private void tratar(Recebida r) {
		try {
			if (r.getTopico().startsWith("alertas/")) {
				Alerta a = Alerta.deJson(r.getConteudo());
				for (IObservadorMensagens o : observadores) {
					o.aoReceberAlerta(a);
				}
			} else {
				Leitura l = Leitura.deJson(r.getConteudo());
				for (IObservadorMensagens o : observadores) {
					o.aoReceberLeitura(l);
				}
			}
		} catch (Exception e) {
			// mensagem mal formada, ignora e segue escutando
			System.out.println("mensagem ignorada em " + r.getTopico());
		}
	}
}
