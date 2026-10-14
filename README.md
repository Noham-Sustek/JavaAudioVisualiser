# C Audio Visualiser

Un visualiseur audio en temps reel pour fichiers WAV et microphone, developpe en C.
Portage de la version Java/JavaFX (branche `master`).

![C](https://img.shields.io/badge/C-11-blue)
![raylib](https://img.shields.io/badge/raylib-5.5-black)
![miniaudio](https://img.shields.io/badge/miniaudio-0.11.21-orange)

## Fonctionnalites

- **Visualisation en temps reel** de la forme d'onde (domaine temporel) et du spectre (domaine frequentiel)
- **Entree microphone** : visualisation du son capte en direct
- **9 themes de couleurs** : Bleu sombre, Ocean, Coucher de soleil, Foret, Violet, Monochrome, Synthwave, EVA-01, Feu & Glace
- **Persistence des parametres** : les options sont sauvegardees et restaurees au redemarrage
- **Historique des morceaux** : acces rapide aux 20 derniers fichiers joues
- **Modes d'affichage** : echelle lineaire/logarithmique, mode miroir
- **Glisser-deposer** d'un fichier WAV sur la fenetre, ou passage en argument de ligne de commande

## Compilation

Prerequis : CMake >= 3.20 et un compilateur C (MSVC, GCC ou Clang).
Les dependances (raylib, raygui, miniaudio) sont telechargees automatiquement par CMake.

```sh
cmake -S . -B build
cmake --build build --config Release
```

Lancement :

```sh
# Windows
build\Release\CAudioVisualiser.exe ["assets\Devil May Cry 3 - Devils Never Cry.wav"]

# Linux / macOS
./build/CAudioVisualiser [fichier.wav]
```

Sous Linux, la boite de dialogue d'ouverture utilise `zenity` et raylib necessite les
paquets de developpement X11/OpenGL (ex. `libx11-dev libxrandr-dev libxinerama-dev
libxcursor-dev libxi-dev libgl1-mesa-dev`).

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    main_window (UI, raylib + raygui)            │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │ WaveformView │  │ SpectrumView │  │ Controles (boutons,   │ │
│  │  (BaseView)  │  │  (BaseView)  │  │ slider, historique)   │ │
│  └──────────────┘  └──────────────┘  └───────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
         ▲                   ▲
         │    float waveform[], spectrum[]   (thread UI, 60 FPS)
         │                   │
┌─────────────────────────────────────────────────────────────────┐
│                      AudioAnalyzer                              │
│       (buffer circulaire protege par mutex + FFT a la demande)  │
└─────────────────────────────────────────────────────────────────┘
         ▲                                     ▲
         │  AudioDataListener (thread audio)   │
┌──────────────────────────┐     ┌──────────────────────────┐
│       AudioPlayer        │     │    MicrophoneCapture     │
│ (decodage WAV, lecture,  │     │  (capture 44100 Hz,      │
│  pause, stop, seek)      │     │   stereo, float)         │
└──────────────────────────┘     └──────────────────────────┘
         ▲                                     ▲
    ┌────┴────┐                          ┌─────┴─────┐
    │ Fichier │                          │ Microphone│
    │   WAV   │                          └───────────┘
    └─────────┘
```

## Structure du Projet

```
CMakeLists.txt                  # Build + recuperation des dependances
assets/                         # Fichier WAV d'exemple
src/
├── main.c                      # Point d'entree
├── audio/
│   ├── audio_listener.h        # Callback AudioDataListener
│   ├── audio_analyzer.c/h      # Buffer circulaire et analyse FFT
│   ├── audio_player.c/h        # Lecture audio (miniaudio)
│   ├── microphone_capture.c/h  # Capture microphone (miniaudio)
│   └── miniaudio_impl.c        # Implementation de miniaudio
├── error/
│   └── audio_errors.c/h        # Types d'erreur fichier / microphone
├── fft/
│   └── fft.c/h                 # Algorithme Fast Fourier Transform
├── ui/
│   ├── base_view.h             # "Classe abstraite" des vues (vtable)
│   ├── color_theme.c/h         # Definition des themes de couleurs
│   ├── main_window.c/h         # Fenetre principale et controles
│   ├── settings.c/h            # Gestion de la persistence
│   ├── spectrum_view.c/h       # Visualisation du spectre
│   └── waveform_view.c/h       # Visualisation de la forme d'onde
└── util/
    ├── config.c/h              # Dossier de configuration utilisateur
    ├── file_dialog.c/h         # Boite de dialogue native d'ouverture
    └── logger.c/h              # Systeme de logging fichier
```

## Flux de Donnees

### 1. Chargement d'un fichier
```
Clic "Ouvrir" → boite de dialogue native → audio_player_load_file()
                                                   │
                                                   ▼
                                     Validation du fichier
                                                   │
                                                   ▼
                                     Initialisation decodeur + sortie audio
                                                   │
                                                   ▼
                                     Ajout a l'historique
```

### 2. Lecture Audio
```
Clic sur Play → audio_player_play() → demarrage du peripherique miniaudio
                                              │
                                              ▼
                              ┌───────────────────────────────┐
                              │  Callback audio (thread audio) │
                              │  - Lire 512 frames du decodeur │
                              │  - Envoyer aux haut-parleurs   │
                              │  - Notifier AudioAnalyzer      │
                              └───────────────────────────────┘
```

### 3. Analyse Audio (FFT)
```
AudioAnalyzer recoit float[] (thread audio)
           │
           ▼
   Moyenne des canaux → buffer circulaire (8192 samples)
           │
           ▼   (thread UI, chaque frame)
   Copie ordonnee du buffer + FFT
           │
           ├──► Donnees waveform (domaine temporel)
           │
           └──► Donnees spectrum (domaine frequentiel)
```

La FFT est calculee sur le thread UI plutot que dans le callback audio : le thread
audio ne fait que copier des echantillons, ce qui evite tout risque de craquement.

### 4. Rendu (60 FPS)
```
Boucle principale raylib (chaque frame)
           │
           ▼
   base_view_draw(waveform)
   - Effacer la zone
   - Dessiner la grille       (vtable->draw_grid)
   - Lissage + barres + ligne (vtable->draw_content)
           │
           ▼
   base_view_draw(spectrum)
   - Binning frequentiel (256 barres)
   - Echelle lineaire ou log
   - Mode miroir (optionnel)
```

## Correspondance Java → C

| Java | C |
|------|---|
| Classe abstraite `BaseView` | `struct BaseView` + table de fonctions virtuelles (`BaseViewVTable`) |
| Interface `AudioDataListener` | Pointeur de fonction + `user_data` |
| `AudioFileException`, `MicrophoneException` | Structs d'erreur remplies par les fonctions, qui retournent `false` |
| Enum `ThemeType` + reflexion (`ThemeLoader`) | Enum + table statique de themes, recherche par nom |
| Methode generique `parseEnum` | `color_theme_parse()` |
| `synchronized` | `ma_mutex` (miniaudio) |
| JavaFX `Canvas` / `AnimationTimer` | raylib (`BeginDrawing` / `EndDrawing` a 60 FPS) |
| Controles JavaFX | raygui (interface en mode immediat) |
| `javax.sound.sampled` | miniaudio |
| `Properties` | Fichier `cle=valeur` lu/ecrit a la main |

## Themes de Couleurs
Chaque theme definit :
- `background_color` : Fond de la fenetre
- `canvas_background` : Zone de visualisation
- `grid_line_color` : Lignes de grille
- `text_color` : Labels
- `hue_start`, `hue_end` : Gradient de couleur selon l'amplitude

La couleur varie selon l'amplitude :
- Amplitude faible → `hue_start` (ex: vert/cyan)
- Amplitude forte → `hue_end` (ex: rouge/magenta)

## Persistence des Parametres
```
~/.c-audio-visualizer/              (%USERPROFILE% sous Windows)
├── visualizer_settings.properties  # Theme, echelle, miroir
├── song_history.txt                # Historique des morceaux
└── visualizer.log                  # Fichier de logs
```

## Interactions Utilisateur

| Action | Resultat |
|--------|----------|
| **Ouvrir** | Dialogue de selection de fichier WAV |
| **▶ / ⏸** | Lecture / Pause |
| **■** | Arret et reinitialisation |
| **Micro** | Active / desactive la visualisation du microphone |
| **Slider** | Deplacement dans le morceau |
| **Historique** | Liste des 20 derniers fichiers |
| **Glisser-deposer** | Charge le fichier depose sur la fenetre |
