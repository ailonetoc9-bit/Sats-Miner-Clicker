// Use os dados da sessão autenticada do Telegram; o modo local usa o jogador de teste do backend.
const TELEGRAM_USER = window.Telegram?.WebApp?.initDataUnsafe?.user || {};
const TELEGRAM_ID = String(TELEGRAM_USER.id || "");
const NOME = [TELEGRAM_USER.first_name, TELEGRAM_USER.last_name].filter(Boolean).join(" ") || "Jogador";
const TELEGRAM_INIT_DATA = window.Telegram?.WebApp?.initData || "";
const API_BASE = window.GAME_API_BASE || (location.protocol === "file:" ? "http://localhost:8080/api/game" : `${location.origin}/api/game`);
let cliquesAcumulados = 0;
let cliquesEmEnvio = 0;
let moedasServidor = 0;
let ultimaConfirmacaoMs = Date.now();
let sincronizando = false;
let toastTimer;
let jogadorAtual = null;
let contractorsRenderKey = null;
let taskRenderKey = null;
let monetagReady = null;
let anunciosHabilitados = false;
let anuncioEmAndamento = false;
let zonaMonetag = "";
let interstitialAutomaticoConfigurado = false;
let erroInicializacaoAnuncios = null;

const INTERVALO_INTERSTITIAL_SEGUNDOS = 5 * 60;
const MAX_INTERSTITIALS_POR_HORA = 12;
const PASSOS_TUTORIAL = [
  { titulo: "Bem-vindo ao Miner Club!", texto: "Aqui você evolui sua garagem, monta uma equipe e cumpre tarefas para acumular GRAM beta. Nesta fase, esse saldo é apenas de teste: ainda não está na blockchain TON e não pode ser transferido ou sacado.", alvo: null },
  { titulo: "Comece ganhando moedas", texto: "Cada clique no cenário rende 1 moeda no início. Seu saldo fica no alto da tela; você também pode aumentá-lo com upgrades e renda passiva.", alvo: "#palco-jogo" },
  { titulo: "Evolua seu setup", texto: "Use moedas para comprar upgrades como teclado, mesa, monitor e placa de vídeo. Cada melhoria aumenta o ganho por clique; o preço e o bônus aparecem nesta seção.", aba: "loja", alvo: "#painel-loja" },
  { titulo: "Monte sua equipe", texto: "Contrate profissionais e compre novos níveis para aumentar a renda passiva. Eles produzem moedas enquanto você joga e também enquanto está fora; ao voltar, os ganhos offline são contabilizados.", aba: "equipe", alvo: "#painel-equipe" },
  { titulo: "Complete tarefas e ganhe GRAM", texto: "Você verá uma tarefa por vez. Cumpra metas de cliques, moedas, renda passiva, upgrades ou níveis de contratados e resgate de 2 a 8 GRAM beta. As tarefas se repetem com metas variadas. Nesta beta, o saldo ainda não é um token na blockchain TON e não pode ser transferido ou sacado.", aba: "tarefas", alvo: "#painel-tarefas" },
  { titulo: "Acompanhe seu GRAM beta", texto: "O saldo GRAM beta fica acumulado no seu perfil para testar o progresso do jogo. A integração com carteira TON e os saques ainda não estão habilitados.", aba: "pontos", alvo: "#painel-pontos" },
  { titulo: "Mantenha o PC resfriado", texto: "A cada 10 minutos jogando, o PC superaquece e a renda passiva cai para 10%. Toque em Resfriar e assista ao anúncio para voltar à renda normal. Os ganhos offline são calculados quando você retorna.", aba: "loja", alvo: "#cooling-card", posicao: "topo" },
  { titulo: "Anúncios e dobro de ganhos", texto: "Se quiser, assista a um anúncio no botão Ganhe em dobro para duplicar moedas por 10 minutos. Além dos anúncios solicitados pelos botões, a Monetag pode exibir um anúncio automático a cada 5 minutos enquanto o Mini App estiver aberto no Telegram.", aba: "loja", alvo: ["#bonus-card", "#cooling-card"], posicao: "topo" },
  { titulo: "Pronto para começar!", texto: "Agora é com você: clique para ganhar moedas, invista na garagem, fortaleça a equipe e conclua tarefas para acumular GRAM beta. Se quiser rever este guia, toque em Ajuda no topo da tela.", alvo: null }
];
let passoTutorialAtual = 0;

