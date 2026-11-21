# Contrato MQTT do monitoramento hospitalar

O simulador, o classificador, o notificador e as telas trocam JSON em UTF-8 pelo Mosquitto. Cada conexão tem um `clientId` próprio. O paciente, o profissional e os valores dos exemplos são fictícios.

## Tópicos oficiais

| Tópico | Publica | Recebe | QoS publicado | Retain |
| --- | --- | --- | --- | --- |
| `sensores/{ala}/{leito}/{sinal}` | Simulador | Classificador | 1 | false |
| `hospital/{ala}/{leito}/{sinal}/{prioridade}` | Classificador | Telas interessadas e, nos críticos, notificador | 0 rotina, 1 atenção, 2 crítico | false |
| `comandos/alertas/{idAlerta}/{acao}` | Tela | Notificador | 1 | false |
| `alertas/{ala}/{leito}/{idAlerta}/estado` | Notificador | Telas interessadas e Simulador (para iniciar recuperação gradual em `RECONHECIDO`) | 1 | true |
| `respostas/alertas/{idAlerta}/{idComando}` | Notificador | Tela que aguarda aquele comando | 1 | false |

`{...}` é um lugar a preencher. No Java, `Topicos` monta endereços e fornece filtros. No navegador, concentre a montagem em JavaScript compartilhado segundo esta mesma tabela. As páginas usam MQTT.js.

- Escreva segmentos em minúsculas, sem acentos nem espaços. Para ala e leito, a implementação transforma `Clínica Médica` em `clinica-medica` e `Leito 01` em `leito-01`. IDs que entram no caminho já nascem canônicos, por exemplo `alt-001` ou um UUID minúsculo. O mesmo ID aparece no JSON. Maiúsculas como `PAC-101` são permitidas em `pacienteId`, que não entra no caminho.
- `+` seleciona um nível e `#` seleciona zero ou mais níveis ao final de um filtro de assinatura. Uma publicação tem endereço concreto. Use `sensores/#` para o classificador, `hospital/+/+/+/critico` com QoS 2 e `comandos/alertas/+/+` com QoS 1 para o notificador. As telas interessadas no estado assinam o filtro correspondente, por exemplo `alertas/+/+/+/estado`.
- QoS determina a entrega entre clientes e broker. QoS 0 permite perda, QoS 1 permite duplicata e QoS 2 usa confirmação própria do protocolo. A entrega ao assinante considera o menor valor entre o publicado e o máximo concedido na assinatura. A urgência é o campo `prioridade`.
- Só o estado usa retenção. Quem entra depois recebe o último retrato de cada alerta correspondente. Na Fundação, o Mosquitto está com `persistence false`, então reiniciar o processo perde o conteúdo retido.
- O Java usa TCP em `tcp://localhost:1883` quando roda no computador do broker. As telas no mesmo computador usam `ws://localhost:9001`. Outros dispositivos usam o endereço do computador em que o broker está rodando.
- Datas usam ISO 8601 com deslocamento, como `2026-11-19T14:30:05-03:00` ou `2026-11-19T17:30:05Z`. Números são números JSON. Os enums no JSON usam os nomes Java em maiúsculas. O produtor emite campos sem uso como `null`. Campos opcionais também podem ser omitidos na leitura. Campos obrigatórios ausentes ou nulos, tipo incorreto, enum desconhecido, sintaxe inválida ou tópico incompatível invalidam a mensagem. O receptor registra a ocorrência e segue atendendo.
- O esquema do núcleo usa `versao` no estado e na resposta. `idComando` é obrigatório para associar o pedido à resposta. `versaoEsperada`, idempotência entre reenvios e `schemaVersion` ficam entre os reforços do planejamento.

## Sinais e classificação

| Sinal no JSON | Segmento | Unidade | Campos numéricos |
| --- | --- | --- | --- |
| `FREQUENCIA` | `frequencia` | `bpm` | `valor` |
| `TEMPERATURA` | `temperatura` | `°C` | `valor` |
| `SATURACAO` | `saturacao` | `%` | `valor` |
| `PRESSAO` | `pressao` | `mmHg` | `sistolica` e `diastolica` no mesmo objeto, com `valor` nulo |

Toda leitura inclui `idLeitura`, `pacienteId`, `ala`, `leito`, `tipoSinal`, `unidade` e `instanteColeta`. O classificador conserva esses campos e as medidas e acrescenta `prioridade` e `motivo`. Numa leitura bruta, ambos ficam nulos. Os tópicos e JSON a seguir são exemplos do mesmo caso.

### Leitura bruta

Tópico `sensores/uti/leito-01/pressao`.

```json
{
  "idLeitura": "leit-001",
  "pacienteId": "PAC-101",
  "ala": "uti",
  "leito": "leito-01",
  "tipoSinal": "PRESSAO",
  "unidade": "mmHg",
  "instanteColeta": "2026-11-19T14:30:05-03:00",
  "valor": null,
  "sistolica": 185,
  "diastolica": 110,
  "prioridade": null,
  "motivo": null
}
```

### Leitura classificada

Tópico `hospital/uti/leito-01/pressao/critico`.

```json
{
  "idLeitura": "leit-001",
  "pacienteId": "PAC-101",
  "ala": "uti",
  "leito": "leito-01",
  "tipoSinal": "PRESSAO",
  "unidade": "mmHg",
  "instanteColeta": "2026-11-19T14:30:05-03:00",
  "valor": null,
  "sistolica": 185,
  "diastolica": 110,
  "prioridade": "CRITICO",
  "motivo": "Sistólica na faixa crítica"
}
```

