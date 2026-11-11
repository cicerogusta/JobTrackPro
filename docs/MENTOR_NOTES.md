# Guia técnico por etapa

Este documento relaciona as decisões ao código real e serve como roteiro de estudo.

## Textos e recursos dos módulos

Os textos visuais ficam nos strings.xml dos módulos responsáveis. Compose usa
stringResource e as Views usam getString; rótulos compartilhados ficam no Design
System e mensagens de falha em core:common. ApplicationValidator retorna campos
inválidos tipados, sem mensagens ou recursos Android; ValidationMessages.kt faz a
associação na apresentação. Formatos com valores usam argumentos posicionais.
Os testes de interface também consultam recursos. Pergunta de entrevista: por que
retornar uma mensagem já traduzida no ViewModel dificulta mudanças de idioma?

## Etapa 1 — Inicialização

settings.gradle.kts define os módulos lógicos; libs.versions.toml centraliza versões.
Os build.gradle.kts configuram Java 17, Compose, KSP e Hilt. JobTrackApplication
inicializa o container. A dependência aponta para contratos (DIP), e cada módulo
encapsula uma responsabilidade (SRP). A estrutura existente de home/designsystem
foi preservada por mapeamento no Gradle. Valide com gradlew.bat assembleDebug.
Pergunta de entrevista: qual a diferença entre implementation e api?

## Etapa 2 — Arquitetura

Models.kt, Contracts.kt e Validation.kt concentram tipos, contratos e regras puras.
UiState.kt define estados imutáveis; as features expõem StateFlow somente leitura.
Hilt fornece implementações para interfaces pequenas (ISP/DIP), permitindo mocks
sem reescrever ViewModels (OCP). CancellationException não vira erro de negócio.
Teste com testDebugUnitTest. Pergunta: por que não criar um UseCase para cada chamada?

## Etapa 3 — Design System

JobTrackTheme.kt e Components.kt concentram tema, espaçamento e componentes.
Os textos de status vêm de enums; componentes recebem eventos sem acessar dados.
SRP evita misturar UI e infraestrutura; funções com nomes claros reduzem duplicação.
Verifique tema claro/escuro, fonte ampliada e elementos de toque.
Pergunta: como semântica e tamanho mínimo de toque afetam acessibilidade?

## Etapa 4 — Dashboard

HomeViewModel observa ApplicationRepository e produz HomeData com DashboardStats.
HomeScreen renderiza loading, erro/retry, vazio, estatísticas e recentes.
A regra de contagem está no modelo, fora do Composable; a interface recebe callbacks.
HomeViewModelTest cobre atualizações e retry. Pergunta: por que coletar estado com
collectAsStateWithLifecycle em vez de iniciar uma coroutine a cada recomposição?

## Etapa 5 — XML

ApplicationsFragment usa FragmentApplicationsBinding, RecyclerView e
ApplicationsAdapter com DiffUtil. ApplicationsViewModel combina filtros e dados.
ApplicationNavigation desacopla a feature da Activity concreta (ISP/DIP).
A referência do binding é liberada em onDestroyView para respeitar o ciclo da View.
Teste o fluxo XML com connectedDebugAndroidTest.
Pergunta: por que o ciclo de vida da View do Fragment difere do ciclo do Fragment?

## Etapa 6 — Room e CRUD

ApplicationEntity, ApplicationDao, JobTrackDatabase e LocalApplicationDataSource
separam representação persistida, consultas e acesso local. OfflineApplicationRepository
valida usuário e dados, normaliza entradas e escreve revisões. O formulário reutiliza
ApplicationDraft na criação/edição. O detalhe confirma exclusão e altera status.
DAO transacional rejeita revisão desatualizada, evitando sobrescrita silenciosa.
ApplicationDaoTest verifica CRUD, isolamento e tombstones.
Pergunta: como uma transação evita apagar uma edição durante o acknowledgement?

## Etapa 7 — REST

JobsApi e JobDto descrevem a API; toDomain valida o payload.
RemoteJobsRepository controla cache e JobsViewModel controla estados e salvamento.
Para o Brasil, a API okemprego recebe country=BR, q e where. JobSearch representa
os filtros e JobPage informa a paginação. A busca ocorre ao confirmar o botão,
em vez de filtrar somente a primeira página em memória. O cache distingue filtros
e páginas; uma falha ao carregar mais preserva os resultados anteriores.
JobOpening usa WorkMode tipado e opcional: modalidade ausente exige escolha antes
do salvamento, evitando classificar uma vaga híbrida como presencial.
Gson foi escolhido pelo conversor oficial Retrofit; uma interface coesa permite
trocar o provedor. MockWebServer testa o contrato sem internet.
Pergunta: por que DTO e modelo de domínio não precisam ser a mesma classe?

## Etapa 8 — Autenticação

FirebaseProvider fornece SDKs configurados. FirebaseAuthRepository implementa o
contrato, AuthViewModel publica operação/erro e AuthScreen apresenta o formulário.
A árvore autenticada é descartada no logout; senhas não são serializadas.
Use emuladores ou configure google-services.json para testar registro/login/reset.
Pergunta: autenticação é suficiente para autorizar acesso a documentos de outro usuário?

## Etapa 9 — Sincronização

FirestoreApplicationDataSource encapsula transações remotas; FirestoreSyncRepository
coordena outbox, merge e feedback. ConflictResolver implementa ordenação determinística.
SRP evita sincronização no Composable e LSP exige que mocks preservem os resultados
e falhas do contrato. Regras e testes verificam autorização, schemas e exclusão lógica.
Teste dois bancos locais, interrupção/retomada e usuário sem permissão.
Pergunta: quais problemas um relógio de dispositivo incorreto causa no last-write-wins?

## Etapa 10 — Qualidade

Os testes cobrem validação, ViewModels, repositórios, API, DAO, segurança e interface.
README.md orienta execução; ARCHITECTURE.md explica fluxo e DECISIONS.md os trade-offs.
VERIFICATION.md registra comandos realmente executados. Pendências externas devem
continuar visíveis; sucesso de build não comprova comportamento em produção.
Pergunta: quais testes você adicionaria antes de distribuir o aplicativo publicamente?

## Revisão de espaçamento das ações

HomeScreen.kt e JobsScreen.kt agora agrupam seus cabeçalhos em Column com
Arrangement.spacedBy(Spacing.medium). Em LazyColumn, o espaçamento entre itens não
separa os componentes emitidos dentro de um mesmo item. Os cartões de vagas também
separam a descrição e as ações de abrir/salvar, com 16 dp entre os botões.

fragment_applications.xml usa control_spacing em dimens.xml para separar filtros,
mensagem de estado, botão de tentar novamente e lista. As margens para alvos GONE mantêm
um intervalo previsível quando carregamento, mensagem ou botão estão ocultos.
A dimensão screen_padding foi movida de strings.xml para dimens.xml.

A responsabilidade pelo espaçamento pertence ao layout que organiza os componentes;
JobTrackButton continua responsável pelo botão e sua área mínima de toque. Essa
separação preserva SRP e evita margens implícitas em todos os consumidores. Não foram
introduzidas strings na interface nem alterações na lógica de negócio.

Para testar, abra Início, Vagas e Candidaturas, role até as ações e repita com fonte
ampliada. JobsContentTest e UiStateTest verificam a distância entre o campo de
localização e Buscar, e entre a descrição do dashboard e Adicionar candidatura.
Pergunta de entrevista: qual é a diferença entre contentPadding de LazyColumn,
Arrangement.spacedBy entre itens e o espaçamento interno de um item composto?
