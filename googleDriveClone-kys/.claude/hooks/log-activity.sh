#!/usr/bin/env bash
# PostToolUse(*) — 모든 도구 호출 종료 시 한 줄 JSONL 기록.
# 로그 실패가 작업을 막지 않도록 exit 0 보장.
#
# Claude Code 공식 훅 프로토콜: stdin JSON
#   { tool_name, tool_input, tool_response, hook_event_name, session_id, ... }
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
log_dir="$project_dir/.claude/logs"
log_file="$log_dir/activity-$(date +%Y-%m).jsonl"
mkdir -p "$log_dir" 2>/dev/null || exit 0

# python -c 로 실행해야 sys.stdin 이 훅 페이로드를 가리킨다.
# (heredoc 을 쓰면 python 이 소스를 stdin 으로 읽어버려 페이로드가 소실된다.)
LOG_FILE="$log_file" python3 -c '
import sys, os, json, datetime
raw = sys.stdin.read()
try:
    d = json.loads(raw) if raw.strip() else {}
except Exception:
    d = {}
ti = d.get("tool_input") or {}
tr = d.get("tool_response") or {}
file_v = (ti.get("file_path") or "")[:500]
cmd_v = (ti.get("command") or "")[:200]
tool = d.get("tool_name", "unknown")

# 노이즈 필터: file/cmd 둘 다 비어있고 알려진 무신호 도구면 스킵
# (TodoWrite, TaskCreate 같은 메타 도구는 file/cmd 가 본질적으로 없어서 패스)
NOISY_NO_FILE = {"TodoWrite", "TaskCreate", "TaskUpdate", "TaskList", "TaskGet"}
if not file_v and not cmd_v and tool in NOISY_NO_FILE:
    pass  # 스킵
else:
    # 성공/실패 신호 추출 (Bash 의 stderr 길이로 어림짐작)
    success = True
    err_hint = ""
    if isinstance(tr, dict):
        err = tr.get("stderr") or tr.get("error") or ""
        if err:
            success = False
            err_hint = str(err)[:120]
    rec = {
        "ts": datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
        "event": d.get("hook_event_name", ""),
        "tool": tool,
        "file": file_v,
        "cmd": cmd_v,
        "ok": success,
        "err": err_hint,
        "session": d.get("session_id", ""),
    }
    log_file = os.environ.get("LOG_FILE")
    if log_file:
        with open(log_file, "a", encoding="utf-8") as f:
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")
' 2>/dev/null || true

exit 0
