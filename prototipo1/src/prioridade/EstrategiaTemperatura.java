package prioridade;

import comum.Prioridade;

// faixas ilustrativas pra simulacao, nao sao valores clinicos
public class EstrategiaTemperatura implements IEstrategiaPrioridade {

	@Override
	public Classificacao classificar(double valor) {
		if (valor >= 38.5) {
			return new Classificacao(Prioridade.CRITICO, "febre alta");
		}
		if (valor < 35.0) {
			return new Classificacao(Prioridade.CRITICO, "hipotermia");
		}
		if (valor >= 37.6) {
			return new Classificacao(Prioridade.ATENCAO, "febre leve");
		}
		if (valor < 36.0) {
			return new Classificacao(Prioridade.ATENCAO, "temperatura baixa");
		}
		return new Classificacao(Prioridade.ROTINA, "normal");
	}
}
