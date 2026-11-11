# SDD: Atualizar poc-rss-collector para Java 27

**Complexity:** M — biblioteca e executável Spring com validação de URLs
**Security-sensitive:** yes — URLs externas, parsing XML e validação de rede

## 1. Contexto

O `pom.xml` usa Java 21 e Spring Boot 3.4.5. O build produz um JAR comum e outro com sufixo `-exec`; o dashboard incorpora a biblioteca. Java 27 chegou à disponibilidade geral em 15/09/2026 (build 35) e é a versão estável mais recente em 10/11/2026. É uma versão sem suporte de longo prazo (LTS); acompanhar o prazo de suporte da distribuição e o próximo ciclo de atualização.

## 2. Objetivo

Compilar, testar e executar os componentes Java deste repositório no Java 27, preservando os fluxos e contratos atuais.

## 3. Escopo

### 3.1 Incluído

- `pom.xml`, empacotamento dos dois JARs e compatibilidade de parsing e validação de feeds com o dashboard.
- Checagem de compatibilidade, regressão e procedimento de retorno para este repositório.

### 3.2 Fora de escopo

- Mudança de regras de negócio, contratos públicos, dados ou esquema de banco.
- APIs preview, refatorações independentes e publicação de artefatos ou deploy em produção como parte desta migração.

## 4. Requisitos

### 4.1 Funcionais

- RF-01: O Java 27 é usado nas superfícies Java do escopo, com a versão efetiva demonstrada pelo ambiente.
- RF-02: Os fluxos existentes de poc-rss-collector continuam operando nos mesmos contratos.

### 4.2 Não funcionais

- RNF-01: Usar uma distribuição estável e o patch 27.0.x suportado mais recente disponível para os alvos no momento da implementação. Registrar distribuição, patch e imagens ou instaladores usados como evidência.
- RNF-02: Preservar os gates existentes e comparar o resultado com o baseline de Java 21. Não aceitar regressões nem ignorar verificações para contornar incompatibilidades.

## 5. Arquitetura e desenho

Confirmar suporte do Spring Boot/BOM, compilador, processadores e plugins de teste/cobertura ao JDK e ao bytecode 27. Registrar as versões candidatas a partir das matrizes oficiais. Se exigir mudança maior de Spring, dimensionar a migração e revisar o impacto antes de implementar. Alinhar o build, a versão efetiva do JDK no Maven e, quando houver, a JVM da imagem final. Fixar o release de compilação em 27 no POM efetivo; se uma biblioteca precisar publicar bytecode 21 durante a transição, registrar a exceção e verificar o consumidor até concluir a migração coordenada. Se não houver suporte comprovado para um alvo obrigatório, registrar o bloqueio com evidência e manter a configuração anterior nesse alvo até a resolução.

**Spring Boot 4 (decisão de 11/11/2026):** migrar o Spring Boot de 3.4.5 para a linha 4.x. O Boot 3.5 teve fim de suporte open source em 30/06/2026 (fontes: HeroDevs e eosl.date). Dimensionar as mudanças de API do Boot 4 e confirmar as datas de suporte em spring.io antes da implementação.

## 6. Plano de implementação

1. Registrar versões e resultados do baseline Java 21 em ambiente isolado.
2. Verificar o suporte ao Java 27 nos componentes do escopo e registrar as versões candidatas.
3. Atualizar somente os pontos identificados na seção 3, incluindo instruções de uso que ainda fixem a versão anterior.
4. Executar as verificações da seção 7 e coordenar a troca com os consumidores ou produtores vinculados, quando houver.

## 7. Critérios de aceite

- AC-01a: `java -version`, `mvn -v` e a versão dentro das imagens, quando aplicáveis, confirmam Java 27; conferir também o release de compilação no POM efetivo (27, ou exceção registrada na §5).
- AC-01b: `mvn -B clean verify`; verificar os dois artefatos em `target/`, executar o JAR `-exec` em loopback e consumir o JAR comum no build do dashboard candidato.
- AC-02: RSS e Atom válidos continuam sendo parseados; URLs rejeitadas pelos validadores continuam bloqueadas. Os testes determinísticos passam sem depender de feeds externos.
- AC-03: Usar testes existentes e fixtures para verificar as regras de entrada e acesso aplicáveis; não expor novos serviços nem depender de dados ou credenciais reais. A exposição é medida comparando portas, rotas e binds publicados com o baseline Java 21, com a lista anexada ao PR. Os logs e artefatos de verificação não contêm segredos ou dados pessoais reais.
- AC-04: Registrar como voltar para o build e runtime de Java 21 e confirmar que os contratos e dados não exigem conversão para esse retorno.
- AC-05: Spring Boot 4.x confirmado no POM efetivo (`mvn help:effective-pom` ou `mvn dependency:tree`), com as suítes de teste existentes passando na nova linha.

## 8. Observabilidade e operação

Registrar a versão do JDK e o resultado das verificações no PR de implementação. Quando houver serviço, acompanhar erros de inicialização, saúde e a primeira operação funcional; manter a referência anterior até o smoke passar.

## 9. Riscos e mitigações

- Atualização de JDK/framework pode afetar parser XML, rede e TLS; manter as defesas de entrada e usar fixtures positivas e negativas.
- Java 27 tem janela de suporte mais curta; registrar quando reavaliar a próxima versão e confirmar o calendário da distribuição antes da promoção.
- Cenários negativos: banco indisponível, dependência fora do ar e entrada malformada devem falhar com o mesmo código e mensagem do baseline Java 21, verificados pelos testes do AC-02.

### 9.1 Ameaças

| Questão | Ameaça | Mitigação | Verificação |
|---|---|---|---|
| Entrada não confiável | Mudança de parsing, HTTP ou TLS pode alterar a validação de entradas existentes. | Comparar com o baseline usando fixtures representativas de entradas aceitas e rejeitadas; preservar as validações. | AC-02, AC-03 |
| Autorização e exposição | Uma atualização de dependência ou imagem pode mudar controles de acesso ou exposição. | Preservar a configuração de segurança e checar os fluxos protegidos existentes. Sem controle de acesso no componente, verificar que a exposição não aumentou. | AC-03 |
| Dados pessoais | Dados reais podem entrar em evidências ou logs de smoke. | Usar apenas fixtures e ambientes isolados; revisar os logs. | AC-03 |
| Segredos | Credenciais podem vazar nos comandos ou artefatos de verificação. | Manter credenciais de teste fora dos arquivos versionados e omitir valores das evidências. | AC-03 |

## 10. Retorno

Reverter o commit da migração e voltar para o build e runtime de Java 21, incluindo toolchain, imagem ou artefato anterior quando aplicável. Nos consumidores, restaurar o conjunto compatível de versões antes de reiniciar e repetir a checagem funcional. Preservar os dados.

## 11. Referências

- Origem: solicitação direta de atualizar os projetos de `github-hp` para a versão estável mais recente.
- [Disponibilidade do Java 27 (Oracle)](https://docs.oracle.com/en-us/iaas/releasenotes/java-management/jdk-27-release-note.htm) e [calendário de suporte Java SE](https://www.oracle.com/technetwork/java/javase/tech/eol-135779.html).
- [Eclipse Temurin 27](https://adoptium.net/news/2026/10/eclipse-temurin-27-available).
- [Requisitos do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html): conferir a matriz da linha candidata antes de fixar a versão.
- [poc-dashboard: atualização para Java 27](../../../../../poc-dashboard/docs/sdd/specs/20261110-atualizar-java-27/spec.md).
