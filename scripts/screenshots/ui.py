#!/usr/bin/env python3
"""Petit outil uiautomator : touche l'élément dont le texte (ou la description) contient une chaîne.

Usage : ui.py tap "texte" [index]   |   ui.py dump
« =Texte » : correspondance exacte. Un index négatif compte depuis la fin (-1 = dernier).
"""
import re, subprocess, sys, time, xml.etree.ElementTree as ET

def dump():
    for _ in range(5):
        r = subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True, text=True)
        if "dumped" in r.stdout:
            break
        time.sleep(1)
    xml = subprocess.run(["adb", "shell", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout
    return ET.fromstring(xml[xml.index("<"):])

def find(root, text):
    # « =Texte » : correspondance exacte ; sinon, le texte doit contenir la chaîne.
    exact = text.startswith("=")
    t = text.lstrip("=").lower()
    def ok(v):
        v = (v or "").strip().lower()
        return v == t if exact else t in v
    return [n for n in root.iter("node") if ok(n.get("text")) or ok(n.get("content-desc"))]

def main():
    if sys.argv[1] == "dump":
        for n in dump().iter("node"):
            s = (n.get("text") or n.get("content-desc") or "").strip()
            if s:
                print(n.get("bounds"), s[:70])
        return
    text = sys.argv[2]
    idx = int(sys.argv[3]) if len(sys.argv) > 3 else 0
    for attempt in range(6):
        nodes = find(dump(), text)
        if nodes and -len(nodes) <= idx < len(nodes):
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", nodes[idx].get("bounds")))
            subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])
            print(f"tap '{text}' at {(x1 + x2) // 2},{(y1 + y2) // 2}")
            return
        time.sleep(1.5)
    print(f"introuvable : '{text}'", file=sys.stderr)
    sys.exit(1)

main()
