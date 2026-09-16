# Reusable locations and accident search

The application now has four tabs: Accident Scene Editor, Saved Accident Scenes,
Saved Locations, and Add Location.

## Accident file names and editing modes

The editor's Accident Scene section contains an Accident file name field. It belongs
to the accident, not its location. Two accidents may share a location and have different
file names. Selecting or saving a location does not change the accident file name.
Saved Accident Scenes uses this field for card titles, detail titles, and the text
search. Location names remain separate and can be filtered with the location selector.
Blank accident file names are allowed and display as "Unnamed accident scene".

JSON export places `fileName` at the root alongside `location`, `cars`, and
`measurements`. JSON and PNG downloads use the accident file name. Import still
accepts older JSON with `location.file_name` and moves that value to the accident.
Reset Scene saves the previous accident and clears the name for the new empty scene.

After successful analysis, the status reads "Current mode: AI" and the button reads
"Switch to Edit Mode". Clicking it changes the status to "Current mode: Edit" and
the button to "Switch to AI Mode". Reopening/importing a scene starts in Edit mode;
failed analysis leaves the previous mode intact. The mode switch is disabled while
analysis is running.

## Using locations

1. In Add Location, choose a starting template or fill in the form. Example template
   dimensions must be replaced with the real location measurements. A PNG or JPEG
   picture is optional (maximum 10 MB and 25 megapixels).
2. Save the location. It appears in Saved Locations and in the editor's searchable
   location selector. Select Use in Editor to load the road picture and all fields.
3. Add the accident's vehicles and measurements and save the accident as usual.
4. Alternatively, enter location details in the editor. Give the location a name;
   Save to Database saves/reuses the location as part of saving the accident.
   Save location template saves it immediately without creating an accident.

Locations are reusable, immutable templates. Editing their values for one accident
creates or reuses a matching location when saved; it does not update other accidents.
Matching compares all normalized form values and photo contents. Text is trimmed;
empty optional text becomes null. Case differences in location text remain significant.
A database uniqueness constraint also prevents duplicates from concurrent submissions.

Choosing a location preserves current vehicles and measurements. A blank draft can
have no location. Selecting a built-in road background replaces the chosen location
photo; the edited details will be resolved again on the next save.

Add Location offers all eight editor backgrounds as starting templates: Four-way
Intersection, the four roundabout variants (3 or 4 exits, 1 or 2 lanes), T-Junction,
Boulevard — Solid Median, and Boulevard — Tree-Lined Median. Selecting a preset
fills example dimensions and shows its road image; missing road arms stay blank.
The T-junction image has a bottom arm and no top arm. Switching presets clears
irrelevant values, including the roundabout diameter and T-junction flag.

Each Saved Locations card has a Delete Location button with confirmation. Only
locations unused by saved accidents can be deleted. Draft, finalized, and archived
accidents all protect their location. After deletion, the location is removed from
the library and selectors. If selected in an unsaved editor change, its input values,
vehicles, and measurements remain; the deleted reference and uploaded source photo
are cleared and the built-in road background is used. Saving those details again
creates/reuses a location. Deleting the final card on a page returns to the previous page.

Pictures use the editor's existing pixel-to-meter scale. Entering road widths does not
automatically calibrate an arbitrary photograph, and uploading a template does not run AI.

## Searching accidents

Saved Accident Scenes has separate accident-file-name/ID, plate, location, and status controls.
The text search performs case-insensitive partial matching on the accident file name
or scene ID. Location names/descriptions are searched through the location selector.
The plate search accepts partial matches and ignores case, spaces, and hyphens.
All supplied filters are combined in the backend before pagination. Two vehicles
matching the same plate search do not cause duplicate accident cards.

## Persistence and API

- `Location` is a JPA entity in `location`.
- `AccidentScene.fileName` writes `accident_scene.file_name`; current Location
  entities and location request/response models contain no accident file name.
- `AccidentScene.locationId` writes the `location_id` foreign key. Its read-only
  `@ManyToOne` association loads the linked Location; no delete cascade is configured.
- Photo bytes are stored in `location_photo`, separate from location listings.
  The existing PostgreSQL Docker volume therefore persists uploaded images too.
- `GET /api/locations?query=...&page=0&size=20` lists/searches templates.
- `POST /api/locations` accepts a LocationInput JSON body, or multipart parts named
  `location` (application/json) and optional `photo` (PNG/JPEG).
- `GET /api/locations/{id}` returns the form fields and photo URL.
- `DELETE /api/locations/{id}` returns 204 when deleted, 404 when missing, or 409
  when a saved scene uses the location. The database foreign key also guards against
  concurrent scene creation. Shared photo blobs are retained in `location_photo`,
  so deleting a source location cannot remove another template's picture.
