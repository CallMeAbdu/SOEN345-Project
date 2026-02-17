# Firebase Setup (Team)

This project ignores the real Firebase config file:
- `app/google-services.json`

Use this process on each machine:

1. Open Firebase Console for this project.
2. Go to `Project settings -> Your apps -> Android app (com.soen345.project)`.
3. Download `google-services.json`.
4. Place it at:
   - `app/google-services.json`
5. Confirm Firebase Auth Email/Password is enabled:
   - `Authentication -> Sign-in method -> Email/Password`

Reference template:
- `app/google-services.example.json`

If the real file was committed previously, untrack it once:

```bash
git rm --cached app/google-services.json
git commit -m "Stop tracking google-services.json"
```
