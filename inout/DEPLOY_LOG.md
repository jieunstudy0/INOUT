# 배포 이력 (DEPLOY_LOG)

실제 AWS EC2 배포와 관련된 사건(인프라 변경, 장애, 수정)을 기록한다.
코드 변경 자체의 이력은 `git log`로 충분히 추적되므로, 여기는 "언제·왜·무엇을"
중심으로 짧게 남긴다. IP 등 유동적인 접속 정보는 기록하지 않는다
(재시작 시 퍼블릭 IP가 바뀌므로 의미가 없고, 공개 저장소에 굳이 남길 정보도 아님).

---

## 2026-08-28 — 최초 배포 (1차, IP 기반)

- **환경**: AWS EC2 `t3.small`, Amazon Linux 2023, 서울 리전, 스토리지 20GB(gp3)
- **구성**: Docker Compose 4개 컨테이너(mysql, redis, backend, frontend), 도메인/HTTPS 미적용
- **진행 순서**: 로컬 Docker Compose로 먼저 스모크 테스트 → 이슈 수정 → EC2 인스턴스 생성 →
  Docker/Compose/buildx 설치 → 스왑 2GB 설정 → `.env` 구성 → 빌드/기동 → 최초 DB 스키마 생성
  (`DDL_AUTO=update` 1회) → `validate` 모드로 복귀
- **배포 전 로컬 테스트에서 발견·수정한 이슈**
  - Docker 빌드가 WSL2 환경에서 IPv6로 빠지며 응답 없이 멈추는 문제
    → Gradle/Node 모두 IPv4 우선 사용하도록 강제 (`Dockerfile`, `frontend/Dockerfile`)
  - `.env`에서 OAuth2 키를 빈 값(`KEY=`)으로 두면 코드의 더미 기본값이 적용되지 않고
    기동이 실패하는 문제 확인 → 값이 없을 땐 줄 자체를 주석 처리해야 함을 확인
  - `User.userRoles`의 불필요한 `cascade = CascadeType.ALL` 제거
    (UserRole은 항상 UserRoleRepository로 직접 저장하므로 사용되지 않는 cascade였음)
- **EC2에서 처음 발견한 이슈**
  - Amazon Linux 2023의 `dnf install docker`에는 `buildx` 플러그인이 기본 포함되어 있지 않아
    `docker compose build`가 "compose build requires buildx" 오류로 조용히 스킵됨
    → `docker-buildx` CLI 플러그인 수동 설치로 해결
- **결과**: 4개 컨테이너 모두 healthy, 외부 인터넷에서 로그인 → 대시보드까지 정상 동작 확인
- **비용 관리**: 상시 가동 대신 데모/테스트 시에만 EC2를 시작(Start)하고, 끝나면 중지(Stop)하는
  방식으로 운영. 탄력적 IP는 아직 할당하지 않음 — 재시작할 때마다 퍼블릭 IP가 바뀔 수 있음
  (필요 시 재기동 후 `.env`의 `APP_SERVER_URL`/`APP_FRONTEND_URL`/`CORS_ALLOWED_ORIGINS`를
  새 IP로 갱신 후 `docker compose up -d`로 반영)

<!-- 다음 항목은 이 위에 최신순으로 추가할 것 -->
