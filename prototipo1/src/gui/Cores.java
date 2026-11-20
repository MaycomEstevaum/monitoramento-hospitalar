package gui;

import java.awt.Color;

import comum.Prioridade;

public class Cores {

	public static Color da(Prioridade p) {
		if (p == Prioridade.CRITICO) {
			return new Color(240, 140, 140);
		}
		if (p == Prioridade.ATENCAO) {
			return new Color(255, 236, 160);
		}
		return new Color(200, 235, 200);
	}

	// versao mais forte, usada na linha do grafico
	public static Color linha(Prioridade p) {
		if (p == Prioridade.CRITICO) {
			return new Color(190, 30, 30);
		}
		if (p == Prioridade.ATENCAO) {
			return new Color(200, 140, 0);
		}
		return new Color(30, 130, 60);
	}
}