### Estado retido do alerta

Tópico `alertas/uti/leito-01/alt-001/estado`.

```json
{
  "idAlerta": "alt-001",
  "idLeituraOrigem": "leit-001",
  "pacienteId": "PAC-101",
  "ala": "uti",
  "leito": "leito-01",
  "tipoSinal": "PRESSAO",
  "unidadeOrigem": "mmHg",
  "instanteLeituraOrigem": "2026-11-19T14:30:05-03:00",
  "valorOrigem": null,
  "sistolicaOrigem": 185,
  "diastolicaOrigem": 110,
  "motivoOrigem": "Sistólica na faixa crítica",
  "estado": "ABERTO",
  "versao": 1,
  "abertoEm": "2026-11-19T14:30:06-03:00",
  "reconhecidoPor": null,
  "reconhecidoEm": null,
  "resolvidoPor": null,
  "resolvidoEm": null,
  "motivoResolucao": null
}
```

### Comando de reconhecimento

Tópico `comandos/alertas/alt-001/reconhecer`.

```json
{
  "idComando": "cmd-01",
  "idAlerta": "alt-001",
  "acao": "RECONHECER",
  "usuarioId": "enf-01",
  "instanteComando": "2026-11-19T14:31:00-03:00",
  "motivoResolucao": null
}
```

### Resposta ao reconhecimento

Tópico `respostas/alertas/alt-001/cmd-01`.

```json
{
  "idComando": "cmd-01",
  "idAlerta": "alt-001",
  "resultado": "APLICADO",
  "motivo": null,
  "estado": "RECONHECIDO",
  "versao": 2
}
```

### Comando de resolução, depois de receber RECONHECIDO

Tópico `comandos/alertas/alt-001/resolver`.

```json
{
  "idComando": "cmd-02",
  "idAlerta": "alt-001",
  "acao": "RESOLVER",
  "usuarioId": "med-01",
  "instanteComando": "2026-11-19T14:35:00-03:00",
  "motivoResolucao": "Pressão estabilizada após atendimento; pico de 185/110 mmHg às 14:30:05"
}
```

### Resposta à resolução

Tópico `respostas/alertas/alt-001/cmd-02`.

```json
{
  "idComando": "cmd-02",
  "idAlerta": "alt-001",
  "resultado": "APLICADO",
  "motivo": null,
  "estado": "RESOLVIDO",
  "versao": 3
}
```

## Ciclo previsto para as próximas etapas

| Situação atual | Reconhecer | Resolver |
| --- | --- | --- |
| `ABERTO` | Passa para `RECONHECIDO` | Rejeita |
| `RECONHECIDO` | Rejeita | Passa para `RESOLVIDO`, com motivo |
| `RESOLVIDO` | Rejeita | Rejeita |

O notificador abre o alerta em versão 1 e registra a hora de abertura. Em cada transição aceita, registra o profissional e a hora do notificador, aumenta a versão e publica o estado completo com retenção. A resposta vai ao tópico específico sem retenção. Um comando rejeitado mantém a versão. Quando o alerta existe, a rejeição informa o estado e a versão atuais. Para `ALERTA_INEXISTENTE`, ambos ficam nulos. Um exemplo de outro motivo é `TRANSICAO_INVALIDA`. A tela assina estado e resposta e aguarda o broker confirmar as assinaturas (SUBACK) antes de publicar o comando. A tela acompanha o estado divulgado pelo notificador.

O `EventoAlerta` é interno ao Java. Ele transporta o estado anterior (nulo na abertura), o retrato atual e o instante do evento aos observadores locais. O State que autoriza transições e o Observer que avisa os observadores serão implementados no notificador.

## Controle da área de demonstração do simulador (Etapa 6)

Para acionar crises e recuperação pelo navegador sem acoplamento direto entre interface e programa Java, o simulador assina comandos e publica o modo corrente de cada leito pelo broker.

| Tópico | Publica | Recebe | QoS publicado | Retain |
| --- | --- | --- | --- | --- |
| `comandos/simulador/{ala}/{leito}/{sinal}` | Tela | Simulador | 1 | false |
| `simulador/{ala}/{leito}/estado` | Simulador | Telas com área de demonstração | 1 | true |

No tópico de comando, `{sinal}` aceita `frequencia`, `temperatura`, `saturacao`, `pressao` ou `todos` (quando `tipoSinal` no JSON é `null`). O campo `cenario` aceita `ROTINA` ou `CRISE`. Além disso, o simulador assina `alertas/+/+/+/estado` (QoS 1) para iniciar uma recuperação gradual de 15 segundos (`CRITICO` → `ATENCAO` → `ROTINA`) quando um alerta de uma crise ativa passa para `RECONHECIDO`.

### Comando para o simulador (`ComandoSimulador`)

Tópico `comandos/simulador/uti/leito-01/frequencia`.

```json
{
  "idComando": "sim-01",
  "ala": "uti",
  "leito": "leito-01",
  "tipoSinal": "FREQUENCIA",
  "cenario": "CRISE",
  "instanteComando": "2026-11-20T19:40:00-03:00"
}
```

### Estado retido do simulador (`EstadoSimulador`)

Tópico `simulador/uti/leito-01/estado`.

```json
{
  "ala": "uti",
  "leito": "leito-01",
  "pacienteId": "PAC-101",
  "frequencia": "CRISE",
  "temperatura": "ROTINA",
  "saturacao": "ROTINA",
  "pressao": "ROTINA",
  "atualizadoEm": "2026-11-20T19:40:00-03:00"
}
```