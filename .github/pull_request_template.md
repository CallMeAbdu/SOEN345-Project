## Summary
<!-- Briefly describe what this PR does and why. Reference the branch name and its purpose. -->

## Related Issues
<!-- Link every issue this PR resolves. Use "Closes #XX" so GitHub auto-closes them on merge. -->
- Closes #
- Closes #

## What was implemented / changed
<!-- List the features, fixes, or refactors included. Be specific enough that a reviewer knows what to look at. -->
-
-

## Testing
<!-- Describe how this was tested. Check all that apply and add notes where relevant. -->

**Test types covered:**
- [ ] Unit tests (JUnit)
- [ ] Robolectric UI tests
- [ ] Instrumented / Espresso tests
- [ ] Maestro E2E tests
- [ ] Manual testing on emulator or device

**To run the tests locally:**
```bash
# Unit + Robolectric
./gradlew test

# Instrumented (requires emulator)
./gradlew connectedAndroidTest

# Maestro E2E (requires emulator + Maestro CLI installed)
maestro test .maestro/
```

**If any test type is not applicable, explain why:**

## Checklist
- [ ] This PR is focused on one feature/fix/refactor
- [ ] All new code has corresponding tests, or tests are not required (explain why)
- [ ] CI is green (all GitHub Actions checks pass)
- [ ] Codecov patch coverage meets the 80% threshold
- [ ] I have linked this PR to its related issues and user stories
- [ ] I have requested review from at least one teammate
- [ ] No dead code, debug logs, or commented-out blocks left in

## Notes for reviewers
<!-- Anything tricky, out of scope, or worth calling out specifically. -->
