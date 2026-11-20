package estados;

import comum.Alerta;

// quem recebe os alertas gerados pelos estados e publica no broker
public interface IEmissorAlerta {

	public void emitir(Alerta alerta);
}
