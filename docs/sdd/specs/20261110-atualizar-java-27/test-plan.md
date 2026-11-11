# Test plan — Java 27 no poc-rss-collector

## Execução candidata

- Ambiente: OpenJDK Temurin 27.0.1, Maven 3.9.16; repositório Maven isolado.
- Versões: Spring Boot 4.1.2, springdoc-openapi 3.2.0.
- Comando: `mvn -B clean verify` e `mvn -B clean install`.

## Evidência da implementação (11/11/2026)

Ambos os builds passaram; 32 testes, 0 falhas, 0 erros e 1 teste de smoke real ignorado conforme configuração existente. JAR compilado com release 27 e instalado no repositório temporário para consumo pelo runner/dashboard.

## Gates pendentes

- Repetir testes do consumidor runner/dashboard após integração no branch final.
- Inspecionar portas/binds e executar o perfil de integração PostgreSQL do dashboard quando Docker estiver ativo.
- Validar retorno ao baseline JDK 21.