const $ = (id) => document.getElementById(id);
const elMoedas = $("moedas");
const palco = $("palco-jogo");

function mostrarToast(mensagem) {
  const toast = $("toast");
  toast.textContent = mensagem;
  toast.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("show"), 2600);
}

function formatar(valor) {
  const numero = Math.max(0, Number(valor || 0));
  if (numero < 1000) return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 }).format(Math.floor(numero));
  const sufixos = ["", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg"];
  const grupo = Math.floor(Math.log10(numero) / 3);
  const divisor = 1000 ** grupo;
  const compacto = Math.floor((numero / divisor) * 10) / 10;
  const valorCompacto = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(compacto);
  return `${valorCompacto}${sufixos[grupo] || `e${grupo * 3}`}`;
}

async function requisitar(caminho, params = {}) {
  const parametros = { ...params };
  if (TELEGRAM_INIT_DATA) {
    delete parametros.telegramId;
    delete parametros.nome;
  }
  const query = new URLSearchParams(parametros);
  const headers = TELEGRAM_INIT_DATA ? { "X-Telegram-Init-Data": TELEGRAM_INIT_DATA } : {};
  const resposta = await fetch(`${API_BASE}${caminho}?${query}`, { method: "POST", headers });
  const dados = await resposta.json().catch(() => ({}));
  if (!resposta.ok) {
    const mensagem = dados.message || dados.error || dados.detail;
    if (resposta.status === 401 && /expirad/i.test(mensagem || "")) {
      throw new Error("Sessão do Telegram expirada. Feche e abra o jogo novamente.");
    }
    throw new Error(`Servidor do jogo (HTTP ${resposta.status}): ${mensagem || "não foi possível concluir."}`);
  }
  return dados;
}

async function requisitarLeitura(caminho) {
  const headers = TELEGRAM_INIT_DATA ? { "X-Telegram-Init-Data": TELEGRAM_INIT_DATA } : {};
  const resposta = await fetch(`${API_BASE}${caminho}`, { headers });
  const dados = await resposta.json().catch(() => ({}));
  if (!resposta.ok) throw new Error(`Servidor do jogo (HTTP ${resposta.status}): ${dados.message || "não foi possível consultar."}`);
  return dados;
}

async function prepararAnuncios() {
  try {
    const config = await requisitarLeitura("/ads/config");
    anunciosHabilitados = Boolean(config.enabled);
    zonaMonetag = String(config.zoneId || "");
    if (!anunciosHabilitados) return;
    monetagReady = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      const timeoutInicializacao = setTimeout(() => reject(new Error("O provedor demorou para responder.")), 15000);
      script.src = config.sdkUrl;
      script.dataset.zone = zonaMonetag;
      script.dataset.sdk = `show_${zonaMonetag}`;
      script.async = true;
      script.onload = () => {
        clearTimeout(timeoutInicializacao);
        if (typeof window[`show_${zonaMonetag}`] !== "function") {
          reject(new Error("O provedor de anúncios não inicializou. Verifique se o navegador está bloqueando o SDK e tente novamente."));
          return;
        }
        resolve();
      };
      script.onerror = () => {
        clearTimeout(timeoutInicializacao);
        reject(new Error("O navegador não conseguiu carregar o provedor de anúncios."));
      };
      document.head.appendChild(script);
    });
    await monetagReady;
    configurarInterstitialAutomatico();
  } catch (erro) {
    anunciosHabilitados = false;
    erroInicializacaoAnuncios = erro;
    console.warn("Anúncios ainda não disponíveis:", erro);
  }
}