- `GET /api/locations/{id}/photo` serves the validated image.
- Scene create/full-save requests accept either `locationId` or `locationInfo`
  containing LocationInput. A request with both is rejected.
- Scene create/full-save requests accept top-level `fileName` (trimmed, maximum
  255 characters). On full save, omitted/null preserves the existing name and an
  empty string clears it. Create/full-save events carry this field too; older event
  payloads remain compatible.
- Scene responses include `locationId`, `location`, and a compatibility
  `locationInfo` representation used by the existing details screen.
- Scene details and summaries include `fileName`. Summary `name` is an alias for
  `fileName`, while `locationName` contains the independently named location.
- `GET /api/accident-scenes?plate=SK1234&locationId=...&status=DRAFT` combines filters.

LegacyLocationMigration backfills formerly embedded location fields on startup.
It leaves original columns and historical event rows intact and skips scenes already
linked to a location. Blank legacy names receive an imported-location name; invalid
non-positive legacy dimensions remain in the original columns but become unspecified
in the new template. No database reset is needed. This is a migration of the current
JPA state, not a redesign of historical Axon event replay.

SceneFileNameMigration runs first on startup. Once per database, it fills blank scene
file names from the former `location.file_name` column, preserving any existing
`accident_scene.file_name`. Its completion is recorded in
`roadwatch_schema_migration`, so clearing a name later does not restore it on restart.
The retired location column and legacy LocationInfo event payload class remain for
historical data compatibility; the active application does not store file names there.
No database reset or destructive column drop is required.

Location matching excludes accident file names. Locations with old fingerprints are
matched using their remaining fields, without deleting/rekeying existing templates or
changing historical references. Existing duplicates are retained; new saves reuse a
matching template.

## Main implementation files

Backend package: `src/main/kotlin/com/example/accidentscatchmanagement`.

- `domain/Location.kt`, `domain/LocationPhoto.kt`, `domain/Accident_Scene.kt`:
  entities, relationship, and scene command/event state changes.
- `service/impl/LocationServiceImpl.kt`, `service/LocationFingerprintService.kt`:
  validation, image handling, normalization, and duplicate resolution.
- `web/LocationController.kt`, `web/AccidentSceneCommandController.kt`, and
  `web/dto/`: API and request/response mapping.
- `repository/AccidentSceneJpaRepository.kt` and the query service/controller:
  plate/location filtering and left joins for scenes without a location.
- `config/LegacyLocationMigration.kt`: existing scene backfill.
- `config/SceneFileNameMigration.kt`: one-time file-name backfill.

Frontend directory: `AccidentProject/AccidentProject/RoadWatch`.

- `locations.js`, `location-presets.js`, `navigation.js`, `index.html`, `style.css`: location screens,
  searchable selectors, templates, forms, and navigation.
- `backend-api.js`: save/load mappings, location API, and filter parameters.
- `canvas.js`: uploaded backgrounds, CORS-safe image export, and resize handling.
- `ui.js`, `app.js`, `saved-scenes.js`: editor integration and saved-scene filters.
- `ai.js`, `ui.js`: synchronized AI/Edit mode state and labels.
- `storage.js`: accident-file-name JSON import/export and downloads.

## Running and testing

From the repository root, rebuild/restart the services with:

```powershell
docker compose up -d --build
```

Open `http://localhost:5000` and refresh the browser to load the new scripts.
Compose now builds the AI service from a context containing both accident-ai and
RoadWatch so Flask can serve the frontend inside Docker.

Backend tests use an isolated in-memory H2 database and never connect to the
configured PostgreSQL database:

```powershell
.\mvnw.cmd clean test
```

`LocationWorkflowTests.kt` covers shared locations, manual resolution, concurrent
deduplication, image validation/storage, plate/location queries, legacy migration,
location switching, AI measurement/event persistence, deletion, shared-photo
preservation, and protection of locations used by draft/archived accidents.
It also checks scene file names, search/projection/event persistence, migration
idempotency, AI name preservation, and reuse of legacy location fingerprints.

`tests/location-ui.cjs` is an isolated Playwright browser test using fixture API
responses. With Playwright available to Node, run:

```powershell
$env:PLAYWRIGHT_CHANNEL = "msedge"
node tests/location-ui.cjs
```

It checks all eight templates and previews, autofill, uploads, preservation of vehicles,
filter requests, navigation, access to the T-junction checkbox, deletion/cancellation,
in-use errors, stale selection cleanup, and deletion of the last card on a page.
It additionally exercises AI mode success/failure/toggling and scene name
save/load/search/export/import without changing a location.
Screenshots are written
under ignored `target/ui`. Actual PostgreSQL deployment and image builds still need
to be checked in the local Docker environment after rebuilding.
