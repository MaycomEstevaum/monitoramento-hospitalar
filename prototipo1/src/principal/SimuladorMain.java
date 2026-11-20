package principal;

import java.util.Scanner;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import gui.Avisos;
import gui.SimuladorFrame;
import publicadores.SimuladorHospital;

// sobe os sensores simulados dos 4 leitos e a janela de controle
// tambem aceita comandos no console, por exemplo: crise leito02 frequencia_cardiaca
public class SimuladorMain {

	public static void main(String[] args) throws Exception {
		final SimuladorHospital simulador = new SimuladorHospital();
		final SimuladorFrame[] janela = new SimuladorFrame[1];

		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				janela[0] = new SimuladorFrame(simulador);
				janela[0].setVisible(true);
			}
		});

		simulador.setSaida(new Consumer<String>() {
			@Override
			public void accept(String texto) {
				System.out.println(texto);
				janela[0].escrever(texto);
			}
		});

		try {
			simulador.iniciar();
		} catch (Exception e) {
			Avisos.brokerIndisponivel(e);
			System.exit(1);
		}

		Thread console = new Thread(new Runnable() {
			@Override
			public void run() {
				Scanner entrada = new Scanner(System.in);
				while (entrada.hasNextLine()) {
					String[] partes = entrada.nextLine().trim().split("\\s+");
					if (partes.length == 3 && partes[0].equals("crise")) {
						simulador.provocarCrise(partes[1], partes[2]);
					} else if (partes.length == 3 && partes[0].equals("normalizar")) {
						simulador.normalizar(partes[1], partes[2]);
					}
				}
			}
		});
		console.setDaemon(true);
		console.start();
	}
}
