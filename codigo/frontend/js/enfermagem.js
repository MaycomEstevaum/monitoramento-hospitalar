const campo = id => document.getElementById(id);
const base = "hospital/uti/leito-01/frequencia/";
const niveis = {
  ROTINA: "Rotina",
  ATENCAO: "Atenção",
  CRITICO: "Crítico"
};

const formatoHora = new Intl.DateTimeFormat("pt-BR", { timeStyle: "medium" });
let ultimaColeta = null;

function informarConexao(texto) {
  campo("conexao").textContent = texto;
}

function atualizarAviso() {
  if (ultimaColeta === null) return;

  const atraso = Date.now() - ultimaColeta > 10000;
  campo("aviso").textContent = atraso
    ? "Leitura desatualizada · confira a conexão e o simulador."
    : "Leitura recente";
}

const cliente = mqtt.connect("ws://" + location.hostname + ":9001", {
  resubscribe: false
});

cliente.on("connect", () => {
  informarConexao("Assinando tópico…");

  cliente.subscribe(base + "+", { qos: 2 }, (erro, assinaturas) => {
    if (!cliente.connected) return;

    const aceita = !erro && assinaturas?.some(
      a => a.topic === base + "+" && a.qos <= 2
    );

    informarConexao(aceita ? "Conectado" : "Falha na assinatura");
  });
});

cliente.on("close", () => informarConexao("Sem conexão"));
cliente.on("error", () => informarConexao("Falha de conexão"));

cliente.on("message", (topico, mensagem) => {
  let leitura;
  try {
    leitura = JSON.parse(mensagem.toString());
  } catch {
    return;
  }

  if (!leitura || !Object.hasOwn(niveis, leitura.prioridade)) return;

  if (
    topico !== base + leitura.prioridade.toLowerCase() ||
    leitura.ala !== "uti" ||
    leitura.leito !== "leito-01" ||
    leitura.tipoSinal !== "FREQUENCIA" ||
    leitura.unidade !== "bpm" ||
    !Number.isFinite(leitura.valor)
  ) return;

  const instante = Date.parse(leitura.instanteColeta);
  if (!Number.isFinite(instante)) return;
  if (ultimaColeta !== null && instante < ultimaColeta) return;

  ultimaColeta = instante;
  campo("paciente").textContent = "Paciente " + leitura.pacienteId;
  campo("valor").textContent = leitura.valor.toLocaleString("pt-BR");
  campo("hora").textContent = formatoHora.format(instante);
  campo("prioridade").textContent = niveis[leitura.prioridade];
  campo("prioridade").dataset.nivel = leitura.prioridade;
  atualizarAviso();
});

setInterval(atualizarAviso, 1000);
