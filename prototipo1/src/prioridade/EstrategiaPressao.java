package prioridade;

import comum.Prioridade;

// usa so a pressao sistolica, faixas ilustrativas pra simulacao
public class EstrategiaPressao implements IEstrategiaPrioridade {

	@Override
	public Classificacao classificar(double valor) {
		if (valor >= 160) {
			return new Classificacao(Prioridade.CRITICO, "hipertens\u00e3o grave");
		}
		if (valor < 80) {
			return new Classificacao(Prioridade.CRITICO, "hipotens\u00e3o grave");
		}
		if (valor >= 140) {
			return new Classificacao(Prioridade.ATENCAO, "press\u00e3o elevada");
		}
		if (valor < 90) {
			return new Classificacao(Prioridade.ATENCAO, "press\u00e3o baixa");
		}
		return new Classificacao(Prioridade.ROTINA, "normal");
	}
}
