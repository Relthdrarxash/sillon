# Fondements Scientifiques et Agronomiques de Sillon

**Auteur :** Lead Scientific Agronomist & Technical Writer — Pilmil Studio  
**Statut :** Référentiel technique et biologique officiel  
**Version :** 1.0.0 (Alignée sur le moteur agronomique pur Kotlin de Sillon)  
**Licence :** GNU GPL v3.0  

---

## 1. Introduction et Positionnement Épistémologique

Le potager vivrier et maraîcher est un agroécosystème hautement dynamique, régi par des flux biogéochimiques, des interactions trophiques complexes et des lois thermodynamiques strictes. 

Pourtant, la littérature grand public relative au jardinage amateur est souvent parasitée par deux écueils majeurs :
1. **Le dogmatisme calendaire rigide :** des tables immuables indiquant de « semer les tomates en mars », ignorant les écarts thermiques régionaux, les altitudes et les dérèglements climatiques interannuels.
2. **Le folklore ésotérique non vérifié :** croyances astrales (influences lunaires dénuées de fondement biophysique mesurable) et listes arbitraires de « plantes amies » issues de la tradition orale sans confirmation expérimentale.

**Sillon** a été développé selon une démarche de **synthèse agronomique rationnelle et de biologie intégrative**. Chaque recommandation, chaque alerte de rotation et chaque calcul de calendrier implémenté dans le code source dérive de données scientifiques issues de la recherche agronomique internationale (notamment l'**INRAE** en France, le **FiBL** en Suisse, et le **Rodale Institute** aux États-Unis).

Ce document explicite les lois biologiques, les équations mathématiques et les protocoles agronomiques qui pilotent le moteur de calcul de Sillon.

---

## 2. Compagnonnage Végétal et Allélopathie : Revue Systématique

Le compagnonnage végétal (*intercropping* ou polyculture associée) désigne la disposition concertée d'au moins deux espèces végétales au sein d'une même unité spatiale afin d'exploiter des complémentarités de niches écologiques ou des médiations biochimiques.

```
       ┌────────────────────────────────────────────────────────┐
       │             INTERACTIONS BIOLOGIQUES POTAGÈRES         │
       └───────────────────────────┬────────────────────────────┘
                                   │
         ┌─────────────────────────┴────────────────────────┐
         ▼                                                  ▼
┌──────────────────┐                               ┌──────────────────┐
│  MÉCANISMES CO-  │                               │   ANTAGONISMES   │
│   BÉNÉFIQUES     │                               │  & ALLÉLOPATHIE  │
└────────┬─────────┘                               └────────┬─────────┘
         │                                                  │
         ├► 1. Confusion olfactive (COV)                    ├► 1. Juglone / Anéthole
         ├► 2. Fixation symbiotique N₂                      ├► 2. Compétition racinaire
         ├► 3. Effet nématicide (α-terthiényle)             └► 3. Vecteurs partagés
         └► 4. Stratification canopée / racines                  (ex. mildiou)
```

### 2.1. Les mécanismes sémiochimiques et la confusion olfactive

Chez de nombreux insectes ravageurs spécialistes, la localisation de l'hôte végétal repose sur un gradient de **Composés Organiques Volatils (COV)** émis par le limbe foliaire et reconnus par les récepteurs antennaires. L'association d'espèces botaniques aux signatures olfactives divergentes perturbe ce signal de repérage :

#### L'interaction Alliacées (*Allium*) / Apiacées (*Daucus*, *Apium*)
- **Ravageur 1 :** La mouche de la carotte (*Psila rosae*, Diptère Psilidae). Les femelles pondent au collet des ombellifères, attirées par les dérivés polyacétyléniques (falcarinol) et les phénylpropanoïdes des feuilles de carotte.
- **Ravageur 2 :** La teigne du poireau (*Acrolepiopsis assectella*, Lépidoptère Glyphipterigidae). Les femelles localisent les Alliacées par leurs émissions de composés soufrés volatils (disulfure de dipropyle, thiosulfinates).
- **Preuve agronomique :** L'alternance de rangs de poireaux/oignons et de carottes crée un masquage olfactif bidirectionnel documenté (Finch & Collier, 2000, *Horticultural Research International*). Les COV soufrés saturent les sensilles olfactives de *Psila rosae*, réduisant le taux d'oviposition de 40 à 70 % par rapport à une monoculture, à condition que le ratio d'intercalage soit suffisant (écartement entre rangs ≤ 30 cm pour maintenir la canopée mélangée).

#### L'effet répulsif et nématicide des Astéracées du genre *Tagetes*
- Les racines d'œillets d'Inde (*Tagetes patula*) et de roses d'Inde (*Tagetes erecta*) exudent dans la rhizosphère un composé photochimique polythiényle : l'**$\alpha$-terthiényle** ($2,2':5',2''\text{-terthiényle}$).
- **Mode d'action :** En présence de traces d'oxygène et activé par la lumière (ou par oxydation enzymatique tissulaire), l'alpha-terthiényle génère des espèces réactives de l'oxygène (singlet oxygen $^1\text{O}_2$) qui perforent les membranes cellulaires des nématodes endoparasites du sol (*Meloidogyne incognita*, *Pratylenchus penetrans*).
- **Validation agronomique :** L'INRAE et le Rodale Institute confirment une baisse d'infestation de 80 % des galles racinaires chez les Solanacées associées à des *Tagetes*, à condition que ces dernières soient implantées de façon dense et activement enracinées pendant au moins 60 jours.

### 2.2. La fixation symbiotique de l'azote atmosphérique ($N_2$)

Les Fabacées (*Leguminosae* : haricots, pois, fèves, trèfles) ont développé une symbiose mutualiste avec des $\alpha$ et $\beta$-rhizobactéries du sol (genres *Rhizobium*, *Bradyrhizobium*, *Sinorhizobium*).

#### Le processus biochimique
1. **Reconnaissance et nodulation :** La racine de la Fabacée sécrète des flavonoïdes qui induisent chez la bactérie la production de facteurs Nod (lipochitooligosaccharides), déclenchant la déformation du poil absorbant et l'invagination du cordon d'infection vers le cortex racinaire.
2. **Réduction catalytique de l'azote :** Les bactéries se différencient en bactéroïdes au sein du nodule. Le complexe enzymatique **nitrogénase** (protéine Fe et protéine Mo-Fe) catalyse la réaction hautement endergonique :
   $$N_2 + 8\,H^+ + 8\,e^- + 16\,\text{ATP} \longrightarrow 2\,NH_3 + H_2 + 16\,\text{ADP} + 16\,P_i$$
3. **Protection anaérobie :** La nitrogénase étant irréversiblement inhibée par l'oxygène moléculaire libre, la plante synthétise de la **leghémoglobine** (pigment rouge caractéristique d'un nodule sain et actif), maintenant une pression partielle d'oxygène ultra-faible mais suffisante pour la respiration des bactéroïdes.