function mensagemFalhaAnuncio(erro) {
  const mensagem = String(erro?.message || erro || "");
  if (/empty feed|ad is not ready|sem anúncio|no ad/i.test(mensagem)) {
    return "Não há anúncio disponível agora. Tente novamente mais tarde.";
  }
  if (/timeout|demorou para responder/i.test(mensagem)) {
    return "O anúncio demorou para carregar. Confira a conexão e tente novamente.";
  }
  if (/network|failed to fetch|conexão|carregar o provedor/i.test(mensagem)) {
    return "Não foi possível carregar o anúncio. Confira a conexão e tente novamente.";
  }
  return mensagem || "O anúncio não está disponível agora. Tente novamente mais tarde.";
}

function configurarInterstitialAutomatico() {
  if (interstitialAutomaticoConfigurado || !TELEGRAM_INIT_DATA || !anunciosHabilitados) return;
  const mostrar = window[`show_${zonaMonetag}`];
  if (typeof mostrar !== "function") return;

  interstitialAutomaticoConfigurado = true;
  try {
    Promise.resolve(mostrar({
      type: "inApp",
      inAppSettings: {
        frequency: MAX_INTERSTITIALS_POR_HORA,
        capping: 1,
        interval: INTERVALO_INTERSTITIAL_SEGUNDOS,
        timeout: INTERVALO_INTERSTITIAL_SEGUNDOS,
        everyPage: false
      }
    })).catch((erro) => {
      interstitialAutomaticoConfigurado = false;
      console.warn("Interstitial automático da Monetag não foi configurado:", erro);
    });
  } catch (erro) {
    interstitialAutomaticoConfigurado = false;
    console.warn("Interstitial automático da Monetag não foi configurado:", erro);
  }
}

async function assistirAnuncio(tipo, posicionamento, botaoId, mensagemSucesso) {
  if (anuncioEmAndamento) return;
  if (!anunciosHabilitados || !monetagReady) {
    mostrarToast(erroInicializacaoAnuncios ? mensagemFalhaAnuncio(erroInicializacaoAnuncios) : "Os anúncios estão indisponíveis agora. Tente novamente em instantes.");
    return;
  }
  const botao = $(botaoId);
  anuncioEmAndamento = true;
  if (botao) {
    botao.disabled = true;
    botao.dataset.textoOriginal = botao.textContent;
    botao.textContent = "AGUARDE";
  }
  try {
    await monetagReady;
    const sessao = await requisitar("/ads/sessao", { telegramId: TELEGRAM_ID, tipo });
    const mostrar = window[`show_${zonaMonetag}`];
    if (typeof mostrar !== "function") throw new Error("O SDK de anúncios não foi carregado.");

    let resultadoSdk;
    try {
      resultadoSdk = await mostrar({ type: "end", ymid: sessao.sessionId, requestVar: posicionamento, timeout: 20 });
    } catch (erroSdk) {
      throw new Error(mensagemFalhaAnuncio(erroSdk));
    }
    if (resultadoSdk?.reward_event_type !== "valued") {
      mostrarToast("O anúncio não gerou uma visualização válida. Nenhum prêmio foi liberado.");
      return;
    }

    // A resposta do navegador serve para a interface; a recompensa só vale após o postback do Monetag.
    mostrarToast("Anúncio confirmado. Validando o prêmio...");
    for (let tentativa = 0; tentativa < 30; tentativa++) {
      const estado = await requisitarLeitura(`/ads/sessao/${encodeURIComponent(sessao.sessionId)}`);
      if (estado.status === "REWARDED") {
        atualizarEcra(await requisitar("/login", { telegramId: TELEGRAM_ID, nome: NOME }));
        mostrarToast(mensagemSucesso);
        return;
      }
      if (estado.status === "NON_VALUED" || estado.status === "EXPIRED") {
        mostrarToast("O anúncio não gerou uma recompensa válida.");
        return;
      }
      await new Promise((resolve) => setTimeout(resolve, 1000));
    }
    mostrarToast("Anúncio confirmado. O servidor ainda está validando o prêmio; aguarde alguns instantes.");
  } catch (erro) {
    console.warn("Falha ao assistir anúncio:", erro);
    mostrarToast(mensagemFalhaAnuncio(erro));
  } finally {
    anuncioEmAndamento = false;
    if (botao) {
      botao.disabled = false;
      botao.textContent = botao.dataset.textoOriginal || botao.textContent;
    }
  }
}

