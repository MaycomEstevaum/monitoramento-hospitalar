package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedList;

import javax.swing.JPanel;

// grafico de linha bem simples com os ultimos valores
public class GraficoSimples extends JPanel {

	private static final int MAXIMO_PONTOS = 40;

	private LinkedList<Double> pontos = new LinkedList<Double>();
	private double minimo;
	private double maximo;
	private Color cor = Color.GRAY;

	public GraficoSimples(double minimo, double maximo) {
		this.minimo = minimo;
		this.maximo = maximo;
		setPreferredSize(new Dimension(180, 80));
		setBackground(Color.WHITE);
	}

	public void adicionar(double valor, Color cor) {
		pontos.add(valor);
		if (pontos.size() > MAXIMO_PONTOS) {
			pontos.removeFirst();
		}
		this.cor = cor;
		repaint();
	}

	public void limpar() {
		pontos.clear();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (pontos.size() < 2) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int largura = getWidth() - 10;
		int altura = getHeight() - 10;
		int[] xs = new int[pontos.size()];
		int[] ys = new int[pontos.size()];

		int i = 0;
		for (double p : pontos) {
			double proporcao = (p - minimo) / (maximo - minimo);
			proporcao = Math.max(0, Math.min(1, proporcao));
			xs[i] = 5 + (int) (i * (double) largura / (MAXIMO_PONTOS - 1));
			ys[i] = 5 + (int) (altura - proporcao * altura);
			i++;
		}

		g2.setColor(cor);
		g2.setStroke(new BasicStroke(2f));
		g2.drawPolyline(xs, ys, xs.length);
	}
}
