# RSS Link Collector Mini

Mini projeto Java privado para validar feeds e extrair links de publicações RSS.

## Estado atual

- Parser RSS 2.0 local, sem chamadas de rede.
- Validador de URL para feeds HTTP(S) e de documento RSS 2.0 fornecido pelo chamador.
- XML protegido contra DTD e entidades externas; limites de tamanho do documento e quantidade de itens.
- Atom, coleta pela rede, armazenamento, endpoints REST e agendamento ainda não implementados.

O validador recebe o conteúdo XML junto com a URL. Ele confirma a forma da URL e do documento, mas não baixa nem comprova que o servidor remoto continuará respondendo como RSS. A busca de feeds será uma etapa posterior, com controles de SSRF, redirecionamentos e limites de rede.

## Executar os testes

```bash
mvn test
```

Os testes JUnit usam XML local e não dependem de disponibilidade externa.

## Usar o parser

```java
var urlValidator = new RssFeedLinkValidator();
var parser = new RssFeedParser(urlValidator);
var validator = new RssFeedValidator(urlValidator, parser);

boolean valid = validator.isValidRssFeed(feedUrl, xmlContent);
if (valid) {
    var items = parser.parse(feedUrl, xmlContent);
}
```

`RssFeedParser` extrai GUID (ou usa o link como identidade), título, link normalizado e `pubDate`. Itens sem link HTTP(S) válido são ignorados sem descartar os demais itens válidos do documento.

## Feeds RSS reais para testes manuais

Estes feeds públicos responderam com HTTP 200 e conteúdo XML RSS quando verificados:

- NASA News Releases: <https://www.nasa.gov/news-release/feed/>
- NASA Image of the Day: <https://www.nasa.gov/feeds/iotd-feed/>
- BBC News: <https://feeds.bbci.co.uk/news/rss.xml>

Para buscar os três feeds e validar os documentos com o parser, execute os testes externos opcionais:

```bash
RSS_EXTERNAL_TESTS=true mvn test
```

O teste externo não segue redirecionamentos, usa timeout e limite de 1 MB. A suíte padrão não acessa a rede.

Exemplo para inspecionar uma resposta:

```bash
curl -fsSL 'https://www.nasa.gov/news-release/feed/' | head -c 300
```

Essas URLs também servem para inspeção manual; elas não são usadas pela suíte padrão.

## Stack

Java 21, Spring Boot, Maven e JUnit 5. O projeto ainda não publica uma API HTTP.

Veja [SPEC.md](SPEC.md) para escopo e decisões, e [TODO.md](TODO.md) para as próximas fatias.
