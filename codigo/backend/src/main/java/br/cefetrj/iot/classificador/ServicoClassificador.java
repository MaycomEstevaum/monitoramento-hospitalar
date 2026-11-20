package br.cefetrj.iot.classificador;

import br.cefetrj.iot.contrato.ConversorJson;
import br.cefetrj.iot.contrato.Leitura;
import br.cefetrj.iot.contrato.TipoSinal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ServicoClassificador {
    public static final int CAPACIDADE_CACHE_PADRAO = 1024;

    private final EnumMap<TipoSinal, RegraClassificacao> regras = new EnumMap<>(TipoSinal.class);
    private final PublicadorLeituraClassificada publicador;
    private final Map<String, Boolean> idsPublicados;

    public ServicoClassificador(PublicadorLeituraClassificada publicador) {
        this(publicador, CAPACIDADE_CACHE_PADRAO);
    }

    public ServicoClassificador(PublicadorLeituraClassificada publicador, int capacidadeCacheDeduplicacao) {
        this.publicador = Objects.requireNonNull(publicador, "Informe o publicador.");
        if (capacidadeCacheDeduplicacao < 1) {
            throw new IllegalArgumentException("A capacidade do cache deve ser positiva.");
        }
        regras.put(TipoSinal.FREQUENCIA, new RegraFrequencia());
        regras.put(TipoSinal.TEMPERATURA, new RegraTemperatura());
        regras.put(TipoSinal.SATURACAO, new RegraSaturacao());
        regras.put(TipoSinal.PRESSAO, new RegraPressao());

        idsPublicados = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> maisAntiga) {
                return size() > capacidadeCacheDeduplicacao;
            }
        };
    }

    public Leitura classificar(Leitura leituraBruta) {
        Objects.requireNonNull(leituraBruta, "Informe uma leitura.");
        if (leituraBruta.prioridade() != null) {
            throw new IllegalArgumentException("O classificador aceita apenas leituras brutas.");
        }
        RegraClassificacao regra = regras.get(leituraBruta.tipoSinal());
        if (regra == null) {
            throw new IllegalArgumentException("Sinal sem estratégia: " + leituraBruta.tipoSinal());
        }
        ResultadoClassificacao resultado = regra.classificar(leituraBruta);
        return leituraBruta.classificada(resultado.prioridade(), resultado.motivo());
    }

    public synchronized void processar(String topico, String json) {
        final Leitura saida;
        try {
            Leitura entrada = ConversorJson.lerNoTopico(topico, json, Leitura.class);
            saida = classificar(entrada);
        } catch (IllegalArgumentException e) {
            System.err.println("[Classificador] Entrada inválida em " + topico + ": " + e.getMessage());
            return;
        }

        String id = saida.idLeitura();
        if (idsPublicados.get(id) != null) {
            // Em ordem de acesso, get atualiza a posição do ID no cache.
            return;
        }
        try {
            publicador.publicar(saida.topico(), ConversorJson.paraJson(saida),
                    saida.prioridade().getQos(), false);
        } catch (Exception e) {
            System.err.println("[Classificador] Falha no envio de " + id + ": " + e.getMessage());
            return;
        }
        idsPublicados.put(id, Boolean.TRUE);
    }
}
