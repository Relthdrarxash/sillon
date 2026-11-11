#!/usr/bin/env python3
"""Génère le site de Sillon (docs/index.html et docs/confidentialite.html). Usage : python3 scripts/gen_site.py docs"""
import os, re, sys
OUT = sys.argv[1]
CSS = """
:root { --bg:#F7F5EF; --fg:#1B2A22; --muted:#55645B; --line:#DAD8CC; --accent:#2F6B4F; --accent-solid:#2F6B4F; --warm:#C8742A; --on-accent:#fff;
  --panel: color-mix(in srgb,#000 3%,transparent); --serif:'Iowan Old Style','Palatino Linotype',Georgia,serif; --sans:system-ui,-apple-system,'Segoe UI',Roboto,sans-serif; color-scheme:light; }
@media (prefers-color-scheme: dark) { :root { --bg:#111815; --fg:#EEF2EC; --muted:#A5B3A9; --line:#2B3831; --accent:#7FC29C; --accent-solid:#2F6B4F; --warm:#E39A55; --panel:color-mix(in srgb,#fff 5%,transparent); color-scheme:dark; } }
* { box-sizing:border-box; }
html { scroll-behavior:smooth; }
body { margin:0; background-color:var(--bg);
  background-image: radial-gradient(48rem 34rem at 92% -6%, color-mix(in srgb,var(--accent-solid) 26%,transparent), transparent 62%),
    radial-gradient(44rem 32rem at -8% 24%, color-mix(in srgb,var(--warm) 16%,transparent), transparent 62%),
    radial-gradient(46rem 34rem at 100% 64%, color-mix(in srgb,var(--accent-solid) 14%,transparent), transparent 62%),
    radial-gradient(44rem 30rem at 0% 96%, color-mix(in srgb,var(--warm) 12%,transparent), transparent 62%);
  background-repeat:no-repeat; color:var(--fg); font-family:var(--sans); font-size:18px; line-height:1.6; }
a { color:inherit; }
:focus-visible { outline:3px solid var(--accent); outline-offset:3px; border-radius:6px; }
.wrap { max-width:1060px; margin:0 auto; padding-inline:20px; }
.top { display:flex; align-items:center; justify-content:space-between; gap:16px; padding-block:18px; flex-wrap:wrap; }
.brand { font-family:var(--serif); font-weight:700; font-size:1.5rem; text-decoration:none; }
.nav { display:flex; gap:18px; flex-wrap:wrap; font-size:.95rem; color:var(--muted); }
.nav a { text-decoration:none; } .nav a:hover { color:var(--fg); text-decoration:underline; }
.hero { display:grid; gap:32px; padding-block:28px 44px; align-items:center; }
.kicker { font-size:.8rem; font-weight:700; letter-spacing:.14em; text-transform:uppercase; color:var(--accent); margin:0 0 14px; }
h1,h2,h3 { font-family:var(--serif); }
h1 { font-size:clamp(2.3rem,7vw,4rem); line-height:1.04; letter-spacing:-.02em; margin:0; text-wrap:balance; }
.lead { color:var(--muted); font-size:1.15rem; max-width:34em; margin:20px 0 0; }
.cta { display:flex; gap:12px; flex-wrap:wrap; margin-top:28px; }
.btn { display:inline-flex; padding:13px 22px; border-radius:999px; font-weight:700; text-decoration:none; border:1.5px solid var(--line); }
.btn.primary { background:linear-gradient(120deg,var(--accent-solid),color-mix(in srgb,var(--accent-solid) 70%,#0c2a1c)); border-color:transparent; color:var(--on-accent); }
.btn:hover { border-color:var(--fg); } .btn.primary:hover { filter:brightness(1.1); }
.shot { justify-self:center; } .shot img { display:block; width:min(300px,70vw); height:auto; border-radius:28px; box-shadow:0 30px 70px -20px rgba(0,0,0,.45); }
@media (min-width:860px) { .hero { grid-template-columns:1.15fr .85fr; } .shot { justify-self:end; } }
section { padding-block:34px; }
h2 { font-size:clamp(1.6rem,4vw,2.3rem); line-height:1.1; margin:0 0 8px; text-wrap:balance; }
.sub { color:var(--muted); margin:0 0 24px; max-width:38em; }
.cards { display:grid; gap:14px; grid-template-columns:repeat(auto-fit,minmax(230px,1fr)); }
.card { padding:22px; border-radius:20px; border:1px solid var(--line); background:linear-gradient(150deg,color-mix(in srgb,var(--accent-solid) 12%,var(--bg)),var(--panel)); min-width:0; }
.card h3 { margin:0 0 8px; font-size:1.2rem; } .card p { margin:0; color:var(--muted); font-size:.98rem; line-height:1.5; }
.gallery { display:flex; gap:16px; overflow-x:auto; padding:4px 20px 18px; margin-inline:-20px; scroll-snap-type:x proximity; }
.gallery img { flex:0 0 auto; width:210px; height:auto; border-radius:20px; scroll-snap-align:start; border:1px solid var(--line); display:block; }
.rules { display:grid; grid-template-columns:repeat(auto-fit,minmax(190px,1fr)); border-top:1px solid var(--line); list-style:none; padding:0; margin:0; }
.rules li { padding:16px 16px 16px 0; border-bottom:1px solid var(--line); min-width:0; }
.rules strong { display:block; font-size:1.08rem; } .rules span { color:var(--muted); font-size:.95rem; }
.doc { max-width:46em; padding-block:24px 40px; } .doc h1 { font-size:clamp(2rem,5vw,2.8rem); margin-bottom:8px; }
.doc h2 { font-size:1.4rem; margin:34px 0 8px; } .doc p,.doc li { color:var(--muted); } .doc strong { color:var(--fg); } .doc li { margin-bottom:6px; }
.foot { border-top:1px solid var(--line); padding-block:30px 46px; color:var(--muted); font-size:.95rem; display:grid; gap:10px; } .foot p { margin:0; } .foot a { color:var(--fg); }
@media (prefers-reduced-motion:reduce) { html { scroll-behavior:auto; } }
"""
ICON = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64'%3E%3Crect width='64' height='64' rx='14' fill='%232F6B4F'/%3E%3Cpath d='M18 46c0-16 10-26 28-28 0 18-10 28-26 28z' fill='%23F7F5EF'/%3E%3C/svg%3E"
SHOTS = ["Travaux de la quinzaine", "Plan du potager", "Encyclopédie des plantes", "Fiche d'une plante", "Docteur du potager", "Réglages"]
DESC = "Sillon est un compagnon de potager : tâches de la quinzaine, plan des parcelles, fiches de plantes et diagnostic des maladies. Gratuit, libre, sans compte, sans réseau."

