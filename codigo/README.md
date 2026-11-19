# Monitoramento hospitalar com IoT

Projeto acadêmico em Java que simula quatro sinais vitais em cinco leitos fictícios. Até a etapa 3, o simulador gera as leituras e as publica por MQTT. O Mosquitto é o broker que recebe cada publicação e a encaminha aos assinantes do tópico. Esse fluxo implementa Publish-Subscribe. O Factory Method define qual sensor cada publicador utiliza.

## Executar o simulador de sensores (etapa 3)

Abra o Docker Desktop e dois terminais PowerShell na raiz do projeto, `monitoramento-hospitalar`, a pasta que contém `codigo`.

### Terminal 1 — receber as leituras

```powershell
Set-Location .\codigo
docker compose up -d
docker compose ps
docker compose exec mosquitto mosquitto_sub -t 'sensores/#' -q 1 -v
```

Confira se o serviço `mosquitto` está em execução. O último comando assina todos os tópicos de sensores e mostra o tópico ao lado do JSON recebido. Deixe esse terminal aberto enquanto inicia o simulador.

### Terminal 2 — iniciar o simulador

```powershell
Set-Location .\codigo\backend
mvn compile exec:java "-Dexec.mainClass=br.cefetrj.iot.sensores.SimuladorMain"
```

O simulador agenda uma rodada a cada 1,5 segundo. Cada rodada completa publica 20 leituras, uma por sinal em cada leito. Os leitos `01` e `02` ficam na UTI e os leitos `03`, `04` e `05` na enfermaria. Os quatro sinais são frequência cardíaca, temperatura, saturação e pressão arterial.

### Simular uma crise e voltar à rotina

Quando aparecer `>` no terminal 2, digite um comando por vez. Aguarde uma nova leitura no terminal 1 entre a mudança para crise e a volta à rotina.

| Comando no simulador | O que observar |
| --- | --- |
| `status` | Exibe o modo de cada sinal em cada leito. Todos começam em `ROTINA`. |
| `crise leito-01 frequencia` | A próxima coleta de frequência do `PAC-101` entra na faixa simulada de 132 a 148 bpm. Um novo `status` mostra `FREQUENCIA=CRISE` só nesse leito. |
| `rotina leito-01 frequencia` | A frequência do mesmo paciente volta à faixa simulada de 65 a 95 bpm. As publicações continuam. |
| `ajuda` | Lista a sintaxe dos comandos. O argumento `todos` altera os quatro sinais de um leito, como em `crise leito-02 todos` e `rotina leito-02 todos`. |

Para acompanhar só a frequência do leito 01, pressione `Ctrl+C` no **terminal 1** e troque a assinatura com o comando abaixo, ainda na pasta `codigo`.

```powershell
docker compose exec mosquitto mosquitto_sub -t 'sensores/uti/leito-01/frequencia' -q 1 -v
```

O paciente e o tópico se repetem porque a simulação continua coletando dados. Cada nova coleta ganha outro `idLeitura` e registra seu próprio `instanteColeta`. Na validação manual, o `status` mostrou apenas a frequência do leito 01 em crise. O assinante recebeu, entre outras, leituras de 143, 142 e 140 bpm para `PAC-101`.

Para encerrar, digite `sair` no terminal 2. No terminal 1, pressione `Ctrl+C` e execute `docker compose down` em `codigo` quando terminar de usar o broker.

## Testes automatizados

O Maven compila o backend e roda a suíte com este comando na pasta `codigo/backend`.

```powershell
mvn clean test
```

As três classes de teste somam **13 testes** e podem rodar com o broker desligado. Para essa suíte, o resultado esperado é `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`.

