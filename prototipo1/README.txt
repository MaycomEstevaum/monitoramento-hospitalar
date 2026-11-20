Monitoramento de saúde hospitalar, Publish Subscribe com IoT

É uma simulação de sensores de 4 leitos que mandam os sinais vitais pra um broker MQTT (o Mosquitto). Tem 3 telas que recebem só o que precisam (enfermagem, médico e celular da equipe) e uma tela do simulador pra provocar crises na hora da demonstração.


O que precisa ter instalado

JDK 8 ou mais novo.

Mosquitto instalado e rodando.

As bibliotecas MQTT já vieram junto na pasta lib, não precisa baixar nada.


Como rodar

No VS Code: abre a pasta do projeto (a que tem src e lib) e roda as 4 classes da pasta principal, começando pelo SimuladorMain. As outras são EnfermagemMain, MedicoMain e NotificacoesMain.

Vão abrir 4 janelas.


Como testar

Na janela do simulador escolhe um leito e um sinal e clica em Provocar crise. Em 2 ou 3 segundos o painel da enfermagem fica vermelho naquele leito e o celular recebe a notificação. O painel médico mostra o alerta se for o leito que ele está acompanhando. Clica em Normalizar pro paciente voltar ao normal, e o celular avisa que estabilizou.

O painel médico acompanha um leito por vez, dá pra trocar no menu de cima.

Também dá pra mandar comando pelo terminal onde o simulador foi aberto, por exemplo:
crise leito02 frequencia_cardiaca
normalizar leito02 frequencia_cardiaca

Os sinais são frequencia_cardiaca, temperatura, saturacao_oxigenio e pressao_arterial. Os leitos vão de leito01 até leito04.


Tópicos usados

As leituras vão em hospital/leito/sinal/prioridade, por exemplo hospital/leito02/frequencia_cardiaca/critico. A prioridade pode ser rotina, atencao ou critico.
Os alertas vão em alertas/leito/critico e alertas/leito/estabilizado.

A enfermagem assina tudo, o médico assina só o leito que está olhando e o celular assina só os alertas críticos e as estabilizações.

A prioridade define o QoS da mensagem. Rotina usa 0, atenção usa 1 e crítico usa 2. O MQTT não passa uma mensagem na frente da outra por prioridade, a prioridade serve pra filtrar pelo tópico e pra definir o QoS.


Padrões usados

Publish Subscribe como arquitetura, com o Mosquitto no meio.
Factory Method nos sensores, uma fábrica pra cada sinal vital.
Strategy nas faixas de prioridade, cada sinal tem a sua.
State no nível de alerta de cada sinal (normal, atenção e crítico). O alerta só sai quando muda de estado, então não repete a cada leitura.
Observer nos assinantes, quem recebe a mensagem do broker avisa as telas.


Se der problema

Se abrir um aviso dizendo que não conseguiu conectar, o Mosquitto não está rodando.
Se o Mosquitto cair e voltar, as telas reconectam sozinhas mas perdem o que foi enviado enquanto ele estava fora.
Os valores dos sinais são inventados, só servem pra simulação.


Pastas

src/principal tem as classes que iniciam tudo. O resto do código fica nas outras pastas dentro de src. A pasta lib tem as bibliotecas MQTT.
