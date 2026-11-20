package prioridade;

// Strategy: cada sinal vital tem as suas proprias faixas
public interface IEstrategiaPrioridade {

	public Classificacao classificar(double valor);
}
