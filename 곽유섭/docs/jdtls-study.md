# jdtls — Claude Code에서 Java 언어 서버 사용하기

## jdtls란

**Eclipse JDT Language Server**의 약자.
Eclipse IDE에서 수십 년간 Java를 분석해온 엔진(Eclipse JDT)을 떼어내서 외부에서도 사용할 수 있게 만든 Java 전용 언어 서버다.

---

## LSP와 jdtls의 관계

LSP(Language Server Protocol)는 Microsoft가 만든 **에디터 ↔ 언어 분석기 간의 통신 표준**이다.

```
┌─────────────────┐   JSON-RPC   ┌──────────────┐
│  Claude Code    │ ←──────────→ │    jdtls     │
│  (클라이언트)    │              │  (서버)       │
└─────────────────┘              └──────────────┘
```

- jdtls는 **서버**: Java 코드를 분석해서 결과를 내보냄
- Claude Code는 **클라이언트**: 결과를 받아서 활용

LSP 덕분에 jdtls는 VSCode, IntelliJ, Vim, Claude Code 등 **어느 에디터에서도 동일하게 동작**한다.

---

## jdtls가 제공하는 주요 기능

| LSP 메서드 | 기능 |
|-----------|------|
| `textDocument/diagnostic` | 오류·경고 목록 수집 |
| `textDocument/documentSymbol` | 클래스·메서드 구조 파악 |
| `textDocument/definition` | 심볼 정의 위치 이동 |
| `textDocument/references` | 모든 참조 찾기 |
| `textDocument/completion` | 타입 기반 자동완성 |
| `textDocument/codeAction` | "import 추가", "필드 생성" 등 |
| `textDocument/rename` | 전체 참조 포함 이름 변경 |

이 프로젝트의 `fix-code` 스킬은 이 중 **`diagnostic`과 `documentSymbol`** 두 가지를 핵심으로 사용한다.

---

## 왜 "정적 분석"인가

jdtls는 **코드를 실행하지 않는다.** 텍스트를 파싱해서 분석할 뿐이다.

```java
// jdtls가 잡는 것 ✓
int x = "123";  // 타입 에러

// jdtls가 못 잡는 것 ✗
@Value("${TELEGRAM_BOT_TOKEN}")
private String botToken;
// → "String 타입이다" 까지만 앎
//   실제 환경변수가 존재하는지는 앱 실행 전까지 모름
```

| 구분 | jdtls (정적) | bootRun (런타임) |
|------|-------------|----------------|
| 타입 에러 | O | O |
| 문법 오류 | O | O |
| 환경변수 누락 | X | O |
| DB 연결 실패 | X | O |
| Docker 미실행 | X | O |
| API 키 만료 | X | O |

---

## 이 프로젝트에서의 사전 설정 3단계

### 1단계 — jdtls 바이너리 설치

플러그인은 Claude Code가 jdtls와 통신하는 방법만 알려줄 뿐, **jdtls 바이너리 자체는 포함하지 않는다.**
WSL 환경은 Homebrew(macOS), AUR(Arch) 같은 패키지 매니저를 쓸 수 없어서, Eclipse 공식 사이트에서 wget으로 직접 다운받아 설치한다.

```bash
# 디렉토리 생성
mkdir -p ~/.local/share/jdtls ~/.local/bin

# Eclipse 공식 릴리즈에서 다운로드
wget -O /tmp/jdtls.tar.gz \
  "https://www.eclipse.org/downloads/download.php?file=/jdtls/milestones/1.40.0/jdt-language-server-1.40.0-202410311357.tar.gz"

# 압축 해제
tar -xzf /tmp/jdtls.tar.gz -C ~/.local/share/jdtls
```

```
~/.local/share/jdtls/           ← jdtls 본체
~/.local/share/jdtls-workspace/ ← 분석 캐시 저장 공간
```

### 2단계 — 래퍼 스크립트 작성 (`~/.local/bin/jdtls`)

단순 실행 스크립트가 아니라 **Lombok 지원을 위한 설정**이 핵심이다.

```bash
LOMBOK_JAR="$HOME/.gradle/caches/.../lombok-1.18.44.jar"

exec java \
  -Xms256m \
  -Xmx2g \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -javaagent:"$LOMBOK_JAR" \   # ← 핵심
  -jar ~/.local/share/jdtls/plugins/org.eclipse.equinox.launcher_*.jar \
  -configuration ~/.local/share/jdtls/config_linux \
  "$@"
```

`-javaagent`로 Lombok JAR을 주입하지 않으면:

```
@Getter    → "메서드 getXxx를 찾을 수 없음" 에러
@Builder   → "빌더 클래스를 찾을 수 없음" 에러
@Slf4j     → "log 변수가 없음" 에러
```

이 프로젝트는 Lombok을 전면적으로 사용하기 때문에, 이 설정 없이는 jdtls가 코드 전체를 에러 투성이로 판정하게 된다.

### 3단계 — Claude Code에 플러그인 활성화

```bash
# Claude Code 플러그인 설치
jdtls-lsp@claude-plugins-official
```

`~/.claude/settings.json`에 자동으로 기록된다:

```json
{
  "enabledPlugins": {
    "jdtls-lsp@claude-plugins-official": true
  }
}
```

### 설정 흐름 요약

```
Claude Code
    ↓ (플러그인이 연결 방법 알려줌)
jdtls-lsp 플러그인
    ↓ (PATH에서 jdtls 명령어 찾음)
~/.local/bin/jdtls  (래퍼 스크립트)
    ↓ (Lombok JAR 주입 후 실행)
~/.local/share/jdtls/  (실제 jdtls 엔진)
```

세 가지가 모두 갖춰져야 동작한다.

---

## fix-code 스킬에서의 활용

```
사용자 요청
    ↓
[1단계] jdtls 진단 — 수정 전 기준선 확보
    │   (textDocument/diagnostic)
    ↓
[2단계] 에이전트 코드 수정
    │   (domain-agent / integration-agent / qa-agent)
    ↓
[3단계] jdtls 재진단 — 신규 에러 발생 여부 확인
    │
    ├── 신규 에러 없음 → 다음 단계
    └── 신규 에러 있음 → 에이전트에게 재수정 지시
    ↓
[4단계] gradlew test 실행
    ↓
[5단계] 결과 보고 (에러 수 before/after 포함)
```

---

## jdtls의 한계

| 못 잡는 상황 | 이유 |
|------------|------|
| `.env` 파일 누락 | 환경변수는 런타임에 주입됨 |
| `google-credentials.json` 경로 오류 | 파일 존재 여부는 OS 레벨 |
| MySQL 컨테이너 미실행 | 네트워크 연결은 런타임 |
| Telegram 토큰 만료 | 외부 API 상태는 알 수 없음 |
| Lombok 애노테이션 | 래퍼 스크립트에 `-javaagent` 미설정 시 false positive 발생 |

---

## 한 줄 요약

> jdtls는 앱을 실행하기 전에 Java 컴파일러 수준의 오류를 잡아주는 도구다. 실행해야 알 수 있는 환경 설정 문제는 여전히 사람 몫이다.
