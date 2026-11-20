package comum;

// todos os nomes de topicos ficam aqui
// leituras: hospital/<leito>/<sinal>/<prioridade>
// alertas: alertas/<leito>/<tipo>
public class Topicos {

	public static final String SINAL_FC = "frequencia_cardiaca";
	public static final String SINAL_TEMP = "temperatura";
	public static final String SINAL_SPO2 = "saturacao_oxigenio";
	public static final String SINAL_PA = "pressao_arterial";

	public static final String ALERTA_CRITICO = "critico";
	public static final String ALERTA_ESTABILIZADO = "estabilizado";

	public static String leitura(String leito, String sinal, Prioridade prioridade) {
		return "hospital/" + leito + "/" + sinal + "/" + prioridade.getTopico();
	}

	public static String alerta(String leito, String tipo) {
		return "alertas/" + leito + "/" + tipo;
	}

	// filtros usados pelos assinantes
	public static final String TODAS_LEITURAS = "hospital/#";
	public static final String TODOS_ALERTAS = "alertas/#";
	public static final String ALERTAS_CRITICOS = "alertas/+/critico";
	public static final String ALERTAS_ESTABILIZADOS = "alertas/+/estabilizado";

	public static String leiturasDoLeito(String leito) {
		return "hospital/" + leito + "/#";
	}

	public static String alertasDoLeito(String leito) {
		return "alertas/" + leito + "/#";
	}
}
