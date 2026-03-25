# Maestro End-to-End Testing Guide

This project uses **Maestro** for automated UI and End-to-End (E2E) testing. Maestro allows us to define test flows in simple YAML files that simulate real user interactions.

---

## 1. Installation

### 🪟 Windows
1. Download the latest Maestro binary: [Maestro Releases](https://github.com/mobile-dev-inc/maestro/releases).
2. Extract the zip to a folder (e.g., `C:\Maestro`).

---

### 🍎 macOS

Install using Homebrew-style script:

```bash
curl -fsSL "https://get.maestro.mobile.dev" | bash

~/.maestro/bin
```

## 2. Add Maestro to PATH

Maestro must be available globally from the command line.

### 🪟 Windows

1. Open **Environment Variables**
2. Under **System variables**, select `Path` → **Edit**
3. Add the path to the Maestro `bin` directory:
   C:\tools\maestro\bin
4. Restart your terminal

Verify installation:

```powershell
maestro --version
```

---

### 🍎 macOS

Add Maestro to your shell configuration file:

```bash
export PATH="$HOME/.maestro/bin:$PATH"
```

Reload your shell:

```bash
source ~/.zshrc
```

Verify installation:

```bash
maestro --version
```
Restart your terminal and verify:
   ```powershell
   maestro --version
   ```


---

## 3. Project Structure

Our tests are organized into directories within the `maestro/` folder:

- **`common/`**: Reusable sub-flows (e.g., `login.yml`).
- **`user/`**: Flows for the regular customer role (Browse, Reserve, Cancel).
- **`admin/`**: Flows for the administrator role (Create, Edit, Cancel events).
- **`complete/`**: Full end-to-end integration flows.

---

## 4. Running Tests

### Prerequisites
1. Start an Android Emulator.
2. Ensure the app is installed on the emulator:
   ```bash
   ./gradlew installDebug
   ```

### Run a Single Flow
To run a specific test file:
```bash
maestro test maestro/user/reserve_event.yml
```

### Run All Tests in a Directory
To run all flows within a specific folder:
```bash
maestro test maestro/admin/
```

### Run with a Report
To generate an HTML report after the tests:
```bash
maestro test maestro/ --format junit --output report.xml
```

---

## 4. Useful Maestro Commands

- **`maestro studio`**: Opens a web-based visual editor in your browser. It shows your emulator screen and lets you click elements to automatically generate YAML code.
- **`maestro hierarchy`**: Dumps the current UI tree to the console (useful for finding `resource-ids`).
- **`maestro record <flow>.yaml`**: Records a video of the test execution.

---

## 5. Troubleshooting

- **"Device Offline"**: This usually happens if the emulator crashes or the ADB connection is lost. Try a **Cold Boot** of your emulator from the Device Manager or increase the emulator's ram to 6 GB and core to 8.
- **"Config Section Required"**: Ensure every `.yml` file starts with:
  ```yaml
  appId: com.soen345.project
  ---
  ```
- **Tests failing on Login**: Ensure the credentials used in the `env` block of your `runFlow` (in the test file) match a real user in your Firebase Authentication console.
