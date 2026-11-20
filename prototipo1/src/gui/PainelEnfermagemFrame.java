package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import assinantes.IObservadorMensagens;
import comum.Alerta;
import comum.Leitura;
import comum.Prioridade;
import comum.Rotulos;
import comum.Topicos;

// painel geral da enfermagem, uma linha por leito com os 4 sinais
public class PainelEnfermagemFrame extends JFrame implements IObservadorMensagens {

	private static final String[] SINAIS = { Topicos.SINAL_FC, Topicos.SINAL_TEMP, Topicos.SINAL_SPO2,
			Topicos.SINAL_PA };
	private static final int PRIMEIRA_COLUNA_SINAL = 2;
	private static final int COLUNA_SITUACAO = 6;

	private DefaultTableModel modelo;
	private JTextArea areaAlertas;

	public PainelEnfermagemFrame() {
		super("Painel de Enfermagem");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		String[] colunas = { "Leito", "Paciente", Rotulos.nomeSinal(SINAIS[0]), Rotulos.nomeSinal(SINAIS[1]),
				Rotulos.nomeSinal(SINAIS[2]), Rotulos.nomeSinal(SINAIS[3]), "Situa\u00e7\u00e3o" };

		modelo = new DefaultTableModel(colunas, 0) {
			@Override
			public boolean isCellEditable(int linha, int coluna) {
				return false;
			}
		};

		JTable tabela = new JTable(modelo);
		tabela.setRowHeight(30);
		tabela.setFont(new Font("SansSerif", Font.PLAIN, 14));
		tabela.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
		tabela.setDefaultRenderer(Object.class, new RenderizadorCelula());

		// a coluna da situacao precisa de mais espaco pro nome da condicao
		int[] larguras = { 65, 105, 95, 90, 90, 100, 215 };
		for (int i = 0; i < larguras.length; i++) {
			tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
		}

		areaAlertas = new JTextArea(6, 40);
		areaAlertas.setEditable(false);

		JPanel painelAlertas = new JPanel(new BorderLayout());
		painelAlertas.setBorder(BorderFactory.createTitledBorder("Alertas recentes"));
		painelAlertas.add(new JScrollPane(areaAlertas), BorderLayout.CENTER);

		add(new JScrollPane(tabela), BorderLayout.CENTER);
		add(painelAlertas, BorderLayout.SOUTH);

		setSize(760, 300);
		setLocation(10, 10);
	}

	@Override
	public void aoReceberLeitura(final Leitura leitura) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				atualizar(leitura);
			}
		});
	}

	@Override
	public void aoReceberAlerta(final Alerta alerta) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mostrarAlerta(alerta);
			}
		});
	}

	private void atualizar(Leitura leitura) {
		int linha = linhaDoLeito(leitura);
		int coluna = PRIMEIRA_COLUNA_SINAL + indiceDoSinal(leitura.getSinal());

		modelo.setValueAt(leitura, linha, coluna);
		modelo.setValueAt(piorLeitura(linha), linha, COLUNA_SITUACAO);
	}

	private int linhaDoLeito(Leitura leitura) {
		String nome = Rotulos.nomeLeito(leitura.getLeito());

		for (int i = 0; i < modelo.getRowCount(); i++) {
			if (modelo.getValueAt(i, 0).equals(nome)) {
				return i;
			}
		}

		// leito novo entra na posicao certa pra manter a ordem
		int posicao = 0;
		while (posicao < modelo.getRowCount() && modelo.getValueAt(posicao, 0).toString().compareTo(nome) < 0) {
			posicao++;
		}
		modelo.insertRow(posicao, new Object[] { nome, leitura.getPaciente(), "-", "-", "-", "-", "-" });
		return posicao;
	}

	private int indiceDoSinal(String sinal) {
		for (int i = 0; i < SINAIS.length; i++) {
			if (SINAIS[i].equals(sinal)) {
				return i;
			}
		}
		return 0;
	}

	// a situacao do leito e a pior prioridade entre os sinais
	private Leitura piorLeitura(int linha) {
		Leitura pior = null;
		for (int c = PRIMEIRA_COLUNA_SINAL; c < COLUNA_SITUACAO; c++) {
			Object valor = modelo.getValueAt(linha, c);
			if (valor instanceof Leitura) {
				Leitura l = (Leitura) valor;
				if (pior == null || l.getPrioridade().maiorQue(pior.getPrioridade())) {
					pior = l;
				}
			}
		}
		return pior;
	}

	private void mostrarAlerta(Alerta a) {
		String texto = a.getHora() + "  " + Rotulos.nomeLeito(a.getLeito()) + " (" + a.getPaciente() + "): ";
		if (a.isCritico()) {
			texto += "CR\u00cdTICO, " + a.getCondicao() + " (" + a.getValorTexto() + " " + a.getUnidade() + ")";
		} else {
			texto += "estabilizou, " + Rotulos.nomeSinal(a.getSinal()) + " (" + a.getValorTexto() + " "
					+ a.getUnidade() + ")";
		}
		areaAlertas.append(texto + "\n");
		areaAlertas.setCaretPosition(areaAlertas.getDocument().getLength());
	}

	private class RenderizadorCelula extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
				boolean foco, int linha, int coluna) {
			super.getTableCellRendererComponent(tabela, valor, false, false, linha, coluna);
			setHorizontalAlignment(coluna < PRIMEIRA_COLUNA_SINAL ? LEFT : CENTER);
			setBackground(Color.WHITE);

			if (valor instanceof Leitura) {
				Leitura l = (Leitura) valor;
				if (coluna == COLUNA_SITUACAO) {
					if (l.getPrioridade() == Prioridade.ROTINA) {
						setText(Rotulos.nomePrioridade(l.getPrioridade()));
					} else {
						setText(Rotulos.nomePrioridade(l.getPrioridade()) + ": " + l.getCondicao());
					}
				} else {
					setText(l.getValorTexto() + " " + l.getUnidade());
				}
				setBackground(Cores.da(l.getPrioridade()));
			}
			return this;
		}
	}
}
