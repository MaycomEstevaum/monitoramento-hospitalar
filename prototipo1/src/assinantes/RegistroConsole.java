package assinantes;

import comum.Alerta;
import comum.Leitura;

// outro observador, escreve no console o que o assinante recebe
public class RegistroConsole implements IObservadorMensagens {

	private String nome;
	private boolean mostrarLeituras;

	public RegistroConsole(String nome, boolean mostrarLeituras) {
		this.nome = nome;
		this.mostrarLeituras = mostrarLeituras;
	}

	@Override
	public void aoReceberLeitura(Leitura leitura) {
		if (mostrarLeituras) {
			System.out.println("[" + nome + "] " + leitura);
		}
	}

	@Override
	public void aoReceberAlerta(Alerta alerta) {
		System.out.println("[" + nome + "] alerta " + alerta.getTipo() + ": " + alerta.getLeito() + " "
				+ alerta.getCondicao() + " " + alerta.getHora());
	}
}
