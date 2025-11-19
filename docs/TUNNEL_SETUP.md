# 로컬 개발 환경에서 AI 콜백 설정 가이드

## 문제 상황

AI 서버가 분석 완료 후 콜백을 보낼 때, `localhost:8080`으로는 접근할 수 없습니다.
- AI 서버 입장에서 `localhost`는 AI 서버 자기 자신을 의미
- 개발자 PC의 Spring Boot 애플리케이션에 도달 불가

## 해결 방법: SSH 터널링 (localhost.run)

**localhost.run**은 무료 SSH 터널 서비스로, 인증 없이 로컬 포트를 공개 URL로 노출할 수 있습니다.

### 특징
- ✅ 무료
- ✅ 인증 불필요
- ✅ SSH만 있으면 사용 가능
- ⚠️ **URL이 매번 변경됨** (재시작 시마다 새 URL 생성)

---

## 실행 방법

### 1. 터널 시작

터미널에서 다음 명령어 실행:

```bash
ssh -R 80:localhost:8080 nokey@localhost.run
```

### 2. 생성된 URL 확인

터널이 시작되면 다음과 같은 출력이 나타납니다:

```
===============================================================================
Welcome to localhost.run!
...
da7941d39b603e.lhr.life tunneled with tls termination, https://da7941d39b603e.lhr.life
...
===============================================================================
```

**공개 URL**: `https://da7941d39b603e.lhr.life` (예시)

### 3. .env.dev 파일 업데이트

생성된 URL을 복사하여 `.env.dev` 파일 수정:

```bash
# Before
APP_CALLBACK_BASE_URL=http://localhost:8080

# After (생성된 URL로 변경)
APP_CALLBACK_BASE_URL=https://da7941d39b603e.lhr.life
```

### 4. 애플리케이션 재시작

```bash
# Gradle 실행 중이면 중지 (Ctrl+C)
./gradlew bootRun
```

또는 IntelliJ에서 재시작

### 5. 터널 동작 확인

브라우저나 curl로 헬스체크:

```bash
curl https://da7941d39b603e.lhr.life/api/analysis/callback/health
```

예상 응답:
```json
{"status":"healthy","service":"analysis-callback"}
```

---

## IntelliJ에서 실행 방법

### 방법 1: Gradle Task 실행 (추천)

1. IntelliJ 우측 **Gradle** 탭 열기
2. `capstone_java` > `Tasks` > `application` > `bootRun` 더블클릭
3. 애플리케이션이 시작되면 로그에서 확인:
   ```
   Started CapstoneJavaApplication in X.XXX seconds
   ```

### 방법 2: Run Configuration 생성

1. 상단 메뉴: `Run` > `Edit Configurations...`
2. `+` 클릭 > `Gradle` 선택
3. 설정:
   - **Name**: `BootRun`
   - **Gradle project**: `capstone_java`
   - **Tasks**: `bootRun`
   - **Environment variables**:
     ```
     APP_CALLBACK_BASE_URL=https://xxxxxxxx.lhr.life
     ```
     (터널 URL로 변경)
4. `Apply` > `OK`
5. 상단 Run 버튼 클릭

### 방법 3: Main 클래스 직접 실행

1. `src/main/java/.../CapstoneJavaApplication.java` 열기
2. 클래스 옆 ▶️ 버튼 클릭 > `Run 'CapstoneJavaApplication'`
3. 단, 이 방법은 환경변수 설정이 어려우므로 비추천

---

## 주의사항

### 1. 터널 URL은 매번 바뀝니다
- 터널을 재시작할 때마다 새로운 URL 생성
- 재시작 후 **반드시** `.env.dev` 업데이트 필요

### 2. 터널 유지
- SSH 연결이 끊기면 터널도 종료됨
- 터미널 창을 닫지 말 것
- 백그라운드 실행 방법:
  ```bash
  ssh -R 80:localhost:8080 nokey@localhost.run > tunnel.log 2>&1 &
  ```

### 3. 애플리케이션이 8080 포트에서 실행 중이어야 함
- 터널 시작 전에 Spring Boot 애플리케이션 먼저 실행
- 또는 동시에 실행 가능

---

## 대안: ngrok (유료/제한적)

ngrok을 사용하려면 계정 생성 및 authtoken 필요:

1. https://ngrok.com 회원가입
2. authtoken 발급
3. 실행:
   ```bash
   ngrok config add-authtoken YOUR_TOKEN
   ngrok http 8080
   ```

**장점**: 고정 도메인 사용 가능 (유료)
**단점**: 무료는 제한적, 인증 필요

---

## 프로덕션 환경 (EC2)

EC2 배포 시에는 터널 불필요:
- EC2 공개 IP 사용: `http://52.79.145.91:8080`
- `.env` 파일에 이미 설정됨
- AI 서버가 직접 접근 가능

---

## 문제 해결

### "no tunnel here :(" 메시지
- 터널이 종료되었거나 URL이 만료됨
- 터널 재시작 필요

### 콜백이 오지 않음
1. 터널 URL 확인: `.env.dev` 파일 점검
2. 애플리케이션 재시작 확인
3. AI 서버 로그 확인 (AI 팀 문의)
4. 방화벽/보안 그룹 점검

### "Connection refused"
- Spring Boot 애플리케이션이 실행 중인지 확인
- 8080 포트가 사용 가능한지 확인:
  ```bash
  netstat -ano | grep 8080
  ```
