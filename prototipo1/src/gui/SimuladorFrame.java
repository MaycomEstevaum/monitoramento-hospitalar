package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import comum.Hospital;
import comum.Rotulos;
import comum.Topicos;
import publicadores.SimuladorHospital;

// janela de controle do simulador, serve pra provocar crises durante a demonstracao
public class SimuladorFrame extends JFrame {

	private static final String[] SINAIS = { Topicos.SINAL_FC, Topicos.SINAL_TEMP, Topicos.SINAL_SPO2,
			Topicos.SINAL_PA };

	private SimuladorHospital simulador;
	private JComboBox<String> comboLeito;
	private JComboBox<String> comboSinal;
	private JTextArea log = new JTextArea();
	private JLabel contador = new JLabel("mensagens publicadas: 0");

	public SimuladorFrame(SimuladorHospital simulador) {
		super("Simulador de Sensores");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.simulador = simulador;

		String[] leitos = new String[Hospital.LEITOS.length];
		for (int i = 0; i < leitos.length; i++) {
			leitos[i] = Rotulos.nomeLeito(Hospital.LEITOS[i]);
		}
		comboLeito = new JComboBox<String>(leitos);

		String[] sinais = new String[SINAIS.length];
		for (int i = 0; i < sinais.length; i++) {
			sinais[i] = Rotulos.nomeSinal(SINAIS[i]);
		}
		comboSinal = new JComboBox<String>(sinais);

		JButton botaoCrise = new JButton("Provocar crise");
		botaoCrise.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SimuladorFrame.this.simulador.provocarCrise(leitoEscolhido(), sinalEscolhido());
			}
		});

		JButton botaoNormalizar = new JButton("Normalizar");
		botaoNormalizar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SimuladorFrame.this.simulador.normalizar(leitoEscolhido(), sinalEscolhido());
			}
		});

		JPanel controles = new JPanel(new GridLayout(3, 2, 6, 6));
		controles.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
		controles.add(new JLabel("Leito:"));
		controles.add(comboLeito);
		controles.add(new JLabel("Sinal vital:"));
		controles.add(comboSinal);
		controles.add(botaoCrise);
		controles.add(botaoNormalizar);

		log.setEditable(false);
		JPanel painelLog = new JPanel(new BorderLayout());
		painelLog.setBorder(BorderFactory.createTitledBorder("Eventos do simulador"));
		painelLog.add(new JScrollPane(log), BorderLayout.CENTER);

		contador.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));

		add(controles, BorderLayout.NORTH);
		add(painelLog, BorderLayout.CENTER);
		add(contador, BorderLayout.SOUTH);

		// atualiza o contador a cada segundo
		new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				contador.setText("mensagens publicadas: " + SimuladorFrame.this.simulador.getTotalPublicadas());
			}
		}).start();

		setSize(380, 300);
		setLocation(780, 10);
	}

	private String leitoEscolhido() {
		return Hospital.LEITOS[comboLeito.getSelectedIndex()];
	}

	private String sinalEscolhido() {
		return SINAIS[comboSinal.getSelectedIndex()];
	}

	public void escrever(final String texto) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				log.append(texto + "\n");
				log.setCaretPosition(log.getDocument().getLength());
			}
		});
	}
}
