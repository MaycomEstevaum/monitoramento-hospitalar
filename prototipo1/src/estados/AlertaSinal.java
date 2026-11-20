package estados;

import comum.Alerta;
import comum.Hora;
import comum.Leitura;
import prioridade.Classificacao;

// contexto do State, guarda o nivel de alerta de um sinal de um paciente
// o alerta so e emitido na mudanca de estado, nao a cada leitura
public class AlertaSinal {

	private String leito;
	private String paciente;
	private String sinal;
	private IEmissorAlerta emissor;

	// todo sinal comeca no estado normal
	private IEstadoAlerta estado = new EstadoNormal();

	public AlertaSinal(String leito, String paciente, String sinal, IEmissorAlerta emissor) {
		this.leito = leito;
		this.paciente = paciente;
		this.sinal = sinal;
		this.emissor = emissor;
	}

	// delega pro estado atual
	public void processar(Classificacao classificacao, Leitura leitura) {
		estado.processar(this, classificacao, leitura);
	}

	public void setEstado(IEstadoAlerta novoEstado) {
		System.out.println(leito + " " + sinal + ": " + estado.getNome() + " para " + novoEstado.getNome());
		this.estado = novoEstado;
	}

	public void emitir(String tipo, Leitura leitura) {
		emissor.emitir(new Alerta(leito, paciente, sinal, leitura.getCondicao(), leitura.getValor(),
				leitura.getUnidade(), leitura.getCasas(), tipo, Hora.agora()));
	}

	public String getNomeEstado() {
		return estado.getNome();
	}
}