async function iniciarJogo() {
  window.Telegram?.WebApp?.ready();
  window.Telegram?.WebApp?.expand();
  try {
    atualizarEcra(await requisitar("/login", { telegramId: TELEGRAM_ID, nome: NOME }));
    $("statusMessage").textContent = "Garagem conectada · progresso sincronizado";
    if (!tutorialJaVisto()) setTimeout(() => abrirTutorial(0), 450);
    prepararAnuncios();
  } catch (erro) {
    $("statusMessage").textContent = erro.message || "Não foi possível conectar ao jogo.";
    console.error("Erro ao ligar ao servidor:", erro);
  }
}

function clicarPC(evento) {
  cliquesAcumulados++;
  const poder = Number(jogadorAtual?.poderDeMineracaoPorClique || 1);
  const ganho = poder * (jogadorAtual?.bonusDobroAtivo ? 2 : 1);
  renderizarSaldoPrevisto();
  const bounds = palco.getBoundingClientRect();
  const fx = document.createElement("span");
  fx.className = "float-coin";
  fx.textContent = `+${formatar(ganho)} 🟡`;
  fx.style.left = `${Math.max(14, Math.min((evento?.clientX || bounds.left + bounds.width / 2) - bounds.left, bounds.width - 60))}px`;
  fx.style.top = `${Math.max(16, (evento?.clientY || bounds.top + bounds.height / 2) - bounds.top)}px`;
  $("floating-layer").appendChild(fx);
  setTimeout(() => fx.remove(), 850);
}

async function sincronizarComServidor() {
  while (sincronizando) await new Promise((resolve) => setTimeout(resolve, 25));
  if (!jogadorAtual) return;
  sincronizando = true;
  const cliquesParaEnviar = cliquesAcumulados;
  cliquesAcumulados = 0;
  cliquesEmEnvio = cliquesParaEnviar;
  try {
    const dados = await requisitar("/sync", { telegramId: TELEGRAM_ID, cliques: cliquesParaEnviar });
    cliquesEmEnvio = 0;
    atualizarEcra(dados);
  } catch (erro) {
    cliquesEmEnvio = 0;
    cliquesAcumulados += cliquesParaEnviar;
    console.warn("Erro ao sincronizar:", erro);
    renderizarSaldoPrevisto();
  } finally { sincronizando = false; }
}

async function comprarUpgrade() {
  try { await sincronizarComServidor(); atualizarEcra(await requisitar("/comprar", { telegramId: TELEGRAM_ID })); mostrarToast("Upgrade de setup comprado!"); }
  catch (erro) { await sincronizarComServidor(); mostrarToast(erro.message || "Moedas insuficientes para esse upgrade."); }
}

async function comprarContratado(id) {
  try { await sincronizarComServidor(); atualizarEcra(await requisitar("/contratados/comprar", { telegramId: TELEGRAM_ID, contratadoId: id })); mostrarToast("Novo nível de contratado comprado!"); }
  catch (erro) { await sincronizarComServidor(); mostrarToast(erro.message); }
}

async function resgatarTarefa(id) {
  try { atualizarEcra(await requisitar("/tarefas/resgatar", { telegramId: TELEGRAM_ID, tarefaId: id })); mostrarToast("GRAM beta adicionado ao seu saldo!"); }
  catch (erro) { mostrarToast(erro.message); }
}

async function verAnuncioDobrar() {
  return assistirAnuncio("dobrar", "boost_x2", "botaoAnuncioDobrar", "Dobro de ganhos ativado por 10 minutos!");
}

