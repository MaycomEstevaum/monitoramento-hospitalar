package prioridade;

import comum.Prioridade;

// faixas ilustrativas pra simulacao, nao sao valores clinicos
public class EstrategiaFrequenciaCardiaca implements IEstrategiaPrioridade {

	@Override
	public Classificacao classificar(double valor) {
		if (valor > 120) {
			return new Classificacao(Prioridade.CRITICO, "taquicardia");
		}
		if (valor < 50) {
			return new Classificacao(Prioridade.CRITICO, "bradicardia");
		}
		if (valor > 100) {
			return new Classificacao(Prioridade.ATENCAO, "taquicardia leve");
		}
		if (valor < 60) {
			return new Classificacao(Prioridade.ATENCAO, "bradicardia leve");
		}
		return new Classificacao(Prioridade.ROTINA, "normal");
	}
}
