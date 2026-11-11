# RSS Link Collector Mini

Mini projeto Java privado para validar URLs e extrair links de feeds RSS e Atom.

## Estado atual

- Parser RSS 2.0 e Atom 1.0 com XML protegido contra DTD e entidades externas.
- API `POST /v1/feeds/validate` que recebe uma URL e informa se a resposta é um feed RSS/Atom válido.
- API para cadastrar e listar feeds, fazer refresh manual e consultar links coletados.
- Normalização de links e deduplicação por feed e identidade do item.
- Busca HTTP com timeout, tamanho máximo de resposta, limite de redirecionamentos e validação de destino público.
- Feeds e links ficam em memória; não há persistência durável nem atualização agendada.

A validação comprova apenas a resposta disponível naquele momento; um feed pode mudar ou ficar indisponível depois.

## Integração

Este projeto continua independente e também é usado como biblioteca Maven pelo repositório privado `poc-dashboard`, que reaproveita o código para validar e ler feeds RSS/Atom. Por isso, `mvn package` gera dois jars: o comum, usado como dependência, e o executável com sufixo `-exec`. Para o hub enxergar a versão local, rode `mvn install` neste projeto.

## Executar

```bash
mvn test
mvn spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui/index.html>
OpenAPI JSON: <http://localhost:8080/v3/api-docs>

## Cadastrar e atualizar feeds

Cadastre um feed público válido. A resposta inclui um ID estável até o processo reiniciar:

```bash
curl -X POST http://localhost:8080/v1/feeds -H 'Content-Type: application/json' -d '{"feedUrl":"https://feeds.bbci.co.uk/news/rss.xml"}'
```

Consulte feeds e links com `GET /v1/feeds` e `GET /v1/links`. Links aceitam `feedId`, `from` e `to` como filtros; datas usam ISO-8601 UTC. Para atualizar um feed manualmente, use `POST /v1/feeds/{id}/refresh`. O refresh preserva a identidade dos itens, atualiza metadados e não duplica entradas.

O armazenamento é limitado a 100 feeds e 1.000 links por feed e é volátil. Não exponha a API sem autenticação e controles operacionais; cadastro/refresh fazem requisições de saída a URLs fornecidas pelo cliente.

## Consultar se um link é um feed válido

```bash
curl -X POST http://localhost:8080/v1/feeds/validate \\
  -H 'Content-Type: application/json' \\
  -d '{"feedUrl":"https://feeds.bbci.co.uk/news/rss.xml"}'
```

Exemplo de resposta:

```json
{"feedUrl":"https://feeds.bbci.co.uk/news/rss.xml","valid":true,"itemCount":30,"message":"Valid RSS 2.0 or Atom 1.0 feed"}
```

A API aceita apenas HTTP(S) público, verifica endereços DNS antes de cada conexão e redirect, limita a resposta a 1 MB, usa timeout e não segue mais de três redirects. A suíte JUnit padrão não acessa rede: usa XML local e mocks.

## Usar o parser localmente

```java
var urlValidator = new RssFeedLinkValidator();
var parser = new RssFeedParser(urlValidator);
var validator = new RssFeedValidator(urlValidator, parser);

boolean valid = validator.isValidRssFeed(feedUrl, xmlContent); // RSS 2.0 ou Atom 1.0
if (valid) {
    var items = parser.parse(feedUrl, xmlContent);
}
```

O parser extrai GUID/Atom ID (ou usa o link como identidade), título, link normalizado e data de publicação. Um feed Atom precisa conter `id`, `title` e uma data `updated` válida. Para entradas Atom, escolhe links alternativos HTML e resolve links relativos contra a URL do feed. Itens sem link HTTP(S) válido são ignorados sem descartar os demais itens válidos.

## Feeds RSS reais para testes manuais

- NASA News Releases: <https://www.nasa.gov/news-release/feed/>
- NASA Image of the Day: <https://www.nasa.gov/feeds/iotd-feed/>
- BBC News: <https://feeds.bbci.co.uk/news/rss.xml>

Para buscar esses feeds e validar os documentos com o parser, execute o teste externo opcional:

```bash
RSS_EXTERNAL_TESTS=true mvn test
```

O teste externo usa timeout e limite de 1 MB. Essas URLs também servem para inspeção manual; não são usadas pela suíte padrão.

## Stack

Java 21, Spring Boot, Maven, JUnit 5, Spring MVC e springdoc-openapi.

Veja [SPEC.md](SPEC.md) para escopo e decisões, e [TODO.md](TODO.md) para as próximas fatias.
