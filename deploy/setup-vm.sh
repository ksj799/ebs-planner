#!/usr/bin/env bash
# OCI Always Free VM (Ubuntu 22.04/24.04) 설치 스크립트
#   이 폴더(deploy/)와 ebs-planner-0.1.0.jar 를 VM 의 같은 디렉터리에 올린 뒤:
#   sudo ./setup-vm.sh 152-67-1-2.sslip.io
set -euo pipefail
DOMAIN="${1:?사용법: sudo ./setup-vm.sh <도메인, 예: 152-67-1-2.sslip.io>}"
cd "$(dirname "$0")"
JAR="$(ls ebs-planner-*.jar 2>/dev/null | head -1)"
[ -n "$JAR" ] || { echo "ebs-planner-*.jar 가 이 폴더에 없습니다"; exit 1; }

apt-get update
apt-get install -y openjdk-17-jre-headless curl gnupg debian-keyring debian-archive-keyring apt-transport-https netfilter-persistent

# Caddy (HTTPS 리버스 프록시) – 공식 저장소
if ! command -v caddy >/dev/null; then
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' -o /etc/apt/sources.list.d/caddy-stable.list
  apt-get update
  apt-get install -y caddy
fi

# 앱 사용자·파일
id planner >/dev/null 2>&1 || useradd --system --home /opt/ebs-planner --shell /usr/sbin/nologin planner
install -d -o planner -g planner /opt/ebs-planner
install -m 644 -o planner -g planner "$JAR" /opt/ebs-planner/app.jar
if [ ! -f /etc/ebs-planner.env ]; then
  install -m 600 -o planner -g planner planner.env.example /etc/ebs-planner.env
  echo ">>> /etc/ebs-planner.env 에 DB 접속 정보를 채운 뒤: sudo systemctl restart ebs-planner"
fi
install -m 644 ebs-planner.service /etc/systemd/system/ebs-planner.service
sed "s/{DOMAIN}/${DOMAIN}/" Caddyfile > /etc/caddy/Caddyfile

# OCI Ubuntu 이미지는 iptables 가 80/443 을 막고 있다 (VCN 보안 목록도 콘솔에서 따로 열어야 함)
for p in 80 443; do
  iptables -C INPUT -p tcp --dport "$p" -j ACCEPT 2>/dev/null || iptables -I INPUT 6 -p tcp --dport "$p" -j ACCEPT
done
netfilter-persistent save

systemctl daemon-reload
systemctl enable --now ebs-planner
systemctl restart caddy
echo ">>> 완료: https://${DOMAIN}"
