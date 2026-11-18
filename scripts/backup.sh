#!/usr/bin/env bash
# 글·댓글(DB)과 올린 이미지를 파일로 떠서 backups/ 폴더에 둔다.
# docker-compose.prod.yml로 띄운 컨테이너가 실행 중일 때 쓴다.
#
#   사용: ./scripts/backup.sh
#   결과: backups/db-날짜-시각.dump          (DB 전체)
#         backups/uploads-날짜-시각.tar.gz   (올린 이미지)
#
# 되돌리는 방법은 docs/deploy-aws.md의 "백업과 복구"에 있다.

# 중간에 명령 하나라도 실패하면 거기서 멈춘다(성공한 척 넘어가지 않도록).
set -euo pipefail

# 어느 폴더에서 실행하든 저장소 맨 위 폴더를 기준으로 동작하게 한다.
cd "$(dirname "$0")/.."

# 종류별로 남겨 둘 개수. 이보다 오래된 것은 지운다.
KEEP=7

compose=(docker compose -f docker-compose.prod.yml)
stamp=$(date +%Y%m%d-%H%M%S)
mkdir -p backups

# 끝난 파일만 제 이름을 갖도록, 먼저 .partial로 쓰고 성공하면 이름을 바꾼다.
# 도중에 실패하면 .partial 파일을 치운다.
trap 'rm -f backups/*.partial' EXIT

# -T: 결과를 파일로 받을 때는 터미널 연결을 끈다.
# --format=custom: pg_restore로 되돌릴 수 있는 압축된 형식.
"${compose[@]}" exec -T db pg_dump -U blog -d blog --format=custom > "backups/db-$stamp.dump.partial"
mv "backups/db-$stamp.dump.partial" "backups/db-$stamp.dump"

"${compose[@]}" exec -T backend tar -czf - -C /app/uploads . > "backups/uploads-$stamp.tar.gz.partial"
mv "backups/uploads-$stamp.tar.gz.partial" "backups/uploads-$stamp.tar.gz"

# 최신순으로 늘어놓고 KEEP개 뒤의 것부터 지운다.
ls -1t backups/db-*.dump | tail -n +$((KEEP + 1)) | xargs -r rm -f --
ls -1t backups/uploads-*.tar.gz | tail -n +$((KEEP + 1)) | xargs -r rm -f --

echo "백업 완료:"
ls -lh "backups/db-$stamp.dump" "backups/uploads-$stamp.tar.gz"
