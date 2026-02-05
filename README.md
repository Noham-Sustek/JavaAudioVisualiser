# Java Audio Visualiser

Un visualiseur audio en temps reel pour fichiers WAV, developpe en JavaFX.

![Java](https://img.shields.io/badge/Java-25-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-25-blue)

## Fonctionnalites

- **Visualisation en temps reel** de la forme d'onde (domaine temporel) et du spectre (domaine frequentiel)
- **6 themes de couleurs** : Bleu sombre, Ocean, Coucher de soleil, Foret, Violet, Monochrome
- **Persistence des parametres** : les options sont sauvegardees et restaurees au redemarrage
- **Historique des morceaux** : acces rapide aux 20 derniers fichiers joues
- **Modes d'affichage** : echelle lineaire/logarithmique, mode miroir

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainWindow (UI)                          │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │ WaveformView │  │ SpectrumView │  │ Controles (boutons,   │ │
│  │ (Canvas)     │  │ (Canvas)     │  │ slider, historique)   │ │
│  └──────────────┘  └──────────────┘  └───────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
         ▲                   ▲
         │    double[] waveform, spectrum
         │                   │
┌─────────────────────────────────────────────────────────────────┐
│                      AudioAnalyzer                              │
│            (Traitement FFT, analyse frequentielle)              │
└─────────────────────────────────────────────────────────────────┘
         ▲
         │    byte[] donnees audio
         │
┌─────────────────────────────────────────────────────────────────┐
│                       AudioPlayer                               │
│        (charge WAV, lecture audio, streaming)                   │
└─────────────────────────────────────────────────────────────────┘
         ▲
         │
    ┌────┴────┐
    │ Fichier │
    │   WAV   │
    └─────────┘
```

## Structure du Projet

```
src/
├── Main.java                    # Point d'entree JavaFX
├── audio/
│   ├── AudioAnalyzer.java       # Analyse FFT et extraction des donnees
│   └── AudioPlayer.java         # Lecture audio et streaming
├── exception/
│   └── AudioFileException.java  # Exception personnalisee
├── fft/
│   └── FFT.java                 # Algorithme Fast Fourier Transform
├── ui/
│   ├── BaseView.java            # Classe abstraite pour les vues
│   ├── ColorTheme.java          # Definition des themes de couleurs
│   ├── MainWindow.java          # Fenetre principale et controles
│   ├── Settings.java            # Gestion de la persistence
│   ├── SpectrumView.java        # Visualisation du spectre
│   └── WaveformView.java        # Visualisation de la forme d'onde
└── util/
    ├── Cache.java               # Classe generique de cache
    ├── Logger.java              # Systeme de logging fichier
    └── ThemeLoader.java         # Chargement des themes par reflexion
```

## Flux de Donnees

### 1. Chargement d'un fichier
```
Utilisateur clique "Ouvrir" → FileChooser → AudioPlayer.loadFile()
                                                   │
                                                   ▼
                                     Validation du fichier
                                                   │
                                                   ▼
                                     Lecture du format audio
                                                   │
                                                   ▼
                                     Ajout a l'historique
```

### 2. Lecture Audio
```
Clic sur Play → AudioPlayer.play()
                      │
                      ▼
           Creation du thread de lecture
                      │
                      ▼
        ┌─────────────────────────────┐
        │     Boucle de lecture       │
        │  - Lire 512 octets          │
        │  - Envoyer aux haut-parleurs│
        │  - Notifier AudioAnalyzer   │
        └─────────────────────────────┘
```

### 3. Analyse Audio (FFT)
```
AudioAnalyzer recoit byte[]
           │
           ▼
   Conversion en samples (double[])
           │
           ▼
   Accumulation dans buffer circulaire (8192 samples)
           │
           ▼
   Application de la FFT
           │
           ├──► Donnees waveform (domaine temporel)
           │
           └──► Donnees spectrum (domaine frequentiel)
           │
           ▼
   Notification de MainWindow
```

### 4. Rendu (60 FPS)
```
AnimationTimer (chaque frame)
           │
           ▼
   WaveformView.draw()
   - Effacer le canvas
   - Dessiner la grille
   - Appliquer le lissage
   - Dessiner barres + ligne colorees
           │
           ▼
   SpectrumView.draw()
   - Effacer le canvas
   - Dessiner la grille
   - Binning frequentiel (256 barres)
   - Echelle lineaire ou log
   - Mode miroir (optionnel)
   - Dessiner les barres colorees
```

## Composants Principaux

### FFT (Fast Fourier Transform)
Transforme les donnees du domaine temporel vers le domaine frequentiel :
- **Entree** : Amplitude au cours du temps (waveform)
- **Sortie** : Amplitude par frequence (spectrum)
- **Algorithme** : Cooley-Tukey iteratif avec fenetre de Hanning

### Themes de Couleurs
Chaque theme definit :
- `backgroundColor` : Fond de la fenetre
- `canvasBackground` : Zone de visualisation
- `gridLineColor` : Lignes de grille
- `textColor` : Labels
- `hueStart`, `hueEnd` : Gradient de couleur selon l'amplitude

La couleur varie selon l'amplitude :
- Amplitude faible → `hueStart` (ex: vert/cyan)
- Amplitude forte → `hueEnd` (ex: rouge/magenta)

### Persistence des Parametres
```
~/.java-audio-visualizer/
├── visualizer_settings.properties  # Theme, echelle, miroir
├── song_history.txt                # Historique des morceaux
└── visualizer.log                  # Fichier de logs
```

## Classes et Responsabilites

| Classe | Responsabilite |
|--------|----------------|
| `Main` | Point d'entree JavaFX |
| `MainWindow` | Interface utilisateur, coordination |
| `AudioPlayer` | Chargement WAV, lecture/pause/stop/seek |
| `AudioAnalyzer` | Conversion audio → waveform + spectrum via FFT |
| `FFT` | Algorithme Cooley-Tukey avec fenetre de Hanning |
| `BaseView` | Classe abstraite pour les vues canvas |
| `WaveformView` | Rendu de la forme d'onde |
| `SpectrumView` | Rendu du spectre frequentiel |
| `ColorTheme` | Definition des couleurs par theme |
| `Settings` | Sauvegarde/chargement des preferences |
| `Logger` | Ecriture des logs avec horodatage |
| `ThemeLoader` | Decouverte des themes par reflexion |
| `Cache<K,V>` | Cache generique avec expiration |
| `AudioFileException` | Exception personnalisee pour erreurs audio |

## Interactions Utilisateur

| Action | Resultat |
|--------|----------|
| **Ouvrir** | Dialogue de selection de fichier WAV |
| **▶ / ⏸** | Lecture / Pause |
| **■** | Arret et reinitialisation |
| **Slider de progression** | Navigation dans le morceau |
| **Historique** | Menu deroulant des morceaux recents |
| **Menu theme** | Changement de theme (persiste) |
| **Echelle lineaire** | Bascule echelle lineaire/logarithmique |
| **Miroir** | Bascule affichage miroir du spectre |

## Elements du Cours Implementes

### Objet General
- **Classes** : 12 classes au total
- **Encapsulation** : Champs prives avec getters/setters
- **Classe abstraite** : `BaseView` avec methodes `draw()`, `reset()`, `drawGrid()`
- **Interfaces** : `AudioDataListener`, `AnalysisListener`

### Generiques
- **Classe generique** : `Cache<K, V>` avec parametres de type

### Exceptions
- **Exception personnalisee** : `AudioFileException` avec types d'erreurs
- **Gestion des erreurs** : Try-catch, try-with-resources, messages utilisateur

### Reflexion
- **Usage** : `ThemeLoader` utilise `Class.forName()`, `getMethod()`, `invoke()`

### Fonctionnel
- **Lambdas** : 10+ expressions lambda pour les listeners et callbacks
- **Reference de methode** : `this::playbackLoop`

### Persistence
- **Fichiers** : Properties pour les parametres, texte pour l'historique

### Logging
- **Fichier de log** : `visualizer.log` avec niveaux DEBUG, INFO, WARN, ERROR

## Compilation et Execution

### Prerequis
- Java 25 (OpenJDK)
- JavaFX SDK 25

### Compilation
```bash
JAVA_HOME="/path/to/jdk-25"
JAVAFX_PATH="/path/to/javafx-sdk-25/lib"

$JAVA_HOME/bin/javac \
    --module-path $JAVAFX_PATH \
    --add-modules javafx.controls,javafx.graphics \
    -d out/production/JavaAudioVisualiser \
    src/**/*.java
```

### Execution
```bash
$JAVA_HOME/bin/java \
    --module-path $JAVAFX_PATH \
    --add-modules javafx.controls,javafx.graphics \
    -cp out/production/JavaAudioVisualiser \
    Main
```

## Auteur

Projet realise dans le cadre du cours "Langage de developpement objet".
