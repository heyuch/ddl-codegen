#!/usr/bin/env bash
# 变更目录命名权威：创建变更目录并分配变更号，或校验既有目录命名。
# 目录命名统一 {YYYYMMDD}-{NN}-{feat|optimize|refactor|fix|chore}-{标题}（早期存量已于 2026-09-05 迁移，无旧格式豁免）。
# 用法:
#   new-change.sh <feat|optimize|refactor|fix|chore> <标题>              # 创建 {YYYYMMDD}-{NN}-{type}-{标题}（今天）与 design.md 骨架
#   new-change.sh --date YYYYMMDD <type> <标题>            # 指定日期（历史归档目录用，如初始建设期）
#   new-change.sh check                                    # 校验 docs/changes 下所有目录命名合规
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
CHANGES="$ROOT/docs/changes"

# ---- check：校验目录命名统一 {8位日期}-{2位序号}-{type}-{标题} ----
check() {
  local bad=0
  local tmp
  tmp="$(mktemp)"
  local d base dup
  for d in "$CHANGES"/*/; do
    [ -d "$d" ] || continue
    base="$(basename "$d")"
    if [[ "$base" =~ ^[0-9]{8}-[0-9]{2}-(feat|optimize|refactor|fix|chore)-[a-z0-9-]+$ ]]; then
      echo "${base:0:11}" >> "$tmp"     # 登记 {YYYYMMDD}-{NN} 供查重
    else
      echo "不合规目录名: $base" >&2
      bad=1
    fi
  done
  dup="$(sort "$tmp" | uniq -d)"
  if [[ -n "$dup" ]]; then
    echo "变更号重复: $dup" >&2
    bad=1
  fi
  rm -f "$tmp"
  if (( bad )); then
    echo "check 失败：docs/changes 存在命名问题（目录只允许由 new-change.sh 创建）" >&2
    exit 1
  fi
  echo "check 通过：docs/changes 目录命名合规（$(ls -d "$CHANGES"/*/ | wc -l | tr -d ' ') 个变更）"
}

if [[ "${1:-}" == "check" ]]; then
  check
  exit 0
fi

DATE="$(date +%Y%m%d)"
if [[ "${1:-}" == "--date" ]]; then
  DATE="${2:-}"
  shift 2
  if [[ ! "$DATE" =~ ^[0-9]{8}$ ]]; then
    echo "错误: --date 必须为 8 位数字（YYYYMMDD），收到: $DATE" >&2
    exit 1
  fi
fi

TYPE="${1:-}"
TITLE="${2:-}"
case "$TYPE" in
  feat|optimize|refactor|fix|chore) ;;
  *) echo "错误: type 必须为 feat|optimize|refactor|fix|chore，收到: $TYPE" >&2; exit 1 ;;
esac
if [[ ! "$TITLE" =~ ^[a-z0-9][a-z0-9-]*$ ]]; then
  echo "错误: 标题必须为小写字母/数字/连字符（如 add-user-query）" >&2
  exit 1
fi

check    # 创建前自检全仓命名

max=0
for d in "$CHANGES/${DATE}-"*/; do
  [ -d "$d" ] || continue
  base="$(basename "$d")"
  if [[ "$base" =~ ^${DATE}-([0-9]{2})- ]]; then
    n=$((10#${BASH_REMATCH[1]}))
    (( n > max )) && max=$n
  fi
done
NN="$(printf '%02d' $((max + 1)))"
CHANGE_ID="${DATE}-${NN}"
DIR="${CHANGES}/${CHANGE_ID}-${TYPE}-${TITLE}"

if [ -e "$DIR" ]; then
  echo "错误: 目录已存在: $DIR" >&2
  exit 1
fi

TEMPLATE="$CHANGES/TEMPLATE.md"
if [ ! -f "$TEMPLATE" ]; then
  echo "错误: 模板不存在: $TEMPLATE" >&2
  exit 1
fi

mkdir -p "$DIR"
sed -e "s/^# <变更名>/# ${TITLE}/" \
    -e "s|{{CHANGE_ID}}|${CHANGE_ID}|g" \
    -e "s|{{CHANGE_DIR}}|${CHANGE_ID}-${TYPE}-${TITLE}|g" \
    -e "s|{{CHANGE_TYPE}}|${TYPE}|g" \
    "$TEMPLATE" > "$DIR/design.md"

echo "已创建: $DIR/design.md"
echo "变更号: $CHANGE_ID"
echo "下一步: 填写 design.md（背景/方案取舍/改动影响/验证），SOLID 自评 +（中/大改动）独立评审后呈现给用户评审，通过后再实现。"
