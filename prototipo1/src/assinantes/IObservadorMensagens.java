package assinantes;

import comum.Alerta;
import comum.Leitura;

// Observer: quem quer reagir ao que o assinante recebe do broker
public interface IObservadorMensagens {

	public void aoReceberLeitura(Leitura leitura);

	public void aoReceberAlerta(Alerta alerta);
}
