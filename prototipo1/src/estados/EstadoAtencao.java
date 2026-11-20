package estados;

import comum.Leitura;
import comum.Prioridade;
import comum.Topicos;
import prioridade.Classificacao;

public class EstadoAtencao implements IEstadoAlerta {

	@Override
	public void processar(AlertaSinal contexto, Classificacao classificacao, Leitura leitura) {
		if (classificacao.getPrioridade() == Prioridade.CRITICO) {
			contexto.setEstado(new EstadoCritico());
			contexto.emitir(Topicos.ALERTA_CRITICO, leitura);
		}
		if (classificacao.getPrioridade() == Prioridade.ROTINA) {
			contexto.setEstado(new EstadoNormal());
		}
	}

	@Override
	public String getNome() {
		return "Atencao";
	}
}
