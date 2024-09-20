# Spring Boot Testing From Zero To Hero

스프링 부트에서 테스트 코드를 처음부터 고급까지 다루는 종합 가이드입니다.

## 소개

많은 좋은 개발자분들께서 이미 테스트의 중요성과 작성 방법에 대해 잘 정리하고 공유하고 있습니다. 이러한 자료들을 참고하여 TDD(Test-Driven Development), BDD(Behavior-Driven Development) 방식으로 개발을 진행하기도 하지만, **기존 코드베이스에 테스트 코드를 작성할 때**는 여전히 많은 고민이 따릅니다.

**산군 개발팀**에서는 테스트 코드를 작성할 시 개발자들이 동일한 수준의 이해도를 가지고 있음에도 불구하고, **테스트의 깊이**나 **테스트의 방향**이 서로 다른 경우가 종종 발생합니다. 이러한 상황을 개선하고, 더 안정적인 개발 프로세스를 구축하고자 **어느 정도의 규칙을 정하고**, 개발팀이 **같은 생각을 하며 같은 방향을 보며 개발**할 수 있도록 이 레포지토리를 만들게 되었습니다. 또한 **신규 백엔드 개발자의 온보딩**을 지원하기 위한 자료로도 활용하고자 합니다.

특히, **최적의 효율을 내는 테스트 방향 및 뎁스를 정하기 위해**, 이 레포지토리는 다음의 네 가지 핵심 질문에 대한 고민을 하고자 합니다.

1. **WHY** - 왜 테스트를 작성해야 하는가?
2. **HOW** - 테스트 코드를 어떻게 작성해야 하는가?
3. **WHEN** - 언제 테스트 코드를 적용해야 하는가?
4. **HOW MUCH** - 얼마나 깊게 테스트 코드를 작성해야 하는가?

간단한 **전자상거래 시스템**을 예제로 삼아, 다양한 테스트 케이스와 실용적인 예제 코드를 통해 위의 핵심 질문에 대한 답을 찾아보고자 합니다.

# 목차

1. [WHY - 왜 테스트를 작성해야 하는가?](https://github.com/junhkang/springboot-testing-from-zero-to-hero/blob/main/docs/1.WHY%20-%20%EC%99%9C%20%ED%85%8C%EC%8A%A4%ED%8A%B8%EB%A5%BC%20%EC%9E%91%EC%84%B1%ED%95%B4%EC%95%BC%20%ED%95%98%EB%8A%94%EA%B0%80%3F.md)
    - 1.1 테스트 코드의 중요성
    - 1.2 테스트 코드 작성의 장점
        - 1.2.1 안정적인 개발 환경 구축
        - 1.2.2 버그 감소 및 코드 품질 향상
        - 1.2.3 리팩토링의 용이성
        - 1.2.4 단일 책임 원칙(SOLID) 준수
    - 1.3 테스트를 작성하지 않았을 때의 문제점
    - 1.4 좋은 테스트 코드 - FIRST 원칙


2. [HOW - 테스트 코드를 어떻게 작성해야 하는가?](https://github.com/junhkang/springboot-testing-from-zero-to-hero/blob/main/docs/2.HOW%20-%20%ED%85%8C%EC%8A%A4%ED%8A%B8%20%EC%BD%94%EB%93%9C%EB%A5%BC%20%EC%96%B4%EB%96%BB%EA%B2%8C%20%EC%9E%91%EC%84%B1%ED%95%B4%EC%95%BC%20%ED%95%98%EB%8A%94%EA%B0%80%3F.md)
    - 2.1 테스트 케이스 선택 방법
    - 2.2 TDD (Test-Driven Development) 방법론
    - 2.3 다양한 테스트 종류와 계층 구조 이해
    - 2.4 JUnit5 활용
    - 2.5 Mockito와 같은 Mocking 프레임워크 사용
    - 2.6 다양한 테스트 어노테이션 및 도구 활용


3. [WHEN - 언제 테스트 코드를 적용해야 하는가?](https://github.com/junhkang/springboot-testing-from-zero-to-hero/blob/main/docs/3.WHEN%20-%20%EC%96%B8%EC%A0%9C%20%ED%85%8C%EC%8A%A4%ED%8A%B8%20%EC%BD%94%EB%93%9C%EB%A5%BC%20%EC%A0%81%EC%9A%A9%ED%95%B4%EC%95%BC%20%ED%95%98%EB%8A%94%EA%B0%80%3F.md)
    - 3.1 TDD와 BDD의 개념 및 적용 시점
    - 3.2 기존 코드베이스에 테스트 추가하기
    - 3.3 새로운 기능 개발 시 테스트 작성 시점
    - 3.4 리팩토링 시 테스트의 역할
    - 3.5 테스트 작성의 우선순위와 체크리스트


4. [HOW DEEP - 얼마나 깊게 테스트 코드를 작성해야 하는가?](https://github.com/junhkang/springboot-testing-from-zero-to-hero/blob/main/docs/4.HOW%20DEEP%20-%20%EC%96%BC%EB%A7%88%EB%82%98%20%EA%B9%8A%EA%B2%8C%20%ED%85%8C%EC%8A%A4%ED%8A%B8%20%EC%BD%94%EB%93%9C%EB%A5%BC%20%EC%9E%91%EC%84%B1%ED%95%B4%EC%95%BC%20%ED%95%98%EB%8A%94%EA%B0%80%3F.md)
    - 4.1 테스트 깊이를 결정하는 기준
    - 4.2 테스트 커버리지 및 품질 지표 활용
    - 4.3 오버테스팅의 문제점
    - 4.4 효율적인 테스트 범위 설정을 위한 체크리스트
    - 4.5 리팩토링과 테스트의 균형 잡기