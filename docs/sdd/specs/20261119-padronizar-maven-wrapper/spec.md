# Spec: Maven Wrapper em `poc-rss-collector`

**Parent:** `poc-fleet/docs/sdd/specs/20261119-padronizar-maven-wrapper/spec.md`
**Complexity:** S
**Status:** Approved

## 1. Context & objetivo

Este repositório contém os seguintes POMs Maven e não possui `mvnw` na raiz. A spec filha aplica o contrato aprovado da macro-spec a estes builds, mantendo suas versões de Java e o comportamento atual.

- `pom.xml`

### Checkouts e POMs cobertos

- `poc-rss-collector (pom.xml)`.
- `poc-rss-collector-java27 (pom.xml)`.
- `poc-rss-collector-java27-impl (pom.xml)`.

## 2. Escopo

### Incluído

- Adicionar `mvnw`, `mvnw.cmd` e `.mvn/wrapper/maven-wrapper.properties` na raiz do repositório.
- Fixar Apache Maven 3.10.1, salvo incompatibilidade concreta de plugin/build documentada nesta spec e aprovada pelo responsável.
- Executar todos os POMs listados pelo wrapper da raiz:

- `pom.xml`: `./mvnw <goals>`

- Atualizar pipelines e instruções locais deste repositório para substituir chamadas equivalentes a `mvn` pelo wrapper, mantendo goals, perfis e argumentos existentes.
- Usar distribuição Apache oficial por HTTPS e validar SHA-256 dos artefatos suportados pelo tipo de wrapper escolhido.

### Fora do escopo

- Alterar `pom.xml`, dependências, plugins, JDK/Java alvo, lógica de aplicação ou resultado funcional esperado.
- Alterar builds ou arquivos de outro repositório.
- Implementar enquanto esta spec permanecer em Draft.

## 3. Contrato aplicável

- O wrapper fica na raiz Git e é o ponto de entrada único para Maven neste repositório.
- Maven fica fixado em 3.10.1; uma exceção precisa de evidência reproduzível de incompatibilidade e aprovação explícita.
- A distribuição é obtida de endereço oficial Apache por HTTPS. Checksums SHA-256 são configurados quando suportados; URLs privadas, credenciais e tokens não são gravados nos arquivos versionados.
- Cada POM acima é chamado com o comando indicado. CI usa o mesmo wrapper e a mesma versão Maven.
- JDK e bytecode alvo atuais são preservados. O wrapper exige Java para executar Maven, mas remove a dependência de uma instalação global do Maven.
- macOS/Linux usam `./mvnw`; Windows usa `mvnw.cmd`.

## 4. Critérios de aceitação

- **AC-01:** Todos os POMs listados podem executar os goals de teste e build já usados pelo repositório por meio do wrapper da raiz.
- **AC-02:** Cada pipeline que executa Maven usa o wrapper, sem mudar goals, perfis, JDK ou artefatos esperados.
- **AC-03:** O build inicia usando Maven 3.10.1 sem depender do `mvn` global; com distribuição em cache, não precisa baixá-la novamente.
- **AC-04:** Falha de rede sem cache, checksum inválido e caminho de POM inexistente encerram o comando com falha clara, sem iniciar um build com distribuição não validada.
- **AC-05:** A execução equivalente via wrapper mantém os resultados de compilação e testes; nenhum `pom.xml` ou comportamento da aplicação muda.
- **AC-06:** A instrução documentada funciona com `mvnw` em macOS/Linux e `mvnw.cmd` em Windows.

## 5. Cenários negativos e requisitos não funcionais

- Primeira execução sem rede e sem cache: falhar informando que não foi possível baixar Maven.
- Checksum divergente: impedir a execução da distribuição.
- POM nested ausente ou passado por caminho incorreto: falhar sem executar outro POM por engano.
- Cache preenchido: executar sem download novo e sem consultar uma instalação global de Maven.
- Preservar logs diagnósticos do Maven sem expor valores de `settings.xml`, credenciais ou tokens.

## 6. Segurança

A fonte da distribuição é fixa e oficial, usando HTTPS; checksum SHA-256 protege a distribuição baixada. Os scripts são versionados e revisáveis. Nenhuma credencial é embutida na URL ou nos scripts. Falhas de validação interrompem o build.

## 7. Observabilidade e rollback

O log de CI deve identificar a versão Maven usada e deixar visíveis falhas de download/checksum, sem segredos. Rollback: remover os arquivos do wrapper deste repositório e restaurar em seus pipelines e instruções os comandos Maven anteriores. Não há migração de dados nem alteração funcional a reverter.
