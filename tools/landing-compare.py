#!/usr/bin/env python3
"""Puts the landing page's and the READMEs' pictures beside the ones they
replace, on one HTML page, so that a new set is looked through before it is
committed.

Compares the working tree with a commit (HEAD by default): every changed
picture of site/images and docs/images, the smallest width of each, with the
share of pixels that differ by more than a few percent. The page carries the
pictures inline and needs nothing beside it.

    tools/landing-compare.py [--base <commit>] [--out build/landing-compare.html]

Needs ImageMagick 7 (`magick`) for the share of changed pixels.
"""

import argparse
import base64
import html
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# The smallest copy of each picture is enough to see what changed.
SMALLEST = re.compile(r"-(720|960)\.webp$")


def git_bytes(*args: str) -> bytes:
    return subprocess.run(["git", "-C", str(ROOT), *args], check=True, capture_output=True).stdout


def git(*args: str) -> str:
    return git_bytes(*args).decode()


def changed(base: str) -> list[str]:
    names = git("diff", "--name-only", base, "--", "site/images", "docs/images").split()
    return sorted(n for n in names if SMALLEST.search(n) or n.startswith("docs/images/"))


def share_changed(old: bytes, new: bytes) -> float | None:
    with tempfile.TemporaryDirectory() as tmp:
        a, b = Path(tmp, "a.webp"), Path(tmp, "b.webp")
        a.write_bytes(old)
        b.write_bytes(new)
        size = subprocess.run(["magick", "identify", "-format", "%w %h", str(b)],
                              capture_output=True, text=True)
        diff = subprocess.run(["magick", "compare", "-metric", "AE", "-fuzz", "4%",
                               str(a), str(b), "null:"], capture_output=True, text=True)
        try:
            w, h = (int(x) for x in size.stdout.split())
            return float(diff.stderr.split()[0]) / (w * h)
        except (ValueError, IndexError):
            return None  # different sizes, or magick missing


def data_uri(content: bytes) -> str:
    return "data:image/webp;base64," + base64.b64encode(content).decode()


def page(base: str, rows: list[tuple[str, bytes | None, bytes, float | None]]) -> str:
    sections = []
    for i, (name, old, new, share) in enumerate(rows, 1):
        label = "новая" if old is None else (
            "размеры разные" if share is None else f"изменилось {share:.1%} пикселей")
        old_img = ('<div class="none">не было</div>' if old is None
                   else f'<img alt="было" src="{data_uri(old)}">')
        wide = " wide" if "hero" in name or "step-project" in name or "step-setup" in name else ""
        sections.append(f"""<section><h2><span class="n">{i}</span> {html.escape(name)}</h2>
<p>{label}</p>
<div class="pair{wide}"><figure>{old_img}<figcaption>было</figcaption></figure>
<figure><img alt="стало" src="{data_uri(new)}"><figcaption>стало</figcaption></figure></div>
</section>""")
    return f"""<!doctype html><html lang="ru"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Картинки лендинга geoshare</title>
<style>
:root {{ --bg:#f3f5f4; --fg:#18201d; --muted:#5a6762; --line:#d3dbd7; --accent:#1f6f5c; }}
@media (prefers-color-scheme: dark) {{ :root:not([data-theme="light"]) {{ --bg:#101513; --fg:#e1e7e4; --muted:#97a39e; --line:#2b3430; --accent:#6cc4a9; color-scheme:dark }} }}
:root[data-theme="dark"] {{ --bg:#101513; --fg:#e1e7e4; --muted:#97a39e; --line:#2b3430; --accent:#6cc4a9; color-scheme:dark }}
body {{ background:var(--bg); color:var(--fg); font:16px/1.55 system-ui, sans-serif; margin:0; padding:24px 16px 56px }}
main {{ max-width:1100px; margin:0 auto; display:flex; flex-direction:column; gap:28px }}
h1 {{ font-size:26px; margin:0 }}
h2 {{ font-size:18px; margin:0; overflow-wrap:anywhere }}
.n {{ color:var(--accent); font-variant-numeric:tabular-nums }}
p {{ margin:0; color:var(--muted) }}
section {{ display:flex; flex-direction:column; gap:12px; border-top:1px solid var(--line); padding-top:20px }}
.pair {{ display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; max-width:620px }}
.pair.wide {{ max-width:none }}
figure {{ margin:0; display:flex; flex-direction:column; gap:6px }}
img {{ width:100%; height:auto; border:1px solid var(--line); border-radius:8px }}
figcaption {{ color:var(--muted); font-size:14px }}
.none {{ aspect-ratio:9/20; border:1px dashed var(--line); border-radius:8px; display:grid; place-items:center; color:var(--muted) }}
</style></head><body><main>
<h1>Картинки лендинга и README: было и стало</h1>
<p>Против {html.escape(base)}; по одной, самой узкой копии каждой картинки. Доля изменившихся пикселей — с допуском 4%.</p>
{''.join(sections)}
</main></body></html>
"""


def main() -> None:
    parser = argparse.ArgumentParser(description=(__doc__ or "").splitlines()[0])
    parser.add_argument("--base", default="HEAD")
    parser.add_argument("--out", default=str(ROOT / "build" / "landing-compare.html"))
    args = parser.parse_args()

    rows = []
    for name in changed(args.base):
        new_path = ROOT / name
        if not new_path.exists():
            continue
        new = new_path.read_bytes()
        try:
            old = git_bytes("show", f"{args.base}:{name}")
        except subprocess.CalledProcessError:
            old = None
        rows.append((name, old, new, None if old is None else share_changed(old, new)))

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(page(git("rev-parse", "--short", args.base).strip(), rows))
    print(f"{len(rows)} pictures compared: {out}")


if __name__ == "__main__":
    main()
