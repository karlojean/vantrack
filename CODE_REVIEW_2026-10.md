# Code review — VanTrack

Revisão feita em 09/10/2026 sobre a branch `main`, commit `82a5df8` (`fix start trip test`).
Nenhum código do projeto foi alterado. Os bugs marcados como **confirmados** foram reproduzidos com um teste temporário, apagado depois.

## Sumário

- [Estado geral](#estado-geral)
- [Resultado dos testes](#resultado-dos-testes)
- [Bugs confirmados](#bugs-confirmados)
- [Segurança e configuração](#segurança-e-configuração)
- [WebSocket](#websocket)
- [Qualidade de código](#qualidade-de-código)
- [O que falta para terminar](#o-que-falta-para-terminar)
- [Pontos positivos](#pontos-positivos)

## Estado geral

A base está boa:

- os pacotes estão organizados por funcionalidade, com casos de uso;
- o banco usa migrations Flyway;
- as senhas usam BCrypt;
- a autenticação JWT funciona no HTTP e no STOMP;
- há 129 testes de integração com Testcontainers.

Os itens da revisão de agosto (`CODE_REVIEW.md`) foram resolvidos:

- os testes de início de viagem esperam 201;
- `GET /trips/active` tem testes;
- o teste concorrente tem `finally` e confere o que foi gravado no banco.

## Resultado dos testes

Comando: `./mvnw verify`, com Docker disponível.

| Resultado | Quantidade |
| --- | --- |
| Passaram | 128 |
| Falha intermitente | 1 |

**A falha:** `WebSocketSecurityIT.shouldDenyCompletedTripSubscription`. Ao rodar a classe mais 3 vezes, ela passou em todas.

**A causa:** o `@AfterEach` (`WebSocketSecurityIT.java:81`) chama `disconnect()` numa sessão que o servidor já fechou depois de enviar o frame ERROR. Isso gera `MessageDeliveryException: ... is not open: current state CLOSED`.

**A correção:** envolver o `disconnect()` em try/catch, ou ignorar sessões já fechadas.

## Bugs confirmados

### 1. Erros do Spring viram HTTP 500 - Feito

- **Onde:** `GlobalExceptionHandler.java:72`
- **Problema:** o `@ExceptionHandler(Exception.class)` captura também as exceções internas do Spring, como `NoResourceFoundException` e `HttpRequestMethodNotSupportedException`.
- **Resultado real:**
  - `GET /nao-existe` retorna **500**; deveria ser 404;
  - `DELETE /routes` retorna **500**; deveria ser 405;
  - os dois casos são registrados no log como "Erro não tratado".
- **Correção:** fazer a classe estender `ResponseEntityExceptionHandler`, ou tratar exceções que implementam `ErrorResponse` antes do handler genérico.

### 2. `GET /routes/{id}/parent` lança `LazyInitializationException`

- **Onde:** `ListRouteParentsUseCase.java:26`
- **Problema:** o método não tem `@Transactional` e acessa `route.getVan().getDriver()` e `route.getUserRoutes()`, que são relações LAZY.
- **Resultado real:** **500** com `Could not initialize proxy [Van#...] - no session`.
- **Por que não aparece no dev:** o `spring.jpa.open-in-view` vem ligado por padrão e esconde o erro. No perfil de teste ele está desligado, por isso quebra. Não existe teste para esse endpoint.
- **Correção:**
  - adicionar `@Transactional(readOnly = true)` ou um `@EntityGraph`;
  - desligar o `open-in-view` em todos os perfis.

### 3. `POST /routes/{id}/parent` sem validação

- **Onde:** `RouteController.java:57` e `AddParentRequest.java`
- **Problema:** o DTO não tem anotações de validação e o controller não usa `@Valid`.
- **Resultado real:** body `{}` retorna **500**; deveria ser 400.
- **Correção:**
  - adicionar `@NotNull` em `parentId`;
  - adicionar `@NotBlank` e `@Size(max = 200)` em `studentName`;
  - usar `@Valid` no controller.

### 4. `RouteResponse` não devolve o `id`

- **Onde:** `RouteResponse.java:5`
- **Problema:** criar e listar rotas devolve só `name`, `plate` e `driver`.
- **Impacto:** quem usa a API não descobre o `routeId`. Sem ele não dá para iniciar viagem nem adicionar pai, então o fluxo só funciona copiando o UUID do banco. Os arquivos `.http` têm IDs fixos por esse motivo.

## Segurança e configuração

- [ ] **Perfil `dev` ativo por padrão** (`application.yaml`). Isso causa três coisas:
  - o `DataSeederConfig` cria `admin@vantrack.com` com a senha `admin123` e imprime a senha no console;
  - o log de segurança fica em `TRACE`;
  - o CORS aceita `*`.

  O perfil deve vir do ambiente (`SPRING_PROFILES_ACTIVE`), e a senha do seed deve ser configurável e não aparecer no log.
- [ ] **Login permite descobrir e-mails cadastrados** (`LoginUseCase.java:30-38`).
  - O código busca o usuário e confere a senha na mão, e depois chama o `AuthenticationManager` de novo.
  - Quando o e-mail não existe, o BCrypt não roda e a resposta volta mais rápido.
  - Correção: chamar só o `authenticationManager.authenticate(...)` e converter a `AuthenticationException` em 401.
- [ ] **E-mail e placa diferenciam maiúsculas de minúsculas.** `A@x.com` e `a@x.com` viram dois usuários diferentes.
- [ ] **Corrida de unicidade vira 500.** E-mail ou placa duplicados criados ao mesmo tempo estouram a constraint e caem no handler genérico. Falta tratar `DataIntegrityViolationException` como 409.
- [ ] **Falta limite de tamanho.** `CreateUserRequest.name` não tem `@Size(max = 200)` e `email` não tem `@Size(max = 254)`; valores maiores geram erro de banco (500).
- [ ] **Detalhes do JWT** (`SecurityConfig.java:73`):
  - `jwtSecret.getBytes()` está sem charset; use `StandardCharsets.UTF_8`;
  - o tamanho mínimo do segredo não é validado (HS256 exige 32 bytes);
  - o decoder não valida o `issuer`.
- [ ] **Escopo das listagens.** Um DRIVER consegue listar as vans de qualquer motorista (`GET /vans?userId=...`). Decidir se isso é intencional.
- [ ] **Variáveis de ambiente inconsistentes:**
  - o `.env` não é carregado pelo Spring; falta `spring.config.import: optional:file:.env[.properties]`, ou é preciso exportar as variáveis;
  - o `.env.example` tem `APP_DB_PORT`, mas nada usa essa variável; o yaml e o compose usam `DB_PORT`;
  - o perfil prod usa outros nomes (`DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`).
- [ ] **Versão fixa no `pom.xml`.** O `spring-security-messaging` declara `<version>7.1.0</version>`. Remova a versão e deixe o BOM do Spring Boot gerenciar.
- [ ] **`tracking-test.html` público.** Está liberado no `SecurityConfig` e iria para produção junto com a aplicação. Mova para um perfil dev ou para fora de `static/`.

## WebSocket

### O que está bem feito

- A autenticação acontece no frame `CONNECT`, via header `Authorization`.
- A autorização acontece em cada `SUBSCRIBE` (`TripAccessPolicy`): pai vinculado ou admin, e só em viagem ativa.
- Tudo que não é permitido explicitamente é negado (`.anyMessage().denyAll()`).
- A publicação só acontece depois do commit (`@TransactionalEventListener(AFTER_COMMIT)`).
- Desligar o CSRF do STOMP faz sentido, porque o token vai no header e não em cookie.

### O que falta ou merece atenção

1. **Falta um teste ponta a ponta do fluxo principal.** Os testes STOMP publicam direto pelo `SimpMessagingTemplate`. Nenhum teste comprova que `POST /trackings` resulta num frame entregue ao pai inscrito.
2. **O fim da viagem não é notificado.** `FinishTripUseCase` não publica nada, então o pai continua inscrito sem saber que a viagem acabou. Sugestão: publicar um evento como `{ "type": "TRIP_FINISHED" }` no mesmo tópico.
3. **A primeira localização não é transmitida.** O ponto gravado em `StartTripUseCase` não dispara evento.
4. **Quem se inscreve no meio da viagem não vê nada até o próximo ponto.** Falta um endpoint de última posição ou histórico, por exemplo `GET /trips/{id}/locations`, paginado.
5. **A autorização só é checada no `SUBSCRIBE`.** Se o token expirar ou o pai for desvinculado, a sessão continua recebendo mensagens. Para um projeto de estudo é aceitável, mas vale saber.
6. **Não há heartbeat configurado.** Atrás de proxy ou load balancer, a conexão cai quando fica ociosa. Configure `enableSimpleBroker("/topic").setHeartbeatValue(...).setTaskScheduler(...)`.
7. **O motorista ainda envia a posição por REST.** O próximo passo natural é um `@MessageMapping("/trips/{id}/location")`. Hoje isso está bloqueado pelo `denyAll`, então será preciso liberar `SEND` em `/app/**`, com autorização.
8. **O endpoint `/ws` só aceita SockJS.** Um cliente WebSocket nativo precisa usar `/ws/websocket`. Documente isso, ou registre o endpoint duas vezes, com e sem SockJS.
9. **O `SimpleBroker` vive em memória e numa instância só.** Escalar horizontalmente exigiria um broker relay, como RabbitMQ. É um bom tema de estudo depois.

## Qualidade de código

São itens de manutenção, não falhas.

- **Nome copiado:** `TrackingListener.handleUserRegistration` deveria se chamar algo como `onLocationSent`.
- **Classe de evento:** `SendLocationEvent` usa `@Data` com campo público; troque por um `record`.
- **Pacote errado:** `UserRouteRepository` está em `tracking`, mas pertence a `routes`.
- **Imports não usados:**
  - `TripLocation` em `Trip`;
  - `Route`, `UserRoute` e `Set` em `TripRepository`;
  - `Trip` em `TripController`;
  - `User` em `UserController` e `CreateVanRequest`;
  - `UserRole` em `VanResponse`.
- **Código repetido:**
  - `UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")))` aparece em todos os controllers e poderia virar um helper ou um `HandlerMethodArgumentResolver`;
  - `findById(userId).orElseThrow(... UNAUTHORIZED)` está copiado em 8 casos de uso.
- **Status HTTP inconsistentes:**
  - criar usuário, van e rota retorna 200, enquanto iniciar viagem e tracking retornam 201;
  - `addParent` retorna 200 sem corpo; o adequado seria 201 ou 204.
- **Status errado em `AddParentUseCase`:** responde 403 quando o usuário informado não é PARENT; 422 seria mais adequado.
- **Formato do login:** `/auth/login` devolve o token como texto puro. O comum é `{ "token": "..." }`.
- **Configuração sem efeito:** `spring.jackson.date-format` só se aplica a `java.util.Date`, não a `OffsetDateTime`.
- **Busca por texto:** o `LIKE` dos filtros de nome e placa não escapa `%` e `_`.
- **Erros de digitação nas mensagens:**
  - "mdeve ser no máximo";
  - "outro usuári";
  - "Não foi buscar pais";
  - "e obrigatório" (deveria ser "é obrigatório");
  - "Erro ao criar usuário" em `CreateVanUseCase`.
- **Espaçamento:** há linhas em branco sobrando em `AuthController`, `CreateRouteUseCase`, `ListRouteParentsUseCase` e `WebSocketConfig`.

## O que falta para terminar

### 1. Fechar o MVP

- [ ] Corrigir os 4 bugs confirmados.
- [ ] Adicionar testes para `POST /routes/{id}/parent` e `GET /routes/{id}/parent`.
- [ ] Incluir o `id` em `RouteResponse`.
- [ ] Criar um endpoint "minhas rotas" para o PARENT.
- [ ] Criar `GET /trips/{id}/locations` (última posição ou histórico paginado).
- [ ] Publicar no tópico o evento de fim de viagem e a primeira localização.
- [ ] Escrever o teste ponta a ponta REST → STOMP.
- [ ] Corrigir a intermitência no teardown do `WebSocketSecurityIT`.
- [ ] Tirar o `dev` do `application.yaml` e desligar o `open-in-view` globalmente.

### 2. Completar o domínio

- [ ] Remover um pai de uma rota.
- [ ] Editar e excluir usuários, vans e rotas.
- [ ] Criar uma nova migration com:
  - `NOT NULL` em `trips.started_at` e `trip_locations.created_at`;
  - `CHECK` ligando status e datas (`ACTIVE` sem `ended_at`, `COMPLETED` com `ended_at >= started_at`);
  - unicidade em `user_routes`;
  - índices em `vans(driver_id)`, `routes(van_id)`, `user_routes(user_id)`, `user_routes(route_id)` e `trip_locations(trip_id, created_at)`.
- [ ] Adicionar paginação nas listagens.

### 3. Fechar o aprendizado de WebSocket

- [ ] Envio de localização pelo motorista via `@MessageMapping`.
- [ ] Heartbeat no broker.
- [ ] Opcional: broker relay (RabbitMQ) para várias instâncias.

### 4. Entrega

- [ ] README explicando como rodar, as variáveis de ambiente e o fluxo de teste com `tracking-test.html`.
- [ ] Dockerfile e a aplicação dentro do `docker-compose.yml`.
- [ ] Carregamento do `.env`.
- [ ] CI rodando `./mvnw verify`.

### Decisões de domínio em aberto

- A regra de "uma viagem ativa por vez" deve valer por rota, van ou motorista? Hoje o índice garante só por rota, então a mesma van pode ter duas viagens ativas em rotas diferentes.
- A unicidade de `user_routes` deve ser `(user_id, route_id)` ou `(user_id, route_id, student_name)`? Ou o aluno deve virar uma entidade própria?
- Por quanto tempo o histórico de GPS será mantido? O horário deve ser o de captura no dispositivo ou o de recebimento no servidor?

## Pontos positivos

- A estrutura por funcionalidade (`users`, `vans`, `routes`, `trips`, `tracking`) é clara, com um caso de uso por operação.
- O índice único parcial (`V2__trip_active_index.sql`) garante no banco uma única viagem ativa por rota. O `saveAndFlush`, junto com a tradução para 409, trata a concorrência, e há teste concorrente para isso.
- A autorização STOMP nega de forma segura token, claim, destino e UUID inválidos, e tem 17 cenários de teste.
- A localização só é publicada depois do commit, então nunca vai para o tópico um dado que sofreu rollback.
- Os testes de integração rodam com PostgreSQL real (Testcontainers) e limpam o banco entre os testes.
- Os erros seguem o formato `ProblemDetail` (RFC 9457) e a validação de coordenadas está correta.
