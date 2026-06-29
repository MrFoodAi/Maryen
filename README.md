# Maryen — v1 (telefono-only, build via GitHub Actions)

## Cosa è e cosa NON è questa v1

**È:** un'app Android che parla con te in italiano (testo o voce con tap-to-talk),
usa un'API LLM remota per generare le risposte, ricorda la conversazione in locale
(SQLite), ed è pensata per essere compilata interamente dal cloud — niente PC,
niente Android Studio sul tuo dispositivo.

**NON è (ancora):**
- Non ha un modello LLM locale sul telefono (serviva compilazione nativa C++/NDK,
  incompatibile con "build solo da telefono")
- Non ha wake-word always-listening ("Ehi Maryen") — usi un pulsante microfono
- Non genera immagini/video (quelle skill dipendono da un "nodo" PC esterno che
  non esiste ancora; il codice di riferimento è in `reference_v2_node_dependent/`,
  NON fa parte della build)
- Le notizie non sono "live": l'LLM risponde dalla sua conoscenza, con un disclaimer

Questi tagli sono deliberati per avere qualcosa che **compila e funziona oggi**.
Si aggiunge complessità dopo, un pezzo alla volta, sempre testando che ogni
aggiunta non rompa quello che già funziona.

## Setup da telefono (una volta sola)

1. Crea un account su [github.com](https://github.com) se non l'hai già
2. Installa l'app **GitHub** da Play Store (ti serve per creare/gestire repo da telefono)
3. Crea un nuovo repository, nome `maryen` o quello che preferisci, **pubblico**
   (i repo pubblici hanno minuti Actions illimitati gratis; privato ha un tetto mensile)
4. Carica tutti i file di questo progetto nel repo. Il modo più semplice da telefono:
   - usa l'app **Termux** (che probabilmente hai già) con `git`:
     ```
     pkg install git
     cd ~/storage/downloads/maryen   # dove hai scaricato/estratto lo zip
     git init
     git add .
     git commit -m "Maryen v1"
     git branch -M main
     git remote add origin https://github.com/TUO_USERNAME/maryen.git
     git push -u origin main
     ```
   - ti chiederà username e un **Personal Access Token** (non la password):
     crealo su GitHub → Settings → Developer settings → Personal access tokens

## Cosa succede dopo il push

GitHub Actions parte da solo (vedi `.github/workflows/build.yml`):
1. prepara Java 17, Gradle, Android SDK
2. compila il progetto (`assembleDebug`)
3. carica l'APK risultante come "artifact" scaricabile

Per scaricarlo: vai sul repo → tab **Actions** → clicca sull'ultima run verde →
in fondo alla pagina trovi **Artifacts** → `maryen-debug-apk` → scarica.
È uno zip che contiene `app-debug.apk`: lo estrai e lo installi (Android ti
chiederà di abilitare "installa da fonti sconosciute" la prima volta).

**Tempo tipico:** 4-8 minuti per build. Se la build fallisce (icona rossa ❌),
apri la run, clicca sullo step fallito, e mandami l'errore: lo risolviamo.

## Prima apertura dell'app

1. Apri Maryen → vedrai un messaggio che chiede la API key
2. Tocca il menu (in alto a destra) → Impostazioni
3. Inserisci la tua API key Anthropic (la trovi su console.anthropic.com,
   sezione API Keys — è diversa dal tuo accesso a Claude.ai)
4. Salva, torna alla chat, scrivi o premi il microfono

## Struttura del progetto

```
app/src/main/java/com/maryen/app/
  core/llm/LlmEngine.kt          → chiamata API Claude
  core/memory/MemoryStore.kt     → SQLite via Room
  core/orchestrator/             → instrada le richieste (chat vs skill)
  core/security/Vault.kt         → cifra la API key con AndroidKeystore
  core/security/ConsentGate.kt   → blocca skill sensibili finché non approvi
  voice/TtsEngine.kt             → sintesi vocale nativa Android
  voice/SpeechInput.kt           → riconoscimento vocale nativo Android
  skills/news/NewsSkill.kt       → riassunto via LLM (non live)
  skills/recall/RecallSkill.kt   → richiamo memoria recente
  MainActivity.kt / SettingsActivity.kt
reference_v2_node_dependent/     → codice futuro per Image/Video/Film/Site
                                    (richiede un backend PC, NON compilato ora)
```

## Prossimi passi possibili (da decidere insieme, uno alla volta)

- Ricerca semantica reale in memoria (embeddings) invece di ricerca testuale
- Notizie "live" da una fonte RSS reale
- Streaming vero della risposta LLM (token-by-token) invece di blocco unico
- Wake-word always-listening (richiede valutare consumo batteria)
- Nodo PC per Image/Video/Film/Site, se/quando avrai accesso a un PC o server
