package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import assinantes.IObservadorMensagens;
import comum.Alerta;
import comum.Leitura;
import comum.Rotulos;

// simula o celular da equipe, mostra so os alertas criticos e quando o paciente estabiliza
public class NotificadorMovelFrame extends JFrame implements IObservadorMensagens {

	private static final int MAXIMO_CARTOES = 30;

	private JPanel lista = new JPanel();
	private JLabel contador = new JLabel("0 notifica\u00e7\u00f5es");
	private int total = 0;

	public NotificadorMovelFrame() {
		super("Celular da equipe");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));

		// o painel de cima mantem os cartoes encostados no topo
		JPanel topoDaLista = new JPanel(new BorderLayout());
		topoDaLista.add(lista, BorderLayout.NORTH);

		contador.setFont(new Font("SansSerif", Font.BOLD, 13));
		contador.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

		add(contador, BorderLayout.NORTH);
		add(new JScrollPane(topoDaLista), BorderLayout.CENTER);

		setSize(330, 390);
		setLocation(740, 320);
	}

	@Override
	public void aoReceberLeitura(Leitura leitura) {
		// este assinante nao assina leituras, so alertas
	}

	@Override
	public void aoReceberAlerta(final Alerta alerta) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				adicionarCartao(alerta);
			}
		});
	}

	private void adicionarCartao(Alerta a) {
		JPanel cartao = new JPanel(new GridLayout(3, 1));
		cartao.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
				BorderFactory.createEmptyBorder(6, 10, 6, 10)));
		cartao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
		cartao.setAlignmentX(LEFT_ALIGNMENT);

		JLabel titulo = new JLabel();
		titulo.setFont(new Font("SansSerif", Font.BOLD, 14));

		if (a.isCritico()) {
			cartao.setBackground(new Color(240, 140, 140));
			titulo.setText("ALERTA CR\u00cdTICO, " + a.getCondicao());
			Toolkit.getDefaultToolkit().beep();
		} else {
			cartao.setBackground(new Color(200, 235, 200));
			titulo.setText("Paciente estabilizou");
		}

		cartao.add(titulo);
		cartao.add(new JLabel(Rotulos.nomeLeito(a.getLeito()) + ": " + a.getPaciente()));
		cartao.add(new JLabel(Rotulos.nomeSinal(a.getSinal()) + " " + a.getValorTexto() + " " + a.getUnidade()
				+ "   " + a.getHora()));

		// o mais novo fica em cima
		lista.add(cartao, 0);
		if (lista.getComponentCount() > MAXIMO_CARTOES) {
			lista.remove(MAXIMO_CARTOES);
		}
		lista.revalidate();
		lista.repaint();

		total++;
		contador.setText(total + " notifica\u00e7\u00f5es");
	}
}
