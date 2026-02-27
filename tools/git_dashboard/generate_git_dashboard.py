#!/usr/bin/env python3
from __future__ import annotations

import argparse
import datetime as dt
import json
import re
import subprocess
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple

BUGFIX_KEYWORDS = ("fix", "bug", "hotfix", "corrig", "erro", "falha", "defect", "issue")
CRITICAL_HINTS = ("auth", "billing", "payment", "security", "backup", "notification", "receiver", "database", "firestore", "drive")
REVERT_RE = re.compile(r"This reverts commit ([0-9a-f]{7,40})", re.IGNORECASE)


@dataclass
class FileStat:
    path: str
    additions: int
    deletions: int

    @property
    def churn(self) -> int:
        return self.additions + self.deletions


@dataclass
class Commit:
    hash: str
    author: str
    email: str
    date_iso: str
    subject: str
    body: str
    files: List[FileStat] = field(default_factory=list)
    risk: int = 0
    risk_reasons: List[str] = field(default_factory=list)
    is_bugfix: bool = False
    status: str = "Risco"
    evidence: List[str] = field(default_factory=list)
    linked_fixes: List[str] = field(default_factory=list)
    reverted_by: Optional[str] = None

    @property
    def short(self) -> str:
        return self.hash[:8]

    @property
    def churn(self) -> int:
        return sum(f.churn for f in self.files)


