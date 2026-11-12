// Identificação temporária. O Telegram poderá fornecer esses dados ao abrir o jogo.
const TELEGRAM_ID = "123456789";
const NOME = "JoseAilo";
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

const INTERVALO_INTERSTITIAL_SEGUNDOS = 5 * 60;
const MAX_INTERSTITIALS_POR_HORA = 12;

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
      script.src = config.sdkUrl;
      script.dataset.zone = zonaMonetag;
      script.dataset.sdk = `show_${zonaMonetag}`;
      script.async = true;
      script.onload = () => {
        if (typeof window[`show_${zonaMonetag}`] !== "function") {
          reject(new Error("O provedor de anúncios não inicializou. Verifique se o navegador está bloqueando o SDK e tente novamente."));
          return;
        }
        resolve();
      };
      script.onerror = () => reject(new Error("O navegador não conseguiu carregar o provedor de anúncios. Verifique o bloqueador de conteúdo e tente novamente."));
      document.head.appendChild(script);
    });
    await monetagReady;
    configurarInterstitialAutomatico();
  } catch (erro) {
    anunciosHabilitados = false;
    console.warn("Anúncios ainda não disponíveis:", erro);
  }
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
    mostrarToast("Os anúncios ainda estão sendo configurados.");
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
      resultadoSdk = await mostrar({ ymid: sessao.sessionId, requestVar: posicionamento });
    } catch (erroSdk) {
      throw new Error(`Monetag: ${erroSdk?.message || "não foi possível abrir o anúncio."}`);
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
    mostrarToast(erro.message || "O anúncio não está disponível agora.");
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
  try { atualizarEcra(await requisitar("/tarefas/resgatar", { telegramId: TELEGRAM_ID, tarefaId: id })); mostrarToast("Satoshis adicionados ao seu saldo!"); }
  catch (erro) { mostrarToast(erro.message); }
}

async function verAnuncioDobrar() {
  return assistirAnuncio("dobrar", "boost_x2", "botaoAnuncioDobrar", "Dobro de ganhos ativado por 10 minutos!");
}

async function verAnuncioResfriar() {
  return assistirAnuncio("resfriar", "cool_pc", "botaoAnuncioResfriar", "Anúncio assistido, prêmio resgatado. PC resfriado!");
}

async function verAnuncioSaque() {
  return assistirAnuncio("saque", "withdrawal_token", "botaoAnuncioSaque", "Anúncio assistido, prêmio resgatado. Token de saque liberado!");
}

async function sacarSatoshis(valor) {
  const quantia = Number(valor ?? $("valorSaque").value);
  if (!Number.isInteger(quantia) || quantia < 1) return mostrarToast("Informe uma quantidade válida de sats.");
  try { atualizarEcra(await requisitar("/sacar", { telegramId: TELEGRAM_ID, valorSatoshis: quantia })); mostrarToast(`${quantia} sats retirados (saque fictício).`); }
  catch (erro) { mostrarToast(erro.message || "Confira os sats liberados e seu token de saque."); }
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

function renderizarTarefas(tarefas = []) { const itemAtual = tarefas.find((item)=>!item.resgatada); const chave = itemAtual ? JSON.stringify([itemAtual.id,itemAtual.nome,itemAtual.descricao,itemAtual.recompensaSatoshis,itemAtual.objetivo,itemAtual.pronta]) : (tarefas.length ? "complete" : "empty"); if (chave === window.__taskRenderKey) { if (itemAtual) { const progresso = `${formatar(itemAtual.progresso)} / ${formatar(itemAtual.objetivo)}`; const texto = $("task-list").querySelector(".task-bottom > span:first-child"); if (texto) texto.textContent = progresso; const barra = $("task-list").querySelector(".task-track i"); if (barra) barra.style.width = `${Math.min(100,(itemAtual.progresso/itemAtual.objetivo)*100)}%`; } return; } window.__taskRenderKey = chave;
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
    tarefa.id, tarefa.nome, tarefa.descricao, tarefa.recompensaSatoshis,
    tarefa.objetivo, tarefa.pronta
  ]);
  const progresso = `${formatar(tarefa.progresso)} / ${formatar(tarefa.objetivo)}`;
  if (chaveRenderizacao !== taskRenderKey) {
    const acao = tarefa.pronta
      ? `<button class="task-button ready" onclick="resgatarTarefa('${tarefa.id}')">RESGATAR +${tarefa.recompensaSatoshis} SATS</button>`
      : `<span class="task-progress">${progresso}</span>`;
    $("task-list").innerHTML = `<article class="task-card"><div class="task-top"><div><strong>${tarefa.nome}</strong><small>${tarefa.descricao}</small></div><b>◈ ${tarefa.recompensaSatoshis} sats</b></div><div class="task-track"><i></i></div><div class="task-bottom"><span class="task-progress-value"></span><span class="task-action">${acao}</span></div></article>`;
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
  $("satoshisAcumulados").textContent = formatar(dados.saldoSatoshisAcumulados);
  $("satoshisLiberados").textContent = formatar(dados.satoshisLiberadosHoje);
  $("poderClique").textContent = formatar(dados.poderDeMineracaoPorClique);
  $("rendaPassiva").textContent = formatar(dados.rendaPassivaPorSegundo);
  $("tokensSaque").textContent = formatar(dados.tokensDeSaque);
  const liberado = Math.max(0, (dados.satoshisLiberadosHoje || 0) - (dados.satoshisSacadosHoje || 0));
  const disponivel = Math.min(dados.saldoSatoshisAcumulados || 0, liberado);
  $("saldoCarteira").textContent = formatar(disponivel);
  $("valorSaque").max = String(disponivel);
  if (Number($("valorSaque").value) > disponivel) $("valorSaque").value = String(disponivel || 1);
  $("custoUpgrade").textContent = formatar(dados.custoProximoUpgrade);
  $("nomeUpgrade").textContent = dados.nomeProximoUpgrade;
  $("descricaoUpgrade").textContent = dados.descricaoProximoUpgrade;
  $("bonusUpgrade").textContent = formatar(dados.bonusProximoUpgrade);
  $("iconeUpgrade").textContent = dados.iconeProximoUpgrade;
  $("boostTitle").textContent = dados.bonusDobroAtivo ? "Dobro de ganhos ativo" : "Ganhe em dobro";
  $("boostDescription").textContent = dados.bonusDobroAtivo ? "Seu anúncio está valendo · bônus por 10 min." : "Anúncio ativa o dobro de ganhos por 10 min.";
  const statusSala = $("roomStatus");
  statusSala.lastChild.textContent = dados.pcAquecido ? " PC SUPERAQUECIDO" : " MINERANDO";
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

palco.addEventListener("click", clicarPC);
document.querySelectorAll(".tab").forEach((tab) => tab.addEventListener("click", () => {
  document.querySelectorAll(".tab").forEach((item) => item.classList.toggle("active", item === tab));
  ["loja", "equipe", "tarefas", "saque"].forEach((id) => $("painel-" + id).classList.toggle("hidden", tab.dataset.tab !== id));
}));
iniciarJogo();
setInterval(sincronizarComServidor, 1000);
setInterval(() => { if (jogadorAtual) renderizarSaldoPrevisto(); }, 100);
