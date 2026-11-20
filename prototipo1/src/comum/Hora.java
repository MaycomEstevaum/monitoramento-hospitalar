package comum;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Hora {

	private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static String agora() {
		return LocalTime.now().format(FORMATO);
	}
}
