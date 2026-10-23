"""Tempo totale realistico di una ricetta: non meno della somma delle durate scritte nei passaggi
(cottura, riposo, frigo, marinatura...). I passaggi che si svolgono "mentre" altro cuoce non si sommano."""
import math, re

_DUR = re.compile(r"(\d+)\s*(?:-|–|—|/|e|o|a)?\s*(\d+)?\s*(minut\w*|min\b|ore\b|ora\b|h\b)", re.I)
_PARALLEL = re.compile(r"\b(mentre|nel frattempo|intanto|contemporaneamente)\b", re.I)


def step_minutes(step: str) -> int:
    t = step.lower().replace("’", "'")
    if _PARALLEL.search(t):
        return 0
    tot = 0
    for m in _DUR.finditer(t):
        a = int(m.group(1)); b = int(m.group(2)) if m.group(2) else a
        v = max(a, b)
        unit = m.group(3)
        if unit.startswith(("ore", "ora", "h")):
            v *= 60
        tot += v
    if "mezz'ora" in t or "mezz ora" in t:
        tot += 30
    return tot


def effective_minutes(steps, minutes: int) -> int:
    total = sum(step_minutes(s) for s in steps)
    m = max(int(minutes), total)
    if m >= 10 and m % 5:
        m = int(math.ceil(m / 5) * 5)
    return m
