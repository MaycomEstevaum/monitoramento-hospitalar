package estados;

import comum.Leitura;
import comum.Prioridade;
import comum.Topicos;
import prioridade.Classificacao;

public class EstadoCritico implements IEstadoAlerta {

	@Override
	public void processar(AlertaSinal contexto, Classificacao classificacao, Leitura leitura) {
		// continuar critico nao gera alerta novo, so a saida do estado gera
		if (classificacao.getPrioridade() == Prioridade.ATENCAO) {
			contexto.setEstado(new EstadoAtencao());
			contexto.emitir(Topicos.ALERTA_ESTABILIZADO, leitura);
		}
		if (classificacao.getPrioridade() == Prioridade.ROTINA) {
			contexto.setEstado(new EstadoNormal());
			contexto.emitir(Topicos.ALERTA_ESTABILIZADO, leitura);
		}
	}

	@Override
	public String getNome() {
		return "Critico";
	}
}
