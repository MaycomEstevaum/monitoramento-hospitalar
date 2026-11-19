package br.cefetrj.iot.demo;

import java.time.OffsetDateTime;
import java.util.UUID;
import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.Topicos;
import br.cefetrj.iot.mqtt.ClienteMqtt;
import br.cefetrj.iot.mqtt.ConfiguracaoMqtt;

public final class ConferirContratoMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !("ouvir".equals(args[0]) || "publicar".equals(args[0])))
            throw new IllegalArgumentException("Use ouvir ou publicar.");

        try (ClienteMqtt cliente = new ClienteMqtt(ConfiguracaoMqtt.padrao("contrato-" + args[0]),
                (topico, json) -> {
                    Leitura l = ConversorJson.lerNoTopico(topico, json, Leitura.class);
                    System.out.println("Recebida: " + l.pacienteId() + " | " + topico + " | " + json);
                })) {
            cliente.conectar();
            if ("ouvir".equals(args[0])) {
                cliente.assinar(Topicos.LEITURAS_BRUTAS, 1);
                System.out.println("Assinante pronto. Pressione Enter para encerrar.");
                System.in.read();
            } else {
                Leitura l = Leitura.pressao(UUID.randomUUID().toString(), "PAC-101",
                        "uti", "leito-01", 185, 110, OffsetDateTime.now().toString());
                cliente.publicar(l.topico(), ConversorJson.paraJson(l), 1, false);
                System.out.println("Publicada em " + l.topico());
            }
        }
    }
}
