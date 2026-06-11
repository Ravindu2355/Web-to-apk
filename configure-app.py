#!/usr/bin/env python3
import json
import os
import re
import shutil

def configure():
    print("Reading configuration from app-config.json...")
    config_path = "app-config.json"
    if not os.path.exists(config_path):
        print(f"Error: {config_path} not found!")
        return

    with open(config_path, "r", encoding="utf-8") as f:
        config = json.load(f)

    app_name = config.get("appName", "WebToApp Sandbox")
    start_url = config.get("startUrl", "https://localhost/")
    app_id = config.get("applicationId", "com.aistudio.webtoapp.vshrt")

    # 1. Update app/src/main/res/values/strings.xml
    strings_path = "app/src/main/res/values/strings.xml"
    if os.path.exists(strings_path):
        print(f"Updating app name in {strings_path} to '{app_name}'...")
        with open(strings_path, "r", encoding="utf-8") as f:
            content = f.read()
        
        # Replace app_name tag contents
        updated_content = re.sub(
            r'<string name="app_name">.*?</string>',
            f'<string name="app_name">{app_name}</string>',
            content
        )
        with open(strings_path, "w", encoding="utf-8") as f:
            f.write(updated_content)
    else:
        print(f"Warning: {strings_path} not found!")

    # 2. Update app/build.gradle.kts
    gradle_path = "app/build.gradle.kts"
    if os.path.exists(gradle_path):
        print(f"Updating applicationId in {gradle_path} to '{app_id}'...")
        with open(gradle_path, "r", encoding="utf-8") as f:
            content = f.read()

        updated_content = re.sub(
            r'applicationId = ".*?"',
            f'applicationId = "{app_id}"',
            content
        )
        with open(gradle_path, "w", encoding="utf-8") as f:
            f.write(updated_content)
    else:
        print(f"Warning: {gradle_path} not found!")

    # 3. Update metadata.json (Platform Sync Rule)
    metadata_path = "metadata.json"
    if os.path.exists(metadata_path):
        print(f"Updating metadata.json naming to '{app_name}'...")
        with open(metadata_path, "r", encoding="utf-8") as f:
            meta = json.load(f)
        meta["name"] = app_name
        with open(metadata_path, "w", encoding="utf-8") as f:
            json.dump(meta, f, indent=2)

    # 4. Copy app-config.json to app/src/main/assets/app-config.json
    assets_dir = "app/src/main/assets"
    os.makedirs(assets_dir, exist_ok=True)
    shutil.copy2(config_path, os.path.join(assets_dir, "app-config.json"))
    print("Copied configuration to app assets.")

    print("\nModification successful!")
    print(f"- App Name: {app_name}")
    print(f"- Package ID: {app_id}")
    print(f"- Start URL: {start_url}")

if __name__ == "__main__":
    configure()