| Etapa | Testes | Cobertura | Comando para rodar só esse grupo |
| --- | ---: | --- | --- |
| 2 — Contrato | 10 | `TopicosTest` valida endereços e QoS. `ConversorJsonTest` verifica a conversão de mensagens e a rejeição de dados inválidos. | `mvn "-Dtest=TopicosTest,ConversorJsonTest" test` |
| 3 — Sensores | 3 | `PublicadorSensorTest` verifica qual sensor cada publicador cria, as leituras publicadas e a mudança de modo por sinal e leito. Captura as mensagens em memória. | `mvn "-Dtest=PublicadorSensorTest" test` |

## Onde está o Factory Method

No exemplo do Refactoring.Guru, `Dialog` declara `createButton()` e as subclasses escolhem o botão. Aqui, `PublicadorSensor` declara `criarSensor()` e as subclasses escolhem o sensor. [Referência do padrão](https://refactoring.guru/design-patterns/factory-method)

| Papel do padrão | Implementação na etapa 3 |
| --- | --- |
| Criador | `PublicadorSensor` declara `public abstract Sensor criarSensor()` e implementa `publicar(leito)`. |
| Criadores concretos | `PublicadorFrequencia`, `PublicadorTemperatura`, `PublicadorSaturacao` e `PublicadorPressao` sobrescrevem `criarSensor()`. |
| Produto | A interface `Sensor` define `getTipoSinal()` e `coletar(leito)`. |
| Produtos concretos | `SensorFrequencia`, `SensorTemperatura`, `SensorSaturacao` e `SensorPressao` implementam a interface. |

Por exemplo, `PublicadorPressao.criarSensor()` devolve um `SensorPressao`. Na primeira publicação, `obterSensor()` chama a fábrica e guarda o sensor. Nas seguintes, o publicador reutiliza essa instância. O método comum `publicar(leito)` coleta a leitura, converte-a em JSON e a envia com QoS 1 e `retain=false`. [Código dos sensores e publicadores](backend/src/main/java/br/cefetrj/iot/sensores)

## Contrato e módulos

| Pacote Java | Responsabilidade |
| --- | --- |
| `br.cefetrj.iot.contrato` | Define os registros `Leitura`, `Alerta`, `ComandoAlerta` e `ResultadoComando` e os enums `TipoSinal`, `Prioridade` e `EstadoCicloAlerta`. `Topicos` monta os endereços MQTT e `ConversorJson` valida e converte as mensagens. |
| `br.cefetrj.iot.mqtt` | `ConfiguracaoMqtt` define os parâmetros de conexão. `ClienteMqtt` publica, assina, reconecta e refaz as assinaturas. `ReceptorMensagemMqtt` entrega a mensagem recebida ao módulo. |
| `br.cefetrj.iot.sensores` | `SimuladorMain` agenda as coletas. `LeitoSimulado` guarda o modo por sinal. Os publicadores criam os sensores e enviam as leituras ao broker. |
| `br.cefetrj.iot.demo` | Contém os programas de verificação das etapas 1 e 2 descritos abaixo. |

O simulador publica em `sensores/{ala}/{leito}/{sinal}`. Frequência, temperatura e saturação usam o campo `valor`. A pressão usa `sistolica` e `diastolica` na mesma mensagem, com `valor` nulo. Mesmo em `CRISE`, `prioridade` e `motivo` ficam nulos na leitura bruta. [Contrato MQTT e exemplos de JSON](contrato.md)

Nas próximas etapas, **Strategy** classificará as leituras, **Observer** notificará os observadores internos do notificador e **State** controlará o ciclo obrigatório dos alertas (`ABERTO` → `RECONHECIDO` → `RESOLVIDO`). A etapa 2 já definiu os tópicos e formatos MQTT para reconhecer e resolver alertas. A execução dessas ações pelo notificador e pelas telas faz parte das etapas seguintes.

## Repetir as validações das etapas anteriores

Para cada demonstração, suba o broker com `docker compose up -d` em `codigo`. Execute os comandos Maven em terminais separados, ambos em `codigo/backend`.

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

Em ambas as demonstrações, `Enter` encerra o assinante Java. Quando concluir, execute `docker compose down` em `codigo`.

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
