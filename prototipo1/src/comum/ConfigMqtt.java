package comum;

// endereco do broker, o padrao e o Mosquitto local na porta 1883
// da pra mudar passando as propriedades mqtt.host e mqtt.porta ao rodar
public class ConfigMqtt {

	public static final String HOST = System.getProperty("mqtt.host", "localhost");

	public static final int PORTA = Integer.parseInt(System.getProperty("mqtt.porta", "1883"));
}