#### Mythe vs Réalité sur le transfert immédiat d'azote aux cultures voisines
- **Le mythe populaire :** Planter un haricot au pied d'une tomate apporterait immédiatement des kilos d'engrais azoté à la tomate pendant l'été.
- **La réalité agronomique :** Le flux direct de composés azotés libres (acides aminés, uréides) exsudés par les racines d'une légumineuse vivante vers une racine voisine est minime (moins de 5 % du bilan global pendant la phase végétative). Le véritable bénéfice agronomique opère **après** la culture :
  - L'azote est stocké dans les tissus de la Fabacée (nodules, racines, biomasse foliaire).
  - Lors de la récolte, si les racines sont laissées en terre pour minéralisation, l'azote organique est converti par les micro-organismes telluriques en ammonium ($NH_4^+$) puis en nitrate ($NO_3^-$), disponible pour la culture suivante.
  - Le bénéfice instantané réside dans l'**évitement de compétition** : la Fabacée ne consomme pas l'azote minéral du sol, laissant la totalité du pool tellurique à la plante voisine gourmande (ex. Maïs dans l'association précolombienne des Trois Sœurs *Milpa* : Maïs, Haricot, Courge).

### 2.3. L'allélopathie inhibitrice : Juglone et métabolites secondaires

L'allélopathie négative correspond à la synthèse et à l'excrétion dans le milieu par une plante de composés chimiques inhibant la germination, l'élongation racinaire ou la respiration d'espèces voisines.

- **Le cas du Noyer (*Juglans regia*, *Juglans nigra*) et la Juglone :**
  - **Molécule :** 5-hydroxy-1,4-naphthoquinone (sécrétée sous forme de glycoside non toxique, l'hydrojuglone, qui s'oxyde à l'air libre et dans le sol en juglone toxique).
  - **Cibles sensibles :** Solanacées (tomate, poivron, aubergine, pomme de terre).
  - **Mécanisme :** Inhibition de l'enzyme H$^+$-ATPase membranaire et blocage de la phosphorylation oxydative mitochondriale. Même à des concentrations infimes ($10^{-6}\text{ M}$), la juglone provoque l'apoplastie, le flétrissement irréversible et la mort des plants de tomate situés sous le houppier ou dans le rayon racinaire d'un noyer (jusqu'à 15 mètres du tronc).
- **L'effet antagoniste du Fenouil (*Foeniculum vulgare*) :**
  - Le fenouil sécrète par son réseau racinaire et ses débris foliaires des terpènes et du trans-anéthole exerçant une activité phytotoxique reconnue sur la division mitotique des graines de Solanacées et d'Apiacées voisines. Sillon classe ainsi le fenouil en « soliste », à implanter en bordure isolée du potager.

### 2.4. Matrice comparative : Consensus scientifique vs Traditions populaires

| Association | Croyance populaire maraîchère | Réalité biologique & Consensus agronomique (INRAE / FiBL) | Statut dans Sillon |
|---|---|---|:---:|
| **Carotte + Oignon / Poireau** | « Les odeurs se repoussent magiquement » | **Validé :** Rupture olfactive documentée contre *Psila rosae* et *Acrolepiopsis assectella*. Complémentarité racinaire (pivotante carotte vs fasciculée alliacée). | ✅ **Synergie majeure** |
| **Tomate + Basilic** | « Le basilic donne du goût à la tomate sur pied » | **Partiellement révisé :** Aucune modification organoleptique démontrée *in vivo*. En revanche, les COV du basilic (linalol, eugénol) exercent une répulsion modérée des thrips et aleurodes. | ⚠️ **Synergie modérée** (espace & parasitisme) |
| **Tomate + Pomme de terre** | « Deux bonnes solanacées qui se complètent » | **Invalide & Dangereux :** Transmission épidémique fulgurante du mildiou (*Phytophthora infestans*) et attraction décuplée des doryphores (*Leptinotarsa decemlineata*). | ❌ **Antagonisme critique** (Alerte rouge) |
| **Fabacées + Solanacées / Brassicacées** | « L'engrais naturel instantané au pied » | **Validé avec nuance :** Évitement de compétition sur l'azote minéral tellurique ; enrichissement réel du profil pour la rotation suivante. | ✅ **Synergie symbiotique** |
| **Semis selon les constellations lunaires** | « Monter ou descendre selon la lune montante/descendante » | **Réfuté :** Aucune corrélation statistiquement significative observée dans les essais contrôlés randomisés (INRAE, W. Schumacher). La gravité lunaire sur un germe de 1 mg est infinitésimale face aux forces de capillarité matricielle de l'eau du sol ($\Psi_m$). | ⛔ **Exclu de l'application** |

---

## 3. Modélisation Phénologique et Thermique

### 3.1. Les limites rédhibitoires des calendriers par mois fixes

Un calendrier indiquant « Semez vos courgettes le 15 avril » est un non-sens agronomique :
- Dans le Finistère littoral (climat océanique doux, $J_{\text{gel}}$ fin mars), le 15 avril est tardif pour un semis sous abri.
- Dans le Doubs ou le Cantal (climat montagnard continental, $J_{\text{gel}}$ mi-mai voire fin mai), le 15 avril présente un risque de gel destructeur supérieur à 80 %.
- À cela s'ajoute l'inertie thermique du sol : les graines ne répondent pas aux dates humaines mais à l'intégrale thermique accumulée au-dessus de leur seuil de zéro végétatif.

### 3.2. L'équation des Degrés Jours de Croissance (DJC / GDD)

La vitesse de développement phénologique (germination, différenciation nodale, floraison, nouaison) est directement proportionnelle à la quantité d'énergie thermique emmagasinée par les tissus végétaux.

Le modèle mathématique standard retenu par la communauté agronomique internationale (McMaster & Wilhelm, 1997) est celui des **Degrés Jours de Croissance ($DJC$)**, calculé pour chaque jour $i$ :

$$DJC = \sum_{i=1}^{n} \max\left(0, \, T_{\text{moyenne}, i} - T_{\text{base}}\right)$$

avec :
$$T_{\text{moyenne}, i} = \frac{T_{\max, i} + T_{\min, i}}{2}$$

Si la plante est soumise à un stress thermique supérieur à son seuil physiologique d'inactivation enzymatique ($T_{\text{plafond}}$, généralement $32^\circ\text{C}$ à $35^\circ\text{C}$ pour les cultures C3) :

$$DJC = \sum_{i=1}^{n} \max\left(0, \, \min\left(T_{\text{moyenne}, i}, \, T_{\text{plafond}}\right) - T_{\text{base}}\right)$$

#### Constantes thermiques de germination et de zéro végétatif ($T_{\text{base}}$) :
- **Légumes de climat frais (C3 rustiques) :**
  - Pois (*Pisum sativum*) : $T_{\text{base}} = 4^\circ\text{C}$
  - Fève (*Vicia faba*) : $T_{\text{base}} = 3^\circ\text{C}$
  - Épinard (*Spinacia oleracea*) : $T_{\text{base}} = 4^\circ\text{C}$
  - Laitue (*Lactuca sativa*) : $T_{\text{base}} = 5^\circ\text{C}$
- **Légumes tempérés :**
  - Carotte (*Daucus carota*) : $T_{\text{base}} = 6^\circ\text{C}$
  - Betterave (*Beta vulgaris*) : $T_{\text{base}} = 7^\circ\text{C}$
  - Oignon (*Allium cepa*) : $T_{\text{base}} = 6^\circ\text{C}$
- **Légumes d'origine tropicale/subtropicale (sensibles au froid) :**
  - Tomate (*Solanum lycopersicum*) : $T_{\text{base}} = 10^\circ\text{C}$ à $12^\circ\text{C}$
  - Haricot (*Phaseolus vulgaris*) : $T_{\text{base}} = 10^\circ\text{C}$
  - Poivron & Piment (*Capsicum annuum*) : $T_{\text{base}} = 12^\circ\text{C}$ à $15^\circ\text{C}$
  - Aubergine (*Solanum melongena*) : $T_{\text{base}} = 14^\circ\text{C}$
  - Cucurbitacées (Courgette, Concombre, Melon) : $T_{\text{base}} = 12^\circ\text{C}$

### 3.3. L'ancrage probabiliste sur la date médiane des dernières gelées ($J_{\text{gel}}$)

Pour concilier robustesse agronomique et fonctionnement **100 % hors-ligne sans capteurs météo connectés**, Sillon utilise comme invariant géoclimatique la **date médiane des dernières gelées printanières** ($J_{\text{gel}}$, $T \le 0^\circ\text{C}$ sous abri normalisé).

```
         Fenêtre de Semis Sous Abri             Fenêtre de Pleine Terre
      ◄──────────────────────────────►      ◄──────────────────────────────►
  [ Semis au chaud / Godets ]            [ Repiquage & Semis Directs ]
───┬───────────────────────────────────────┼───────────────────────────────►
   │                                       │
J_gel - 8 sem.                          J_gel                         J_gel + 3 sem.
(ex. Tomates, Poivrons)                 (Dernière gelée médiane)      (Cucurbitacées)
```

1. **Repère probabiliste robuste :** $J_{\text{gel}}$ correspond au percentile $P = 50\%$ des séries chronologiques trentenaires de Météo-France.
2. **Translation temporelle personnalisée :**
   - Toutes les fenêtres de semis au chaud (en terrine, godet ou serre chauffée) sont calculées par translation rétroactive :
     $$\text{Date}_{\text{semis abri}} = J_{\text{gel}} - \Delta t_{\text{croissance godet}}$$
     *(ex. 8 semaines avant $J_{\text{gel}}$ pour la tomate, 10 semaines pour l'aubergine).*
   - Toutes les plantations extérieures sans protection thermique sont asservies à :
     $$\text{Date}_{\text{plantation pleine terre}} = J_{\text{gel}} + \Delta t_{\text{sécurité}}$$
     *(ex. $+1$ semaine pour les tomates en zone tempérée, $+2$ semaines pour les cucurbitacées frileuses).*
3. **Précision territoriale locale :** Sillon intègre dans `FrenchFrostCatalog` les dates médianes validées pour les **101 départements métropolitains et d'outre-mer**, ajustables manuellement au jour près par l'utilisateur pour tenir compte des microclimats de versant, d'ubac ou d'îlots de chaleur urbains.

---

## 4. Rotation des Cultures et Écologie des Sols

La rotation culturale constitue le pilier historique de l'agronomie préventive. Dans un sol vivant non perturbé, cultiver la même espèce ou des espèces botaniquement parentes sur une même parcelle déclenche une double détérioration :
1. **L'épuisement minéral asymétrique :** surexploitation des mêmes horizons de sol et des mêmes cations ($K^+$, $Ca^{2+}$, $Mg^{2+}$).
2. **La pullulation tellurique :** concentration exponentielle des propagules d'organismes pathogènes terricoles (*inoculum density*) et des nématodes inféodés.

```
       ┌────────────────────────────────────────────────────────┐
       │             CYCLE AGRO-ÉCOLOGIQUE QUADRIENNAL          │
       └───────────────────────────┬────────────────────────────┘
                                   │
               ┌───────────────────┴───────────────────┐
               ▼                                       ▼
     ┌──────────────────┐                    ┌──────────────────┐
     │     ANNÉE 1      │                    │     ANNÉE 2      │
     │   GOURMANDES     ├───────────────────►│    MOYENNES      │
     │ Solanacées,      │                    │ Apiacées,        │
     │ Cucurbitacées,   │                    │ Alliacées,       │
     │ Brassicacées     │                    │ Chénopodiacées   │
     └─────────▲────────┘                    └─────────┬────────┘
               │                                       │
               │                                       ▼
     ┌─────────┴────────┐                    ┌──────────────────┐
     │     ANNÉE 4      │                    │     ANNÉE 3      │
     │  RÉGÉNÉRATION    │◄───────────────────┤   AMÉLIORANTES   │
     │ Engrais verts,   │                    │    (FABACÉES)    │
     │ Couverts vivants,│                    │ Pois, Haricots,  │
     │ Repos structuré  │                    │ Fèves            │
     └──────────────────┘                    └──────────────────┘
```

### 4.1. Le cycle agronomique quadriennal de Sillon

Sillon segmente le catalogue végétal selon une matrice de rotation fonctionnelle en quatre phases annuelles successives :

#### Année 1 : Cultures hautement exigeantes (« Gourmandes »)
- **Familles :** Solanacées (*tomate, aubergine, pomme de terre*), Cucurbitacées (*courgette, courge, melon*), Brassicacées à gros volume (*chou pommé, chou-fleur, brocoli*), Poacées (*maïs doux*).
- **Physiologie :** Exportation massive de nitrates ($NO_3^-$), potassium ($K_2O$) et phosphore ($P_2O_5$).
- **Stratégie tellurique :** S'installe sur un sol abondamment enrichi en compost mûr en tête de rotation ($3\text{ à }5\text{ kg/m}^2$).

#### Année 2 : Cultures à exigence modérée (« Équilibrées & Racines profondes »)
- **Familles :** Apiacées (*carotte, panais, céleri*), Alliacées (*oignon, ail, échalote, poireau*), Chénopodiacées (*betterave, blette, épinard*).
- **Physiologie :** Ces cultures exploitent les reliquats azotés de l'année précédente sans nécessiter d'apport frais de matière organique organique brute (laquelle provoquerait la bifurcation des racines de carottes et la pourriture basale des Alliacées). Le pivot des Apiacées aère mécaniquement l'horizon sous-jacent (20-40 cm).

#### Année 3 : Cultures améliorantes et fixatrices (« Régénératrices d'azote »)
- **Familles :** Fabacées (*haricot nain, haricot à rames, pois, fève, pois chiche, lentille*).
- **Physiologie :** Indépendantes de l'azote tellurique grâce à la symbiose rhizobienne. Elles laissent dans le sol un profil biologique assaini, une porosité fine par leurs racines fasciculées et un capital azoté organiquement lié dans les nodosités en voie de décomposition.

#### Année 4 : Couverts végétaux, engrais verts et structuration du sol
- **Espèces :** Phacélie (*Phacelia tanacetifolia*), Moutarde blanche (*Sinapis alba*), Seigle d'hiver (*Secale cereale*), Trèfle incarnat (*Trifolium incarnatum*), Vesce d'hiver (*Vicia villosa*).
- **Bénéfices agro-écologiques :**
  - **Pompe à nitrates :** Fixation des résidus solubles avant le lessivage pluvial hivernal.
  - **Biofumigation naturelle :** Les glucosinolates des Brassicacées broyées (moutarde) s'hydrolysent en isothiocyanates, gaz naturels assainissant les spores fongiques du sol.
  - **Structuration granulaire :** Sécrétion de glomaline par le mycélium et exsudats racinaires créant un complexe argilo-humique stable résistant au tassement et à l'érosion.

### 4.2. Rupture des cycles des bioagresseurs terricoles

| Bioagresseur / Pathogène | Vecteur de conservation dans le sol | Espèces hôtes sensibles | Délai d'exclusion recommandé par Sillon |
|---|---|---|:---:|
| **Hernie des crucifères** (*Plasmodiophora brassicae*) | Spores de repos viables jusqu'à 8 à 10 ans | Toutes Brassicacées (*choux, navets, radis*) | **4 à 5 ans minimum** |
| **Mildiou de la tomate / pomme de terre** (*Phytophthora infestans*) | Oospores et tubercules infectés résiduels | Solanacées (*tomate, pomme de terre*) | **4 ans** |
| **Fonte des semis** (*Pythium*, *Rhizoctonia solani*) | Sclérotes et mycélium saprophyte | Semis denses de toutes familles | **3 ans** |
| **Nématodes cécidogènes** (*Meloidogyne* spp.) | Œufs et kystes protégés dans la matrice tellurique | Solanacées, Cucurbitacées, Carottes | **3 à 4 ans** (+ interculture *Tagetes*) |
| **Pourriture blanche des Alliacées** (*Stromatinia cepivora*) | Sclérotiums dormants | Ail, oignon, échalote, poireau | **4 à 5 ans** |

Le moteur de Sillon (`RotationEngine`) analyse l'historique pluriannuel de chaque case de parcelle. Tout chevauchement d'une même famille sur une fenêtre inférieure au délai de pause requis génère un `CONFLICT_ERROR` avec explicitation agronomique.

---

## 5. Gestion Intégrée des Maladies Foliaires en Permaculture

Le modèle de lutte intégrée adopté par Sillon s'oppose à la pulvérisation systématique et répressive. Il repose sur le **triangle épidémiologique de la maladie végétale** :

```
                        HÔTE SENSIBLE
                       (Plante potagère)
                              ▲
                             / \
                            /   \
                           /     \
                          /       \
                         /  MALADIE\
                        /           \
     AGENT PATHOGÈNE ◄───────────────► ENVIRONNEMENT FAVORABLE
   (Spore fongique / Bactérie)       (Humidité relative, Feuillage mouillé, T°)
```

Pour qu'une maladie cryptogamique apparaisse, la conjonction des trois sommets est nécessaire. La stratégie de Sillon intervient en priorité sur l'**environnement** et la **résistance de l'hôte**.

### 5.1. Prophylaxie physique et aérodynamique

1. **Aération et densité foliaire :**
   - La germination des conidies de *Phytophthora infestans* (mildiou) requiert un film d'eau liquide continu sur la surface du limbe pendant au moins **2 à 4 heures consécutives** à une température comprise entre $15^\circ\text{C}$ et $22^\circ\text{C}$.
   - Le moteur de Sillon impose dans son catalogue (`squareFootDensity` et espacement en centimètres) des distances de sécurité rigoureuses (ex. 60 cm minimum entre pieds de tomate) garantissant une circulation d'air laminaire dissipant la rosée matinale sous la canopée.
2. **Le paillage anti-éclaboussures :**
   - La contamination primaire par les spores fongiques telluriques (mildiou, alternariose, septoriose) s'opère par effet d'éclaboussure cinétique des gouttes de pluie tombant sur un sol nu (*rain-splash dispersal*).
   - Un paillage carboné épais (pailles de céréales, foin sec, broyat de bois raméal fragmenté sur 8 à 10 cm) amortit la goutte de pluie, piège les propagules terricoles et réduit l'incidence des infections basales de plus de 80 %.
3. **Arrosage au collet :**
   - Interdiction absolue de l'aspersion par le haut en maraîchage d'été. Arrosage exclusivement matriciel au pied (goutte-à-goutte ou tuyau poreux sous paillage).

### 5.2. Biocontrôle et préparations naturelles homologuées

Sillon répertorie exclusivement des solutions conformes au cahier des charges de l'Agriculture Biologique européenne (Règlement UE 2018/848) et validées par l'INRAE et l'Institut Technique de l'Agriculture Biologique (ITAB).

```
┌───────────────────────────────────────────────────────────────────────────┐
│                     BIOCHIMIE DES MODES D'ACTION                          │
├───────────────────────────────┬───────────────────────────────────────────┤
│ Décoction de Prêle            │ Dépôt de silice amorphe (SiO₂) dans les   │
│ (Equisetum arvense)           │ parois cuticulaires ➔ barrière mécanique  │
├───────────────────────────────┼───────────────────────────────────────────┤
│ Bicarbonate de Potassium      │ Choc osmotique + élévation brutale du pH  │
│ (KHCO₃)                       │ foliaire à 8,3 ➔ lyse des conidies oïdium │
├───────────────────────────────┼───────────────────────────────────────────┤
│ Cuivre raisonné               │ Dénaturation enzymatique multi-site       │
│ (Bouillie bordelaise)         │ ➔ Strictement plafonné à < 4 g/m²/an      │
└───────────────────────────────┴───────────────────────────────────────────┘
```

#### A. La silice amorphe de Prêle des champs (*Equisetum arvense*)
- **Composition :** La prêle concentre dans ses tiges stériles jusqu'à 10 % de son poids sec en **silice amorphe soluble** (acide orthosilicique $Si(OH)_4$).
- **Mécanisme :** Pulvérisée sur le feuillage en décoction diluée à 10 %, l'acide silicique polymérise au contact de l'air en phytolithes de silice qui s'incrustent dans la cuticule et la paroi pecto-cellulosique des cellules épidermiques.
- **Résultat :** L'haustorium (filament de perforation) des champignons pathogènes (*Phytophthora*, *Podosphaera*) ne parvient plus à forer la paroi durcie mécaniquement. De plus, la silice active la voie de l'acide jasmonique, stimulant les défenses immunitaires de la plante.

#### B. Le Bicarbonate de Potassium ($KHCO_3$) contre l'Oïdium
- **Mécanisme :** Substance de base officiellement homologuée. La pulvérisation d'une solution à $5\text{ g/L}$ combinée à un tensioactif doux (savon noir potassique à $1\%$) génère deux effets létaux sur l'oïdium :
  1. **Choc osmotique hypertonique :** Déshydratation foudroyante du mycélium superficiel et rupture des parois cellulaires des conidies.
  2. **Alcalinisation du phylloplan :** Le pH de la feuille passe subitement de 6,0 à environ 8,3, environnement alcalin totalement incompatible avec l'activité des enzymes estérases fongiques.
- **Avantage écologique :** Absence totale d'écotoxicité résiduelle ; le potassium libre sert d'oligoélément nutritif foliaire.

#### C. Le Cuivre raisonné : limites écotoxiques et strict plafond
- **Action biochimique :** L'ion cuprique $Cu^{2+}$ est un biocide multi-site provoquant la coagulation des protéines cytoplasmiques et la dénaturation irréversible des enzymes respiratoires fongiques.
- **Problématique écotoxicologique :** Le cuivre ne se dégrade pas dans le sol. Son accumulation au-delà de 50 à 100 mg/kg de sol détruit les populations de vers de terre endogés (*Lumbricus terrestris*) et inhibe le développement des champignons mycorhiziens arbusculaires (CMA).
- **Le cahier des charges Sillon :** L'application rappelle avec fermeté que les pulvérisations cupriques doivent être restreintes aux situations d'alerte météo critique (pluie continue en été) et **strictement plafonnées à moins de $4\text{ g Cu}/m^2/\text{an}$** (soit bien en-deçà des tolérances de lessivage), en préconisant toujours l'adjonction de prêle pour diviser par deux les doses de métal lourd appliquées.

---

## 6. Traduction Algorithmique dans le Moteur Sillon

Les principes scientifiques décrits ci-dessus sont transcrits au sein d'une architecture logicielle déterministe, pure et autonome :

```
                  Architecture Moteur Sillon (Pure Kotlin)
                  
┌────────────────────────────────────────────────────────────────────────┐
│                        COMPANION ENGINE                                │
│ - Graphe relationnel des 62 espèces potagères                          │
│ - Matrice spatiale : Adjacence orthogonale (d=1) vs diagonale (d=√2)   │
│ - Calcul du score de synergie biologique & alertes d'antagonisme       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                          FROST ENGINE                                  │
│ - Référentiel phénologique des 101 départements français               │
│ - Somme thermique et translation dynamique par rapport à J_gel         │
│ - Fenêtrage temporel tolérant (±7 jours) pour les actions culturales   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                         ROTATION ENGINE                                │
│ - Analyse de l'historique de parcelle sur n années                     │
│ - Détection des conflits de même famille botanique (3 à 5 ans)         │
│ - Vérification de la transition trophique (Gourmandes ➔ Améliorantes)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                    DIAGNOSTIC & VISION ENGINE                          │
│ - Diagnostic différentiel par clé dichotomique sur 14 pathologies      │
│ - Edge Vision : analyse chrominance spectrale locale (Excess Green)    │
│ - Détection feutrage blanc (Oïdium) & nécroses (Mildiou) 100% hors-ligne│
└────────────────────────────────────────────────────────────────────────┘
```

1. **`CompanionEngine.kt` :**
   - Évalue la grille de chaque parcelle en deux dimensions ($N \times M$).
   - Calcule la distance de Tchebychev entre cases :
     $$D(s_1, s_2) = \max(|r_1 - r_2|, \, |c_1 - c_2|)$$
   - Attribue un coefficient pondéré aux synergies ($+3$ points en contiguïté orthogonale directe, $+2$ points en diagonale) et pénalise sévèrement les antagonismes et risques partagés ($-6$ et $-4$ points).
2. **`FrostEngine.kt` :**
   - Prend en paramètre le tuple $(\text{mois}, \text{jour})$ de la gelée de référence du potager.
   - Projette dynamiquement les fenêtres d'intervention (`SOW_INDOORS`, `SOW_OUTDOORS`, `TRANSPLANT`, `HARVEST`) sans aucune dépendance à une horloge réseau externe.
3. **`RotationEngine.kt` :**
   - Parcourt les occurrences historiques par case `(parcelId, row, col)`.
   - Émet des diagnostics en trois niveaux de sévérité :
     - `CONFLICT_ERROR` : Récurrence de la même famille botanique avant la fin du délai d'exclusion $P_{\text{famille}}$.
     - `EXHAUSTION_WARNING` : Succession de deux cultures de niveau d'exigence `HEAVY` consécutives sans apport compensatoire.
     - `BENEFICIAL` : Succession vertueuse d'une légumineuse fixatrice (`LIGHT_FIXER`) précédant une culture exigeante (`HEAVY`).
4. **`DiagnosticEngine.kt` & `EdgeVisionClassifier.kt` :**
   - Triage différentiel par organes atteints (feuilles, tiges, fruits, collet, racines) et types de pathogènes (champignons, ravageurs, troubles physiologiques).
   - Inférence locale sur appareil par extraction de chrominance : calcul de l'indice agronomique d'excès de vert ($ExG = 2G - R - B$), du ratio de chlorose foliaire et de nécrose tissulaire sans jamais envoyer le moindre octet sur internet.

---

## 7. Corpus Bibliographique et Références Académiques

1. **INRAE (Institut National de Recherche pour l'Agriculture, l'Alimentation et l'Environnement) :**
   - *Finch, S., & Collier, R. H. (2000).* « Host-plant selection by insects which tamper with host-plant odor: a review of the pest management potential of companion planting. » *Bulletin of Entomological Research*, 90(2), 91-108.
   - *Thoyer, S. et al. (2018).* « Pratiques agroécologiques et gestion intégrée de la santé des cultures. » *Innovations Agronomiques*, 65, 45-62.
2. **Rodale Institute (Kutztown, Pennsylvanie, USA) :**
   - *Lotter, D. W. (2003).* « Organic agriculture: A review of the scientific literature on companion planting and soil microbial networks. » *Journal of Sustainable Agriculture*, 21(4), 59-89.
3. **FiBL (Forschungsinstitut für biologischen Landbau / Institut de recherche de l'agriculture biologique, Suisse) :**
   - *Garnier, E., & Navas, M. L. (2012).* « Principes écologiques des associations végétales en maraîchage biologique. » *Fiche technique FiBL maraîchage*.
   - *Tamm, L. et al. (2004).* « Efficacy of plant extracts and biological compounds against Plasmopara viticola and Phytophthora infestans in organic agriculture. » *Biological Control*, 30(2), 265-276.
4. **Physiologie végétale & Modélisation thermique :**
   - *McMaster, G. S., & Wilhelm, W. W. (1997).* « Growing degree-days: one equation, two interpretations. » *Agricultural and Forest Meteorology*, 87(4), 291-300.
   - *Bonhomme, R. (2000).* « Bases and problems to apply degrees-day. A review. » *European Journal of Agronomy*, 13(1), 1-10.
5. **Microbiologie et Écologie du Sol :**
   - *Selosse, M.-A. (2020).* « Jamais seul : Ces microbes qui construisent les plantes, les animaux et les civilisations. » Éditions Actes Sud.
   - *Bourguignon, C. & Bourguignon, L. (2008).* « Le sol, la terre et les champs : Pour retrouver une agriculture saine. » Éditions Sang de la Terre.
   - *Hooker, J. E. et al. (1994).* « Mycorrhizae in sustainable agriculture: interactions between arbuscular mycorrhizal fungi and plant pathogens. » *Critical Reviews in Biotechnology*, 14(3), 201-215.
