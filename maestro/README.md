# Installing & Using Maestro (E2E Mobile Testing)

This guide explains how to install **Maestro**, set it up on your system, understand its configuration files, and run end-to-end (E2E) tests.

---

## 1. Install Maestro

### 🪟 Windows

1. Download Maestro:
   https://github.com/mobile-dev-inc/maestro/releases/latest/download/maestro.zip

2. Unzip it to an **arbitrary location outside your project folder**, for example:
   C:\tools\maestro

You should end up with:
C:\tools\maestro\bin\maestro.exe

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

## 3. Maestro File Structure

A typical project structure for Maestro E2E tests:

```css
project-root\/
├── maestro\/
│   ├── config.yml
│   ├── flow.yml
│   └── login.yml
└── src\/
```

- maestro/: Contains Maestro test flows

- config.yml: Optional global configuration

- src/: Application source code

## 4. Test File

A test would be like the flow.yml file.

```yaml
appId: com.example.app
---
- launchApp
- assertVisible: "Login"
- tapOn: "Login"
- assertVisible: "Home"
```

- appId is the app package name (Android) or bundle ID (iOS)

- --- separates configuration from test steps

- Steps execute from top to bottom

- Assertions fail the test immediately if unmet

## 5. Running Test
```

### Have your Emulator running

To run all flows inside the e2e folder from the /mobile directory:

```bash
maestro test maestro/
```

Example output:

```bash
Waiting for flows to complete...
[Passed] flow (9s)

1/1 Flow Passed in 9s
```

To run a single test from the /mobile directory:

```bash
maestro test maestro/flow.yml
```

Example output:

```bash
Running on Medium_Phone_API_36.1

 ║
 ║  > Flow: flow

Running on Medium_Phone_API_36.1

 ║
 ║  > Flow: flow
 ║
 ║    +   Launch app "com.anonymous.mobile"
 ║    +   Tap on "Call /API/HEALTH"
 ║    +   Assert that "Result: OK" is visible
 ║
```

If the example flow.yml did not complete succesfully it might because on the first time the app opens, there are some developper pop-up that the flow script is not designed to handle.
Just click continue on the app and rerun the flow.