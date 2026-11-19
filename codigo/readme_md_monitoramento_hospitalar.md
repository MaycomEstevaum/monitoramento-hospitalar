# Monitoramento hospitalar com IoT

## Ambiente utilizado

- **Sistema Operacional:** Windows 10 (`amd64`)
- **JDK:** OpenJDK `25.0.5` LTS (Eclipse Temurin `25.0.5+8`), compilado para Java `17` (`maven.compiler.release=17`)
- **Maven:** Apache Maven `3.10.1`
- **Docker Compose:** `v5.7.0` (Docker Desktop)
- **Git:** `2.49.0.windows.1`
- **Broker:** `eclipse-mosquitto:2` _(preencher a versão exata da primeira linha de `docker compose exec mosquitto mosquitto -h` ao subir o container)_
- **Dependências Java:** Eclipse Paho `1.2.5`, Gson `2.11.0` e JUnit `5.10.2`
- **Front-end:** MQTT.js `5.16.1` incluído localmente em `frontend/js/vendor/mqtt.min.js`

## Documentação e estrutura do código

- **`codigo/contrato.md`:** Especificação oficial dos tópicos MQTT, níveis de QoS, regras de retenção e formatos JSON trocados entre os módulos.
- **`br.cefetrj.iot.contrato`:** Records imutáveis (`Leitura`, `Alerta`, `ComandoAlerta`, `ResultadoComando`), enums (`TipoSinal`, `Prioridade`, `EstadoCicloAlerta`), validação de campos, montagem de tópicos (`Topicos`) e serialização estrita (`ConversorJson`).
- **`br.cefetrj.iot.mqtt`:** Cliente assíncrono (`ClienteMqtt`), configurações de conexão (`ConfiguracaoMqtt`) e interface de recepção (`ReceptorMensagemMqtt`) com reconexão e reassinatura automáticas.

## Como validar a Fundação (Etapa 1)

1. Dentro de `codigo`, executar `docker compose up -d`.
2. Dentro de `codigo/backend`, executar `mvn clean test`.
3. Abrir `codigo/frontend/teste-ws.html` com Live Server e esperar a confirmação da assinatura (`Pronto para receber em teste/#`).
4. Em um terminal dentro de `codigo/backend`, executar:
   ```powershell
   mvn exec:java "-Dexec.mainClass=br.cefetrj.iot.demo.AssinanteTesteMain"
   ```
5. Em outro terminal na mesma pasta, executar:
   ```powershell
   mvn exec:java "-Dexec.mainClass=br.cefetrj.iot.demo.PublicadorTesteMain"
   ```
6. Conferir se a leitura de `P003` com valor `128` apareceu no terminal do assinante e no navegador.
7. Para encerrar, pressionar `Enter` no terminal do assinante e executar `docker compose down` dentro de `codigo`.

## Como validar o Contrato e Cliente MQTT (Etapa 2)

1. **Testes automatizados (offline):** dentro de `codigo/backend`, executar `mvn clean test` (ou `mvn "-Dtest=TopicosTest,ConversorJsonTest" test` para rodar apenas os testes do contrato) e conferir `Tests run: 10, Failures: 0, Errors: 0`.
2. **Validação em rede pelo Mosquitto:**
   - Subir o broker dentro de `codigo` com `docker compose up -d`.
   - Em um terminal dentro de `codigo/backend`, iniciar o assinante:
     ```powershell
     mvn exec:java "-Dexec.mainClass=br.cefetrj.iot.demo.ConferirContratoMain" "-Dexec.args=ouvir"
     ```
   - Em outro terminal na mesma pasta, publicar uma leitura de exemplo:
     ```powershell
     mvn exec:java "-Dexec.mainClass=br.cefetrj.iot.demo.ConferirContratoMain" "-Dexec.args=publicar"
     ```
   - Conferir no terminal do assinante o recebimento da leitura de `PAC-101` no tópico `sensores/uti/leito-01/pressao`.
   - Para encerrar, pressionar `Enter` no terminal do assinante e executar `docker compose down` dentro de `codigo`.