def page(title, desc, body):
    return f"""<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<meta name="description" content="{desc}">
<meta property="og:type" content="website">
<meta property="og:title" content="{title}">
<meta property="og:description" content="{desc}">
<link rel="icon" href="{ICON}">
<style>{CSS}</style>
</head>
<body>
<header class="wrap top">
  <a class="brand" href="./">Sillon</a>
  <nav class="nav" aria-label="Navigation">
    <a href="confidentialite.html">Confidentialité</a>
    <a href="https://github.com/Relthdrarxash/sillon">Code source</a>
  </nav>
</header>
{body}
<footer class="wrap foot">
  <p>Sillon fait partie des applis de <a href="https://relthdrarxash.github.io/">Pilmil Studio</a>.</p>
  <p>Contact : pilmil.studio@proton.me · <a href="https://github.com/Relthdrarxash/sillon/blob/main/LICENSE">Licence MPL-2.0</a></p>
</footer>
</body>
</html>
"""

cards = [
 ("Au potager aujourd'hui", "Les semis, repiquages et récoltes de la quinzaine, calculés à partir de votre date de dernières gelées."),
 ("Un plan vivant", "Carrés potagers et planches maraîchères, avec les affinités positives et les alertes de voisinage entre plantes."),
 ("Une encyclopédie", "Des fiches de plantes avec conseils de culture, bons et mauvais voisins, température de germination et besoins en eau."),
 ("Un docteur du potager", "Choisissez l'organe touché, comparez les symptômes et repérez maladies et ravageurs probables, sans connexion."),
]
rules = [("Aucun compte", "Elle s'ouvre, elle sert."), ("Aucune publicité", "Ni bannière, ni achat intégré."), ("Aucun traceur", "Aucune mesure d'usage."), ("Aucun accès à Internet", "L'appli n'en a même pas la permission."), ("Gratuite et libre", "Code ouvert sous licence MPL-2.0.")]

