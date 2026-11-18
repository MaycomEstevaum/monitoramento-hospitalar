# Monitoramento hospitalar com IoT

## Ambiente utilizado

- **Sistema Operacional:** Windows 10 (`amd64`)
- **JDK:** OpenJDK `25.0.5` LTS (Eclipse Temurin `25.0.5+8`), compilado para Java `17` (`maven.compiler.release=17`)
- **Maven:** Apache Maven `3.10.1`
- **Docker Compose:** `v5.7.0` (Docker Desktop)
- **Git:** `2.49.0.windows.1`
- **Broker:** `eclipse-mosquitto:2` _(preencher a versÃ£o exata da primeira linha de `docker compose exec mosquitto mosquitto -h` ao subir o container)_
- **DependÃªncias Java:** Eclipse Paho `1.2.5`, Gson `2.10.1` e JUnit `5.10.2`
- **Front-end:** MQTT.js `5.16.1` incluÃ­do localmente em `frontend/js/vendor/mqtt.min.js`

## Como validar a Fundação

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
