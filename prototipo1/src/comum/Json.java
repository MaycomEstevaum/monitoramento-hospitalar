package comum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// json simples, so objetos planos com texto e numeros
// o JDK nao tem biblioteca de json e assim o projeto nao precisa de jar extra
public class Json {

	// recebe pares chave, valor
	public static String criar(Object... pares) {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < pares.length; i += 2) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("\"").append(pares[i]).append("\":");
			Object valor = pares[i + 1];
			if (valor instanceof String) {
				sb.append("\"").append(valor).append("\"");
			} else {
				sb.append(valor);
			}
		}
		return sb.append("}").toString();
	}

	public static String texto(String json, String chave) {
		Matcher m = Pattern.compile("\"" + chave + "\":(\"([^\"]*)\"|([^,}]*))").matcher(json);
		if (m.find()) {
			return m.group(2) != null ? m.group(2) : m.group(3);
		}
		return "";
	}

	public static double numero(String json, String chave) {
		return Double.parseDouble(texto(json, chave));
	}
}