async function verAnuncioResfriar() {
  return assistirAnuncio("resfriar", "cool_pc", "botaoAnuncioResfriar", "Anúncio assistido, prêmio resgatado. PC resfriado!");
}

function renderizarContratados(contratados = []) {
  const chaveRenderizacao = JSON.stringify(contratados);
  if (chaveRenderizacao === contractorsRenderKey) return;
  contractorsRenderKey = chaveRenderizacao;
  if (!Array.isArray(contratados) || contratados.length === 0) {
    $("contractor-list").innerHTML = "<p class='empty-state'>A equipe não carregou. Confirme a conexão e reinicie o backend atualizado.</p>";
    return;
  }
  $("contractor-list").innerHTML = contratados.map((item) => {
    const marco = item.proximoMarco ? `Próximo dobro no nível ${item.proximoMarco}` : "Todos os marcos iniciais alcançados";
    return `<article class="contractor-card"><span class="contractor-art">👩‍💻</span><div class="contractor-copy"><strong>${item.nome}</strong><small>Nível ${item.nivel} · +${formatar(item.rendaProximoNivel)} moedas/s no próximo nível</small><em>${marco}</em></div><button type="button" class="hire-button" onclick="comprarContratado('${item.id}')">🟡 ${formatar(item.custo)}<small>COMPRAR NÍVEL</small></button></article>`;
  }).join("");
}

function renderizarTarefas(tarefas = []) { const itemAtual = tarefas.find((item)=>!item.resgatada); const chave = itemAtual ? JSON.stringify([itemAtual.id,itemAtual.nome,itemAtual.descricao,itemAtual.recompensaPontos,itemAtual.objetivo,itemAtual.pronta]) : (tarefas.length ? "complete" : "empty"); if (chave === window.__taskRenderKey) { if (itemAtual) { const progresso = `${formatar(itemAtual.progresso)} / ${formatar(itemAtual.objetivo)}`; const texto = $("task-list").querySelector(".task-bottom > span:first-child"); if (texto) texto.textContent = progresso; const barra = $("task-list").querySelector(".task-track i"); if (barra) barra.style.width = `${Math.min(100,(itemAtual.progresso/itemAtual.objetivo)*100)}%`; } return; } window.__taskRenderKey = chave;
  if (!Array.isArray(tarefas) || tarefas.length === 0) {
    if (taskRenderKey !== "empty") {
      $("task-list").innerHTML = "<p class='empty-state'>As tarefas não carregaram. Reinicie o backend atualizado.</p>";
      taskRenderKey = "empty";
    }
    return;
  }
  const atual = tarefas.find((item) => !item.resgatada);
  if (!atual) {
    if (taskRenderKey !== "complete") {
      $("task-list").innerHTML = "<p class='empty-state'>Você concluiu todas as tarefas de hoje. Novas tarefas chegam amanhã.</p>";
      taskRenderKey = "complete";
    }
    return;
  }
  const tarefa = atual;
  const chaveRenderizacao = JSON.stringify([
    tarefa.id, tarefa.nome, tarefa.descricao, tarefa.recompensaPontos,
    tarefa.objetivo, tarefa.pronta
  ]);
  const progresso = `${formatar(tarefa.progresso)} / ${formatar(tarefa.objetivo)}`;
  if (chaveRenderizacao !== taskRenderKey) {
    const acao = tarefa.pronta
      ? `<button class="task-button ready" onclick="resgatarTarefa('${tarefa.id}')">RESGATAR +${tarefa.recompensaPontos} GRAM</button>`
      : `<span class="task-progress">${progresso}</span>`;
    $("task-list").innerHTML = `<article class="task-card"><div class="task-top"><div><strong>${tarefa.nome}</strong><small>${tarefa.descricao}</small></div><b class="gram-reward">G ${tarefa.recompensaPontos} GRAM</b></div><div class="task-track"><i></i></div><div class="task-bottom"><span class="task-progress-value"></span><span class="task-action">${acao}</span></div></article>`;
    taskRenderKey = chaveRenderizacao;
  }
  $("task-list").querySelector(".task-progress-value").textContent = progresso;
  const percentual = Math.min(100, (tarefa.progresso / tarefa.objetivo) * 100);
  $("task-list").querySelector(".task-track i").style.width = `${percentual}%`;
}

