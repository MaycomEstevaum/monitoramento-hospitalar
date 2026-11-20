package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import comum.Leitura;
import comum.Prioridade;
import comum.Rotulos;

// cartao de um sinal vital no painel medico, com o valor atual e o grafico
public class CartaoSinal extends JPanel {

	private JLabel valor = new JLabel("-", SwingConstants.CENTER);
	private JLabel estado = new JLabel(" ", SwingConstants.CENTER);
	private GraficoSimples grafico;

	public CartaoSinal(String sinal, double minimo, double maximo) {
		setLayout(new BorderLayout(6, 6));
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(6, 6, 6, 6)));
		setBackground(Color.WHITE);

		JLabel titulo = new JLabel(Rotulos.nomeSinal(sinal), SwingConstants.CENTER);
		titulo.setFont(new Font("SansSerif", Font.BOLD, 13));
		valor.setFont(new Font("SansSerif", Font.BOLD, 24));
		estado.setFont(new Font("SansSerif", Font.PLAIN, 12));

		JPanel texto = new JPanel(new GridLayout(3, 1));
		texto.setOpaque(false);
		texto.setPreferredSize(new Dimension(150, 90));
		texto.add(titulo);
		texto.add(valor);
		texto.add(estado);

		grafico = new GraficoSimples(minimo, maximo);

		add(texto, BorderLayout.WEST);
		add(grafico, BorderLayout.CENTER);
	}

	public void atualizar(Leitura leitura) {
		valor.setText(leitura.getValorTexto() + " " + leitura.getUnidade());

		String texto = Rotulos.nomePrioridade(leitura.getPrioridade());
		if (leitura.getPrioridade() != Prioridade.ROTINA) {
			texto += ": " + leitura.getCondicao();
		}
		estado.setText(texto);

		setBackground(Cores.da(leitura.getPrioridade()));
		grafico.adicionar(leitura.getValor(), Cores.linha(leitura.getPrioridade()));
	}

	public void limpar() {
		valor.setText("-");
		estado.setText(" ");
		setBackground(Color.WHITE);
		grafico.limpar();
	}
}
