package principal;

import javax.swing.SwingUtilities;

import assinantes.AssinanteMqtt;
import assinantes.RegistroConsole;
import comum.Topicos;
import gui.Avisos;
import gui.PainelEnfermagemFrame;

// painel da enfermagem, assina todas as leituras e todos os alertas
public class EnfermagemMain {

	public static void main(String[] args) {
		final AssinanteMqtt assinante = new AssinanteMqtt("enfermagem");

		try {
			assinante.conectar();
			assinante.assinar(Topicos.TODAS_LEITURAS, Topicos.TODOS_ALERTAS);
		} catch (Exception e) {
			Avisos.brokerIndisponivel(e);
			System.exit(1);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				PainelEnfermagemFrame painel = new PainelEnfermagemFrame();
				assinante.adicionarObservador(painel);
				assinante.adicionarObservador(new RegistroConsole("enfermagem", false));
				painel.setVisible(true);
				assinante.iniciar();
			}
		});
	}
}
