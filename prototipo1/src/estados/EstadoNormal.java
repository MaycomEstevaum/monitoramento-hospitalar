package estados;

import comum.Leitura;
import comum.Prioridade;
import comum.Topicos;
import prioridade.Classificacao;

public class EstadoNormal implements IEstadoAlerta {

	@Override
	public void processar(AlertaSinal contexto, Classificacao classificacao, Leitura leitura) {
		if (classificacao.getPrioridade() == Prioridade.ATENCAO) {
			contexto.setEstado(new EstadoAtencao());
		}
		if (classificacao.getPrioridade() == Prioridade.CRITICO) {
			contexto.setEstado(new EstadoCritico());
			contexto.emitir(Topicos.ALERTA_CRITICO, leitura);
		}
	}

	@Override
	public String getNome() {
		return "Normal";
	}
}