function atualizarEcra(dados) {
  jogadorAtual = dados;
  moedasServidor = Number(dados.moedasVirtuais || 0);
  ultimaConfirmacaoMs = Date.now();
  renderizarSaldoPrevisto();
  $("pontosBeta").textContent = formatar(dados.pontosBeta);
  $("pontosBetaDetalhe").textContent = formatar(dados.pontosBeta);
  $("poderClique").textContent = formatar(dados.poderDeMineracaoPorClique);
  $("rendaPassiva").textContent = formatar(dados.rendaPassivaPorSegundo);
  $("custoUpgrade").textContent = formatar(dados.custoProximoUpgrade);
  $("nomeUpgrade").textContent = dados.nomeProximoUpgrade;
  $("descricaoUpgrade").textContent = dados.descricaoProximoUpgrade;
  $("bonusUpgrade").textContent = formatar(dados.bonusProximoUpgrade);
  $("iconeUpgrade").textContent = dados.iconeProximoUpgrade;
  $("boostTitle").textContent = dados.bonusDobroAtivo ? "Dobro de ganhos ativo" : "Ganhe em dobro";
  $("boostDescription").textContent = dados.bonusDobroAtivo ? "Seu anúncio está valendo · bônus por 10 min." : "Anúncio ativa o dobro de ganhos por 10 min.";
  const statusSala = $("roomStatus");
  statusSala.lastChild.textContent = dados.pcAquecido ? " PC SUPERAQUECIDO" : " EM ATIVIDADE";
  statusSala.classList.toggle("hot", Boolean(dados.pcAquecido));
  $("coolingTitle").textContent = dados.pcAquecido ? "PC esquentou" : "PC resfriado";
  $("coolingDescription").textContent = dados.pcAquecido ? "Resfrie para recuperar a renda passiva." : "Resfrie a cada 10 min para manter a renda passiva.";
  renderizarContratados(dados.contratados);
  renderizarTarefas(dados.tarefas);
}

function mostrarMoedas(valor) {
  elMoedas.dataset.raw = String(valor);
  elMoedas.textContent = formatar(valor);
}

function renderizarSaldoPrevisto() {
  const poder = Number(jogadorAtual?.poderDeMineracaoPorClique || 1);
  const multiplicador = jogadorAtual?.bonusDobroAtivo ? 2 : 1;
  const cliquesPendentes = cliquesAcumulados + cliquesEmEnvio;
  const segundosDesdeConfirmacao = jogadorAtual ? (Date.now() - ultimaConfirmacaoMs) / 1000 : 0;
  const calor = jogadorAtual?.pcAquecido ? 0.1 : 1;
  const moedasPassivasPrevistas = segundosDesdeConfirmacao * Number(jogadorAtual?.rendaPassivaPorSegundo || 0) * calor * multiplicador;
  mostrarMoedas(moedasServidor + cliquesPendentes * poder * multiplicador + moedasPassivasPrevistas);
}

function chaveTutorial() {
  const usuarioId = window.Telegram?.WebApp?.initDataUnsafe?.user?.id || "visitante";
  return `miner-club-tutorial-v3-${usuarioId}`;
}

function tutorialJaVisto() {
  try { return localStorage.getItem(chaveTutorial()) === "sim"; }
  catch { return false; }
}

function marcarTutorialComoVisto() {
  try { localStorage.setItem(chaveTutorial(), "sim"); }
  catch { /* O tutorial continua disponível pelo botão Ajuda nesta sessão. */ }
}

function selecionarAba(id) {
  document.querySelectorAll(".tab").forEach((tab) => tab.classList.toggle("active", tab.dataset.tab === id));
  ["loja", "equipe", "tarefas", "pontos"].forEach((painel) => $("painel-" + painel).classList.toggle("hidden", painel !== id));
}

