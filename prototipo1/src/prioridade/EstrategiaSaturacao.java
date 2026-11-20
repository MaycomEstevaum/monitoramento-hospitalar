package prioridade;

import comum.Prioridade;

// faixas ilustrativas pra simulacao, nao sao valores clinicos
public class EstrategiaSaturacao implements IEstrategiaPrioridade {

	@Override
	public Classificacao classificar(double valor) {
		if (valor <= 90) {
			return new Classificacao(Prioridade.CRITICO, "hipoxemia");
		}
		if (valor <= 94) {
			return new Classificacao(Prioridade.ATENCAO, "satura\u00e7\u00e3o baixa");
		}
		return new Classificacao(Prioridade.ROTINA, "normal");
	}
}
