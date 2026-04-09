#!/usr/bin/env python3
"""
데일리 브리핑: 오늘·내일 Google Calendar 일정을 Discord로 전송
Claude 없이 gws CLI + Discord REST API 직접 호출
"""

import json
import subprocess
import urllib.request
import sys
from datetime import date, timedelta
from pathlib import Path


def load_env() -> dict:
    env_file = Path(__file__).parent.parent / ".env"
    env = {}
    if not env_file.exists():
        print("Error: .env 파일을 찾을 수 없습니다.", file=sys.stderr)
        sys.exit(1)
    for line in env_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        env[k.strip()] = v.strip()
    return env


def get_events(today_str: str, tomorrow_str: str) -> list:
    params = json.dumps({
        "calendarId": "primary",
        "timeMin": f"{today_str}T00:00:00+09:00",
        "timeMax": f"{tomorrow_str}T23:59:59+09:00",
        "singleEvents": True,
        "orderBy": "startTime",
    })
    # Windows: gws is an npm .cmd wrapper — use shutil.which to find it
    import shutil
    gws_bin = shutil.which("gws") or shutil.which("gws.cmd") or "gws"
    result = subprocess.run(
        [gws_bin, "calendar", "events", "list", "--params", params],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0 or not result.stdout.strip():
        print(f"gws 오류: {result.stderr}", file=sys.stderr)
        return []
    return json.loads(result.stdout).get("items", [])


def fmt_day(items: list, date_str: str, label: str) -> str:
    day_items = [
        e for e in items
        if (e["start"].get("dateTime", "") + e["start"].get("date", "")).startswith(date_str)
    ]
    if not day_items:
        return f"{label}\n  일정 없음"
    lines = [label]
    for e in day_items:
        title = e.get("summary", "(제목 없음)")
        if "dateTime" in e["start"]:
            s = e["start"]["dateTime"][11:16]
            en = e["end"]["dateTime"][11:16]
            lines.append(f"  - {title} {s}~{en}")
        else:
            lines.append(f"  - {title} (종일)")
    return "\n".join(lines)


def send_discord(bot_token: str, channel_id: str, message: str) -> None:
    data = json.dumps({"content": message}).encode("utf-8")
    req = urllib.request.Request(
        f"https://discord.com/api/v10/channels/{channel_id}/messages",
        data=data,
        headers={
            "Authorization": f"Bot {bot_token}",
            "Content-Type": "application/json",
            "User-Agent": "DiscordBot (https://github.com/schedule-bot, 1.0)",
        },
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        if resp.status not in (200, 201):
            print(f"Discord 전송 실패: {resp.status}", file=sys.stderr)
            sys.exit(1)


def main():
    env = load_env()
    bot_token = env.get("DISCORD_BOT_TOKEN", "")
    # BRIEFING_CHANNEL_ID 우선, 없으면 첫 번째 ALLOWED_CHANNEL_IDS 사용
    channel_id = env.get(
        "BRIEFING_CHANNEL_ID",
        env.get("ALLOWED_CHANNEL_IDS", "").split(",")[0].strip(),
    )

    if not bot_token or not channel_id:
        print("Error: DISCORD_BOT_TOKEN 또는 채널 ID 없음", file=sys.stderr)
        sys.exit(1)

    days_kr = ["월", "화", "수", "목", "금", "토", "일"]
    today = date.today()
    tomorrow = today + timedelta(1)
    today_str = today.strftime("%Y-%m-%d")
    tomorrow_str = tomorrow.strftime("%Y-%m-%d")
    today_label = f"[오늘] {today.month}/{today.day} ({days_kr[today.weekday()]})"
    tomorrow_label = f"[내일] {tomorrow.month}/{tomorrow.day} ({days_kr[tomorrow.weekday()]})"

    items = get_events(today_str, tomorrow_str)

    message = (
        f"오늘·내일 일정입니다.\n\n"
        f"{fmt_day(items, today_str, today_label)}\n\n"
        f"{fmt_day(items, tomorrow_str, tomorrow_label)}"
    )

    send_discord(bot_token, channel_id, message)
    print(f"브리핑 전송 완료 → #{channel_id}")


if __name__ == "__main__":
    main()
