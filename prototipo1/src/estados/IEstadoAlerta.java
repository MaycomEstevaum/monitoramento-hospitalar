package estados;

import comum.Leitura;
import prioridade.Classificacao;

// State: cada estado decide se muda de estado e se emite alerta
public interface IEstadoAlerta {

	public void processar(AlertaSinal contexto, Classificacao classificacao, Leitura leitura);

	public String getNome();
}
