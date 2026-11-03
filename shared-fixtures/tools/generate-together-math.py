#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Thomas Kahle
# SPDX-License-Identifier: Apache-2.0
"""Interim generator for shared-fixtures/together-math.json.

The parity rules want shared fixtures generated from the app that defined the
behavior, which is iOS `TogetherLogic.swift` at tag v1.4.2-build19. No Swift
toolchain was available on the Android machine, so this script re-implements
the Wechselwirkungs-Arithmetik and the German phrasing a third time,
independently of both apps, from the iOS source and its test corpus. Agreement
between this file, the Kotlin port and the literal expectations in the iOS
tests is the cross-check. Replace it with an iOS-generated fixture when one
exists (PLAN.md, phase two, open decision 3).

Run from the repository root:

    python3 -I shared-fixtures/tools/generate-together-math.py > shared-fixtures/together-math.json
"""
import json
import datetime as dt
from itertools import combinations

IOS_COMMIT = "089cf5a960f4040d03a78cc052738479d4416bc1"
DAYS_PER_CENTURY = 36_525
MAX_EVENTS = 30
MAX_CONSTELLATIONS = 20
FULL_ENUMERATION_PEOPLE_LIMIT = 12
CAPPED_SUBSET_SIZE = 3
MIN_SUBSET_SIZE = 2
MAX_SUM_GROUP_SIZE = 3
DEFAULT_LIMITS = {"doppelt": 4, "zusammenSoAlt": 4, "jahrhundert": 3}
RARITY = {"doppelt": 0, "zusammenSoAlt": 1, "jahrhundert": 2}
WEEKDAYS = ["Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"]
MONTHS = ["Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August",
          "September", "Oktober", "November", "Dezember"]
EPOCH = dt.date(1970, 1, 1)


def day_index(d):
    return (d - EPOCH).days


def from_index(i):
    return EPOCH + dt.timedelta(days=i)


