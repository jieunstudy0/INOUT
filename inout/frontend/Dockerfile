# ================================================================
# INOUT Frontend (React 19 + Vite) — Multi-Stage Docker Build
#
# ⚠️ 빌드 컨텍스트 주의:
#   이 Dockerfile은 반드시 "inout/" 프로젝트 루트를 빌드 컨텍스트로 사용해야 한다.
#   (frontend/ 폴더 단독이 아님 — nginx/default.conf를 함께 복사하기 위함)
#
# 실행 예시 (inout/ 디렉터리에서 실행):
#   docker build -f frontend/Dockerfile -t inout-frontend:latest .
#   docker run -p 80:80 inout-frontend:latest
#
# docker-compose 사용 시:
#   services:
#     frontend:
#       build:
#         context: .
#         dockerfile: frontend/Dockerfile
# ================================================================

# ────────────────────────────────────────────────
# Stage 1: Build (Node.js 20 — package.json 기준 Vite 8 / React 19 요구사항)
# ────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# package*.json 먼저 복사해 의존성 레이어 캐시 활용 (소스만 바뀌면 npm ci 재실행 안 함)
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci

# 소스 복사 및 정적 파일 빌드
# apiClient.js의 baseURL='/api' 상대경로를 그대로 사용하므로 별도 빌드 타임 환경변수 불필요
COPY frontend/ ./
RUN npm run build

# ────────────────────────────────────────────────
# Stage 2: Runtime (Nginx — 정적 파일 서빙 + 백엔드 리버스 프록시)
# ────────────────────────────────────────────────
FROM nginx:1.27-alpine

# React 빌드 산출물(dist/)을 Nginx 문서 루트로 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# 리버스 프록시 설정 — 동일 오리진에서 /api, /uploads, /oauth2 등을 backend 컨테이너로 전달
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# 컨테이너 헬스체크
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
