package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import assinantes.AssinanteMqtt;
import assinantes.IObservadorMensagens;
import comum.Alerta;
import comum.Hospital;
import comum.Leitura;
import comum.Rotulos;
import comum.Topicos;

// painel medico, acompanha um paciente por vez
// ao trocar de leito os filtros de assinatura mudam e o broker so manda o leito escolhido
public class PainelMedicoFrame extends JFrame implements IObservadorMensagens {

	private AssinanteMqtt assinante;
	private String leitoAtual;

	private JComboBox<String> comboLeitos;
	private JLabel rotuloPaciente = new JLabel("Aguardando dados...");
	private JLabel rotuloAlerta = new JLabel(" ");
	private Map<String, CartaoSinal> cartoes = new LinkedHashMap<String, CartaoSinal>();

	// sinais que estao criticos agora neste leito
	private Map<String, String> criticos = new LinkedHashMap<String, String>();

	public PainelMedicoFrame(AssinanteMqtt assinante) throws Exception {
		super("Painel M\u00e9dico");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.assinante = assinante;

		// comeca acompanhando o primeiro leito
		leitoAtual = Hospital.LEITOS[0];
		assinante.assinar(Topicos.leiturasDoLeito(leitoAtual), Topicos.alertasDoLeito(leitoAtual));

		String[] nomes = new String[Hospital.LEITOS.length];
		for (int i = 0; i < nomes.length; i++) {
			nomes[i] = Rotulos.nomeLeito(Hospital.LEITOS[i]);
		}
		comboLeitos = new JComboBox<String>(nomes);
		comboLeitos.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trocarLeito(Hospital.LEITOS[comboLeitos.getSelectedIndex()]);
			}
		});

		rotuloPaciente.setFont(new Font("SansSerif", Font.BOLD, 15));
		rotuloAlerta.setFont(new Font("SansSerif", Font.BOLD, 15));
		rotuloAlerta.setForeground(new Color(190, 30, 30));

		JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
		topo.add(new JLabel("Acompanhar:"));
		topo.add(comboLeitos);
		topo.add(rotuloPaciente);
		topo.add(rotuloAlerta);

		cartoes.put(Topicos.SINAL_FC, new CartaoSinal(Topicos.SINAL_FC, 40, 180));
		cartoes.put(Topicos.SINAL_TEMP, new CartaoSinal(Topicos.SINAL_TEMP, 35, 41));
		cartoes.put(Topicos.SINAL_SPO2, new CartaoSinal(Topicos.SINAL_SPO2, 80, 100));
		cartoes.put(Topicos.SINAL_PA, new CartaoSinal(Topicos.SINAL_PA, 70, 200));

		JPanel grade = new JPanel(new GridLayout(2, 2, 8, 8));
		grade.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
		for (CartaoSinal c : cartoes.values()) {
			grade.add(c);
		}

		add(topo, BorderLayout.NORTH);
		add(grade, BorderLayout.CENTER);

		setSize(720, 390);
		setLocation(10, 320);
	}

	private void trocarLeito(String novo) {
		if (novo.equals(leitoAtual)) {
			return;
		}
		try {
			assinante.cancelar(Topicos.leiturasDoLeito(leitoAtual), Topicos.alertasDoLeito(leitoAtual));
			assinante.assinar(Topicos.leiturasDoLeito(novo), Topicos.alertasDoLeito(novo));
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "N\u00e3o foi poss\u00edvel trocar de leito: " + e.getMessage());
			return;
		}

		leitoAtual = novo;
		rotuloPaciente.setText("Aguardando dados...");
		criticos.clear();
		atualizarRotuloAlerta();
		for (CartaoSinal c : cartoes.values()) {
			c.limpar();
		}
	}

	@Override
	public void aoReceberLeitura(final Leitura leitura) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// mensagem de um leito antigo que ainda estava a caminho
				if (!leitura.getLeito().equals(leitoAtual)) {
					return;
				}
				rotuloPaciente.setText(Rotulos.nomeLeito(leitura.getLeito()) + ": " + leitura.getPaciente());
				CartaoSinal cartao = cartoes.get(leitura.getSinal());
				if (cartao != null) {
					cartao.atualizar(leitura);
				}
			}
		});
	}

	@Override
	public void aoReceberAlerta(final Alerta alerta) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!alerta.getLeito().equals(leitoAtual)) {
					return;
				}
				if (alerta.isCritico()) {
					criticos.put(alerta.getSinal(), alerta.getCondicao());
				} else {
					criticos.remove(alerta.getSinal());
				}
				atualizarRotuloAlerta();
			}
		});
	}

	private void atualizarRotuloAlerta() {
		if (criticos.isEmpty()) {
			rotuloAlerta.setText(" ");
			return;
		}
		StringBuilder sb = new StringBuilder("CR\u00cdTICO: ");
		boolean primeiro = true;
		for (String condicao : criticos.values()) {
			if (!primeiro) {
				sb.append(", ");
			}
			sb.append(condicao);
			primeiro = false;
		}
		rotuloAlerta.setText(sb.toString());
	}
}