function atualizarPassoTutorial() {
  const passo = PASSOS_TUTORIAL[passoTutorialAtual];
  const overlay = $("tutorialOverlay");
  document.querySelectorAll(".tutorial-target").forEach((elemento) => elemento.classList.remove("tutorial-target"));
  if (passo.aba) selecionarAba(passo.aba);
  $("tutorialStepCount").textContent = `ETAPA ${passoTutorialAtual + 1} DE ${PASSOS_TUTORIAL.length}`;
  $("tutorialProgress").style.width = `${((passoTutorialAtual + 1) / PASSOS_TUTORIAL.length) * 100}%`;
  $("tutorialTitle").textContent = passo.titulo;
  $("tutorialText").textContent = passo.texto;
  $("tutorialBack").disabled = passoTutorialAtual === 0;
  $("tutorialNext").textContent = passoTutorialAtual === PASSOS_TUTORIAL.length - 1 ? "Começar a jogar" : "Continuar";
  const alvos = Array.isArray(passo.alvo) ? passo.alvo : (passo.alvo ? [passo.alvo] : []);
  alvos.forEach((seletor) => document.querySelector(seletor)?.classList.add("tutorial-target"));
  const primeiroAlvo = alvos.length ? document.querySelector(alvos[0]) : null;
  overlay.classList.toggle("tutorial-top", passo.posicao === "topo");
  if (primeiroAlvo) {
    primeiroAlvo.scrollIntoView({ behavior: "smooth", block: "nearest" });
    requestAnimationFrame(() => {
      const centroAlvo = primeiroAlvo.getBoundingClientRect().top + primeiroAlvo.getBoundingClientRect().height / 2;
      const mostrarNoTopo = passo.posicao === "topo" || (passo.posicao !== "baixo" && centroAlvo > window.innerHeight / 2);
      overlay.classList.toggle("tutorial-top", mostrarNoTopo);
    });
  }
}

function abrirTutorial(indice = 0) {
  passoTutorialAtual = Math.max(0, Math.min(indice, PASSOS_TUTORIAL.length - 1));
  $("tutorialOverlay").classList.remove("hidden");
  $("tutorialOverlay").setAttribute("aria-hidden", "false");
  $("game-container").classList.add("tutorial-active");
  atualizarPassoTutorial();
  $("tutorialNext").focus();
}

function fecharTutorial() {
  const overlay = $("tutorialOverlay");
  overlay.classList.add("hidden");
  overlay.setAttribute("aria-hidden", "true");
  $("game-container").classList.remove("tutorial-active");
  document.querySelectorAll(".tutorial-target").forEach((elemento) => elemento.classList.remove("tutorial-target"));
  marcarTutorialComoVisto();
  $("botaoTutorial").focus();
}

function avancarTutorial(direcao) {
  const proximo = passoTutorialAtual + direcao;
  if (proximo < 0) return;
  if (proximo >= PASSOS_TUTORIAL.length) return fecharTutorial();
  passoTutorialAtual = proximo;
  atualizarPassoTutorial();
}

palco.addEventListener("click", clicarPC);
document.querySelectorAll(".tab").forEach((tab) => tab.addEventListener("click", () => selecionarAba(tab.dataset.tab)));
$("botaoTutorial").addEventListener("click", () => abrirTutorial());
$("tutorialNext").addEventListener("click", () => avancarTutorial(1));
$("tutorialBack").addEventListener("click", () => avancarTutorial(-1));
$("tutorialSkip").addEventListener("click", fecharTutorial);
document.addEventListener("keydown", (evento) => {
  if (evento.key === "Escape" && !$("tutorialOverlay").classList.contains("hidden")) fecharTutorial();
});
iniciarJogo();
setInterval(sincronizarComServidor, 1000);
setInterval(() => { if (jogadorAtual) renderizarSaldoPrevisto(); }, 100);
