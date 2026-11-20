package principal;

import javax.swing.SwingUtilities;

import assinantes.AssinanteMqtt;
import assinantes.RegistroConsole;
import gui.Avisos;
import gui.PainelMedicoFrame;

// painel medico, assina so os topicos do leito que esta sendo acompanhado
public class MedicoMain {

	public static void main(String[] args) {
		final AssinanteMqtt assinante = new AssinanteMqtt("medico");

		try {
			assinante.conectar();
		} catch (Exception e) {
			Avisos.brokerIndisponivel(e);
			System.exit(1);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					PainelMedicoFrame painel = new PainelMedicoFrame(assinante);
					assinante.adicionarObservador(painel);
					assinante.adicionarObservador(new RegistroConsole("medico", false));
					painel.setVisible(true);
					assinante.iniciar();
				} catch (Exception e) {
					Avisos.brokerIndisponivel(e);
					System.exit(1);
				}
			}
		});
	}
}
