# 포트폴리오 공개 배포 (Oracle Cloud Always Free)

```
방문자 ──HTTPS──▶ OCI 무료 VM : Caddy(443) → Spring Boot(127.0.0.1:8080)
                                       │ TLS (EBSAPP, SELECT 전용)
                                       ▼
                       Autonomous DB 23ai (Always Free) : EBSDEMO 스키마 = 가상 데이터
```

- 회사 DB 와 완전히 분리된 별도 DB 이며, **모든 데이터는 `db/oracle/02_demo_reset.sql` 이 만든 가상 데이터**다.
- 데이터는 매일 03:00(KST) 오늘 날짜 기준으로 다시 생성된다 (`DEMO_RESET_DAILY` 스케줄러 작업).
- 웹은 SELECT 권한만 있는 `EBSAPP` 으로 접속하고, EBS 쓰기 로직은 코드에서 주석 처리되어 있다.

## 1. OCI 가입
1. https://www.oracle.com/cloud/free/ 에서 가입 (카드 본인인증, Always Free 는 과금 없음)
2. 홈 리전: **South Korea Central (Seoul)** 또는 **Chuncheon** – 가입 후 바꿀 수 없음

## 2. Autonomous Database 만들기
1. 메뉴 → Oracle Database → Autonomous Database → **Create**
2. 워크로드: Transaction Processing, **Always Free 켜기**, 버전 23ai
3. ADMIN 비밀번호 설정
4. 네트워크: *Secure access from allowed IPs and VCNs only* → 나중에 VM 공인 IP 추가
5. **Require mutual TLS (mTLS) authentication: 끄기** → 지갑 파일 없이 TLS 로 접속 가능
6. 생성 후 **Database connection** → *TLS* 연결 문자열 중 `_low` 를 복사해 둔다

## 3. 스키마·데이터 만들기
ADB 화면 → **Database Actions → SQL** (ADMIN 로그인) 에서 파일 내용을 붙여넣고 *Run Script(F5)*:

| 순서 | 파일 | 내용 |
|---|---|---|
| 1 | `db/oracle/00_create_users.sql` | EBSDEMO(소유자)·EBSAPP(조회용) 계정 – **비밀번호를 먼저 바꿀 것** |
| 2 | `db/oracle/01_schema.sql` | EBS 구조 테이블 33개 + EBSAPP 조회 권한 |
| 3 | `db/oracle/02_demo_reset.sql` | 가상 데이터 생성 프로시저 실행 + 매일 초기화 작업 등록 |

마지막에 건수(items 20, so_lines 36, jobs 8, po_shipments 15)가 나오면 성공.

## 4. VM 만들기
1. Compute → Instances → **Create instance**
2. 이미지: **Canonical Ubuntu 22.04 또는 24.04**, Shape: **VM.Standard.A1.Flex (Ampere, Always Free)** 1 OCPU / 6GB
3. 공인 IP 할당, SSH 키 다운로드
4. VCN → Security List → Ingress 규칙에 **TCP 80, 443** (소스 0.0.0.0/0) 추가
5. ADB 의 접근 허용 IP 에 이 VM 의 공인 IP 추가 (2-4 단계)

## 5. 앱 설치
PC 에서 빌드 후 VM 으로 복사:
```bash
mvn -DskipTests package
scp -i <키파일> target/ebs-planner-0.1.0.jar deploy/* ubuntu@<VM_IP>:~/planner/
```
VM 에서:
```bash
cd ~/planner && chmod +x setup-vm.sh
sudo ./setup-vm.sh <VM_IP의 점을 대시로>.sslip.io      # 예: 152-67-1-2.sslip.io
sudo nano /etc/ebs-planner.env                         # EBS_DB_URL, EBSAPP 비밀번호 입력
sudo systemctl restart ebs-planner
```
`https://152-67-1-2.sslip.io` 로 접속. (sslip.io 는 IP 를 그대로 도메인으로 쓰는 무료 서비스 – 개인 도메인이 있으면 그걸 써도 된다)

## 6. 업데이트·확인
```bash
sudo systemctl status ebs-planner      # 상태
sudo journalctl -u ebs-planner -f      # 로그
```
새 버전은 jar 만 `/opt/ebs-planner/app.jar` 로 교체하고 `sudo systemctl restart ebs-planner`.
