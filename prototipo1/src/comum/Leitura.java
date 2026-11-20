package comum;

import java.util.Locale;

// uma medicao de um sinal vital, e o que viaja nos topicos hospital/...
public class Leitura {

	private String leito;
	private String paciente;
	private String sinal;
	private double valor;
	private String unidade;
	private int casas;
	private Prioridade prioridade;
	private String condicao;
	private String hora;

	public Leitura(String leito, String paciente, String sinal, double valor, String unidade, int casas,
			Prioridade prioridade, String condicao, String hora) {
		this.leito = leito;
		this.paciente = paciente;
		this.sinal = sinal;
		this.valor = valor;
		this.unidade = unidade;
		this.casas = casas;
		this.prioridade = prioridade;
		this.condicao = condicao;
		this.hora = hora;
	}

	public String getTopico() {
		return Topicos.leitura(leito, sinal, prioridade);
	}

	public String paraJson() {
		return Json.criar("leito", leito, "paciente", paciente, "sinal", sinal, "valor", valor, "unidade", unidade,
				"casas", casas, "prioridade", prioridade.getTopico(), "condicao", condicao, "hora", hora);
	}

	public static Leitura deJson(String json) {
		return new Leitura(Json.texto(json, "leito"), Json.texto(json, "paciente"), Json.texto(json, "sinal"),
				Json.numero(json, "valor"), Json.texto(json, "unidade"), (int) Json.numero(json, "casas"),
				Prioridade.deTopico(Json.texto(json, "prioridade")), Json.texto(json, "condicao"),
				Json.texto(json, "hora"));
	}

	// valor com o numero de casas certo, 72 pra batimentos e 36.7 pra temperatura
	public String getValorTexto() {
		return String.format(Locale.US, "%." + casas + "f", valor);
	}

	public String getLeito() {
		return leito;
	}

	public String getPaciente() {
		return paciente;
	}

	public String getSinal() {
		return sinal;
	}

	public double getValor() {
		return valor;
	}

	public String getUnidade() {
		return unidade;
	}

	public int getCasas() {
		return casas;
	}

	public Prioridade getPrioridade() {
		return prioridade;
	}

	public String getCondicao() {
		return condicao;
	}

	public String getHora() {
		return hora;
	}

	@Override
	public String toString() {
		return leito + " " + sinal + " " + getValorTexto() + " " + unidade + " (" + prioridade.getTopico() + ")";
	}
}