def ceil_div(a, b):
    return -((-a) // b)


def fmt_number(n):
    return f"{n:,}".replace(",", ".")


def long_date(d):
    return f"{WEEKDAYS[d.weekday()]}, {d.day}. {MONTHS[d.month - 1]} {d.year}"


def civil_key(d):
    return f"{d.year:04d}{d.month:02d}{d.day:02d}"


def is_owner(raw):
    t = raw.strip()
    return t == "" or t.lower() == "ich"


def display(raw):
    t = raw.strip()
    return "Ich" if t == "" else t


def self_or_name(name):
    return "ich" if is_owner(name) else display(name)


def self_or_name_dative(name):
    return "mir" if is_owner(name) else display(name)


def sentence(s):
    return s[:1].upper() + s[1:]


def owner_last(items, key=lambda x: x):
    return [i for i in items if not is_owner(key(i))] + [i for i in items if is_owner(key(i))]


def join_names(names):
    mapped = [self_or_name(n) for n in owner_last(names)]
    if len(mapped) <= 1:
        return mapped[0] if mapped else ""
    return ", ".join(mapped[:-1]) + " und " + mapped[-1]


def valid_date(p):
    try:
        return dt.date(p["birthYear"], p["birthMonth"], p["birthDay"])
    except ValueError:
        return None


def anchors(people):
    out = []
    for p in people:
        d = valid_date(p)
        if d is None:
            continue
        off = min(max(p.get("daysOffEarth", 0), 0), 100_000)
        out.append({"id": p["id"], "name": display(p["name"]), "raw": p["name"], "c": day_index(d) + off})
    return out


def ordered(a, i, j):
    return (i, j) if a[i]["c"] <= a[j]["c"] else (j, i)


def subset_id(group, a):
    return "-".join(sorted(a[i]["id"] for i in group))


def double_headline(older, younger):
    subject = "ich bin" if is_owner(older) else f"{display(older)} ist"
    return sentence(f"{subject} doppelt so alt wie {self_or_name(younger)}")


def double_caption(older, younger):
    return f"so alt, wie {self_or_name(older)} war, als {self_or_name(younger)} geboren wurde"


def double_share(older, younger, d):
    verb = "bin" if is_owner(older) else "ist"
    return f"Am {long_date(d)} {verb} {self_or_name(older)} doppelt so alt wie {self_or_name(younger)}. 🌍"


def sum_headline(group, other):
    return sentence(f"{join_names(group)} sind zusammen so alt wie {self_or_name(other)}")


def sum_share(group, other, d):
    return f"Am {long_date(d)} sind {join_names(group)} zusammen so alt wie {self_or_name(other)}. 🌍"


def century_label(group, a):
    if len(group) == len(a):
        return "Alle zusammen"
    if len(group) == len(a) - 1:
        missing = next(i for i in range(len(a)) if i not in group)
        return f"Alle außer {self_or_name_dative(a[missing]['raw'])}"
    return join_names([a[i]["raw"] for i in group])


def century_subject(group, a):
    if len(group) == len(a):
        return "alle"
    if len(group) == len(a) - 1:
        missing = next(i for i in range(len(a)) if i not in group)
        return f"alle außer {self_or_name_dative(a[missing]['raw'])}"
    return join_names([a[i]["raw"] for i in group])


def century_headline(group, a, years):
    return sentence(f"{century_label(group, a)}: {years} Jahre")


def century_share(group, a, years, d):
    return f"Am {long_date(d)} sind {century_subject(group, a)} zusammen {years} Jahre alt. 🌍"


def century_calendar_title(group, a, years, target):
    return f"{century_headline(group, a, years)} ({fmt_number(target)} Erdentage)"


def double_events(a, today, limit):
    cands = []
    for i in range(len(a)):
        for j in range(i + 1, len(a)):
            o, y = ordered(a, i, j)
            if a[y]["c"] <= a[o]["c"]:
                continue
            t = 2 * a[y]["c"] - a[o]["c"]
            if t < today:
                continue
            cands.append((t, o, y))
    cands.sort()
    out = []
    for t, o, y in cands[:limit]:
        older, younger = a[o], a[y]
        d = from_index(t)
        out.append({
            "kind": "doppelt", "date": d.isoformat(), "t": t,
            "participantIds": [older["id"], younger["id"]],
            "headline": double_headline(older["raw"], younger["raw"]),
            "caption": double_caption(older["raw"], younger["raw"]),
            "shareText": double_share(older["raw"], younger["raw"], d),
            "calendarTitle": double_headline(older["raw"], younger["raw"]),
            "id": f"doppelt-{older['id']}-{younger['id']}",
        })
    return out


def sum_events(a, today, limit):
    n = len(a)
    cands = []
    for size in range(2, MAX_SUM_GROUP_SIZE + 1):
        if size >= n:
            continue
        for group in combinations(range(n), size):
            sum_c = sum(a[i]["c"] for i in group)
            for j in range(n):
                if j in group:
                    continue
                t = (sum_c - a[j]["c"]) // (size - 1)
                if t < today:
                    continue
                cands.append((t, list(group), j))
    cands.sort(key=lambda c: (c[0], c[1], c[2]))
    out = []
    for t, group, j in cands[:limit]:
        names = [a[i]["raw"] for i in group]
        other = a[j]
        d = from_index(t)
        out.append({
            "kind": "zusammenSoAlt", "date": d.isoformat(), "t": t,
            "participantIds": [a[i]["id"] for i in group] + [other["id"]],
            "headline": sum_headline(names, other["raw"]),
            "caption": None,
            "shareText": sum_share(names, other["raw"], d),
            "calendarTitle": sum_headline(names, other["raw"]),
            "id": "summe-" + subset_id(group, a) + "-" + other["id"],
        })
    return out


def century_subsets(n):
    if n < MIN_SUBSET_SIZE:
        return []
    out = []
    if n <= FULL_ENUMERATION_PEOPLE_LIMIT:
        for size in range(MIN_SUBSET_SIZE, n + 1):
            out += [list(g) for g in combinations(range(n), size)]
        return out
    for size in range(MIN_SUBSET_SIZE, CAPPED_SUBSET_SIZE + 1):
        out += [list(g) for g in combinations(range(n), size)]
    out.append(list(range(n)))
    return out


def century_events(a, today, limit):
    cands = []
    for group in century_subsets(len(a)):
        m = len(group)
        sum_c = sum(a[i]["c"] for i in group)
        current = m * today - sum_c
        k = max(1, ceil_div(current, DAYS_PER_CENTURY))
        target = k * DAYS_PER_CENTURY
        t = (target + sum_c) // m
        if t < today:
            continue
        cands.append((t, group, k))
    cands.sort(key=lambda c: (c[0], c[1], c[2]))
    out = []
    for t, group, k in cands[:limit]:
        years = k * 100
        target = k * DAYS_PER_CENTURY
        d = from_index(t)
        out.append({
            "kind": "jahrhundert", "date": d.isoformat(), "t": t,
            "participantIds": [a[i]["id"] for i in group],
            "headline": century_headline(group, a, years),
            "caption": f"{fmt_number(target)} Erdentage",
            "shareText": century_share(group, a, years, d),
            "calendarTitle": century_calendar_title(group, a, years, target),
            "id": f"jahrhundert-{k}-" + subset_id(group, a),
        })
    return out


def possessive(raw, dative):
    if is_owner(raw):
        return "meinem" if dative else "mein"
    name = display(raw)
    sib = any(name.lower().endswith(s) for s in ("s", "ß", "x", "z"))
    return name + ("’" if sib else "s")


def possessives(people, ordinal, dative):
    parts = [possessive(p["name"], dative) for p in people]
    joined = ", ".join(parts[:-1]) + " und " + parts[-1] if len(parts) > 1 else parts[0]
    return joined if ordinal is None else f"{joined} {ordinal}."


def birthday_note(d, people):
    hits = [p for p in people
            if p.get("mentionsBirthday", True) and valid_date(p) is not None
            and p["birthMonth"] == d.month and p["birthDay"] == d.day]
    hits = owner_last(hits, key=lambda p: p["name"])
    if not hits:
        return None
    ordinal = None
    if len(hits) == 1:
        age = d.year - hits[0]["birthYear"]
        ordinal = age if age > 0 else None
    return {
        "caption": f"🎂 an {possessives(hits, ordinal, True)} Geburtstag",
        "sentence": f"Es ist {possessives(hits, ordinal, False)} Geburtstag. 🎂",
        "short": "🎂 " + ", ".join(display(p["name"]) for p in hits),
    }


def events(people, today, limits):
    a = anchors(people)
    if len(a) < 2:
        return []
    ti = day_index(today)
    out = []
    for kind in ("doppelt", "zusammenSoAlt", "jahrhundert"):
        lim = min(max((limits or DEFAULT_LIMITS).get(kind, DEFAULT_LIMITS[kind]), 0), 10)
        if lim <= 0:
            continue
        gen = {"doppelt": double_events, "zusammenSoAlt": sum_events, "jahrhundert": century_events}[kind]
        out += gen(a, ti, lim)
    out.sort(key=lambda e: (RARITY[e["kind"]], e["t"], e["id"]))
    result = []
    for e in out[:MAX_EVENTS]:
        d = dt.date.fromisoformat(e["date"])
        note = birthday_note(d, people)
        e = dict(e)
        del e["t"]
        e["addedKey"] = f"{e['id']}@{civil_key(d)}"
        e["birthdayCaption"] = note["caption"] if note else None
        e["shareMessage"] = e["shareText"] + (" " + note["sentence"] if note else "")
        result.append(e)
    return result


def is_prime(n):
    if n < 2:
        return False
    i = 2
    while i * i <= n:
        if n % i == 0:
            return False
        i += 1
    return True


def gap_tags(gap):
    tags = []
    if is_prime(gap) and (is_prime(gap - 2) or is_prime(gap + 2)):
        tags.append("zwillingsprimzahl")
    elif is_prime(gap):
        tags.append("primzahl")
    s = str(gap)
    if s == s[::-1]:
        tags.append("palindrom")
    r = int(gap ** 0.5)
    if any(k * k == gap for k in (r, r + 1)):
        tags.append("quadrat")
    if gap & (gap - 1) == 0:
        tags.append("zweierpotenz")
    digits = [int(ch) for ch in s]
    if len(s) >= 2 and (all(digits[i + 1] - digits[i] == 1 for i in range(len(s) - 1))
                        or all(digits[i] - digits[i + 1] == 1 for i in range(len(s) - 1))
                        or len(set(digits)) == 1):
        tags.append("ziffernfolge")
    cat = [1, 2, 5, 14, 42, 132, 429, 1430, 4862, 16796, 58786, 208012, 742900, 2674440,
           9694845, 35357670, 129644790, 477638700]
    if gap in cat:
        tags.append("catalan")
    fib = [1, 2]
    while fib[-1] < gap:
        fib.append(fib[-1] + fib[-2])
    if gap in fib:
        tags.append("fibonacci")
    # Order as the Kotlin enum declares the kinds.
    order = ["tausender", "zehntausender", "primzahl", "palindrom", "quadrat", "zweierpotenz",
             "ziffernfolge", "catalan", "fibonacci", "zwillingsprimzahl"]
    return sorted(tags, key=order.index)


def constellation_detail(older, younger, gap, younger_off):
    unit = "Tag" if gap == 1 else "Tage"
    distance = f"{fmt_number(gap)} {unit} auseinander"
    if younger_off != 0:
        return distance + "."
    return distance + f" — so alt war {self_or_name(older)}, als {self_or_name(younger)} geboren wurde."


def constellations(people):
    a = anchors(people)
    if len(a) < 2:
        return []
    offsets = {p["id"]: min(max(p.get("daysOffEarth", 0), 0), 100_000) for p in people}
    out = []
    for i in range(len(a)):
        for j in range(i + 1, len(a)):
            o, y = ordered(a, i, j)
            older, younger = a[o], a[y]
            gap = younger["c"] - older["c"]
            if gap <= 0:
                continue
            disp = [younger, older] if is_owner(older["raw"]) else [older, younger]
            out.append({
                "olderId": older["id"], "youngerId": younger["id"], "gap": gap,
                "displayIds": [p["id"] for p in disp],
                "headline": sentence(" & ".join(self_or_name(p["raw"]) for p in disp)),
                "detail": constellation_detail(older["raw"], younger["raw"], gap, offsets[younger["id"]]),
                "tags": gap_tags(gap),
                "id": f"konstellation-{older['id']}-{younger['id']}",
            })
    out.sort(key=lambda c: (c["gap"], c["id"]))
    return out[:MAX_CONSTELLATIONS]


def person(n, name, y, m, d, off=0, mentions=True):
    return {"id": f"aaaaaaaa-0000-4000-8000-{n:012d}", "name": name, "birthYear": y,
            "birthMonth": m, "birthDay": d, "daysOffEarth": off, "mentionsBirthday": mentions}


CASES = [
    {
        "name": "Kai, Albert und Oskar: the Verdopplungstag on Oskars 31. Geburtstag",
        "today": "2026-08-23",
        "limits": {"doppelt": 10, "zusammenSoAlt": 10, "jahrhundert": 10},
        "people": [person(1, "Kai", 1975, 3, 11), person(2, "Albert", 2012, 7, 20),
                   person(3, "Oskar", 2018, 11, 29)],
    },
    {
        "name": "Owner with Bernd, Mia and Opa at default limits",
        "today": "2026-08-15",
        "limits": None,
        "people": [person(1, "", 1990, 3, 15), person(2, "Bernd", 2015, 6, 2),
                   person(3, "Mia", 2018, 11, 20), person(4, "Opa", 1955, 1, 10)],
    },
    {
        "name": "Anne and an astronaut Bernd: the second reading is dropped",
        "today": "2026-08-15",
        "limits": None,
        "people": [person(1, "Anne", 1984, 5, 22), person(2, "Bernd", 1987, 1, 30, off=12)],
    },
    {
        "name": "Three people sharing a birthday, one of them the owner, one opted out",
        "today": "2026-08-15",
        "limits": {"doppelt": 10, "zusammenSoAlt": 0, "jahrhundert": 10},
        "people": [person(1, "", 1996, 6, 4), person(2, "Lars", 1990, 6, 4),
                   person(3, "Mia", 2001, 6, 4, mentions=False), person(4, "Max", 2010, 6, 4)],
    },
    {
        "name": "Reordered family keeps the same ids and added keys",
        "today": "2026-08-20",
        "limits": {"doppelt": 10, "zusammenSoAlt": 10, "jahrhundert": 10},
        "people": [person(3, "Mia", 2018, 11, 20), person(1, "Anne", 1985, 4, 12),
                   person(2, "Bernd", 2015, 7, 3)],
    },
]


def main():
    cases = []
    for c in CASES:
        today = dt.date.fromisoformat(c["today"])
        cases.append({
            "name": c["name"], "today": c["today"], "limits": c["limits"], "people": c["people"],
            "events": events(c["people"], today, c["limits"]),
            "constellations": constellations(c["people"]),
        })
    print(json.dumps({
        "schemaVersion": 1,
        "referenceIosCommit": IOS_COMMIT,
        "generatedBy": "shared-fixtures/tools/generate-together-math.py (interim, independent re-implementation; replace with an iOS-generated file)",
        "cases": cases,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
