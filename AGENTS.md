# Repository Guidelines

## Project Structure & Module Organization
- Modules: `redis-*/`, `amqp-*/`, `influx2-*/`, `context-*/` (multi-module Maven).
- Source: `<module>/src/main/java`, resources in `<module>/src/main/resources`.
- Auto-config: register classes in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Package root: `com.childrengreens.multi.source`. Class names typically follow `*MultiSources*` (e.g., `RedisMultiSourcesAutoConfiguration`).
- Bean names: generated as `<sourceName><ClassSimpleName>` (e.g., `ordersConnectionFactory`).

## Build, Test, and Development Commands
- `mvn clean install`: build all modules; runs license formatting at `validate`.
- `mvn -pl redis-multi-source-spring-boot-starter -am package`: build a single module and its deps.
- `mvn validate`: apply license headers from `licence-header.template`.
- Release: `mvn -Prelease deploy` (signs artifacts, attaches sources/javadocs). Requires GPG + OSSRH credentials.

## Coding Style & Naming Conventions
- Java 17; Spring Boot BOM 3.4.x via parent POM.
- Indentation: 4 spaces; keep lines readable (~120 cols). Follow existing conventions in modules.
- Keep package `com.childrengreens.multi.source` and class naming consistent: `XxxMultiSources...`, `...Registrar`, `...Properties`.
- Properties prefixes: `spring.multi-sources.redis|rabbitmq|influx` mapped via `@ConfigurationProperties`.
- License headers are enforced on Java and POM files; run `mvn validate` before committing.

## Testing Guidelines
- Place tests in `<module>/src/test/java`; prefer JUnit 5 and `spring-boot-starter-test` (scope `test`).
- Name tests `*Tests.java` (e.g., `RedisRegistrarTests`).
- Run tests with `mvn test` or as part of `mvn verify`.
- For config-heavy code, use Spring Boot tests to validate property binding and bean registration.

## Commit & Pull Request Guidelines
- Commits: concise, imperative (“Add…”, “Fix…”). Prefix module when helpful (e.g., `redis: Fix template registration`).
- PRs: include description, rationale, and usage/config changes (update `README.adoc` if properties change). Link related issues.
- Checks: ensure `mvn -q verify` succeeds and license headers are formatted.

## Extending the Starters
- Add a module `xyz-multi-source-spring-boot-starter/` and include it in root `pom.xml <modules>`.
- Define `XyzMultiSourcesProperties` with prefix `spring.multi-sources.xyz`.
- Implement a registrar by extending `AbstractMultiSourcesRegistrar` and an `@AutoConfiguration` that `@Import`s it.
- Register auto-config in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Document new properties and bean naming examples in `README.adoc`.

