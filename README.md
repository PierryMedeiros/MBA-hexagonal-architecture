# MBA Hexagonal Architecture — Cancelamento de Evento

Plataforma de ingressos em **Arquitetura Hexagonal + Clean Architecture** (Java 17, Gradle, multi-módulo:
`domain`, `application`, `infrastructure`). Este fork adiciona a feature de **cancelamento de evento** com
a cascata assíncrona de cancelamento dos ingressos orientada a **eventos de domínio**.

## Como subir o projeto

Pré-requisitos: **JDK 17** e **Docker** (para o MySQL de produção).

```bash
# 1. Subir o banco MySQL usado em runtime
docker compose up -d

# 2. Rodar a aplicação (Spring Boot)
./gradlew :infrastructure:bootRun
```

A aplicação sobe em `http://localhost:8080`:

- REST: `/events`, `/customers`, `/partners`
- GraphQL / GraphiQL: `http://localhost:8080/graphiql`

> Os testes usam um banco **H2 em memória** (profile `test`) e **não precisam** do Docker.

## Como rodar a suíte de testes

```bash
# Toda a suíte (domínio + casos de uso + integração)
./gradlew test

# Por módulo
./gradlew :domain:test          # regras de domínio
./gradlew :application:test     # casos de uso com repositórios in-memory
./gradlew :infrastructure:test  # integração com persistência real (H2) e REST
```

## Endpoints da feature

### REST

```
POST /events/{id}/cancel      -> 200 { "id", "status": "CANCELLED" }  | 422 "Event already cancelled"
GET  /events/{id}             -> 200 { id, name, date, totalSpots, status }  | 404
GET  /events/{id}  (X-Public) -> 200 { id, status }                          | 404
```

A consulta usa o **padrão presenter**: `GetEventByIdResponseEntity` (representação completa) e
`PublicGetEventByIdResponseEntity` (representação reduzida), alternados pelo header `X-Public` no
`EventController`, via `useCase.execute(input, presenter)`.

### GraphQL

```graphql
mutation { cancelEvent(id: "…") { id status } }
query    { eventOfId(id: "…")  { id name date totalSpots status } }
```

## Onde a cascata de cancelamento acontece

O cancelamento dos ingressos é **desacoplado** do agregado de Evento e acontece por reação a um evento
de domínio, espelhando o fluxo existente `EventTicketReserved → CreateTicket`:

1. `CancelEventUseCase` carrega o `Event` e chama `event.cancel()` (regra de domínio: não cancela duas
   vezes; passa a rejeitar novas reservas). O agregado registra o evento de domínio **`EventCancelled`**
   (`type = "event.cancelled"`) em `allDomainEvents()`.
2. Ao persistir o evento em `EventDatabaseRepository`, os eventos de domínio do agregado são gravados na
   tabela **`outbox`**.
3. O job **`OutboxRelay`** lê a outbox e publica o conteúdo na fila via `QueueGateway`.
4. O **`ConsumerQueueGateway`** roteia por tipo: ao ver `event.cancelled`, dispara
   **`CancelEventTicketsUseCase`**, que busca os ingressos do evento pela porta
   `TicketRepository.ticketsByEventId(...)` e cancela cada `Ticket` (`ticket.cancel()` — idempotente,
   status `CANCELLED`).

Assim, o agregado de Evento **nunca** toca o agregado de Ingresso diretamente, e o comando de cancelamento
**não** cancela ingressos de forma síncrona — a consequência viaja pela outbox e pela fila.
