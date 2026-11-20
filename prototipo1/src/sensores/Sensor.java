package sensores;

import java.util.Random;

import prioridade.Classificacao;
import prioridade.IEstrategiaPrioridade;

// sensor simulado, o valor anda aos poucos em direcao a um alvo e tem um pouco de ruido
// o alvo normal e o valor de repouso, o alvo de crise leva o paciente pra faixa critica
public abstract class Sensor {

	protected String leito;
	protected String paciente;

	// Strategy usada pra classificar as medicoes deste sensor
	protected IEstrategiaPrioridade estrategia;

	private Random sorte = new Random();
	private double base;
	private double valorAtual;
	private volatile double alvo;

	public Sensor(String leito, String paciente, IEstrategiaPrioridade estrategia) {
		this.leito = leito;
		this.paciente = paciente;
		this.estrategia = estrategia;

		// cada paciente comeca com um valor normal um pouco diferente
		this.base = valorNormal() + (sorte.nextDouble() - 0.5) * ruido() * 2;
		this.valorAtual = base;
		this.alvo = base;
	}

	public abstract String getSinal();

	public abstract String getUnidade();

	public abstract int getCasas();

	protected abstract double valorNormal();

	protected abstract double valorCrise();

	protected abstract double ruido();

	protected double limitar(double valor) {
		return valor;
	}

	// faz uma medicao nova
	public double lerValor() {
		valorAtual = valorAtual + (alvo - valorAtual) * 0.3 + (sorte.nextDouble() - 0.5) * ruido();
		valorAtual = limitar(valorAtual);

		double fator = Math.pow(10, getCasas());
		return Math.round(valorAtual * fator) / fator;
	}

	public Classificacao classificar(double valor) {
		return estrategia.classificar(valor);
	}

	public void provocarCrise() {
		alvo = valorCrise();
	}

	public void normalizar() {
		alvo = base;
	}
}
