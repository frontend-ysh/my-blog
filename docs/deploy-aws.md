# AWS에 배포하기

AWS의 가상 서버(EC2) 한 대에 이 블로그를 올리고, 도메인과 HTTPS를 붙이는 순서를 적은 문서다.

```
                        ┌──────────────── EC2 서버 (Ubuntu + Docker) ────────────────┐
브라우저 ──(HTTPS)──▶  │  Caddy (80, 443) ──▶ Next.js ──▶ Spring Boot ──▶ PostgreSQL │
   blog.example.com     └────────────────────────────────────────────────────────────┘
```

- 서버 안의 네 프로그램은 `docker-compose.prod.yml`이 한 번에 띄운다.
- 밖에서 들어올 수 있는 문은 Caddy의 80, 443번뿐이다. Caddy가 HTTPS 인증서를 자동으로 받아 오고 갱신한다.

**확인된 것과 아닌 것.** 이 구성(Caddy 포함)은 개발 환경에서 `localhost`용 인증서로 띄워 로그인, 글쓰기, 이미지 첨부, 댓글, 백업과 복구까지 확인했다. AWS 콘솔에서 하는 작업과 실제 도메인의 인증서 발급은 계정이 있어야 해서 확인하지 못했다. 콘솔의 메뉴 이름은 바뀔 수 있으니, 아래 표의 "무엇을 어떤 값으로"를 기준으로 따라간다.

## 0. 비용부터

요금은 달러로 매겨지고 수시로 바뀐다. 아래는 서울 리전 기준의 어림값이다(2026년 11월에 찾아본 값).

| 항목 | 한 달 어림값 | 비고 |
|---|---|---|
| EC2 `t3.small` (2 vCPU, 메모리 2GB) | 약 19달러 | 시간당 약 0.026달러. 켜 둔 시간만큼 낸다 |
| 디스크 (EBS gp3 20GB) | 약 2달러 | 서버를 꺼 두어도 낸다 |
| 공인 IPv4 주소 1개 | 약 3.6달러 | 시간당 0.005달러. 서버를 꺼 두어도 낸다 |
| 합계 | **약 25달러** | |
| 도메인 | 1년에 1~2만 원대 | 등록 업체와 주소 끝자리(.com, .dev 등)에 따라 다르다 |

