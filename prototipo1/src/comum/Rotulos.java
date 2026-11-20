package comum;

// textos que aparecem nas telas
// os acentos ficam em unicode pra o codigo nao depender da codificacao do arquivo
public class Rotulos {

	public static String nomeSinal(String sinal) {
		if (sinal.equals(Topicos.SINAL_FC)) {
			return "Freq. card\u00edaca";
		}
		if (sinal.equals(Topicos.SINAL_TEMP)) {
			return "Temperatura";
		}
		if (sinal.equals(Topicos.SINAL_SPO2)) {
			return "Satura\u00e7\u00e3o O2";
		}
		if (sinal.equals(Topicos.SINAL_PA)) {
			return "Press\u00e3o arterial";
		}
		return sinal;
	}

	public static String nomePrioridade(Prioridade p) {
		if (p == Prioridade.CRITICO) {
			return "CR\u00cdTICO";
		}
		if (p == Prioridade.ATENCAO) {
			return "Aten\u00e7\u00e3o";
		}
		return "Rotina";
	}

	public static String nomeLeito(String leito) {
		return "Leito " + leito.replace("leito", "");
	}
}
