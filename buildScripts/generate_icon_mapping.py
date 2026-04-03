import shutil
import subprocess
import os
from pathlib import Path

# Pfade relativ zum Skript-Speicherort definieren
GIT_REPO_URL = "https://github.com/PapirusDevelopmentTeam/papirus-icon-theme"
BASE_DIR = Path(__file__).parent.parent
ICON_SET_DIR = "papirus-icon-theme"
RAW_ASSETS_DIR = BASE_DIR / "raw_assets/icons" / ICON_SET_DIR
ICON_SOURCE_DIR = RAW_ASSETS_DIR / "Papirus/64x64/mimetypes"
ASSETS_DEST_DIR = BASE_DIR / "app/src/main/assets/fileicons"
KOTLIN_OUTPUT_FILE = BASE_DIR / "app/src/main/java/com/defname/sendfile/IconMap.kt"
ZIP_TEMP_FILE = BASE_DIR / "icons_temp.zip"

icons_copied = 0

def download_icons():
    if (RAW_ASSETS_DIR / ICON_SET_DIR).exists():
        return

    print(f"Icon Set not found. Downloading...")

    try:
        subprocess.run([
            "git", "clone", "--depth", "1",
            GIT_REPO_URL,
            str(RAW_ASSETS_DIR)
        ], check=True)
        print("Download successful")
    except Exception as e:
        print(f"Error during git clone: {e}")

def copy_icon_file(file):
    dst = ASSETS_DEST_DIR / file.name
    if (dst.is_file()):
        return
    if not file.is_file():
        print(f"{file} is not a file")
        return
    shutil.copy2(file, ASSETS_DEST_DIR / file.name)
    global icons_copied
    icons_copied += 1

def main():
    if not ICON_SOURCE_DIR.exists():
        print(f"❌ Fehler: Quellverzeichnis {ICON_SOURCE_DIR} nicht gefunden!")
        return

    # Zielordner in Assets vorbereiten (leeren/erstellen)
    if ASSETS_DEST_DIR.exists():
        shutil.rmtree(ASSETS_DEST_DIR)
    ASSETS_DEST_DIR.mkdir(parents=True)

    icon_map = {}

    print(f">> Scanne Icons in {ICON_SOURCE_DIR}...")
    for file in ICON_SOURCE_DIR.iterdir():
        if not (file.is_file() or file.is_symlink) or not file.suffix == ".svg":
            continue

        print(f"   - {file}")

        mime = file.stem  # z.B. "application-pdf"

        if file.is_symlink():
            linked_file = file.resolve()
            print("         symlink -> " + str(linked_file))
            if not linked_file.is_file():
                print(f"Linked file {linked_file} does not exist. Skipping")
                continue
            filename = linked_file.name
            copy_icon_file(linked_file)
        else:
            filename = file.name
            copy_icon_file(file)

        icon_map[mime] = filename

    # ✨ Kotlin Datei generieren
    with open(KOTLIN_OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("package com.defname.sendfile\n\n")
        f.write("/**\n * generated automatically by generate_icon_mapping.py\n */\n")
        f.write("object IconMap {\n")
        f.write("    val MAP = mapOf(\n")

        for key in sorted(icon_map.keys()):
            value = icon_map[key]
            f.write(f'        "{key}" to "{value}",\n')

        f.write("    )\n")
        function_string = """
    fun getIcon(mimeType: String): String {
        val parts = mimeType.split('/')
    
        if (parts.size == 1) {
            return MAP[parts[0]]
                ?: (MAP[parts[0] + "-x-generic"])
                ?: "application-x-generic.svg"
    
        }
        
        return MAP[parts[0] + "-" + parts[1]]
            ?: MAP[parts[0] + "-x-generic"]
            ?: "application-x-generic.svg"
    }"""
        f.write(function_string + "\n")
        f.write("}\n")

    global icons_copied
    print(f">> Erledigt!")
    print(f"   - {icons_copied} Icons nach {ASSETS_DEST_DIR} kopiert.")
    print(f"   - Mapping in {KOTLIN_OUTPUT_FILE} mit ({len(icon_map)})generiert.")

if __name__ == "__main__":
    download_icons()
    main()
