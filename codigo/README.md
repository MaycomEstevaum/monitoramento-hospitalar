# Monitoramento hospitalar com IoT

Projeto acadêmico em Java que simula frequência cardíaca, temperatura, saturação e pressão arterial em cinco leitos fictícios. Até a etapa 4, o simulador publica as leituras brutas e o classificador acrescenta uma prioridade (`ROTINA`, `ATENCAO` ou `CRITICO`) e o motivo da classificação. A leitura de saída conserva o ID, o horário da coleta, o paciente e as medidas da entrada.

O Mosquitto encaminha as mensagens aos assinantes de cada tópico (**Publish-Subscribe**). Cada publicador cria seu sensor com **Factory Method**. O classificador usa **Strategy** para delegar a avaliação à regra do sinal recebido.

[Executar a etapa 4](#executar-o-simulador-e-o-classificador-etapa-4) · [Resultados do leito 01](#resultados-confirmados-no-leito-01) · [Testes automatizados](#testes-automatizados) · [Ambiente utilizado](#ambiente-utilizado)

## Executar o simulador e o classificador (etapa 4)

Use um JDK que compile para Java 17 e tenha o Maven disponível no PowerShell (`mvn -version`). Abra o Docker Desktop e três terminais PowerShell na raiz `monitoramento-hospitalar`, a pasta que contém `codigo`. Comece com o simulador parado para observar uma única coleta antes do fluxo contínuo.

### Terminal 1 — iniciar o broker e acompanhar o leito 01

```powershell
Set-Location .\codigo
docker compose up -d
docker compose ps
docker compose exec mosquitto mosquitto_sub -q 2 -t 'sensores/uti/leito-01/frequencia' -t 'hospital/uti/leito-01/frequencia/+' -F 'qos=%q %t %p'
```

Confira se o serviço `mosquitto` está em execução. O último comando mantém o terminal aberto, aguardando a frequência cardíaca do leito 01 antes e depois da classificação. O `+` aceita qualquer prioridade no último nível do tópico. A assinatura solicita QoS 2 para permitir observar os três níveis de entrega. Cada linha mostra o QoS recebido, o tópico e o JSON.

### Terminal 2 — iniciar o classificador

```powershell
Set-Location .\codigo\backend
mvn compile exec:java "-Dexec.mainClass=br.cefetrj.iot.classificador.ClassificadorMain"
```

Aguarde `Classificador pronto` e deixe o programa em execução. Ele assinou `sensores/#` com QoS 1 e publicará em `hospital/{ala}/{leito}/{sinal}/{prioridade}`. As duas publicações do leito observado aparecem no terminal 1.

### Terminal 3 — enviar uma leitura de 110 bpm e repetir o mesmo ID

```powershell
Set-Location .\codigo
$topico = 'sensores/uti/leito-01/frequencia'
$leitura = [ordered]@{
    idLeitura      = [guid]::NewGuid().ToString()
    pacienteId     = 'PAC-101'
    ala            = 'uti'
    leito          = 'leito-01'
    tipoSinal      = 'FREQUENCIA'
    unidade        = 'bpm'
    instanteColeta = [DateTimeOffset]::Now.ToString('o')
    valor          = 110
    sistolica      = $null
    diastolica     = $null
    prioridade     = $null
    motivo         = $null
}
$json = $leitura | ConvertTo-Json -Compress
$json | docker compose exec -T mosquitto mosquitto_pub -q 1 -t $topico -s
```

Compare `idLeitura` e `instanteColeta` nas duas mensagens do terminal 1. A entrada traz `prioridade` e `motivo` nulos. A saída vai para `hospital/uti/leito-01/frequencia/atencao` com `prioridade=ATENCAO` e o motivo `Frequência cardíaca na faixa de atenção da simulação`.

Para repetir **a mesma coleta**, execute **somente a última linha** do bloco. Executar o bloco inteiro gera outro ID e representa uma nova coleta.

### Terminal 3 — ligar o simulador e mudar o modo da frequência

Depois da leitura manual, o terminal 3 está em `codigo`. Execute o bloco abaixo nessa pasta. Se for iniciar o simulador diretamente da raiz do repositório, use `Set-Location .\codigo\backend` na primeira linha.

```powershell
Set-Location .\backend
mvn compile exec:java "-Dexec.mainClass=br.cefetrj.iot.sensores.SimuladorMain"
```

O simulador agenda uma rodada a cada 1,5 segundo. Cada rodada completa publica 20 leituras, uma por sinal em cada leito. Os leitos `01` e `02` ficam na UTI e os leitos `03`, `04` e `05` na enfermaria. O paciente e o tópico podem se repetir. Cada nova coleta ganha um `idLeitura` próprio.

Quando aparecer `>`, digite `status`, aplique `crise leito-01 frequencia` e consulte `status` de novo. Aguarde a saída crítica no terminal 1 antes de digitar `rotina leito-01 frequencia`. O comando `ajuda` lista a sintaxe. O argumento `todos` altera os quatro sinais de um leito, como em `crise leito-02 todos` e `rotina leito-02 todos`.

### Resultados confirmados no leito 01

Os cenários abaixo passaram na validação manual da frequência cardíaca de `PAC-101`, na UTI. O terminal 3 mostrou o modo do sensor. O terminal 1 permitiu conferir a prioridade publicada pelo classificador.

| Ação realizada | Resultado observado |
| --- | --- |
| Enviar 110 bpm | A leitura bruta chegou com QoS 1. A saída `/atencao` chegou com QoS 1, prioridade e motivo preenchidos. ID, horário da coleta, paciente e valor coincidiram entre entrada e saída. |
| Reenviar o mesmo JSON | A entrada apareceu novamente em `sensores/uti/leito-01/frequencia`. O classificador publicou uma única saída para aquele ID. |
| Iniciar o simulador e consultar `status` | A frequência começou em `ROTINA`. A saída classificada chegou em `/rotina` com QoS 0. |
| Digitar `crise leito-01 frequencia` | O simulador mostrou `FREQUENCIA=CRISE` apenas no leito 01. As novas leituras entraram na faixa de crise do simulador (132 a 148 bpm) e saíram em `/critico` com `CRITICO`, motivo preenchido e QoS 2. |
| Digitar `rotina leito-01 frequencia` | A frequência voltou à faixa de rotina do simulador (65 a 95 bpm). As novas saídas voltaram a `/rotina` com QoS 0. |

Para observar as leituras classificadas dos cinco leitos, pressione `Ctrl+C` no terminal 1 e troque a assinatura com o comando abaixo, ainda em `codigo`.

```powershell
docker compose exec mosquitto mosquitto_sub -q 2 -t 'hospital/#' -F 'qos=%q %t %p'
```

Para encerrar, digite `sair` no terminal 3, aguarde o retorno ao PowerShell e pressione `Enter` no terminal 2. No terminal 1, pressione `Ctrl+C` e execute `docker compose down` em `codigo`.

## Testes automatizados

Execute a suíte na pasta `codigo/backend`. Os testes de publicação capturam as mensagens em memória, e a suíte pode rodar com o broker desligado.

```powershell
mvn clean test
```

| Etapa | Casos | Cobertura |
| --- | ---: | --- |
| 2 — Contrato | 10 | `TopicosTest` (4) verifica tópicos e QoS. `ConversorJsonTest` (6) verifica conversão de mensagens e rejeição de dados inválidos. |
| 3 — Sensores | 3 | `PublicadorSensorTest` verifica o sensor criado por cada publicador, o contrato das leituras e a mudança de modo por sinal e leito. |
| 4 — Regras | 75 | `RegrasClassificacaoTest` verifica as fronteiras de cada faixa, inclusive valores decimais, e a prevalência da componente de pressão mais grave. |
| 4 — Serviço | 21 | `ServicoClassificadorTest` verifica as 12 combinações de sinal e prioridade, cinco casos de motivo da pressão e quatro cenários de serviço (cache, falha e reenvio, entrada inválida e concorrência). Inclui tópico, QoS, retenção e preservação dos dados. |

A etapa 4 acrescenta **96 casos** aos 13 anteriores. A suíte descrita totaliza **109 casos esperados**, contando cada entrada parametrizada. O critério de sucesso é `Failures: 0`, `Errors: 0` e `BUILD SUCCESS`. Se o repositório incluir testes adicionais, o total exibido pelo Maven pode ser maior.

Cada linha abaixo executa os testes de uma única etapa.

```powershell
mvn "-Dtest=TopicosTest,ConversorJsonTest" test
mvn "-Dtest=PublicadorSensorTest" test
mvn "-Dtest=RegrasClassificacaoTest,ServicoClassificadorTest" test
```

## Onde está o Factory Method

No exemplo do Refactoring.Guru, `Dialog` declara `createButton()` e as subclasses escolhem o botão. Aqui, `PublicadorSensor` declara `criarSensor()` e as subclasses escolhem o sensor. [Referência do padrão](https://refactoring.guru/design-patterns/factory-method)

| Papel do padrão | Implementação na etapa 3 |
| --- | --- |
| Criador | `PublicadorSensor` declara `public abstract Sensor criarSensor()` e implementa `publicar(leito)`. |
| Criadores concretos | `PublicadorFrequencia`, `PublicadorTemperatura`, `PublicadorSaturacao` e `PublicadorPressao` sobrescrevem `criarSensor()`. |
| Produto | A interface `Sensor` define `getTipoSinal()` e `coletar(leito)`. |
| Produtos concretos | `SensorFrequencia`, `SensorTemperatura`, `SensorSaturacao` e `SensorPressao` implementam a interface. |

Por exemplo, `PublicadorPressao.criarSensor()` devolve um `SensorPressao`. Na primeira publicação, `obterSensor()` chama a fábrica e guarda o sensor. Nas seguintes, o publicador reutiliza essa instância. O método comum `publicar(leito)` coleta a leitura, converte-a em JSON e a envia com QoS 1 e `retain=false`. [Código dos sensores e publicadores](backend/src/main/java/br/cefetrj/iot/sensores)

## Como o classificador usa Strategy

`RegraClassificacao` define `classificar(Leitura)`. `RegraFrequencia`, `RegraTemperatura`, `RegraSaturacao` e `RegraPressao` implementam essa interface. `ServicoClassificador` registra as quatro em um `EnumMap<TipoSinal, RegraClassificacao>`. A cada entrada, `regras.get(leituraBruta.tipoSinal())` escolhe a estratégia e `regra.classificar(leituraBruta)` devolve um `ResultadoClassificacao` com prioridade e motivo. [Código do serviço](backend/src/main/java/br/cefetrj/iot/classificador/ServicoClassificador.java) · [Referência do padrão](https://refactoring.guru/design-patterns/strategy)

As faixas abaixo são regras da simulação acadêmica. `x` representa a medida indicada na linha.

| Medida | ROTINA | ATENCAO | CRITICO |
| --- | --- | --- | --- |
| Frequência (bpm) | `60 ≤ x ≤ 100` | `50 ≤ x < 60` ou `100 < x ≤ 120` | `x < 50` ou `x > 120` |
| Temperatura (°C) | `36 ≤ x ≤ 37,5` | `35 < x < 36` ou `37,5 < x < 39` | `x ≤ 35` ou `x ≥ 39` |
| Saturação (%) | `x ≥ 95` | `90 ≤ x < 95` | `x < 90` |
| Pressão sistólica (mmHg) | `90 ≤ x ≤ 139` | `80 ≤ x < 90` ou `139 < x < 180` | `x < 80` ou `x ≥ 180` |
| Pressão diastólica (mmHg) | `60 ≤ x ≤ 89` | `50 ≤ x < 60` ou `89 < x < 120` | `x < 50` ou `x ≥ 120` |

Para pressão, uma única mensagem contém as duas medidas. Em `185/110`, por exemplo, a sistólica determina `CRITICO` e o motivo indica qual componente levou à classificação. Nos empates de atenção ou de crítico, o motivo cita ambas.

O serviço verifica a compatibilidade entre tópico e JSON com `ConversorJson.lerNoTopico(...)`. Entradas inválidas são registradas e descartadas. As leituras classificadas saem com QoS 0 para `ROTINA`, 1 para `ATENCAO` e 2 para `CRITICO`, sempre com `retain=false`. Depois de um envio bem-sucedido, o serviço guarda o `idLeitura` em um cache em memória com capacidade padrão de 1.024 IDs. Reentregas de um ID ainda presente suprimem nova publicação. Uma falha de envio deixa o ID disponível para nova tentativa caso a entrada seja reenviada. Reiniciar o processo limpa o cache.

## Contrato e módulos

| Pacote Java | Responsabilidade |
| --- | --- |
| `br.cefetrj.iot.contrato` | Define os registros `Leitura`, `Alerta`, `ComandoAlerta` e `ResultadoComando` e os enums `TipoSinal`, `Prioridade` e `EstadoCicloAlerta`. `Topicos` monta os endereços MQTT e `ConversorJson` valida e converte as mensagens. |
| `br.cefetrj.iot.mqtt` | `ConfiguracaoMqtt` define os parâmetros de conexão. `ClienteMqtt` publica, assina, reconecta e refaz as assinaturas. `ReceptorMensagemMqtt` entrega a mensagem recebida ao módulo. |
| `br.cefetrj.iot.sensores` | `SimuladorMain` agenda as coletas. `LeitoSimulado` guarda o modo por sinal. Os publicadores criam os sensores e enviam as leituras ao broker. |
| `br.cefetrj.iot.classificador` | `ClassificadorMain` assina as leituras brutas. `ServicoClassificador` valida a entrada, aplica a estratégia e publica a leitura classificada. |
| `br.cefetrj.iot.demo` | Contém os programas de verificação das etapas 1 e 2 descritos abaixo. |

O simulador publica em `sensores/{ala}/{leito}/{sinal}`. Frequência, temperatura e saturação usam o campo `valor`. A pressão usa `sistolica` e `diastolica` na mesma mensagem, com `valor` nulo. Mesmo em `CRISE`, `prioridade` e `motivo` ficam nulos na leitura bruta. [Contrato MQTT e exemplos de JSON](contrato.md)

O modo `CRISE` controla a geração de valores no simulador. A prioridade `CRITICO` descreve uma leitura. Nas próximas etapas, o notificador usará **Observer** para comunicar eventos aos observadores e **State** para controlar o ciclo obrigatório de cada alerta (`ABERTO` → `RECONHECIDO` → `RESOLVIDO`). O contrato já reserva `comandos/alertas/{idAlerta}/{acao}` para `reconhecer` e `resolver`. A resolução dependerá de uma ação do profissional mesmo que o sensor volte à rotina.

## Repetir as validações das etapas anteriores

Para as demonstrações das etapas 1 e 2, suba o broker com `docker compose up -d` em `codigo`. Execute os comandos Maven em terminais separados, ambos em `codigo/backend`.

### Etapa 1 — Fundação e WebSocket

1. Abra `frontend/teste-ws.html` com **Live Server** no VS Code e aguarde `Pronto para receber em teste/#`. [Página de teste](frontend/teste-ws.html)
2. Inicie o assinante Java e aguarde a mensagem `Assinante pronto`.

   ```powershell
   mvn compile exec:java "-Dexec.mainClass=br.cefetrj.iot.demo.AssinanteTesteMain"
   ```

3. No segundo terminal, publique a leitura de demonstração.

   ```powershell
   mvn compile exec:java "-Dexec.mainClass=br.cefetrj.iot.demo.PublicadorTesteMain"
   ```

4. Confira a leitura de `P003` com valor `128` bpm no assinante Java e no navegador. A página da fundação assina `teste/#`. Para ver as leituras do simulador, use a assinatura `sensores/#` da etapa 3.

### Etapa 2 — Contrato e cliente MQTT

1. Inicie o assinante e aguarde `Assinante pronto`.

   ```powershell
   mvn compile exec:java "-Dexec.mainClass=br.cefetrj.iot.demo.ConferirContratoMain" "-Dexec.args=ouvir"
   ```

2. No segundo terminal, publique o exemplo de pressão.

   ```powershell
   mvn compile exec:java "-Dexec.mainClass=br.cefetrj.iot.demo.ConferirContratoMain" "-Dexec.args=publicar"
   ```

3. Confira o recebimento de `PAC-101` no tópico `sensores/uti/leito-01/pressao`, com `sistolica=185` e `diastolica=110` no JSON.

Nas demonstrações das etapas 1 e 2, `Enter` encerra o assinante Java. Quando concluir, execute `docker compose down` em `codigo`.

### Etapa 3 — verificar o simulador isoladamente

Abra dois terminais para repetir a validação dos sensores. No primeiro, entre em `codigo`, suba o broker e assine as leituras brutas.

```powershell
docker compose up -d
docker compose exec mosquitto mosquitto_sub -t 'sensores/#' -q 1 -v
```

O segundo terminal executa o simulador. Abra-o em `codigo/backend` e use o comando abaixo.

```powershell
mvn compile exec:java "-Dexec.mainClass=br.cefetrj.iot.sensores.SimuladorMain"
```

No prompt `>`, use `status`, `crise leito-01 frequencia` e `rotina leito-01 frequencia`, aguardando uma coleta entre as mudanças de modo. Na validação original da etapa 3, o assinante recebeu, entre outras, leituras brutas de 143, 142 e 140 bpm para `PAC-101` durante a crise.

Para limitar a saída à frequência do leito 01, pressione `Ctrl+C` no primeiro terminal e assine o tópico desse sinal.

```powershell
docker compose exec mosquitto mosquitto_sub -t 'sensores/uti/leito-01/frequencia' -q 1 -v
```

Digite `sair` no simulador, pressione `Ctrl+C` no assinante e execute `docker compose down` em `codigo` ao concluir.

## Ambiente utilizado

Versões registradas no ambiente do projeto.

| Componente | Versão ou configuração |
| --- | --- |
| Sistema operacional | Windows 10 (`amd64`) |
| JDK | OpenJDK `25.0.5` LTS, Eclipse Temurin `25.0.5+8`. O Maven compila para Java 17 (`maven.compiler.release=17`). |
| Maven | Apache Maven `3.10.1` |
| Docker Compose | `v5.7.0` no Docker Desktop |
| Git | `2.49.0.windows.1` |
| Broker | Imagem `eclipse-mosquitto:2`. Java usa `tcp://localhost:1883` e o navegador usa `ws://localhost:9001` na máquina do broker. |
| Dependências Java | Eclipse Paho `1.2.5`, Gson `2.11.0` e JUnit `5.10.2` |
| Front-end | MQTT.js `5.16.1`, incluído em `frontend/js/vendor/mqtt.min.js` |

Para consultar a versão exata do Mosquitto em execução, rode `docker compose exec mosquitto mosquitto -h` em `codigo` e leia a primeira linha da saída.
