package comum;

import java.util.Locale;

// evento publicado quando um sinal entra em estado critico ou sai dele
// viaja nos topicos alertas/...
public class Alerta {

	private String leito;
	private String paciente;
	private String sinal;
	private String condicao;
	private double valor;
	private String unidade;
	private int casas;
	private String tipo;
	private String hora;

	public Alerta(String leito, String paciente, String sinal, String condicao, double valor, String unidade,
			int casas, String tipo, String hora) {
		this.leito = leito;
		this.paciente = paciente;
		this.sinal = sinal;
		this.condicao = condicao;
		this.valor = valor;
		this.unidade = unidade;
		this.casas = casas;
		this.tipo = tipo;
		this.hora = hora;
	}

	public String getTopico() {
		return Topicos.alerta(leito, tipo);
	}

	public String paraJson() {
		return Json.criar("leito", leito, "paciente", paciente, "sinal", sinal, "condicao", condicao, "valor", valor,
				"unidade", unidade, "casas", casas, "tipo", tipo, "hora", hora);
	}

	public static Alerta deJson(String json) {
		return new Alerta(Json.texto(json, "leito"), Json.texto(json, "paciente"), Json.texto(json, "sinal"),
				Json.texto(json, "condicao"), Json.numero(json, "valor"), Json.texto(json, "unidade"),
				(int) Json.numero(json, "casas"), Json.texto(json, "tipo"), Json.texto(json, "hora"));
	}

	public String getValorTexto() {
		return String.format(Locale.US, "%." + casas + "f", valor);
	}

	public boolean isCritico() {
		return tipo.equals(Topicos.ALERTA_CRITICO);
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

	public String getCondicao() {
		return condicao;
	}

	public String getUnidade() {
		return unidade;
	}

	public String getTipo() {
		return tipo;
	}

	public String getHora() {
		return hora;
	}
}