def git(repo: Path, args: List[str]) -> str:
    cp = subprocess.run(["git", *args], cwd=repo, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8")
    return cp.stdout


def parse_log(raw: str) -> List[Commit]:
    marker = "__COMMIT__"
    lines = raw.splitlines()
    out: List[Commit] = []
    i = 0
    while i < len(lines):
        if lines[i] != marker:
            i += 1
            continue
        h, author, email, date_iso, subject, body = lines[i + 1 : i + 7]
        i += 7
        files: List[FileStat] = []
        while i < len(lines) and lines[i] != marker:
            row = lines[i].strip()
            if row:
                p = row.split("\t")
                if len(p) == 3:
                    a = 0 if p[0] == "-" else int(p[0])
                    d = 0 if p[1] == "-" else int(p[1])
                    files.append(FileStat(path=p[2], additions=a, deletions=d))
            i += 1
        out.append(Commit(hash=h, author=author, email=email, date_iso=date_iso, subject=subject, body=body, files=files))
    return out


def has_test_change(files: List[FileStat]) -> bool:
    for f in files:
        p = f.path.lower().replace("\\", "/")
        if "/test/" in p or p.endswith("test.kt") or p.endswith("test.java"):
            return True
    return False


def compute_risk(c: Commit) -> None:
    reasons: List[str] = []
    risk = 0
    txt = f"{c.subject} {c.body}".lower()
    c.is_bugfix = any(k in txt for k in BUGFIX_KEYWORDS)

    files_n = len(c.files)
    churn = c.churn
    if files_n >= 15:
        risk += 2
        reasons.append("Muitos arquivos alterados")
    elif files_n >= 8:
        risk += 1
        reasons.append("Alteracao espalhada")

    if churn >= 500:
        risk += 3
        reasons.append("Alto volume de linhas")
    elif churn >= 180:
        risk += 1
        reasons.append("Volume medio-alto")

    touched_critical = any(any(h in f.path.lower() for h in CRITICAL_HINTS) for f in c.files)
    if touched_critical:
        risk += 2
        reasons.append("Area sensivel alterada")

    tests = has_test_change(c.files)
    if not tests and churn >= 120:
        risk += 2
        reasons.append("Sem testes no commit")
    if tests:
        risk = max(0, risk - 1)
        reasons.append("Inclui ajuste de testes")

    if "wip" in txt or "tmp" in txt:
        risk += 1
        reasons.append("Mensagem sugere parcial")

    c.risk = max(0, min(10, risk))
    c.risk_reasons = reasons


def detect_links(commits: List[Commit]) -> None:
    by_hash = {c.hash: c for c in commits}
    last_toucher: Dict[str, str] = {}

    for c in commits:
        m = REVERT_RE.search(c.subject + "\n" + c.body)
        if m:
            target = m.group(1)
            for h in by_hash:
                if h.startswith(target):
                    by_hash[h].reverted_by = c.short
                    by_hash[h].evidence.append(f"Revertido por {c.short}")
                    break

        if c.is_bugfix:
            seen = set()
            for f in c.files:
                prev = last_toucher.get(f.path)
                if prev and prev != c.hash and prev not in seen:
                    seen.add(prev)
                    origin = by_hash.get(prev)
                    if origin:
                        origin.linked_fixes.append(c.short)
                        origin.evidence.append(f"Ajustado depois por {c.short}")

        for f in c.files:
            last_toucher[f.path] = c.hash

    for c in commits:
        if c.reverted_by:
            c.status = "Confirmado"
        elif c.linked_fixes:
            c.status = "Provavel bug"
        else:
            c.status = "Risco"


def build_payload(commits: List[Commit]) -> Dict:
    authors: Dict[str, Dict] = defaultdict(lambda: {"commits": 0, "churn": 0, "risk_sum": 0, "confirmados": 0, "provaveis": 0, "fixes": 0})
    file_churn: Counter[str] = Counter()
    file_touches: Counter[str] = Counter()
    top_dir_touches: Counter[str] = Counter()
    weekday_commits: Counter[str] = Counter()
    month_commits: Counter[str] = Counter()
    total_additions = 0
    total_deletions = 0
    now_utc = dt.datetime.now(dt.timezone.utc)
    commits_30d = 0
    commits_90d = 0

    for c in commits:
        a = authors[c.author]
        a["commits"] += 1
        a["churn"] += c.churn
        a["risk_sum"] += c.risk
        if c.status == "Confirmado":
            a["confirmados"] += 1
        elif c.status == "Provavel bug":
            a["provaveis"] += 1
        if c.is_bugfix:
            a["fixes"] += 1

        commit_dt = dt.datetime.fromisoformat(c.date_iso)
        if commit_dt.tzinfo is None:
            commit_dt = commit_dt.replace(tzinfo=dt.timezone.utc)
        commit_dt_utc = commit_dt.astimezone(dt.timezone.utc)
        age = now_utc - commit_dt_utc
        if age <= dt.timedelta(days=30):
            commits_30d += 1
        if age <= dt.timedelta(days=90):
            commits_90d += 1

        weekday_commits[commit_dt_utc.strftime("%a")] += 1
        month_commits[commit_dt_utc.strftime("%Y-%m")] += 1

        seen_paths = set()
        for f in c.files:
            normalized = f.path.replace("\\", "/")
            file_churn[normalized] += f.churn
            total_additions += f.additions
            total_deletions += f.deletions
            if normalized not in seen_paths:
                file_touches[normalized] += 1
                seen_paths.add(normalized)
            top_dir = normalized.split("/", 1)[0] if "/" in normalized else "(root)"
            top_dir_touches[top_dir] += 1

    author_rows = []
    for name, st in authors.items():
        n = max(1, st["commits"])
        author_rows.append({
            "author": name,
            "commits": st["commits"],
            "churn": st["churn"],
            "avgRisk": round(st["risk_sum"] / n, 2),
            "confirmados": st["confirmados"],
            "provaveis": st["provaveis"],
            "fixes": st["fixes"],
        })
    author_rows.sort(key=lambda x: (-x["confirmados"], -x["provaveis"], -x["avgRisk"]))

    timeline = []
    for c in commits:
        timeline.append({
            "hash": c.hash,
            "short": c.short,
            "author": c.author,
            "email": c.email,
            "date": c.date_iso,
            "subject": c.subject,
            "body": c.body,
            "filesChanged": len(c.files),
            "churn": c.churn,
            "risk": c.risk,
            "riskReasons": c.risk_reasons,
            "status": c.status,
            "evidence": sorted(set(c.evidence)),
            "isBugfix": c.is_bugfix,
            "files": [{"path": f.path, "add": f.additions, "del": f.deletions} for f in c.files],
        })

    summary = {
        "totalCommits": len(commits),
        "totalAuthors": len(authors),
        "confirmados": sum(1 for c in commits if c.status == "Confirmado"),
        "provaveis": sum(1 for c in commits if c.status == "Provavel bug"),
        "riscosAltos": sum(1 for c in commits if c.risk >= 6),
        "bugfixCommits": sum(1 for c in commits if c.is_bugfix),
        "generatedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
    }
    total_churn = total_additions + total_deletions
    project_stats = {
        "totalAdditions": total_additions,
        "totalDeletions": total_deletions,
        "totalChurn": total_churn,
        "avgChurnPerCommit": round(total_churn / max(1, len(commits)), 2),
        "uniqueFilesTouched": len(file_touches),
        "commitsLast30Days": commits_30d,
        "commitsLast90Days": commits_90d,
        "topDirectories": [{"name": k, "touches": v} for k, v in top_dir_touches.most_common(8)],
        "hotFiles": [
            {"path": p, "touches": file_touches[p], "churn": file_churn[p]}
            for p, _ in file_churn.most_common(10)
        ],
        "weekdayCommits": [{"day": k, "commits": weekday_commits.get(k, 0)} for k in ("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")],
        "monthlyCommits": [
            {"month": k, "commits": v}
            for k, v in sorted(month_commits.items())[-12:]
        ],
    }
    return {"summary": summary, "authors": author_rows, "timeline": timeline, "projectStats": project_stats}


def build_html(payload: Dict) -> str:
    data_json = json.dumps(payload, ensure_ascii=False)
    html_tpl = """<!doctype html>
<html lang=\"pt-BR\"><head>
<meta charset=\"utf-8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>
<title>Git Timeline Bugs</title>
<style>
body{{font-family:Segoe UI,Arial,sans-serif;background:#f1f5f9;margin:0;color:#0f172a}} .wrap{{max-width:1280px;margin:0 auto;padding:16px}}
.cards{{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:8px}} .card{{background:#fff;border:1px solid #cbd5e1;border-radius:10px;padding:10px}} .n{{font-size:24px;font-weight:700}}
.filters{{margin-top:10px;display:grid;grid-template-columns:1fr 220px 180px 180px;gap:8px}} input,select{{padding:8px;border:1px solid #94a3b8;border-radius:8px;background:#fff}}
.layout{{display:grid;grid-template-columns:2fr 1fr;gap:10px;margin-top:10px}} .table{{background:#fff;border:1px solid #cbd5e1;border-radius:10px;overflow:auto;max-height:78vh}}
 table{{width:100%;border-collapse:collapse;font-size:12px}} th,td{{padding:8px;border-bottom:1px solid #e2e8f0;vertical-align:top}} th{{position:sticky;top:0;background:#f8fafc}}
.badge{{padding:2px 6px;border-radius:999px;font-size:11px;font-weight:700}} .s-risk{{background:#dbeafe;color:#1e3a8a}} .s-prob{{background:#fef3c7;color:#92400e}} .s-conf{{background:#fee2e2;color:#991b1b}}
.r-low{{background:#dcfce7;color:#166534}} .r-mid{{background:#fef9c3;color:#854d0e}} .r-hi{{background:#fee2e2;color:#991b1b}}
.panel{{background:#fff;border:1px solid #cbd5e1;border-radius:10px;padding:10px;max-height:78vh;overflow:auto}} .item{{border-bottom:1px solid #e2e8f0;padding:8px 0}} .muted{{color:#475569;font-size:12px}}
.subttl{{margin:14px 0 8px;font-size:15px;font-weight:700}} .proj-grid{{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px}}
.proj-layout{{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:8px}} .small{{font-size:11px}}
</style></head><body><div class=\"wrap\"><h2>Timeline e Bugs (Concreto)</h2><div class=\"muted\">Status: Risco / Provavel bug / Confirmado (revert ou evidencia de correcao posterior)</div>
<div class=\"cards\" id=\"cards\"></div>
<div class=\"subttl\">Estatisticas do Projeto</div>
<div class=\"proj-grid\" id=\"proj-kpis\"></div>
<div class=\"proj-layout\">
  <div class=\"panel\"><h3 style=\"margin-top:0\">Diretorios mais tocados</h3><div id=\"proj-dirs\"></div></div>
  <div class=\"panel\"><h3 style=\"margin-top:0\">Arquivos mais quentes</h3><div id=\"proj-files\"></div></div>
</div>
<div class=\"proj-layout\">
  <div class=\"panel\"><h3 style=\"margin-top:0\">Commits por dia da semana</h3><div id=\"proj-weekdays\"></div></div>
  <div class=\"panel\"><h3 style=\"margin-top:0\">Commits por mes (ultimos 12)</h3><div id=\"proj-months\"></div></div>
</div>
<div class=\"filters\"><input id=\"q\" placeholder=\"Buscar commit, mensagem, arquivo...\"/><select id=\"author\"></select><select id=\"status\"><option value=\"\">Todos os status</option><option>Risco</option><option>Provavel bug</option><option>Confirmado</option></select><select id=\"risk\"><option value=\"0\">Risco >= 0</option><option value=\"3\">Risco >= 3</option><option value=\"6\">Risco >= 6</option><option value=\"8\">Risco >= 8</option></select></div>
<div class=\"layout\"><div class=\"table\"><table><thead><tr><th>Data</th><th>Commit</th><th>Autor</th><th>Status</th><th>Mensagem/Evidencia</th><th>Risco</th></tr></thead><tbody id=\"rows\"></tbody></table></div><div class=\"panel\"><h3 style=\"margin-top:0\">Resumo por dev</h3><div id=\"authors\"></div></div></div></div>
<script>
const data=__DATA_JSON__; const rows=document.getElementById('rows'); const cards=document.getElementById('cards'); const q=document.getElementById('q'); const author=document.getElementById('author'); const status=document.getElementById('status'); const risk=document.getElementById('risk'); const authors=document.getElementById('authors');
const projKpis=document.getElementById('proj-kpis'); const projDirs=document.getElementById('proj-dirs'); const projFiles=document.getElementById('proj-files'); const projWeekdays=document.getElementById('proj-weekdays'); const projMonths=document.getElementById('proj-months');
const esc=s=>String(s||'').replace(/[&<>]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;"})[c]);
const fmtn=n=>Number(n||0).toLocaleString('pt-BR');
const rcls=v=>v>=6?'r-hi':(v>=3?'r-mid':'r-low'); const scls=s=>s==='Confirmado'?'s-conf':(s==='Provavel bug'?'s-prob':'s-risk');
cards.innerHTML=`<div class='card'><div>Total commits</div><div class='n'>${{fmtn(data.summary.totalCommits)}}</div></div><div class='card'><div>Autores</div><div class='n'>${{fmtn(data.summary.totalAuthors)}}</div></div><div class='card'><div>Confirmados</div><div class='n'>${{fmtn(data.summary.confirmados)}}</div></div><div class='card'><div>Provaveis</div><div class='n'>${{fmtn(data.summary.provaveis)}}</div></div><div class='card'><div>Risco alto</div><div class='n'>${{fmtn(data.summary.riscosAltos)}}</div></div>`;
projKpis.innerHTML=`<div class='card'><div>Arquivos unicos tocados</div><div class='n'>${{fmtn(data.projectStats.uniqueFilesTouched)}}</div></div><div class='card'><div>Linhas adicionadas</div><div class='n'>${{fmtn(data.projectStats.totalAdditions)}}</div></div><div class='card'><div>Linhas removidas</div><div class='n'>${{fmtn(data.projectStats.totalDeletions)}}</div></div><div class='card'><div>Churn medio / commit</div><div class='n'>${{fmtn(data.projectStats.avgChurnPerCommit)}}</div></div><div class='card'><div>Commits 30 dias</div><div class='n'>${{fmtn(data.projectStats.commitsLast30Days)}}</div></div><div class='card'><div>Commits 90 dias</div><div class='n'>${{fmtn(data.projectStats.commitsLast90Days)}}</div></div><div class='card'><div>Total churn</div><div class='n'>${{fmtn(data.projectStats.totalChurn)}}</div></div><div class='card'><div>Bugfix commits</div><div class='n'>${{fmtn(data.summary.bugfixCommits)}}</div></div>`;
author.innerHTML=`<option value=''>Todos os autores</option>`+data.authors.map(a=>`<option>${{esc(a.author)}}</option>`).join('');
authors.innerHTML=data.authors.map(a=>`<div class='item'><div><strong>${{esc(a.author)}}</strong></div><div class='muted'>Commits: ${{a.commits}} | Churn: ${{a.churn}} | Risco medio: ${{a.avgRisk}}</div><div class='muted'>Confirmados: ${{a.confirmados}} | Provaveis: ${{a.provaveis}} | Fixes: ${{a.fixes}}</div></div>`).join('');
projDirs.innerHTML=data.projectStats.topDirectories.map(d=>`<div class='item'><strong>${{esc(d.name)}}</strong><div class='muted'>Toques: ${{fmtn(d.touches)}}</div></div>`).join('');
projFiles.innerHTML=data.projectStats.hotFiles.map(f=>`<div class='item'><div><code class='small'>${{esc(f.path)}}</code></div><div class='muted'>Toques: ${{fmtn(f.touches)}} | Churn: ${{fmtn(f.churn)}}</div></div>`).join('');
projWeekdays.innerHTML=data.projectStats.weekdayCommits.map(w=>`<div class='item'><strong>${{esc(w.day)}}</strong><div class='muted'>Commits: ${{fmtn(w.commits)}}</div></div>`).join('');
projMonths.innerHTML=data.projectStats.monthlyCommits.map(m=>`<div class='item'><strong>${{esc(m.month)}}</strong><div class='muted'>Commits: ${{fmtn(m.commits)}}</div></div>`).join('');
function draw(){{
 const s=q.value.toLowerCase().trim(), a=author.value, st=status.value, mr=Number(risk.value||0);
 const list=data.timeline.filter(c=>{{if(a&&c.author!==a)return false; if(st&&c.status!==st)return false; if(c.risk<mr)return false; if(!s)return true; const hay=[c.hash,c.short,c.author,c.subject,c.body,...c.files.map(f=>f.path),...c.evidence].join(' ').toLowerCase(); return hay.includes(s);}});
 rows.innerHTML=list.map(c=>`<tr><td>${{esc(c.date.slice(0,10))}}</td><td><code>${{esc(c.short)}}</code></td><td>${{esc(c.author)}}</td><td><span class='badge ${{scls(c.status)}}'>${{esc(c.status)}}</span></td><td><div>${{esc(c.subject)}}</div><div class='muted'>${{c.evidence.map(esc).join(' | ')||'Sem evidencia direta'}}</div></td><td><span class='badge ${{rcls(c.risk)}}'>${{c.risk}}</span><div class='muted'>${{c.filesChanged}} arqs / ${{c.churn}} linhas</div></td></tr>`).join('');
}}
[q,author,status,risk].forEach(e=>e.addEventListener('input',draw)); draw();
</script></body></html>"""
    html_tpl = html_tpl.replace("{{", "{").replace("}}", "}")
    return html_tpl.replace("__DATA_JSON__", data_json)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=".")
    ap.add_argument("--out-dir", default="build/reports/git-dashboard")
    args = ap.parse_args()

    repo = Path(args.repo).resolve()
    out_dir = (repo / args.out_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    raw = git(repo, [
        "log",
        "--reverse",
        "--date=iso-strict",
        "--pretty=format:__COMMIT__%n%H%n%an%n%ae%n%ad%n%s%n%b",
        "--numstat",
    ])

    commits = parse_log(raw)
    for c in commits:
        compute_risk(c)
    detect_links(commits)
    payload = build_payload(commits)

    (out_dir / "git-dashboard.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    (out_dir / "index.html").write_text(build_html(payload), encoding="utf-8")
    print(out_dir / "index.html")


if __name__ == "__main__":
    main()
