#!/usr/bin/env python3
"""
Оновлює базу загроз «Варти» з відкритого проєкту Echap (stalkerware-indicators, CC BY 4.0).

    git clone --depth 1 https://github.com/AssoEchap/stalkerware-indicators /tmp/ioc
    python3 tools/update_ioc.py /tmp/ioc

Результат — app/src/main/assets/ioc.json: назви програм, а також назви пакетів, відбитки сертифікатів
підпису (SHA-1) і хеші APK (SHA-256), за якими їх можна впізнати.
"""
import csv
import json
import pathlib
import re
import subprocess
import sys

import yaml

src = pathlib.Path(sys.argv[1])
out = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/ioc.json"

apps, pkg, cert, sha, org, cn = [], {}, {}, {}, {}, []
by_name = {}


def add_app(name, kind):
    key = name.lower()
    if key not in by_name:
        by_name[key] = len(apps)
        apps.append({"n": name, "t": "w" if kind == "watchware" else "s"})
    return by_name[key]


HEX40 = re.compile(r"^[0-9A-F]{40}$")
# Публічні тестові ключі AOSP (build/target/product/security): ними підписано системні програми кастомних прошивок,
# службові пакети емуляторів і безліч аматорських програм. Деякі стежилки теж ними підписані, але за таким
# сертифікатом не можна нікого впізнати — лише хибні тривоги.
PUBLIC_KEYS = {
    "61ED377E85D386A8DFEE6B864BD85B0BFAA5AF81",  # testkey
    "27196E386B875E76ADF700E7EA84E4C6EEE33DFA",  # platform
    "5B368CFF2DA2686996BC95EAC190EAA4F5630FE5",  # shared
    "B79DF4A82E90B57EA76525AB7037AB238A42F5D3",  # media
}
HEX64 = re.compile(r"^[0-9a-f]{64}$")

for f in ("ioc.yaml", "watchware.yaml"):
    for a in yaml.safe_load((src / f).read_text(encoding="utf-8")):
        i = add_app(a["name"], a.get("type", "stalkerware"))
        for p in a.get("packages") or []:
            pkg.setdefault(p.strip(), i)
        for c in a.get("certificates") or []:
            c = c.strip().upper().replace(":", "")
            if HEX40.match(c) and c not in PUBLIC_KEYS:
                cert.setdefault(c, i)
        for o in a.get("certificate_organizations") or []:
            org.setdefault(o.strip(), i)
        for r in a.get("certificate_cname_re") or []:
            re.compile(r)
            cn.append([r, i])

with open(src / "samples.csv", encoding="utf-8") as fh:
    for row in csv.DictReader(fh):
        name = (row.get("App") or "").strip()
        if not name:
            continue
        i = by_name.get(name.lower())
        if i is None:
            i = add_app(name, "stalkerware")
        h = (row.get("SHA256") or "").strip().lower()
        if HEX64.match(h):
            sha.setdefault(h, i)
        c = (row.get("Certificate") or "").strip().upper().replace(":", "")
        if HEX40.match(c) and c not in PUBLIC_KEYS:
            cert.setdefault(c, i)
        p = (row.get("Package Name") or "").strip()
        if p:
            pkg.setdefault(p, i)

try:
    rev = subprocess.run(["git", "-C", str(src), "log", "-1", "--format=%h %cs"], capture_output=True, text=True).stdout.split()
except OSError:
    rev = []

data = {
    "source": "https://github.com/AssoEchap/stalkerware-indicators",
    "license": "CC BY 4.0",
    "commit": rev[0] if rev else "",
    "date": rev[1] if len(rev) > 1 else "",
    "apps": apps,
    "pkg": dict(sorted(pkg.items())),
    "cert": dict(sorted(cert.items())),
    "sha256": dict(sorted(sha.items())),
    "certOrg": dict(sorted(org.items())),
    "certCn": cn,
}
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
s = sum(1 for a in apps if a["t"] == "s")
print(f"{out.name}: {s} шпигунських і {len(apps) - s} програм стеження; "
      f"{len(pkg)} пакетів, {len(cert)} сертифікатів, {len(sha)} хешів, {out.stat().st_size // 1024} КБ")
