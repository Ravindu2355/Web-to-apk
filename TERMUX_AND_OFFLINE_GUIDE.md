# 📱 Complete Guide: Build Android Apps on Termux & Use 100% Offline

This guide contains everything you need to compile this Android project directly on your Android phone using **Termux**, and how to convert any static website into a fully **offline app** packaged inside the APK.

---

## 🛠️ Part 1: How to Setup & Build This Code on Termux (Mobile)

**Termux** is an Android terminal emulator and Linux environment that runs directly on your device without root access. You can compile your Android APKs directly on your phone!

### Step 1: Install & Set Up Termux
> ⚠️ **Important:** Do NOT download Termux from the Google Play Store (it is outdated and abandoned). Always download Termux from **F-Droid** or the official **GitHub Releases**.

1. Download and install **Termux** from [F-Droid](https://f-droid.org/en/packages/com.termux/).
2. Open Termux on your phone and run the basic updates:
   ```bash
   pkg update && pkg upgrade -y
   ```
3. Grant Termux file storage permissions so it can read/write local files:
   ```bash
   termux-setup-storage
   ```

### Step 2: Install Android Build Dependencies
You need Java JDK 17 to compile modern Android apps. Install the required tools in Termux:
```bash
pkg install openjdk-17 git python build-essential -y
```

### Step 3: Clone or Copy Your Project
You can clone your code repository directly into Termux or copy files from your phone's storage.
* To check where your phone storage is mounted in Termux: `~/storage/shared`
* To clone directly from GitHub:
  ```bash
  git clone <YOUR_GITHUB_REPOSITORY_URL>
  cd <REPOSITORY_NAME>
  ```

### Step 4: Compile the APK
1. Give execution permission to the Gradle wrapper inside your project directory:
   ```bash
   chmod +x gradlew
   ```
2. Compile a fresh Debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Once completed, your compiled APK is located at:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
4. Copy the APK to your internal storage to install it:
   ```bash
   cp app/build/outputs/apk/debug/app-debug.apk ~/storage/shared/Download/RvX-SubEditor.apk
   ```
Now go to your phone's file manager, navigate to the **Downloads** folder, and open `RvX-SubEditor.apk` to install it!

---

## 🌐 Part 2: How to Make Your Web Apps 100% Offline

To make your web application run entirely offline without requesting any servers, you can bundle all HTML, CSS, JS, images, and other assets **inside the Android APK itself**.

### Step 1: Create Your Web Directory in Assets
All offline assets must live in the `app/src/main/assets/` directory.

1. Create a folder named `www` inside assets:
   ```text
   app/src/main/assets/www/
   ```
2. Place your web files inside this folder:
   - `app/src/main/assets/www/index.html` (The entry page)
   - `app/src/main/assets/www/style.css`
   - `app/src/main/assets/www/app.js`
   - Any images, fonts, icons, etc.

### Step 2: Point the App to Your Local Assets
Open `/app-config.json` in the root of the project and change your `"startUrl"` to point to the local asset path:

```json
{
  "appName": "RvX SubEditor",
  "startUrl": "file:///android_asset/www/index.html",
  "applicationId": "com.aistudio.webtoapp.vshrt",
  "statusBarColor": "#090B11",
  "navigationBarColor": "#141724",
  "immersiveMode": false,
  "enableSandboxMode": false
}
```

*When your application launches with `"startUrl": "file:///android_asset/www/index.html"`, Android loads the files directly from the APK, requiring **zero internet connection**.*

### Step 3: Ensure Clean Absolute Paths
When writing your HTML/CSS/JavaScript, avoid linking to internet-hosted resources:
* ❌ Avoid: `<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>`
*  **Fixed:** Download the JS file, place it in `app/src/main/assets/www/js/bootstrap.bundle.min.js`, and link it locally: `<script src="js/bootstrap.bundle.min.js"></script>`.

### Step 4: Verify WebView WebView-Supported Asset Flags
Our template's `WebViewComponent` in `MainScreen.kt` is already fully optimized with:
* `settings.allowFileAccess = true`
* `settings.allowContentAccess = true`
* `settings.domStorageEnabled = true` — allows your offline app to use `localStorage` and IndexedDB safely on-device!

### Step 5: Compile Your Offline App
Now build the app again using:
```bash
./gradlew assembleDebug
```
And download/install the updated APK. You now have a high-performance, native-wrapped, 100% offline-compatible Android app!