- **새 계정의 무료 혜택**: 2025년 7월 15일 이후에 만든 계정은 가입할 때 크레딧을 받고(가입 시 100달러, 안내된 활동을 하면 최대 100달러 더), 무료 플랜은 6개월이 지나거나 크레딧을 다 쓰면 끝난다. `t3.small`은 무료 혜택 대상 인스턴스에 들어 있다. 정확한 조건은 가입 화면과 [AWS 프리 티어 안내](https://aws.amazon.com/free/)에서 확인한다.
- **요금 폭탄을 막는 두 가지**: 가입 직후 결제 화면(Billing)에서 **예산 알림**(예: 월 10달러를 넘으면 메일)을 만들어 둔다. 그리고 블로그를 그만 운영할 때는 이 문서 끝의 "정리하기"를 따라 전부 지운다.

메모리 2GB짜리를 고른 근거: 이 블로그는 실행 중에 네 컨테이너를 합쳐 약 0.5GB를 쓰고, 서버에서 이미지를 빌드할 때는 최대 1.4GB 안팎을 쓴다(개발 환경에서 잰 값). 1GB짜리(`t3.micro`)는 빌드가 버겁다.

## 1. AWS 계정 준비

1. [aws.amazon.com](https://aws.amazon.com/)에서 계정을 만든다. 결제 카드가 필요하다.
2. 처음 만든 계정(루트 계정)에 **MFA(2단계 인증)**를 건다. 이 계정이 털리면 남이 내 카드로 서버를 마음껏 만들 수 있다.
3. 결제 화면에서 예산 알림을 만든다.
4. 콘솔 오른쪽 위의 리전을 **아시아 태평양(서울) `ap-northeast-2`**로 바꾼다. 리전은 서버가 놓일 지역이고, 방문자와 가까울수록 빠르다. 이후의 작업은 모두 이 리전에서 한다.

## 2. 서버(EC2 인스턴스) 만들기

콘솔에서 EC2 → 인스턴스 시작. 아래 값으로 채운다.

| 항목 | 값 | 이유 |
|---|---|---|
| 이름 | `blog` | 알아보기 위한 이름 |
| 운영체제 이미지(AMI) | Ubuntu Server 24.04 LTS, 64비트(x86) | 자료가 많고 Docker 설치가 쉽다. 더 새로운 LTS 버전이 보이면 그것도 된다 |
| 인스턴스 유형 | `t3.small` | 위 "비용부터" 참고 |
| 키 페어 | 새로 만들기 → `.pem` 파일을 내려받는다 | 서버에 접속할 때 쓰는 열쇠. **다시 받을 수 없으니** 잘 보관하고, 저장소에 올리지 않는다 |
| 퍼블릭 IP 자동 할당 | 활성화 | |
| 스토리지 | 20GB, gp3 | 기본값 8GB는 Docker 이미지와 빌드 캐시로 금방 찬다 |

**보안 그룹**(서버 앞의 방화벽)은 새로 만들고 들어오는 규칙을 세 개만 둔다.

| 유형 | 포트 | 소스 | 용도 |
|---|---|---|---|
| SSH | 22 | **내 IP** | 서버 접속. 누구에게나 열어 두면 비밀번호 대입 시도가 끊임없이 들어온다 |
| HTTP | 80 | 어디서나 (0.0.0.0/0) | 인증서 발급 확인, https로 돌려보내기 |
| HTTPS | 443 | 어디서나 (0.0.0.0/0) | 블로그 접속 |

집이나 카페의 인터넷은 IP가 바뀔 수 있다. 어느 날 SSH 접속이 안 되면 이 규칙의 "내 IP"를 다시 지정한다.

## 3. 고정 IP(탄력적 IP) 연결

인스턴스에 자동으로 붙은 공인 IP는 서버를 껐다 켜면 바뀐다. 도메인이 가리킬 주소가 바뀌면 안 되므로 고정 IP를 받는다.

1. EC2 → 탄력적 IP → 탄력적 IP 주소 할당
2. 받은 주소를 선택 → 탄력적 IP 주소 연결 → 방금 만든 인스턴스

이 주소를 아래에서 `서버IP`라고 부른다. 탄력적 IP는 인스턴스에 연결하지 않고 갖고만 있어도 요금이 나온다.

## 4. 도메인 연결

HTTPS 인증서는 도메인에 대해 발급된다. 도메인이 없으면 먼저 산다(가비아, Cloudflare, Namecheap, AWS Route 53 등 어디서 사도 된다).

도메인을 산 곳의 DNS 설정 화면에서 **A 레코드**를 하나 만든다.

| 유형 | 이름(호스트) | 값 |
|---|---|---|
| A | `blog` (→ `blog.example.com`). 도메인 자체를 쓰려면 `@` | `서버IP` |

퍼지는 데 몇 분에서 길면 몇 시간이 걸린다. 내 컴퓨터에서 확인한다.

```bash
nslookup blog.example.com
```

`서버IP`가 나오면 된 것이다. **이게 되기 전에 7단계(띄우기)로 가지 않는다.** 인증서 발급 기관이 도메인으로 서버를 찾아오지 못하면 발급이 실패하고, 실패가 반복되면 한동안 발급이 막힌다.

## 5. 서버 준비

내 컴퓨터의 터미널에서 서버에 접속한다(윈도우 10 이상의 PowerShell에도 `ssh`가 들어 있다).

```bash
chmod 400 blog-key.pem
```

```bash
ssh -i blog-key.pem ubuntu@서버IP
```

여기서부터의 명령은 서버 안에서 실행한다.

**스왑 추가.** 메모리가 모자랄 때 디스크를 대신 쓰게 하는 설정이다. 느리지만, 빌드 도중 메모리가 잠깐 넘쳐서 프로그램이 죽는 일을 막아 준다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

```bash
free -h
```

`Swap:` 줄에 2.0Gi가 보이면 된다.

**Docker 설치.** Docker가 제공하는 설치 스크립트를 쓴다. Compose도 함께 설치된다.

```bash
curl -fsSL https://get.docker.com | sudo sh
```

```bash
sudo usermod -aG docker ubuntu
```

두 번째 명령은 `sudo` 없이 `docker`를 쓰기 위한 것이다. 적용되려면 `exit`로 나갔다가 다시 `ssh`로 접속한다. 확인:

```bash
docker run --rm hello-world
```

```bash
docker compose version
```

## 6. 코드 받기와 설정

```bash
git clone https://github.com/frontend-ysh/my-blog.git
```

```bash
cd my-blog
```

```bash
cp .env.example .env
```

```bash
chmod 600 .env
```

비밀 값은 서버에서 만든다. 아래 명령을 실행해 나온 값을 `.env`에 붙여 넣는다.

```bash
openssl rand -hex 24
```

```bash
openssl rand -base64 48
```

첫 번째는 `DB_PASSWORD`에, 두 번째는 `JWT_SECRET`에 쓴다. `nano .env`로 열어 채운다.

```
DB_PASSWORD=첫 번째 명령의 결과
JWT_SECRET=두 번째 명령의 결과
ADMIN_USERNAME=내가 쓸 아이디
ADMIN_PASSWORD='길고 추측하기 어려운 비밀번호'
DOMAIN=blog.example.com
COMPOSE_PROFILES=https
```

- `ADMIN_PASSWORD`는 작은따옴표로 감싼다. 감싸지 않으면 `$`나 `#` 같은 글자가 다르게 해석될 수 있다. (비밀번호 안에 작은따옴표는 쓰지 않는다.)
- `COMPOSE_PROFILES=https`는 HTTPS 프록시(Caddy)까지 띄우라는 뜻이다. 이 줄이 없으면 Caddy가 뜨지 않아 밖에서 접속할 수 없다.
- 관리자 아이디로 `admin`, 비밀번호로 개발용 값(`admin1234`)을 쓰지 않는다. 저장소가 공개되어 있어 누구나 아는 값이다. 로그인 시도 횟수를 제한하는 기능이 아직 없으므로, 비밀번호 길이가 유일한 방어다.
- `DB_PASSWORD`와 관리자 계정은 **처음 띄울 때** 만들어진다. 띄운 뒤에 `.env`만 고쳐서는 바뀌지 않는다.
- `DOMAIN`에는 `https://` 없이 4단계에서 연결한 도메인만 적는다.

## 7. 띄우기

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

처음에는 이미지 두 개를 빌드하느라 몇 분 걸린다. 끝나면 상태를 본다.

```bash
docker compose -f docker-compose.prod.yml ps
```

`db`, `backend`, `frontend`가 `healthy`, `caddy`가 `Up`이면 된다. 인증서를 받았는지는 Caddy의 로그로 확인한다.

```bash
docker compose -f docker-compose.prod.yml logs caddy
```

`certificate obtained successfully`가 보이면 성공이다. 브라우저에서 `https://blog.example.com`을 열어 확인한다.

- 주소창에 자물쇠가 보이고 첫 화면에 "아직 글이 없습니다"가 나온다.
- `http://blog.example.com`으로 열어도 `https://`로 바뀐다.
- `/login`에서 `.env`에 적은 관리자 계정으로 로그인하고 글을 하나 써 본다.

서버가 재부팅되어도 Docker가 자동으로 시작되고, 컨테이너도 다시 뜬다(`restart: unless-stopped`).

## 8. 새 버전 올리기

코드를 고쳐 GitHub의 `main`에 합친 뒤, 서버에서:

```bash
cd ~/my-blog
```

```bash
git pull
```

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

바뀐 이미지의 컨테이너만 새로 만들어진다. 교체되는 몇 초에서 수십 초 동안은 사이트가 응답하지 않는다. 글과 이미지는 볼륨에 있어서 그대로 남는다. 쓰지 않게 된 옛 이미지는 가끔 치운다.

```bash
docker image prune -f
```

## 9. 백업과 복구

글·댓글은 DB 볼륨에, 올린 이미지는 `uploads` 볼륨에 있다. 둘 다 서버의 디스크에만 있으므로, 서버를 지우거나 디스크가 망가지면 함께 사라진다.

**백업**

```bash
./scripts/backup.sh
```

`backups/` 폴더에 `db-날짜.dump`와 `uploads-날짜.tar.gz`가 생긴다. 종류별로 최근 7개만 남는다.

매일 새벽 4시에 자동으로 돌리려면 `crontab -e`를 실행해 아래 한 줄을 넣는다.

```
0 4 * * * cd /home/ubuntu/my-blog && ./scripts/backup.sh >> backups/backup.log 2>&1
```

서버 안의 백업은 서버가 사라지면 같이 사라진다. 가끔 내 컴퓨터로 가져온다(내 컴퓨터에서 실행).

```bash
scp -i blog-key.pem -r ubuntu@서버IP:my-blog/backups ./blog-backups
```

**복구** (서버에서, 파일 이름은 되돌리려는 시점의 것으로 바꾼다)

```bash
docker compose -f docker-compose.prod.yml stop frontend backend
```

```bash
docker compose -f docker-compose.prod.yml exec -T db pg_restore -U blog -d blog --clean --if-exists < backups/db-20261118-040000.dump
```

```bash
docker compose -f docker-compose.prod.yml up -d
```

```bash
docker compose -f docker-compose.prod.yml exec -T backend tar -xzf - -C /app/uploads < backups/uploads-20261118-040000.tar.gz
```

DB를 되돌리면 지금의 글·댓글·관리자 계정이 백업 시점의 것으로 **바뀐다**(백업 뒤에 쓴 것은 사라진다). 새 서버로 옮길 때도 같은 순서다. 새 서버에서 7단계까지 마친 뒤 백업 파일을 올려 복구한다.

## 10. 문제가 생기면

| 증상 | 확인할 것 |
|---|---|
| 사이트가 아예 안 열린다(시간 초과) | 보안 그룹에 80, 443이 열려 있는지. 탄력적 IP가 인스턴스에 연결되어 있는지. `nslookup`이 `서버IP`를 돌려주는지 |
| 인증서 경고가 뜬다 | `logs caddy`에서 오류를 본다. 흔한 원인: DNS가 아직 안 퍼짐, `.env`의 `DOMAIN` 오타나 누락(`localhost`용 인증서가 쓰인다), 80번이 막힘. 고친 뒤 `up -d`를 다시 실행한다 |
| 로그인이 안 된다 | 주소가 `https://`인지. 로그인 쿠키는 HTTPS에서만 전달된다 |
| 빌드 중에 서버가 멈추거나 `Killed`가 나온다 | 메모리 부족. `free -h`로 스왑이 켜져 있는지 본다 |
| 어떤 컨테이너가 계속 재시작한다 | `docker compose -f docker-compose.prod.yml logs 서비스이름`. 백엔드는 `.env`의 `JWT_SECRET`이 32자 미만이면 뜨지 않는다 |
| 디스크가 찼다 | `df -h`, `docker system df`로 확인하고 `docker builder prune -f`, `docker image prune -f`로 치운다 |
| SSH 접속이 안 된다 | 내 IP가 바뀌었을 수 있다. 보안 그룹의 SSH 규칙을 "내 IP"로 다시 지정한다 |

## 11. 정리하기 (그만 운영할 때)

켜 두지 않아도 요금이 나오는 것들이 있으므로, 그만둘 때는 전부 지운다.

1. 필요하면 먼저 백업 파일을 내 컴퓨터로 가져온다.
2. EC2 → 인스턴스 → **종료(terminate)**. "중지(stop)"는 디스크와 IP 요금이 계속 나온다.
3. EC2 → 탄력적 IP → **릴리스**. 인스턴스를 지워도 탄력적 IP는 남아서 요금이 나온다.
4. EC2 → 볼륨, 스냅샷에 남은 것이 없는지 본다.
5. 며칠 뒤 결제 화면에서 요금이 더 붙지 않는지 확인한다.

## 알아둘 한계

- 서버가 한 대라서, 서버가 죽으면 블로그도 죽는다. 새 버전을 올릴 때도 잠깐 끊긴다.
- 배포가 수동이다(`git pull` 후 다시 빌드). GitHub Actions에서 이미지를 빌드해 서버에 자동으로 올리는 것(CD)은 다음 단계로 남겨 두었다.
- 이미지와 DB가 서버 디스크에 있다. AWS답게 하려면 이미지는 S3로, DB는 RDS로 옮길 수 있다.
- 로그인과 댓글에 횟수 제한이 없다.
