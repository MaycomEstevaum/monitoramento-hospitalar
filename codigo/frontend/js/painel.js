function iniciarPainel(tela) {
  const el = id => document.getElementById(id);
  const sinais = {
    FREQUENCIA: ["Frequência", "bpm"], TEMPERATURA: ["Temperatura", "°C"],
    SATURACAO: ["Saturação", "%"], PRESSAO: ["Pressão", "mmHg"]
  };
  const niveis = ["ROTINA", "ATENCAO", "CRITICO"];
  const nomes = { ROTINA: "Rotina", ATENCAO: "Atenção", CRITICO: "Crítico" };
  const estados = ["ABERTO", "RECONHECIDO", "RESOLVIDO"];
  const cenarios = ["ROTINA", "CRISE"];
  const leitos = [
    ["uti/leito-01", "PAC-101"], ["uti/leito-02", "PAC-102"],
    ["enfermaria/leito-03", "PAC-201"], ["enfermaria/leito-04", "PAC-202"],
    ["enfermaria/leito-05", "PAC-203"]
  ];
  const INTERVALO_HISTORICO_MS = 5000;
  const JANELA_COMPLETAR_MS = 1200;
  const MAX_CARTOES_HISTORICO = 40;
  const CHAVE_LEITO_MEDICO = "monitoramento:leito-medico";
  const leituras = new Map(), alertas = new Map(), pedidos = new Map(), estadosSimulador = new Map();
  const historicoCartoes = [];
  const hora = new Intl.DateTimeFormat("pt-BR", { timeStyle: "medium" });
  const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "medium" });
  const numero = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 });
  let selecionado = leitos[0][0], online = false, edicao = null;
  let ultimoRegistroHistoricoMs = 0, inicioSelecaoMs = Date.now(), leituraNovaDesdeRegistro = false;
  let ultimaAssinaturaAlertasMedico = "";
  const enfermagem = tela === "enfermagem", medico = tela === "medico", movel = tela === "mobile";
  const escape = texto => String(texto ?? "").replace(/[&<>"']/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
  const local = chave => chave.replace("uti/", "UTI · ").replace("enfermaria/", "Enfermaria · ").replace("leito-", "Leito ");
  const chave = dado => dado.ala + "/" + dado.leito;
  const chaveHistorico = leito => "monitoramento:historico:" + leito;
  const noEscopo = dado => !medico || chave(dado) === selecionado;
  const recente = dado => Date.now() - Date.parse(dado.instanteColeta) <= 10000;
  const textoMedida = (tipo, v, s, d) =>
    tipo === "PRESSAO" ? numero.format(s) + "/" + numero.format(d) : numero.format(v);
  const medida = l => textoMedida(l.tipoSinal, l.valor, l.sistolica, l.diastolica);
  const etiqueta = nivel => '<span class="tag ' + nivel.toLowerCase() + '">' + nomes[nivel] + '</span>';
  const quando = valor => Number.isFinite(Date.parse(valor)) ? dataHora.format(Date.parse(valor)) : "—";
  const horaCurta = valor => Number.isFinite(Date.parse(valor)) ? hora.format(Date.parse(valor)) : "—";
  const permitido = (estado, acao) => (estado === "ABERTO" && acao === "RECONHECER") ||
    (estado === "RECONHECIDO" && acao === "RESOLVER");
  const podeNaTela = acao => (enfermagem && acao === "RECONHECER") || (medico && acao === "RESOLVER");
  const dizer = texto => { el("retorno").textContent = texto; };
  const novoId = prefixo => {
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    return prefixo + Array.from(bytes, b => b.toString(16).padStart(2, "0")).join("");
  };
  const snapshotLeitura = l => ({
    tipoSinal: l.tipoSinal,
    valor: l.valor,
    sistolica: l.sistolica,
    diastolica: l.diastolica,
    unidade: l.unidade,
    prioridade: l.prioridade,
    instanteColeta: l.instanteColeta
  });

  function carregarHistorico(leito) {
    if (!medico) return [];
    try {
      const bruto = sessionStorage.getItem(chaveHistorico(leito));
      if (!bruto) return [];
      const lista = JSON.parse(bruto);
      if (!Array.isArray(lista)) return [];
      return lista
        .filter(item => item && typeof item === "object" && Number.isFinite(item.capturadoEm) && item.sinais && typeof item.sinais === "object")
        .map(item => {
          const sinaisLimpos = {};
          for (const tipo of Object.keys(sinais)) {
            const s = item.sinais[tipo];
            sinaisLimpos[tipo] = s && typeof s === "object" && Object.hasOwn(sinais, s.tipoSinal) && niveis.includes(s.prioridade)
              ? snapshotLeitura(s) : null;
          }
          const criticos = Array.isArray(item.sinaisCriticos)
            ? item.sinaisCriticos.filter(t => Object.hasOwn(sinais, t))
            : (item.sinalCritico && Object.hasOwn(sinais, item.sinalCritico) ? [item.sinalCritico] : []);
          return {
            capturadoEm: item.capturadoEm,
            motivoTipo: item.motivoTipo === "critico" ? "critico" : "intervalo",
            sinaisCriticos: criticos,
            prioridadeGeral: niveis.includes(item.prioridadeGeral) ? item.prioridadeGeral : "",
            sinais: sinaisLimpos
          };
        })
        .slice(0, MAX_CARTOES_HISTORICO);
    } catch {
      return [];
    }
  }

  function salvarHistorico() {
    if (!medico) return;
    try {
      if (historicoCartoes.length) {
        sessionStorage.setItem(chaveHistorico(selecionado), JSON.stringify(historicoCartoes));
      } else {
        sessionStorage.removeItem(chaveHistorico(selecionado));
      }
    } catch {}
  }

  function statusLeituraResolucao(a) {
    const l = leituras.get(chave(a) + "/" + a.tipoSinal);
    if (!l || !recente(l) || l.pacienteId !== a.pacienteId) {
      return { ok: false, texto: "Aguardando leitura recente em rotina para registrar a resolução." };
    }
    if (l.prioridade === "ROTINA") {
      return { ok: true, texto: "Sinal normalizado (" + medida(l) + " " + l.unidade + ") · pronto para registrar resolução." };
    }
    return {
      ok: false,
      texto: "Sinal ainda em " + nomes[l.prioridade] + " (" + medida(l) + " " + l.unidade + ") — aguarde voltar à rotina para registrar a resolução."
    };
  }

  function assinaturaAlertasMedico() {
    return [...alertas.values()]
      .filter(a => noEscopo(a) && a.estado === "RECONHECIDO")
      .sort((a, b) => a.idAlerta.localeCompare(b.idAlerta))
      .map(a => {
        const st = statusLeituraResolucao(a);
        return a.idAlerta + "|" + (st.ok ? "OK" : st.texto);
      })
      .join(";");
  }

  function sincronizarAlertasMedico() {
    if (!medico) return;
    const assinatura = assinaturaAlertasMedico();
    if (assinatura !== ultimaAssinaturaAlertasMedico) {
      ultimaAssinaturaAlertasMedico = assinatura;
      mostrarAlertas();
    }
  }

  function placeholderResolucao(a) {
    const medidaOrigem = textoMedida(a.tipoSinal, a.valorOrigem, a.sistolicaOrigem, a.diastolicaOrigem) + " " + a.unidadeOrigem;
    const horario = horaCurta(a.instanteLeituraOrigem);
    if (a.tipoSinal === "SATURACAO") {
      return "Ex.: Oxigenação estabilizada após suporte; saturação normalizada após queda para " + medidaOrigem + " às " + horario;
    }
    const baixa = (a.tipoSinal === "FREQUENCIA" && a.valorOrigem < 60) ||
      (a.tipoSinal === "TEMPERATURA" && a.valorOrigem <= 35) ||
      (a.tipoSinal === "PRESSAO" && (a.sistolicaOrigem < 90 || a.diastolicaOrigem < 60));
    const nome = sinais[a.tipoSinal][0].toLowerCase();
    return "Ex.: Paciente estabilizado após atendimento; " + nome + " normalizada após " +
      (baixa ? "queda para " : "pico de ") + medidaOrigem + " às " + horario;
  }

  function mostrarLeituras() {
    if (movel) {
      const ativos = [...alertas.values()]
        .filter(a => a.estado !== "RESOLVIDO")
        .sort((a, b) => (a.estado === "ABERTO" ? 0 : 1) - (b.estado === "ABERTO" ? 0 : 1) ||
          Date.parse(b.abertoEm) - Date.parse(a.abertoEm));
      let conteudo;
      if (ativos.length) {
        conteudo = '<ul class="criticos-lista">' + ativos.map(a => {
          const l = leituras.get(chave(a) + "/" + a.tipoSinal);
          const aoVivo = l && recente(l);
          const textoVal = aoVivo
            ? medida(l) + ' ' + escape(l.unidade)
            : textoMedida(a.tipoSinal, a.valorOrigem, a.sistolicaOrigem, a.diastolicaOrigem) + ' ' + escape(a.unidadeOrigem);
          const textoHora = aoVivo
            ? horaCurta(l.instanteColeta) + ' · crítico ao vivo'
            : horaCurta(a.instanteLeituraOrigem) + ' · sem novo crítico recente';
          return '<li class="critico-item"><div><strong>' + escape(local(chave(a))) + ' · ' + escape(a.pacienteId) +
            ' · ' + sinais[a.tipoSinal][0] + '</strong><small>' + textoHora + '</small></div>' +
            '<div class="critico-meta"><strong>' + textoVal + '</strong><span class="tag">' + a.estado + '</span></div></li>';
        }).join("") + '</ul>';
      } else {
        const recentes = [...leituras.values()]
          .filter(recente)
          .sort((a, b) => Date.parse(b.instanteColeta) - Date.parse(a.instanteColeta));
        conteudo = recentes.length
          ? '<ul class="criticos-lista">' + recentes.map(l =>
              '<li class="critico-item"><div><strong>' + escape(local(chave(l))) + ' · ' + escape(l.pacienteId) +
              ' · ' + sinais[l.tipoSinal][0] + '</strong><small>' + horaCurta(l.instanteColeta) +
              ' · crítico ao vivo</small></div><div class="critico-meta"><strong>' + medida(l) + ' ' +
              escape(l.unidade) + '</strong></div></li>').join("") + '</ul>'
          : '<p class="muted">Nenhum sinal crítico ativo no momento.</p>';
      }
      const temCritico = ativos.length > 0 || [...leituras.values()].some(recente);
      el("leituras").innerHTML = '<section class="cartao" data-nivel="' + (temCritico ? 'CRITICO' : 'ROTINA') + '">' +
        '<div class="linha"><h2>Sinais críticos em andamento</h2>' +
        etiqueta(temCritico ? "CRITICO" : "ROTINA") + '</div>' + conteudo + '</section>';
      return;
    }
    const exibir = medico ? leitos.filter(([k]) => k === selecionado) : leitos;
    const cartoes = exibir.map(([k, inicial]) => {
      const atuais = Object.keys(sinais).map(tipo => leituras.get(k + "/" + tipo)).filter(Boolean);
      const gravidade = atuais.filter(recente).reduce((max, l) => Math.max(max, niveis.indexOf(l.prioridade)), -1);
      const paciente = atuais.reduce((a, b) => !a || Date.parse(b.instanteColeta) > Date.parse(a.instanteColeta) ? b : a, null);
      const nivel = niveis[gravidade];
      const metricas = Object.entries(sinais).map(([tipo, [nome, unidade]]) => {
        const l = leituras.get(k + "/" + tipo);
        return '<div class="metrica"><dt>' + nome + '</dt><dd>' + (l ? medida(l) : '—') + ' <small>' + unidade +
          '</small></dd><small>' + (l ? hora.format(Date.parse(l.instanteColeta)) + ' · ' +
          nomes[l.prioridade] + (recente(l) ? '' : ' · desatualizada') : 'Aguardando') + '</small></div>';
      }).join("");
      const alertasLeito = [...alertas.values()]
        .filter(a => chave(a) === k && a.estado !== "RESOLVIDO")
        .sort((a, b) => (a.estado === "ABERTO" ? 0 : 1) - (b.estado === "ABERTO" ? 0 : 1) ||
          Date.parse(b.abertoEm) - Date.parse(a.abertoEm));
      const resumoAlertas = alertasLeito.length
        ? '<p class="resumo-alertas"><strong>' + alertasLeito.length +
          (alertasLeito.length === 1 ? ' alerta ativo: ' : ' alertas ativos: ') +
          '</strong>' + alertasLeito.map(a => sinais[a.tipoSinal][0] + ' (' + a.estado + ')').join(', ') + '</p>'
        : '';
      return '<article class="cartao" data-nivel="' + (nivel || '') + '"><div class="linha"><h2>' + escape(local(k)) +
        '</h2>' + (nivel ? etiqueta(nivel) : '<span class="tag">Sem coleta recente</span>') + '</div><p class="muted">' +
        escape(paciente?.pacienteId || inicial) + '</p><dl class="metricas">' + metricas + '</dl>' + resumoAlertas + '</article>';
    });
    el("leituras").innerHTML = '<div class="grade">' + cartoes.join("") + '</div>';
  }

  function registrarCartaoHistorico(motivoTipo, sinalCritico) {
    if (!medico) return;
    const retrato = {};
    let maxGravidade = -1;
    for (const tipo of Object.keys(sinais)) {
      const l = leituras.get(selecionado + "/" + tipo);
      if (l) {
        retrato[tipo] = snapshotLeitura(l);
        maxGravidade = Math.max(maxGravidade, niveis.indexOf(l.prioridade));
      } else {
        retrato[tipo] = null;
      }
    }
    const agora = Date.now();
    historicoCartoes.unshift({
      capturadoEm: agora,
      motivoTipo,
      sinaisCriticos: sinalCritico ? [sinalCritico] : [],
      prioridadeGeral: niveis[maxGravidade] || "",
      sinais: retrato
    });
    historicoCartoes.splice(MAX_CARTOES_HISTORICO);
    ultimoRegistroHistoricoMs = agora;
    leituraNovaDesdeRegistro = false;
    salvarHistorico();
    mostrarHistorico(true);
  }

  function mostrarHistorico(adicionouNovo = false, preservarScroll = true) {
    if (!medico) return;
    if (el("limpar-historico")) el("limpar-historico").disabled = !historicoCartoes.length;
    const listaAnterior = preservarScroll ? el("historico").querySelector(".hist-lista") : null;
    const scrollAnterior = listaAnterior ? listaAnterior.scrollTop : 0;
    if (!historicoCartoes.length) {
      el("historico").innerHTML = '<p class="muted">Aguardando coletas deste leito.</p>';
      return;
    }
    el("historico").innerHTML = '<div class="hist-lista">' + historicoCartoes.map(c => {
      const criticos = Array.isArray(c.sinaisCriticos) ? c.sinaisCriticos
        : (c.sinalCritico ? [c.sinalCritico] : []);
      const itens = Object.entries(sinais).map(([tipo, [nome, unidade]]) => {
        const l = c.sinais[tipo];
        const classeSinal = l?.prioridade === "CRITICO" ? " sinal-critico"
          : (l?.prioridade === "ATENCAO" ? " sinal-atencao" : "");
        return '<div class="hist-sinal' + classeSinal + '">' +
          '<span class="hist-nome">' + nome + '</span>' +
          '<strong>' + (l ? medida(l) + ' ' + escape(l.unidade) : '— ' + unidade) + '</strong>' +
          '<small>' + (l ? horaCurta(l.instanteColeta) + ' · ' + nomes[l.prioridade] : 'Sem leitura') + '</small>' +
          '</div>';
      }).join("");
      const motivoTag = c.motivoTipo === "critico"
        ? '<span class="tag critico">Evento crítico · ' + criticos.map(t => sinais[t][0]).join(", ") + '</span>'
        : '';
      return '<article class="hist-cartao" data-nivel="' + escape(c.prioridadeGeral) + '">' +
        '<div class="linha"><strong>' + hora.format(c.capturadoEm) + '</strong><div class="hist-tags">' +
        motivoTag + (c.prioridadeGeral ? etiqueta(c.prioridadeGeral) : '') +
        '</div></div><div class="hist-sinais">' + itens + '</div></article>';
    }).join("") + '</div>';
    if (scrollAnterior > 10) {
      const listaNova = el("historico").querySelector(".hist-lista");
      if (listaNova) {
        const deslocamento = adicionouNovo && listaNova.firstElementChild
          ? listaNova.firstElementChild.offsetHeight + 12
          : 0;
        listaNova.scrollTop = scrollAnterior + deslocamento;
      }
    }
  }

  function linhaTempoAlerta(a) {
    const abertura = '<li class="concluido"><strong>Abertura:</strong> ' + quando(a.abertoEm) +
      ' <span class="muted">(coleta ' + quando(a.instanteLeituraOrigem) + ')</span></li>';
    const reconhecimento = a.reconhecidoPor
      ? '<li class="concluido"><strong>Reconhecimento:</strong> ' + quando(a.reconhecidoEm) +
        ' · por <strong>' + escape(a.reconhecidoPor) + '</strong></li>'
      : '<li class="pendente"><strong>Reconhecimento:</strong> Aguardando reconhecimento da enfermagem</li>';
    const resolucao = a.resolvidoPor
      ? '<li class="concluido"><strong>Resolução:</strong> ' + quando(a.resolvidoEm) +
        ' · por <strong>' + escape(a.resolvidoPor) + '</strong>' +
        '<span class="motivo-resolucao"><strong>Motivo da resolução (informado):</strong> ' +
        escape(a.motivoResolucao) + '</span></li>'
      : '<li class="pendente"><strong>Resolução:</strong> ' +
        (a.estado === "RECONHECIDO" ? "Aguardando resolução no Painel médico" : "Disponível após reconhecimento") + '</li>';
    return '<ol class="linha-tempo">' + abertura + reconhecimento + resolucao + '</ol>';
  }

  function acoesAlerta(a, p, trava) {
    if (movel || a.estado === "RESOLVIDO") return "";
    const repetir = online && p?.fase === "incerto" && podeNaTela(p.dados.acao) && permitido(a.estado, p.dados.acao)
      ? '<button data-id="' + escape(a.idAlerta) + '" data-acao="REPETIR">Reenviar mesmo comando</button>' : "";
    if (enfermagem) {
      const podeReconhecer = !trava && permitido(a.estado, "RECONHECER");
      const classe = a.estado === "ABERTO" ? ' class="principal"' : '';
      const info = a.estado === "RECONHECIDO" ? '<span class="aguardo">Reconhecido · aguardando resolução médica</span>' : "";
      return '<div class="botoes"><button' + classe + ' data-id="' + escape(a.idAlerta) +
        '" data-acao="RECONHECER"' + (podeReconhecer ? '' : ' disabled') + '>Reconhecer</button>' +
        repetir + info + '</div>';
    }
    if (a.estado === "ABERTO") {
      return '<div class="botoes"><button data-id="' + escape(a.idAlerta) +
        '" data-acao="RESOLVER" disabled>Registrar resolução</button>' +
        repetir + '<span class="aguardo">Aguardando reconhecimento da enfermagem</span></div>';
    }
    const st = statusLeituraResolucao(a);
    const podeResolver = !trava && st.ok;
    const classe = podeResolver ? ' class="principal"' : '';
    return '<div class="botoes"><button' + classe + ' data-id="' + escape(a.idAlerta) +
      '" data-acao="RESOLVER"' + (podeResolver ? '' : ' disabled') + '>Registrar resolução</button>' +
      repetir + '<span class="aguardo ' + (st.ok ? 'pronto-resolucao' : 'aviso-bloqueio') + '">' +
      escape(st.texto) + '</span></div>';
  }

  function cartaoAlerta(a) {
    const p = pedidos.get(a.idAlerta);
    const incerto = p?.fase === "incerto";
    const trava = !online || Boolean(p && (!incerto || permitido(a.estado, p.dados.acao)));
    const destaque = (enfermagem && a.estado === "ABERTO") ||
      (medico && a.estado === "RECONHECIDO" && statusLeituraResolucao(a).ok);
    const origem = textoMedida(a.tipoSinal, a.valorOrigem, a.sistolicaOrigem, a.diastolicaOrigem);
    return '<article class="alerta"' + (destaque ? ' data-destaque="sim"' : '') + '><div class="linha"><h3>' +
      escape(local(chave(a))) + ' · ' + escape(a.pacienteId) + ' — ' + sinais[a.tipoSinal][0] +
      '</h3><span class="tag">' + a.estado + ' · v' + a.versao + '</span></div><p>' + etiqueta("CRITICO") + ' ' +
      sinais[a.tipoSinal][0] + ' · <strong>' + origem + ' ' + escape(a.unidadeOrigem) + '</strong></p>' +
      '<p class="motivo-campo"><strong>Motivo da abertura (automático):</strong> ' + escape(a.motivoOrigem) + '</p>' +
      linhaTempoAlerta(a) +
      '<small>ID ' + escape(a.idAlerta) + '</small>' + acoesAlerta(a, p, trava) +
      (p ? '<small>' + ({ pendente: "Aguardando resposta…", estado: "Aguardando estado confirmado…", incerto: "Resultado incerto. Confira o estado." })[p.fase] + '</small>' : '') +
      '</article>';
  }

  function mostrarAlertas() {
    const lista = [...alertas.values()].filter(noEscopo);
    const prioridadeTela = a => {
      if (enfermagem) return a.estado === "ABERTO" ? 0 : 1;
      if (medico) {
        if (a.estado === "RECONHECIDO") return statusLeituraResolucao(a).ok ? 0 : 1;
        return 2;
      }
      return 0;
    };
    const ativos = lista.filter(a => a.estado !== "RESOLVIDO")
      .sort((a, b) => prioridadeTela(a) - prioridadeTela(b) || Date.parse(b.abertoEm) - Date.parse(a.abertoEm));
    const resolvidos = lista.filter(a => a.estado === "RESOLVIDO")
      .sort((a, b) => Date.parse(b.resolvidoEm || b.abertoEm) - Date.parse(a.resolvidoEm || a.abertoEm));
    el("quantidade").textContent = ativos.length;
    el("total-resolvidos").textContent = resolvidos.length;
    el("ativos").innerHTML = ativos.map(cartaoAlerta).join("") || '<p class="muted">Nenhum alerta ativo recebido.</p>';
    el("resolvidos").innerHTML = resolvidos.map(cartaoAlerta).join("") || '<p class="muted">Nenhum alerta resolvido recebido.</p>';
    if (medico) ultimaAssinaturaAlertasMedico = assinaturaAlertasMedico();
    if (edicao && el("confirmar")) {
      const alertaAtual = alertas.get(edicao.id);
      const st = edicao.acao === "RESOLVER" && alertaAtual ? statusLeituraResolucao(alertaAtual) : { ok: true, texto: "" };
      const pode = online && podeNaTela(edicao.acao) && permitido(alertaAtual?.estado, edicao.acao) && st.ok;
      el("confirmar").disabled = !pode;
      if (el("aviso-modal")) {
        const mostrarAviso = edicao.acao === "RESOLVER" && Boolean(alertaAtual) && !st.ok;
        el("aviso-modal").hidden = !mostrarAviso;
        el("aviso-modal").textContent = mostrarAviso ? st.texto : "";
      }
    }
  }

  function mostrarSimulador() {
    if (!el("simulador-demo")) return;
    el("sim-crise").disabled = !online;
    el("sim-rotina").disabled = !online;
    if (!estadosSimulador.size) {
      el("sim-estado").textContent = "Aguardando status do simulador (inicie o SimuladorMain).";
      return;
    }
    const emCrise = [];
    for (const [k] of leitos) {
      const st = estadosSimulador.get(k);
      if (!st) continue;
      const sinaisEmCrise = Object.entries(sinais)
        .filter(([tipo]) => st[tipo.toLowerCase()] === "CRISE")
        .map(([, [nome]]) => nome);
      if (sinaisEmCrise.length) emCrise.push(local(k) + " (" + st.pacienteId + "): " + sinaisEmCrise.join(", "));
    }
    el("sim-estado").textContent = emCrise.length
      ? "Em crise no simulador: " + emCrise.join(" | ")
      : "Simulador ativo: todos os cinco leitos em rotina.";
  }

  function concluir(id, texto) {
    clearTimeout(pedidos.get(id)?.timer);
    pedidos.delete(id);
    dizer(texto);
  }

  function receber(topico, d) {
    const partes = topico.split("/");
    if (partes[0] === "respostas") {
      const p = pedidos.get(d?.idAlerta);
      if (!p || p.dados.idComando !== d.idComando || topico !== 'respostas/alertas/' + d.idAlerta + '/' + d.idComando) return;
      if (d.resultado === "REJEITADO") concluir(d.idAlerta, 'Pedido rejeitado · ' + d.motivo + ' · ' + d.idAlerta);
      else if (d.resultado === "APLICADO") {
        p.versao = d.versao;
        if (alertas.get(d.idAlerta)?.versao >= d.versao) concluir(d.idAlerta, 'Comando aplicado · ' + d.idAlerta);
        else { p.fase = "estado"; dizer("Comando aplicado. Aguardando a publicação do estado."); }
      }
      mostrarAlertas(); return;
    }
    if (partes[0] === "simulador" && partes.length === 4 && partes[3] === "estado") {
      const k = partes[1] + "/" + partes[2];
      if (d === null) { estadosSimulador.delete(k); mostrarSimulador(); return; }
      if (chave(d) !== k || !["frequencia", "temperatura", "saturacao", "pressao"].every(s => cenarios.includes(d[s]))) return;
      estadosSimulador.set(k, d);
      mostrarSimulador(); return;
    }
    if (partes[0] === "alertas" && partes.length === 5 && partes[4] === "estado") {
      const id = partes[3];
      if (medico && partes[1] + '/' + partes[2] !== selecionado) return;
      if (d === null) { alertas.delete(id); mostrarAlertas(); mostrarLeituras(); return; }
      if (id !== d.idAlerta || chave(d) !== partes[1] + '/' + partes[2] || !Object.hasOwn(sinais, d.tipoSinal) ||
          !estados.includes(d.estado) || !Number.isInteger(d.versao) || d.versao < 1 || (alertas.get(id)?.versao || 0) >= d.versao) return;
      alertas.set(id, d);
      const p = pedidos.get(id);
      if (p?.versao && d.versao >= p.versao) concluir(id, 'Comando aplicado · ' + id);
      mostrarAlertas(); mostrarLeituras(); return;
    }
    if (!d || partes[0] !== "hospital" || !noEscopo(d) || !Object.hasOwn(sinais, d.tipoSinal) ||
        !niveis.includes(d.prioridade) || (movel && d.prioridade !== "CRITICO") || !Number.isFinite(Date.parse(d.instanteColeta)) ||
        topico !== 'hospital/' + chave(d) + '/' + d.tipoSinal.toLowerCase() + '/' + d.prioridade.toLowerCase() ||
        d.unidade !== sinais[d.tipoSinal][1] || !d.idLeitura ||
        !(d.tipoSinal === "PRESSAO" ? Number.isFinite(d.sistolica) && Number.isFinite(d.diastolica) : Number.isFinite(d.valor))) return;
    const k = chave(d) + "/" + d.tipoSinal, anterior = leituras.get(k);
    if (anterior && (d.idLeitura === anterior.idLeitura || Date.parse(d.instanteColeta) < Date.parse(anterior.instanteColeta))) return;
    leituras.set(k, d);
    if (movel) mostrarLeituras();
    if (medico) {
      leituraNovaDesdeRegistro = true;
      const entrouEmCritico = d.prioridade === "CRITICO" && (!anterior || anterior.prioridade !== "CRITICO");
      const topo = historicoCartoes[0];
      if (topo && Date.now() - topo.capturadoEm <= JANELA_COMPLETAR_MS && topo.sinais[d.tipoSinal] === null) {
        topo.sinais[d.tipoSinal] = snapshotLeitura(d);
        const maxGrav = Object.values(topo.sinais).filter(Boolean)
          .reduce((max, item) => Math.max(max, niveis.indexOf(item.prioridade)), -1);
        topo.prioridadeGeral = niveis[maxGrav] || "";
        if (entrouEmCritico) {
          topo.motivoTipo = "critico";
          const listaCrit = Array.isArray(topo.sinaisCriticos) ? topo.sinaisCriticos : [];
          if (!listaCrit.includes(d.tipoSinal)) listaCrit.push(d.tipoSinal);
          topo.sinaisCriticos = listaCrit;
        }
        leituraNovaDesdeRegistro = false;
        salvarHistorico();
        mostrarHistorico();
      } else if (entrouEmCritico) {
        registrarCartaoHistorico("critico", d.tipoSinal);
      } else if (historicoCartoes.length === 0 && Object.keys(sinais).every(t => leituras.has(selecionado + "/" + t))) {
        registrarCartaoHistorico("intervalo", null);
      }
      sincronizarAlertasMedico();
    }
  }

  const conexao = conectarPainel((texto, ok) => {
    online = ok; el("conexao").textContent = texto; el("conexao").dataset.online = ok ? "sim" : "nao";
    mostrarAlertas(); mostrarSimulador();
  }, () => {
    alertas.clear(); estadosSimulador.clear();
    mostrarAlertas(); mostrarLeituras(); mostrarSimulador();
  }, receber);

  function selecionar() {
    if (medico) {
      selecionado = el("leito").value;
      try {
        sessionStorage.setItem(CHAVE_LEITO_MEDICO, selecionado);
      } catch {}
    }
    if (el("sim-leito") && medico) el("sim-leito").value = selecionado;
    alertas.clear(); leituras.clear();
    historicoCartoes.length = 0;
    if (medico) historicoCartoes.push(...carregarHistorico(selecionado));
    ultimoRegistroHistoricoMs = historicoCartoes.length ? historicoCartoes[0].capturadoEm : 0;
    inicioSelecaoMs = Date.now();
    leituraNovaDesdeRegistro = false;
    ultimaAssinaturaAlertasMedico = "";
    const extras = movel ? {} : { "respostas/alertas/+/+": { qos: 1 }, "simulador/+/+/estado": { qos: 1 } };
    conexao.selecionar(medico ? 'hospital/' + selecionado + '/#' : movel ? 'hospital/+/+/+/critico' : 'hospital/#',
      medico ? 'alertas/' + selecionado + '/+/estado' : 'alertas/+/+/+/estado', extras);
    mostrarLeituras(); mostrarAlertas(); mostrarHistorico(false, false); mostrarSimulador();
  }

  function transmitir(p) {
    clearTimeout(p.timer);
    p.fase = "pendente";
    const id = p.dados.idAlerta;
    function incerto() {
      if (pedidos.get(id) !== p) return;
      p.fase = "incerto";
      dizer("Sem confirmação completa para " + id + ". Confira o estado antes de reenviar.");
      mostrarAlertas();
    }
    p.timer = setTimeout(incerto, 8000);
    conexao.enviar(p.dados, incerto);
    mostrarAlertas();
  }

  if (el("limpar-historico")) {
    el("limpar-historico").onclick = () => {
      historicoCartoes.length = 0;
      salvarHistorico();
      ultimoRegistroHistoricoMs = 0;
      inicioSelecaoMs = Date.now();
      leituraNovaDesdeRegistro = false;
      mostrarHistorico();
    };
  }

  if (el("simulador-demo")) {
    el("sim-leito").innerHTML = leitos.map(([k, p]) => '<option value="' + k + '">' + p + ' · ' + local(k) + '</option>').join("");
    el("sim-sinal").innerHTML = '<option value="TODOS">Todos os sinais</option>' +
      Object.entries(sinais).map(([tipo, [nome]]) => '<option value="' + tipo + '">' + nome + '</option>').join("");
    const dispararSimulador = cenario => {
      if (!online) return;
      const [ala, leito] = el("sim-leito").value.split("/");
      const sinal = el("sim-sinal").value;
      const comando = {
        idComando: novoId("sim-"),
        ala,
        leito,
        tipoSinal: sinal === "TODOS" ? null : sinal,
        cenario,
        instanteComando: new Date().toISOString()
      };
      conexao.enviarSimulador(comando, () => {
        el("sim-retorno").textContent = "Falha ao publicar comando para o simulador.";
      });
      el("sim-retorno").textContent = "Comando " + (cenario === "CRISE" ? "de crise" : "de recuperação (rotina)") +
        " enviado para " + local(ala + "/" + leito) + " (" + (sinal === "TODOS" ? "todos os sinais" : sinais[sinal][0]) + ").";
    };
    el("sim-crise").onclick = () => dispararSimulador("CRISE");
    el("sim-rotina").onclick = () => dispararSimulador("ROTINA");
  }

  if (el("dialogo")) {
    document.querySelector("main").addEventListener("click", e => {
      const b = e.target.closest("button[data-id]");
      if (!b || !online || b.disabled) return;
      const a = alertas.get(b.dataset.id), acao = b.dataset.acao;
      if (!a) return;
      if (acao === "REPETIR") {
        const p = pedidos.get(a.idAlerta);
        if (p?.fase === "incerto" && podeNaTela(p.dados.acao) && permitido(a.estado, p.dados.acao)) transmitir(p);
        return;
      }
      if (!podeNaTela(acao) || !permitido(a.estado, acao)) return;
      if (acao === "RESOLVER" && !statusLeituraResolucao(a).ok) return;
      edicao = { id: a.idAlerta, acao };
      el("acao-titulo").textContent = acao === "RESOLVER" ? "Registrar resolução" : "Reconhecer alerta";
      el("contexto").textContent = local(chave(a)) + " · " + a.pacienteId + " · " + sinais[a.tipoSinal][0];
      if (el("aviso-modal")) { el("aviso-modal").hidden = true; el("aviso-modal").textContent = ""; }
      el("motivo").value = "";
      el("motivo").placeholder = acao === "RESOLVER" ? placeholderResolucao(a) : "";
      el("motivo").required = acao === "RESOLVER";
      el("grupo-motivo").hidden = acao !== "RESOLVER";
      el("confirmar").disabled = false;
      el("dialogo").showModal();
      el("usuario").focus();
    });
    el("cancelar").onclick = () => el("dialogo").close();
    el("dialogo").onclose = () => { edicao = null; };
    el("formulario").onsubmit = e => {
      e.preventDefault();
      const atual = edicao ? alertas.get(edicao.id) : null;
      if (!edicao || !online || !podeNaTela(edicao.acao) || !permitido(atual?.estado, edicao.acao)) return;
      if (edicao.acao === "RESOLVER" && !statusLeituraResolucao(atual).ok) {
        mostrarAlertas();
        return;
      }
      const usuarioId = el("usuario").value.trim(), motivo = el("motivo").value.trim();
      if (!usuarioId || (edicao.acao === "RESOLVER" && !motivo)) {
        const campo = el(!usuarioId ? "usuario" : "motivo");
        campo.setCustomValidity("Preencha este campo com texto.");
        campo.reportValidity();
        campo.setCustomValidity("");
        return;
      }
      const idComando = novoId("cmd-");
      const dados = { idAlerta: edicao.id, idComando, acao: edicao.acao, usuarioId, instanteComando: new Date().toISOString() };
      if (dados.acao === "RESOLVER") dados.motivoResolucao = motivo;
      const p = { dados, fase: "pendente" };
      clearTimeout(pedidos.get(dados.idAlerta)?.timer);
      pedidos.set(dados.idAlerta, p);
      el("dialogo").close();
      dizer("Comando enviado · " + dados.idComando);
      transmitir(p);
    };
  }

  if (medico) {
    el("leito").innerHTML = leitos.map(([k, p]) => '<option value="' + k + '">' + p + ' · ' + local(k) + '</option>').join("");
    try {
      const salvo = sessionStorage.getItem(CHAVE_LEITO_MEDICO);
      if (salvo && leitos.some(([k]) => k === salvo)) {
        el("leito").value = salvo;
      }
    } catch {}
    el("leito").onchange = selecionar;
  }
  selecionar();
  setInterval(() => {
    mostrarLeituras();
    if (medico) {
      sincronizarAlertasMedico();
      if (leituraNovaDesdeRegistro && Date.now() - inicioSelecaoMs >= 2000) {
        const prontoInicial = ultimoRegistroHistoricoMs === 0;
        const venceuIntervalo = ultimoRegistroHistoricoMs > 0 && Date.now() - ultimoRegistroHistoricoMs >= INTERVALO_HISTORICO_MS;
        if (prontoInicial || venceuIntervalo) {
          registrarCartaoHistorico("intervalo", null);
        }
      }
    }
  }, 1000);
}
