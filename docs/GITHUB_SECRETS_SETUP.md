# GitHub Secrets 설정 가이드

## 개요
이 문서는 CD 워크플로우에 필요한 GitHub Secrets 설정 방법을 설명합니다.

---

## 필요한 Secrets (총 5개)

### 1. DOCKER_USERNAME
- **설명**: Docker Hub 계정 사용자명
- **값 예시**: `choikang123`
- **획득 방법**:
  1. https://hub.docker.com 회원가입/로그인
  2. 우측 상단 프로필 → Account Settings
  3. Docker ID 확인

### 2. DOCKER_PASSWORD
- **설명**: Docker Hub Access Token (비밀번호보다 안전)
- **값 예시**: `dckr_pat_1a2b3c4d5e6f7g8h9i0j...`
- **획득 방법**:
  1. Docker Hub → Account Settings → Security
  2. "New Access Token" 클릭
  3. Description: `GitHub Actions CD`
  4. Permissions: `Read, Write, Delete`
  5. Generate → 토큰 복사 (한 번만 표시됨!)

### 3. EC2_HOST
- **설명**: EC2 인스턴스 Public IP 주소
- **값 예시**: `13.125.234.56`
- **획득 방법**:
  1. AWS EC2 콘솔
  2. Instances → 해당 인스턴스 선택
  3. "Public IPv4 address" 복사

### 4. EC2_USERNAME
- **설명**: SSH 접속 사용자명
- **값**: AMI에 따라 다름
  - Ubuntu: `ubuntu`
  - Amazon Linux: `ec2-user`
  - CentOS: `centos`
  - Debian: `admin`
- **가장 일반적**: `ubuntu`

### 5. EC2_SSH_KEY
- **설명**: EC2 접속용 Private Key 전체 내용
- **값 형식**:
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
(여러 줄)
-----END RSA PRIVATE KEY-----
```
- **획득 방법**:
  - EC2 생성 시 다운로드한 `.pem` 파일 내용 복사
  - Windows: `type your-key.pem`
  - Mac/Linux: `cat your-key.pem`

**⚠️ 주의**: BEGIN부터 END까지 전체 복사, 줄바꿈 포함

---

## GitHub에 Secrets 등록하기

### 방법

1. GitHub 저장소 페이지 이동
2. 상단 메뉴 `Settings` 클릭
3. 왼쪽 메뉴 `Secrets and variables` → `Actions` 클릭
4. 우측 상단 `New repository secret` 클릭
5. Name과 Value 입력 후 `Add secret` 클릭

### 등록 순서

```
1. DOCKER_USERNAME 등록
2. DOCKER_PASSWORD 등록
3. EC2_HOST 등록
4. EC2_USERNAME 등록
5. EC2_SSH_KEY 등록 (가장 긴 값)
```

---

## 설정 확인

### 1. Secrets 등록 확인
- Settings → Secrets and variables → Actions
- 5개 Secret이 모두 표시되는지 확인
- ⚠️ 값은 표시되지 않음 (보안)

### 2. 워크플로우 테스트
```bash
# 로컬에서
git add .
git commit -m "test: CD 워크플로우 테스트"
git push origin main
```

### 3. GitHub Actions 로그 확인
- GitHub → Actions 탭
- 실행 중인 워크플로우 클릭
- 각 단계별 로그 확인

---

## 트러블슈팅

### Docker Hub 로그인 실패
```
Error: unauthorized: incorrect username or password
```
**해결**: DOCKER_USERNAME, DOCKER_PASSWORD 재확인

### EC2 SSH 접속 실패
```
Error: Permission denied (publickey)
```
**해결**:
1. EC2_SSH_KEY에 전체 키 내용 포함되었는지 확인
2. EC2_USERNAME이 올바른지 확인 (ubuntu vs ec2-user)
3. EC2 보안 그룹에서 22번 포트 열려있는지 확인

### EC2 연결 타임아웃
```
Error: dial tcp: i/o timeout
```
**해결**:
1. EC2_HOST가 올바른 IP인지 확인
2. EC2 인스턴스가 실행 중인지 확인
3. 보안 그룹 Inbound Rules 확인

---

## 보안 주의사항

### ✅ 해야 할 것
- Docker Hub Access Token 사용 (비밀번호 대신)
- SSH Key는 절대 코드에 포함하지 말 것
- Secrets는 GitHub에서만 관리
- 주기적으로 Token/Key 갱신

### ❌ 하지 말아야 할 것
- SSH Key를 코드에 커밋
- Secrets를 로그에 출력
- 공개 저장소에 민감 정보 포함
- 여러 프로젝트에 같은 Key 재사용

---

## 참고 자료

- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Docker Hub Access Tokens](https://docs.docker.com/docker-hub/access-tokens/)
- [AWS EC2 Key Pairs](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html)
