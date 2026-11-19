# Spring Boot Testing From Zero To Hero

작은 전자상거래 앱으로 Spring Boot 테스트를 익히는 한국어 가이드입니다. JPA와 MyBatis로 구현한 주문 예제를 바탕으로 어떤 동작을 검증할지, 어느 범위에서 테스트할지 살펴봅니다.

[산업의 역군](https://www.sankun.com/) 개발팀이 테스트 작성 기준을 공유하고 신규 백엔드 개발자의 온보딩을 돕기 위해 시작한 프로젝트입니다.

## 빠른 시작

JDK 21이 필요합니다. Maven Wrapper가 프로젝트에 지정된 Maven 버전을 내려받습니다. 예제는 Spring Boot 4.1, MyBatis Spring Boot Starter 4.1, JUnit 6의 Jupiter API를 사용합니다.

| 실행할 테스트 | macOS·Linux | Windows PowerShell |
| --- | --- | --- |
| 전체 테스트와 빌드 | `./mvnw clean verify` | `.\mvnw.cmd clean verify` |
| 주문 수량 인수 테스트 (JPA·MyBatis) | `./mvnw "-Dtest=*OrderQuantitySpecificationTest" test` | `.\mvnw.cmd "-Dtest=*OrderQuantitySpecificationTest" test` |

[GitHub Actions](https://github.com/junhkang/springboot-testing-from-zero-to-hero/actions/workflows/maven.yml)에서도 Ubuntu와 Windows에서 `clean verify`를 실행합니다.

### 예제 앱 실행

아래 명령 중 하나를 실행합니다. 첫 번째 명령은 기본 `jpa` 프로필을 사용합니다. Windows에서는 `./mvnw`를 `.\mvnw.cmd`로 바꿉니다.

```bash
./mvnw spring-boot:run
./mvnw spring-boot:run "-Dspring-boot.run.profiles=mybatis"
```

서버가 시작되면 [http://localhost:8080/orders](http://localhost:8080/orders)에서 샘플 주문을 조회할 수 있습니다. 두 프로필 모두 인메모리 H2를 사용하므로 앱을 재시작하면 데이터가 초기화됩니다. H2 콘솔이 필요하면 실행 명령에 `"-Dspring-boot.run.arguments=--spring.h2.console.enabled=true"`를 추가하고, 시작 로그에 표시된 JDBC URL로 [H2 콘솔](http://localhost:8080/h2-console)에 접속합니다.

## 학습 순서

1. [왜 테스트를 작성하는가?](docs/1.why.md) — 회귀 테스트와 FIRST 원칙을 이해합니다.
2. [어떻게 테스트를 작성하는가?](docs/2.how.md) — 검증하려는 동작에 맞춰 단위·슬라이스·통합 테스트를 선택합니다.
3. [언제 테스트를 작성하는가?](docs/3.when.md) — 신규 기능, 버그 수정, 리팩터링에서 테스트를 추가할 시점을 판단합니다.
4. [어디까지 테스트하는가?](docs/4.how-deep.md) — 경계값, 실패 후 상태, 테스트 계층과 커버리지를 기준으로 범위를 정합니다.
5. [스펙을 실행 가능한 테스트로](docs/5.spec-driven-development.md) — 주문 수량의 인수 조건을 정의하고 같은 시나리오를 JPA와 MyBatis에 적용합니다.

주문 예제부터 실습하려면 [5장](docs/5.spec-driven-development.md)에서 시작하세요. 도구별 공식 문서와 추가 읽을거리는 [참고 자료](REFERENCES.md)에 있습니다.