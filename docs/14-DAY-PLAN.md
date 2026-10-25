# Pack Pack — 14 days of genuine work

The complete starting implementation is supplied now. Commit it as the baseline on day 1. Days 2–14 are actual review, testing, refinement, and documentation work; do not claim the supplied features were developed on later dates. Each teammate should contribute with their own Git identity. One meaningful commit per day is enough; skip a day if there is no completed work.

| Day | Teammate A | Teammate B | Concrete deliverable |
|---|---|---|---|
| 1 | Open and run the baseline | Read the storage and rule classes | Working baseline committed, scope agreed |
| 2 | Interview 2 travellers about packing | Record requirements and exclusions | docs/requirements.md with anonymised findings |
| 3 | Sketch current screens | Map navigation and empty/error states | docs/flows.md and sketches |
| 4 | Test date picker and destination validation | Test 1–12 travellers and quantity boundaries | docs/validation.md and any fixes |
| 5 | Check flight/train/bus/car lists | Add a useful transport rule test | Additional test and rationale |
| 6 | Check hotel picker and saved names | Review amenity and camping behaviour | Accommodation test evidence and fixes |
| 7 | Try image attach/replace/cancel | Check image persists after restart | docs/image-checks.md; use synthetic passes |
| 8 | Test form rotation and interruptions | Verify DB restart persistence | Lifecycle evidence and fixes |
| 9 | Review group/shared quantities | Add missing rule combinations | Expanded meaningful rule tests |
| 10 | Check large font, contrast, touch targets | Test screen reader labels | Small UI/accessibility improvements |
| 11 | Exercise edit/remove/checklist progress | Check missing or inaccessible image handling | Regression checklist with results |
| 12 | Capture clean emulator screenshots | Draw architecture and data-flow diagrams | docs/demo.md plus synthetic screenshots |
| 13 | Explain Java UI and packing rules | Explain SQLite and attachment permissions | Project report and viva preparation |
| 14 | Rehearse demo | Run final tests and build | Release notes, final APK, limitations |

## First GitHub setup
Create an empty GitHub repository called `pack-pack` (avoid adding a remote README). In the Android Studio terminal, from the PackProof project directory:

```sh
git init
git branch -M main
./scripts/daily-commit.sh --dry-run 1 "Add complete Pack Pack baseline" .
./scripts/daily-commit.sh 1 "Add complete Pack Pack baseline" .
git remote add origin YOUR_GITHUB_REPOSITORY_URL
git push -u origin main
```

Replace YOUR_GITHUB_REPOSITORY_URL with your actual URL. Normal GitHub authentication is required. The helper never creates a remote, pushes, schedules commits, backdates activity, or changes author identity.

## Each following day
Complete the milestone, review the actual changes, then pass only the files you want to commit. Example after you have really added requirements:

```sh
./scripts/daily-commit.sh 2 "Document traveller interviews and project requirements" docs/requirements.md
git show --stat
git push
```

The helper runs tests and builds, writes a dated journal entry, stages the journal and the explicit paths, and commits locally. It refuses an already-used day or pre-staged changes. If you pass no paths, it commits only the day's journal: it does not manufacture source changes. Add your findings and individual contributions to journal entries as you work. Review all paths before using `.`.

Never put actual boarding passes, personal hotel details, credentials, keystores, or build outputs in the repository. Use invented data for reports and screenshots. Read .gitignore before the baseline commit.
