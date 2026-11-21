// Uma conexão por página. Cada assinatura é confirmada antes de habilitar ações.
function conectarPainel(aoStatus, aoConectar, receber) {
  const cliente = mqtt.connect("ws://" + location.hostname + ":9001", {
    clean: true, resubscribe: false, reconnectPeriod: 1500
  });
  let filtros = {};
  let geracao = 0;
  let pronto = false;

  function status(texto, ok = false) {
    pronto = ok;
    aoStatus(texto, ok);
  }

  function assinar() {
    const atual = ++geracao;
    status("Conectando aos tópicos…");
    cliente.subscribe(filtros, (erro, concedidos) => {
      if (atual !== geracao || !cliente.connected) return;
      const ok = !erro && Object.entries(filtros).every(([topico, opcoes]) =>
        concedidos?.some(item => item.topic === topico && item.qos === opcoes.qos));
      status(ok ? "Conectado" : "Falha na assinatura", ok);
    });
  }

  cliente.on("connect", () => { aoConectar(); assinar(); });
  cliente.on("close", () => { geracao++; status("Sem conexão"); });
  cliente.on("error", () => status("Falha de conexão"));
  cliente.on("message", (topico, bytes) => {
    if (!bytes.length) { receber(topico, null); return; }
    let dados;
    try { dados = JSON.parse(bytes.toString()); } catch { return; }
    if (dados && typeof dados === "object") receber(topico, dados);
  });

  return {
    selecionar(leituras, estados, extras = {}) {
      const anteriores = Object.keys(filtros);
      filtros = {
        [leituras]: { qos: 2 },
        [estados]: { qos: 1 },
        ...extras
      };
      if (!cliente.connected) return;
      const remover = anteriores.filter(f => !(f in filtros));
      if (remover.length) cliente.unsubscribe(remover);
      assinar();
    },
    enviar(pedido, aoErro) {
      if (!pronto || !cliente.connected) { aoErro(); return; }
      const topico = "comandos/alertas/" + pedido.idAlerta + "/" + pedido.acao.toLowerCase();
      cliente.publish(topico, JSON.stringify(pedido), { qos: 1, retain: false }, erro => {
        if (erro) aoErro();
      });
    },
    enviarSimulador(comando, aoErro) {
      if (!pronto || !cliente.connected) { aoErro(); return; }
      const segSinal = comando.tipoSinal ? comando.tipoSinal.toLowerCase() : "todos";
      const topico = "comandos/simulador/" + comando.ala + "/" + comando.leito + "/" + segSinal;
      cliente.publish(topico, JSON.stringify(comando), { qos: 1, retain: false }, erro => {
        if (erro) aoErro();
      });
    }
  };
}
