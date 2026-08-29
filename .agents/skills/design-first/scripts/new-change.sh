#!/usr/bin/env bash
# 创建变更目录与 design.md 骨架。
# 用法: new-change.sh <feat|opt|fix|chore> <标题(小写-连字符)>
set -euo pipefail

TYPE="$1"
TITLE="$2"

case "$TYPE" in
  feat|opt|fix|chore) ;;
  *) echo "错误: type 必须为 feat|opt|fix|chore，收到: $TYPE" >&2; exit 1 ;;
esac

if [[ ! "$TITLE" =~ ^[a-z0-9][a-z0-9-]*$ ]]; then
  echo "错误: 标题必须为小写字母/数字/连字符（如 add-user-query）" >&2
  exit 1
fi

# skill 目录: .pi/skills/design-first/scripts -> 项目根
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
DATE="$(date +%Y-%m-%d)"
DIR="$ROOT/docs/changes/${DATE}-${TYPE}-${TITLE}"

if [ -e "$DIR" ]; then
  echo "错误: 目录已存在: $DIR" >&2
  exit 1
fi

TEMPLATE="$ROOT/docs/changes/TEMPLATE.md"
if [ ! -f "$TEMPLATE" ]; then
  echo "错误: 模板不存在: $TEMPLATE" >&2
  exit 1
fi

mkdir -p "$DIR"
sed -e "s/^# <变更名>/# ${TITLE}/" "$TEMPLATE" > "$DIR/design.md"

echo "已创建: $DIR/design.md"
echo "下一步: 填写 design.md（背景/方案取舍/改动影响/验证），呈现给用户评审后再实现。"
