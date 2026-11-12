# Publicação na Railway

O serviço Spring Boot serve a API e os arquivos do Mini App pelo mesmo endereço HTTPS. O projeto já inclui um `Dockerfile`, perfil de produção para PostgreSQL e validação de `Telegram.WebApp.initData` no servidor.

## O que será publicado

- `src/main/resources/static/`: interface do jogo e imagens.
- Spring Boot: rotas `/api/game/*`.
- PostgreSQL Railway: dados persistentes dos jogadores.

## Configurar o projeto Railway

1. Envie este repositório para um repositório **privado** no GitHub e conecte-o à Railway. A pasta do projeto Java é `miner-clicker`.
2. Crie um serviço PostgreSQL pelo menu `New` da Railway. Mantenha o banco privado dentro do projeto.
3. Crie o serviço do jogo a partir do repositório e configure **Root Directory** como `/miner-clicker`. A Railway detectará o `Dockerfile`.
4. Nas variáveis do serviço do jogo, configure:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `TELEGRAM_BOT_TOKEN`: token do BotFather, inserido como segredo no painel; não o coloque no GitHub.
   - `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` e `PGPASSWORD`: referências às variáveis do serviço PostgreSQL da Railway.
5. Faça o primeiro deploy. O Spring cria/atualiza as tabelas no banco através do JPA.
6. Em `Settings → Networking → Public Networking`, gere um domínio Railway. A Railway disponibiliza HTTPS nesse domínio.
7. Configure esse endereço como URL do Mini App do bot no BotFather.

O arquivo `.env.railway.example` serve apenas como lembrete dos nomes das variáveis. Não copie valores secretos para esse arquivo nem o preencha com credenciais reais.

## Desenvolvimento local

Sem perfil de produção, o backend usa H2 em arquivo (`data/minerdb`) e o usuário de teste. Para abrir o frontend fora do Telegram, mantenha `TELEGRAM_DEV_MODE=true`. Na Railway, o perfil `prod` desativa esse modo e exige token válido.

## Antes de oferecer recompensas reais

Os três botões de recompensa usam o formato Rewarded Interstitial do Monetag. A recompensa só é aplicada depois que o backend recebe a confirmação `valued` do postback e valida uma sessão única; o retorno visual do navegador não concede o prêmio por conta própria. O jogo também agenda um In-App Interstitial automático a cada 5 minutos, com limite de 12 por hora, apenas no Telegram Mini App. Esse formato não libera moedas, sats ou tokens. O saque continua fictício e o saldo atual não é convertido automaticamente em pagamentos.

### Ativar anúncios reais no Monetag

1. Crie uma conta de publisher no [Monetag](https://publishers.monetag.com/) e cadastre o bot/Mini App na seção **Telegram Mini Apps**. Aguarde a aprovação e crie o SDK/Rewarded Interstitial para obter o **Main Zone ID** e a URL `src` do script.
2. No serviço do jogo na Railway, adicione estas variáveis:
   - `MONETAG_ZONE_ID`: Main Zone ID (não use o sub-zone ID).
   - `MONETAG_SDK_URL`: apenas o endereço que aparece no atributo `src` da tag gerada pelo Monetag (deve começar por `https://`).
   - `MONETAG_POSTBACK_SECRET`: uma senha aleatória longa, igual à que será usada no postback do Monetag.
3. Salve uma URL de postback nas configurações da Main Zone no Monetag. Substitua `SEU-DOMINIO-RAILWAY` pelo domínio HTTPS do serviço e `SUA-CHAVE` pela mesma chave definida em `MONETAG_POSTBACK_SECRET`:

   `https://SEU-DOMINIO-RAILWAY/api/game/ads/postback?secret=SUA-CHAVE&ymid={ymid}&zone_id={zone_id}&event_type={event_type}&reward_event_type={reward_event_type}`

4. Faça novo deploy. Os três botões ficam habilitados quando as variáveis estão corretas. O backend grava cada sessão e processa um único prêmio por sessão, somente quando o Monetag reporta uma impressão `valued`.

Se qualquer variável estiver vazia, o servidor mantém os anúncios desabilitados. Nunca coloque `MONETAG_POSTBACK_SECRET` no JavaScript, no GitHub ou na URL do Mini App. Guarde-o como variável secreta tanto na Railway quanto no campo de postback do Monetag.
