# EC2 초기 설정 가이드

CD 파이프라인을 실행하기 전 EC2 인스턴스에서 필요한 초기 설정입니다.

## 1. Docker 설치

```bash
# Docker 설치
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl software-properties-common

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io
```

## 2. Docker Compose 설치

```bash
# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose

# 설치 확인
docker-compose --version
```

## 3. Ubuntu 사용자 Docker 그룹 추가 (중요)

**CD 파이프라인이 sudo 없이 Docker를 실행하려면 필수입니다.**

```bash
# ubuntu 사용자를 docker 그룹에 추가
sudo usermod -aG docker ubuntu

# 변경사항 적용을 위해 로그아웃 후 재로그인
exit
# SSH로 다시 접속

# 확인 (docker가 출력되어야 함)
groups ubuntu
```

## 4. 프로젝트 디렉토리 생성

```bash
# 프로젝트 디렉토리 생성
mkdir -p /home/ubuntu/capstone-crawler
cd /home/ubuntu/capstone-crawler
```

## 5. 설정 확인

```bash
# Docker 실행 테스트 (sudo 없이 실행되어야 함)
docker ps

# Docker Compose 테스트
docker-compose version
```

## 6. 방화벽/보안 그룹 설정

EC2 보안 그룹에서 다음 포트를 열어야 합니다:

- 8080: Spring Boot 애플리케이션
- 3307: MySQL (필요시)
- 9092: Kafka (필요시)
- 8090: Kafka UI (필요시)

## 트러블슈팅

### "permission denied" 오류 발생 시

```bash
# docker 그룹이 제대로 적용되었는지 확인
groups

# docker가 목록에 없다면 로그아웃 후 재로그인
exit
# SSH로 다시 접속
```

### Docker 데몬이 실행되지 않는 경우

```bash
# Docker 서비스 시작
sudo systemctl start docker

# 부팅 시 자동 시작 설정
sudo systemctl enable docker

# 상태 확인
sudo systemctl status docker
```

## 완료 확인

모든 설정이 완료되면:

```bash
# sudo 없이 실행되어야 함
docker ps
docker-compose version
groups | grep docker
```

위 명령어들이 오류 없이 실행되면 CD 파이프라인을 실행할 준비가 완료된 것입니다.

EC2에 설치해야 할 것들:

1. Java (JDK)

sudo apt update
sudo apt install openjdk-17-jdk -y
java -version

2. MySQL

sudo apt install mysql-server -y
sudo systemctl start mysql
sudo systemctl enable mysql

# MySQL 설정
sudo mysql -u root -p
CREATE DATABASE sigai;

3. MongoDB

# MongoDB 저장소 추가
wget -qO - https://www.mongodb.org/static/pgp/server-6.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list

sudo apt update
sudo apt install -y mongodb-org
sudo systemctl start mongod
sudo systemctl enable mongod

# MongoDB 사용자 생성
mongosh
use admin
db.createUser({user: "root", pwd: "0000", roles: ["root"]})

4. 환경 변수 (.env 파일)

EC2의 프로젝트 루트에 .env 파일 생성:
# MySQL
MYSQL_ROOT_PASSWORD=password
MYSQL_DATABASE=sigai
MYSQL_USERNAME=root
MYSQL_PASSWORD=password

# MongoDB
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=0000
MONGO_INITDB_DATABASE=sigai

# 서울시 API 키
PROFIT_API_KEY=634c42756263686f383679594f4941
STORE_API_KEY=44527a784363686f38384542424c68

5. (선택) Docker & Docker Compose

만약 Docker로 배포하려면:
sudo apt install docker.io docker-compose -y
sudo systemctl start docker
sudo systemctl enable docker
