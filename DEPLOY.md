# Deploy e execução local

API Spring Boot para validar feeds públicos e extrair links. O projeto também é usado como biblioteca Maven.

## Validação local

Use JDK 27 e Maven 3.9. Execute os comandos a partir da raiz do repositório.

```sh
mvn clean verify
mvn spring-boot:run
```

## Pendências de publicação

Para publicar a API, definir o destino e as regras de tráfego de saída para feeds públicos. Para distribuir a biblioteca, definir o registro Maven e o fluxo de release. Preserve os limites de busca documentados no [README](README.md).
