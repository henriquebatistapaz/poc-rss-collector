# Architecture overview

> Serviço sem estado: recebe uma URL, busca o documento com limites rígidos e diz se é RSS 2.0 ou Atom 1.0 válido. O mesmo código é usado como biblioteca pelo `poc-dashboard`.

## Flow

```mermaid
flowchart LR
    C[Cliente] -->|POST /v1/feeds/validate| A(FeedValidationController)
    A --> S(FeedUrlValidationService)
    S --> V1(RssFeedLinkValidator)
    S --> F(BoundedFeedHttpFetcher)
    F -->|HTTP(S) público, 1 MB, 3 redirects, 5 s| X[(Host do feed)]
    S --> P(RssFeedParser)
    P --> I[FeedItem records]
    D(poc-dashboard) -.->|dependência Maven| P
```

## Key classes / entry points

| Class | Role |
|-------|------|
| `FeedValidationController` | Endpoint `POST /v1/feeds/validate` |
| `FeedUrlValidationService` | Orquestra validação de URL, fetch e parse |
| `BoundedFeedHttpFetcher` | Busca HTTP com limites e checagem de destino público |
| `RssFeedParser` | Parser RSS 2.0 / Atom 1.0 endurecido contra XML hostil |
| `RssFeedValidator` | Diz se um XML é feed válido |
| `RssFeedLinkValidator` | Valida URLs HTTP(S) públicas |

## Gotchas

- A validação vale para a resposta daquele momento; o feed pode mudar depois.
- O jar comum é a biblioteca; o executável tem classifier `exec`.

## Related

- [[dominio-feeds]], [[decision-biblioteca-e-executavel]], `SPEC.md`
