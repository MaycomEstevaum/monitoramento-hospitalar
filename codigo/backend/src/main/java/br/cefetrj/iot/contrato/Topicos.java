package br.cefetrj.iot.contrato;

import java.text.Normalizer;
import java.util.Locale;

public final class Topicos {
    public static final String LEITURAS_BRUTAS = "sensores/#";
    public static final String TODAS_CLASSIFICADAS = "hospital/#";
    public static final String CRITICOS = "hospital/+/+/+/critico";
    public static final String COMANDOS = "comandos/alertas/+/+";
    public static final String ESTADOS = "alertas/+/+/+/estado";

    private Topicos() {
    }

    static String segmento(String texto) {
        Campos.texto(texto, "Segmento");
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String resultado = semAcentos.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        Campos.exigir(resultado.matches("[a-z0-9]+(-[a-z0-9]+)*"),
                "Segmento de tópico inválido: " + texto);
        return resultado;
    }

    static String id(String original) {
        Campos.texto(original, "ID do tópico");
        Campos.exigir(original.matches("[a-z0-9]+(-[a-z0-9]+)*"),
                "Use o ID original em minúsculas, com letras, números ou hífens: " + original);
        return original;
    }

    public static String leituraBruta(String ala, String leito, TipoSinal sinal) {
        return "sensores/" + segmento(ala) + "/" + segmento(leito) + "/" + sinal.getSegmentoTopico();
    }

    public static String leituraClassificada(String ala, String leito, TipoSinal sinal, Prioridade prioridade) {
        return "hospital/" + segmento(ala) + "/" + segmento(leito) + "/"
                + sinal.getSegmentoTopico() + "/" + prioridade.getSegmentoTopico();
    }

    public static String comandoAlerta(String idAlerta, ComandoAlerta.Acao acao) {
        return "comandos/alertas/" + id(idAlerta) + "/" + acao.name().toLowerCase(Locale.ROOT);
    }

    public static String estadoAlerta(String ala, String leito, String idAlerta) {
        return "alertas/" + segmento(ala) + "/" + segmento(leito) + "/" + id(idAlerta) + "/estado";
    }

    public static String respostaComando(String idAlerta, String idComando) {
        return "respostas/alertas/" + id(idAlerta) + "/" + id(idComando);
    }

    public static String leiturasDoLeito(String ala, String leito) {
        return "hospital/" + segmento(ala) + "/" + segmento(leito) + "/#";
    }

    public static String estadosDoLeito(String ala, String leito) {
        return "alertas/" + segmento(ala) + "/" + segmento(leito) + "/+/estado";
    }
}