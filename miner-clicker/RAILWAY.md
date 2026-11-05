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

As rotas de anúncio ainda simulam a conclusão e o saque continua fictício. Não habilite pagamentos reais até integrar um provedor de anúncios com confirmação de conclusão no backend, integrar a API de pagamentos e configurar backups do PostgreSQL. O valor da carteira atual não é convertido automaticamente para pagamentos.