body = f"""<main>
<div class="wrap hero">
  <div>
    <p class="kicker">Compagnon de potager</p>
    <h1>Le bon geste au bon moment, au jardin.</h1>
    <p class="lead">Sillon dit ce qu'il y a à faire cette quinzaine, aide à organiser les parcelles et repère les maladies. Tout reste sur votre téléphone, sans réseau.</p>
    <div class="cta">
      <a class="btn primary" href="https://github.com/Relthdrarxash/sillon/releases">Télécharger l'APK</a>
      <a class="btn" href="https://github.com/Relthdrarxash/sillon">Voir le code</a>
    </div>
  </div>
  <div class="shot"><img src="captures/s1.webp" alt="{SHOTS[0]}" width="540" height="960"></div>
</div>
<section class="wrap" id="fonctions">
  <h2>Ce que fait Sillon</h2>
  <p class="sub">Quatre écrans pour suivre la saison, du semis à la récolte.</p>
  <div class="cards">
""" + "".join(f'    <article class="card"><h3>{a}</h3><p>{b}</p></article>\n' for a, b in cards) + """  </div>
</section>
<section class="wrap" id="images">
  <h2>L'application en images</h2>
  <div class="gallery" role="list">
""" + "".join(f'    <img role="listitem" src="captures/s{i}.webp" alt="{SHOTS[i-1]}" width="210" height="373" loading="lazy">\n' for i in range(1, 7)) + """  </div>
  <p class="sub">Captures réalisées avec des données d'exemple.</p>
</section>
<section class="wrap" id="vie-privee">
  <h2>Rien ne sort du téléphone</h2>
  <ul class="rules" style="margin-top:20px">
""" + "".join(f'    <li><strong>{a}</strong><span>{b}</span></li>\n' for a, b in rules) + """  </ul>
</section>
</main>"""

priv = """<main class="wrap doc">
<h1>Confidentialité</h1>
<p>Dernière mise à jour : 10 novembre 2026.</p>
<h2>En bref</h2>
<p><strong>Sillon ne collecte aucune donnée personnelle.</strong> Il n'y a ni compte, ni serveur, ni traceur, ni publicité, ni mesure d'audience. Votre potager, vos parcelles et vos réglages restent sur votre téléphone.</p>
<h2>Internet</h2>
<p>L'application ne déclare pas la permission Internet dans son manifeste Android. Elle est techniquement incapable d'envoyer ou de recevoir des données par le réseau.</p>
<h2>Autorisations demandées</h2>
<ul><li><strong>Notifications</strong> : uniquement pour les rappels de semis, que vous activez vous-même dans les réglages. Vous pouvez refuser cette autorisation sans perdre aucune autre fonction.</li></ul>
<h2>Localisation</h2>
<p>Sillon n'utilise pas la localisation de l'appareil. Vous choisissez vous-même votre département, qui sert à estimer la date des dernières gelées.</p>
<h2>Données conservées sur le téléphone</h2>
<p>Le plan de votre potager et vos réglages sont enregistrés dans l'espace privé de l'application. Ils sont supprimés si vous désinstallez l'application ou effacez ses données.</p>
<h2>Sauvegarde</h2>
<p>La sauvegarde est un fichier que vous enregistrez vous-même à l'endroit de votre choix. Sillon ne l'envoie nulle part.</p>
<h2>Contact</h2>
<p>Une question sur la vie privée : <strong>pilmil.studio@proton.me</strong>. Le code source est public et peut être relu sur GitHub.</p>
</main>"""

for name, html in {"index.html": page("Sillon, compagnon de potager", DESC, body),
                   "confidentialite.html": page("Confidentialité de Sillon", DESC, priv)}.items():
    assert "—" not in html, name
    open(os.path.join(OUT, name), "w").write(html); print(name, len(html))
open(os.path.join(OUT, ".nojekyll"), "w").close()
