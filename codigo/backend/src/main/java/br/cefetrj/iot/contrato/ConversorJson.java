package br.cefetrj.iot.contrato;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Objects;

public final class ConversorJson {
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .serializeNulls()
            .create();

    private ConversorJson() {
    }

    public static String paraJson(Mensagem mensagem) {
        return GSON.toJson(Objects.requireNonNull(mensagem, "mensagem"));
    }

    public static <T extends Record & Mensagem> T deJson(String json, Class<T> tipo) {
        try {
            JsonElement raiz = GSON.fromJson(json, JsonElement.class);
            Campos.exigir(raiz != null && raiz.isJsonObject(), "O JSON deve conter um objeto.");
            JsonObject objeto = raiz.getAsJsonObject();
            for (RecordComponent campo : tipo.getRecordComponents()) {
                JsonElement valor = objeto.get(campo.getName());
                if (valor != null && !valor.isJsonNull())
                    validarTipo(campo, valor);
            }
            // O Gson chama o construtor do record. Ele verifica os campos obrigatÃ³rios e
            // as combinaÃ§Ãµes.
            return GSON.fromJson(objeto, tipo);
        } catch (RuntimeException e) {
            Throwable causa = e;
            while (causa.getCause() != null)
                causa = causa.getCause();
            throw new IllegalArgumentException("JSON de " + tipo.getSimpleName() + ": " + causa.getMessage(), e);
        }
    }

    private static void validarTipo(RecordComponent campo, JsonElement elemento) {
        String nome = campo.getName();
        Class<?> tipo = campo.getType();
        Campos.exigir(elemento.isJsonPrimitive(), nome + " deve conter um valor simples.");
        JsonPrimitive valor = elemento.getAsJsonPrimitive();
        if (tipo == Integer.class || tipo == Double.class) {
            Campos.exigir(valor.isNumber(), nome + " exige um número JSON.");
            if (tipo == Integer.class)
                valor.getAsBigDecimal().intValueExact();
        } else {
            Campos.exigir(valor.isString(), nome + " exige texto.");
            if (tipo.isEnum()) {
                boolean conhecido = Arrays.stream(tipo.getEnumConstants())
                        .anyMatch(item -> ((Enum<?>) item).name().equals(valor.getAsString()));
                Campos.exigir(conhecido, nome + " desconhecido: " + valor.getAsString());
            }
        }
    }

    public static <T extends Record & Mensagem> T lerNoTopico(String topico, String json, Class<T> tipo) {
        T mensagem = deJson(json, tipo);
        Campos.exigir(mensagem.topico().equals(topico), "Tópico incompatível com os campos do JSON.");
        return mensagem;
    }
}
