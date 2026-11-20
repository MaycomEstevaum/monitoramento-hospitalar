package principal;

import javax.swing.SwingUtilities;

import assinantes.AssinanteMqtt;
import assinantes.RegistroConsole;
import comum.Topicos;
import gui.Avisos;
import gui.NotificadorMovelFrame;

// celular da equipe, assina so os alertas criticos e as estabilizacoes
public class NotificacoesMain {

	public static void main(String[] args) {
		final AssinanteMqtt assinante = new AssinanteMqtt("celular");

		try {
			assinante.conectar();
			assinante.assinar(Topicos.ALERTAS_CRITICOS, Topicos.ALERTAS_ESTABILIZADOS);
		} catch (Exception e) {
			Avisos.brokerIndisponivel(e);
			System.exit(1);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				NotificadorMovelFrame celular = new NotificadorMovelFrame();
				assinante.adicionarObservador(celular);
				assinante.adicionarObservador(new RegistroConsole("celular", false));
				celular.setVisible(true);
				assinante.iniciar();
			}
		});
	}
}
