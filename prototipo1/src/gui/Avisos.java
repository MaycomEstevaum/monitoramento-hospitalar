package gui;

import java.awt.HeadlessException;

import javax.swing.JOptionPane;

import comum.ConfigMqtt;

public class Avisos {

	public static void brokerIndisponivel(Exception e) {
		System.err.println("Nao foi possivel conectar ao Mosquitto em " + ConfigMqtt.HOST + ":" + ConfigMqtt.PORTA);
		System.err.println("Verifique se o broker esta rodando e tente de novo. Detalhe: " + e);

		String texto = "N\u00e3o foi poss\u00edvel conectar ao Mosquitto em " + ConfigMqtt.HOST + ":" + ConfigMqtt.PORTA
				+ ".\nVerifique se o broker est\u00e1 rodando e tente de novo.";
		try {
			JOptionPane.showMessageDialog(null, texto, "Broker indispon\u00edvel", JOptionPane.ERROR_MESSAGE);
		} catch (HeadlessException h) {
			// sem tela so o console mostra o aviso
		}
	}
}
