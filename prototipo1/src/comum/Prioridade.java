package comum;

// niveis de prioridade das medicoes
// cada nivel tem o nome usado no topico e o QoS usado na publicacao
public enum Prioridade {

	ROTINA("rotina", 0),
	ATENCAO("atencao", 1),
	CRITICO("critico", 2);

	private String topico;
	private int qos;

	private Prioridade(String topico, int qos) {
		this.topico = topico;
		this.qos = qos;
	}

	public String getTopico() {
		return topico;
	}

	public int getQos() {
		return qos;
	}

	public static Prioridade deTopico(String texto) {
		for (Prioridade p : values()) {
			if (p.topico.equals(texto)) {
				return p;
			}
		}
		return ROTINA;
	}

	public boolean maiorQue(Prioridade outra) {
		return this.ordinal() > outra.ordinal();
	}
}
