package prioridade;

import comum.Prioridade;

// resultado de classificar um valor, a prioridade e o nome da condicao
public class Classificacao {

	private Prioridade prioridade;
	private String condicao;

	public Classificacao(Prioridade prioridade, String condicao) {
		this.prioridade = prioridade;
		this.condicao = condicao;
	}

	public Prioridade getPrioridade() {
		return prioridade;
	}

	public String getCondicao() {
		return condicao;
	}
}
