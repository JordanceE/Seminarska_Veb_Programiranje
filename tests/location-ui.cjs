// Run with Node and Playwright installed. All API calls use in-memory fixtures.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const http = require('node:http');
const fs = require('node:fs/promises');
const path = require('node:path');
const root = path.resolve(__dirname, '../AccidentProject/AccidentProject/RoadWatch');
const locations = [{ id: 'Location:ui-1', name: 'Test T-junction', roadLayoutType: 'T_JUNCTION',
    backgroundFileName: 't-junction.png', topRoadWidth: 9, topRoadLanes: 2, tJunction: true,
    photoUrl: '/api/locations/Location:ui-1/photo' }];
const scenes = new Map();
const requests = [];
let nextLocationId = 2;
const fixturePhoto = path.join(root, 'assets/glavnaulica.png');

(async () => {
    const server = http.createServer(async (req, res) => {
        try {
            const relative = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
            const file = path.resolve(root, relative === '/' ? 'index.html' : `.${relative}`);
            if (!file.startsWith(root + path.sep) && file !== path.join(root, 'index.html')) { res.writeHead(403).end(); return; }
            res.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : file.endsWith('.css') ? 'text/css' : file.endsWith('.png') ? 'image/png' : 'text/html');
            res.end(await fs.readFile(file));
        } catch { res.writeHead(404).end(); }
    });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    let browser;
    try {
        browser = await chromium.launch({ headless: true,
            ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) });
        const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        let acceptDialogs = true;
        let failAnalysis = false;
        page.on('dialog', dialog => acceptDialogs ? dialog.accept() : dialog.dismiss());
        await page.route('http://127.0.0.1:8080/api/**', async route => {
            const request = route.request(), url = new URL(request.url()), method = request.method();
            const pathname = decodeURIComponent(url.pathname);
            const headers = { 'access-control-allow-origin': '*', 'access-control-allow-methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS', 'access-control-allow-headers': '*' };
            const json = body => route.fulfill({ json: body, headers });
            if (method === 'OPTIONS') return route.fulfill({ status: 204, headers });
            if (pathname.endsWith('/photo')) return route.fulfill({ body: await fs.readFile(fixturePhoto), contentType: 'image/png', headers });
            let body;
            if (request.postData() && request.headers()['content-type']?.includes('application/json')) body = request.postDataJSON();
            requests.push({ pathname, method, body, url });
            if (pathname === '/api/locations') {
                if (method === 'POST') {
                    if (!body) {
                        const data = await new Response(request.postDataBuffer(), { headers: { 'content-type': request.headers()['content-type'] } }).formData();
                        body = JSON.parse(await data.get('location').text());
                        assert.ok(data.get('photo'));
                    }
                    const id = `Location:ui-${nextLocationId++}`;
                    const location = { ...body, id, photoUrl: `/api/locations/${id}/photo` };
                    locations.push(location);
                    return json(location);
                }
                const content = locations.filter(l => l.name.toLowerCase().includes((url.searchParams.get('query') || '').toLowerCase()));
                const number = Number(url.searchParams.get('page') || 0), size = Number(url.searchParams.get('size') || 20);
                return json({ content: content.slice(number * size, (number + 1) * size),
                    number, totalPages: Math.ceil(content.length / size), totalElements: content.length });
            }
            if (pathname.startsWith('/api/locations/')) {
                const index = locations.findIndex(l => pathname.endsWith(l.id)), location = locations[index];
                if (!location) return route.fulfill({ status: 404, json: { message: 'Location not found' }, headers });
                if (method === 'DELETE') {
                    if ([...scenes.values()].some(s => s.locationId === location.id)) {
                        return route.fulfill({ status: 409, json: { message: 'Cannot delete this location: it is used by a saved accident scene.' }, headers });
                    }
                    locations.splice(index, 1);
                    return route.fulfill({ status: 204, headers });
                }
                return json(location);
            }
            if (pathname === '/api/accident-scenes') {
                if (method === 'POST') {
                    const id = `AccidentScene:ui-${scenes.size + 1}`;
                    const location = locations.find(l => l.id === body.locationId);
                    scenes.set(id, { id, ...body, location, locationInfo: location || {}, vehicles: [], measurements: [], status: 'DRAFT' });
                    return json({ id });
                }
                const query = (url.searchParams.get('query') || '').toLowerCase();
                const content = [...scenes.values()].filter(s => !query || s.fileName?.toLowerCase().includes(query) || s.id.toLowerCase().includes(query))
                    .map(s => ({ ...s, name: s.fileName, locationName: s.location?.name }));
                return json({ content, number: 0, totalPages: content.length ? 1 : 0, totalElements: content.length });
            }
            const id = pathname.split('/')[3], scene = scenes.get(id);
            if (!scene) return route.fulfill({ status: 404, json: { message: 'Unknown fixture scene' }, headers });
            if (pathname.endsWith('/analyze')) {
                if (failAnalysis) return route.fulfill({ status: 500, json: { message: 'Fixture analysis failed' }, headers });
                scene.status = 'AI_ANALYZED'; scene.aiConfidence = 0.9;
                return json({ cars: scene.vehicles, confidence: 0.9, measurements: [] });
            }
            if (pathname.endsWith('/vehicles')) { scene.vehicles.push(body.vehicle); return route.fulfill({ status: 200, body: '', headers }); }
            if (pathname.endsWith('/full-scene')) {
                Object.assign(scene, body);
                if (body.locationInfo) {
                    scene.location = { ...body.locationInfo, id: `Location:ui-${nextLocationId++}` };
                    locations.push(scene.location); scene.locationId = scene.location.id;
                } else scene.location = locations.find(l => l.id === body.locationId);
                scene.locationInfo = scene.location || {};
                return route.fulfill({ status: 200, body: '', headers });
            }
            return json(scene);
        });
        await page.goto(`http://127.0.0.1:${server.address().port}`);
        assert.equal(await page.textContent('#editModeBtn'), 'Switch to AI Mode');
        assert.equal(await page.textContent('#sceneModeStatus'), 'Current mode: Edit');
        await page.fill('#sceneFileName', 'First accident report');
        await page.locator('#editorLocationSelect option[value="Location:ui-1"]').waitFor({ state: 'attached' });
        await page.selectOption('#editorLocationSelect', 'Location:ui-1');
        await page.waitForFunction(() => document.querySelector('#locName').value === 'Test T-junction');
        assert.equal(await page.inputValue('#topWidth'), '9');
        assert.equal(await page.isChecked('#tjunctionCheck'), true);
        assert.equal(await page.inputValue('#sceneFileName'), 'First accident report');
        await page.locator('.vehicleBtn[data-type="sedan"]').click();
        await page.waitForFunction(() => document.querySelector('#vehicleList').textContent.includes('V1'));
        await page.fill('#topWidth', '11');
        await page.click('#saveDatabaseBtn');
        await page.waitForFunction(() => !document.querySelector('#saveDatabaseBtn').disabled);
        assert.ok(requests.some(r => r.pathname.endsWith('/full-scene') && r.body.locationInfo?.topRoadWidth === 11));
        assert.equal(locations[0].topRoadWidth, 9);
        assert.equal([...scenes.values()][0].fileName, 'First accident report');
        assert.ok(requests.some(r => r.pathname === '/api/accident-scenes' && r.body?.fileName === 'First accident report'));
        assert.ok(requests.filter(r => r.body?.locationInfo).every(r => !('fileName' in r.body.locationInfo)));
        const locationCount = locations.length;
        await page.fill('#sceneFileName', 'Renamed accident report');
        await page.click('#saveDatabaseBtn');
        await page.waitForFunction(() => !document.querySelector('#saveDatabaseBtn').disabled);
        assert.equal(locations.length, locationCount);

        // Successful AI analysis and manual toggles must always agree with the label.
        await page.setInputFiles('#imageUpload', fixturePhoto);
        await page.click('#analyzeBtn');
        await page.waitForFunction(() => !document.querySelector('#analyzeBtn').disabled);
        assert.equal(await page.textContent('#editModeBtn'), 'Switch to Edit Mode');
        assert.equal(await page.textContent('#sceneModeStatus'), 'Current mode: AI');
        await page.locator('.popupClose').click();
        const additions = requests.filter(r => r.pathname.endsWith('/vehicles')).length;
        await page.locator('.vehicleBtn[data-type="sedan"]').click();
        assert.equal(requests.filter(r => r.pathname.endsWith('/vehicles')).length, additions);
        await page.click('#editModeBtn');
        assert.equal(await page.textContent('#editModeBtn'), 'Switch to AI Mode');
        assert.equal(await page.textContent('#sceneModeStatus'), 'Current mode: Edit');
        await page.locator('.popupClose').click();
        await page.click('#editModeBtn');
        assert.equal(await page.textContent('#editModeBtn'), 'Switch to Edit Mode');
        await page.locator('.popupClose').click();
        await page.click('#editModeBtn');
        await page.locator('.popupClose').click();
        failAnalysis = true;
        await page.click('#analyzeBtn');
        await page.waitForFunction(() => !document.querySelector('#analyzeBtn').disabled);
        assert.equal(await page.textContent('#editModeBtn'), 'Switch to AI Mode');
        assert.equal(await page.textContent('#sceneModeStatus'), 'Current mode: Edit');
        await page.locator('.popupClose').click();
        failAnalysis = false;
        assert.equal(await page.inputValue('#sceneFileName'), 'Renamed accident report');
        await page.click('#savedScenesTab');
        await page.fill('#sceneSearchInput', 'renamed accident');
        await page.click('#searchScenesBtn');
        await page.locator('.savedSceneCard h3').getByText('Renamed accident report', { exact: true }).waitFor();
        assert.ok(requests.some(r => r.url.searchParams.get('query') === 'renamed accident'));
        await page.locator('.savedSceneCard').getByRole('button', { name: 'Open in Editor', exact: true }).click();
        assert.equal(await page.inputValue('#sceneFileName'), 'Renamed accident report');
        assert.equal(await page.textContent('#editModeBtn'), 'Switch to AI Mode');
        const downloadPromise = page.waitForEvent('download');
        await page.click('#saveBtn');
        const downloaded = await downloadPromise;
        assert.equal(downloaded.suggestedFilename(), 'Renamed accident report.json');
        const chunks = [];
        for await (const chunk of await downloaded.createReadStream()) chunks.push(chunk);
        const exported = JSON.parse(Buffer.concat(chunks).toString());
        assert.equal(exported.fileName, 'Renamed accident report');
        assert.equal(exported.location.file_name, undefined);
        await page.click('#savedLocationsTab');
        await page.locator('.locationCard').first().waitFor();
        await page.locator('.locationCard').first().getByText('Use in Editor', { exact: true }).click();
        assert.ok((await page.textContent('#vehicleList')).includes('V1'));
        assert.equal(await page.inputValue('#sceneFileName'), 'Renamed accident report');
        await page.click('#addLocationTab');
        assert.equal(await page.locator('#newLocation_fileName').count(), 0);
        const editorOptions = await page.locator('#sceneType option').evaluateAll(options => options.map(o => [o.value, o.textContent]));
        const presetOptions = await page.locator('#locationPreset option').evaluateAll(options => options.filter(o => o.value).map(o => [o.value, o.textContent]));
        assert.equal(presetOptions.length, 8);
        assert.deepEqual(presetOptions, editorOptions);
        for (const [background] of presetOptions) {
            await page.selectOption('#locationPreset', background);
            assert.equal(await page.inputValue('#newLocation_backgroundFileName'), background);
            assert.equal(await page.isChecked('#newLocation_tJunction'), background === 't-junction.png');
            assert.equal(await page.getAttribute('#locationFormPreview', 'src'), `assets/${background}`);
            await page.waitForFunction(() => {
                const img = document.querySelector('#locationFormPreview');
                return img.complete && img.naturalWidth > 0;
            });
            if (background.startsWith('roundabout')) {
                const lanes = background.includes('2-lanes') ? '2' : '1';
                assert.equal(await page.inputValue('#newLocation_topRoadLanes'), lanes);
                assert.ok(Number(await page.inputValue('#newLocation_roundaboutDiameter')) > 0);
                assert.equal(await page.inputValue('#newLocation_bottomRoadLanes'), background.includes('3-exits') ? '' : lanes);
            } else {
                assert.equal(await page.inputValue('#newLocation_roundaboutDiameter'), '');
            }
            if (background.startsWith('boulevard')) {
                assert.equal(await page.inputValue('#newLocation_leftRoadLanes'), '');
                assert.equal(await page.inputValue('#newLocation_rightRoadWidth'), '');
                assert.equal(await page.inputValue('#newLocation_topRoadLanes'), '6');
            }
        }
        await page.selectOption('#locationPreset', '');
        assert.equal(await page.inputValue('#newLocation_topRoadWidth'), '');
        assert.equal(await page.isVisible('#locationFormPreview'), false);
        await page.selectOption('#locationPreset', 't-junction.png');
        assert.equal(await page.inputValue('#newLocation_topRoadWidth'), '');
        assert.equal(await page.inputValue('#newLocation_bottomRoadWidth'), '7');
        await page.fill('#newLocation_name', 'Uploaded location');
        await page.setInputFiles('#locationFormPhoto', fixturePhoto);
        await page.click('#submitLocation');
        await page.waitForFunction(() => !document.querySelector('#savedLocationsPage').hidden);
        await page.locator('.locationCard').getByText('Uploaded location', { exact: true }).waitFor();
        await page.click('#savedScenesTab');
        await page.fill('#scenePlateInput', 'SK-1234 AB');
        await page.selectOption('#sceneLocationFilter', 'Location:ui-1');
        await page.click('#searchScenesBtn');
        await page.waitForTimeout(200);
        assert.ok(requests.some(r => r.url.searchParams.get('plate') === 'SK-1234 AB' && r.url.searchParams.get('locationId') === 'Location:ui-1'));
        await fs.mkdir(path.resolve(__dirname, '../target/ui'), { recursive: true });
        await page.screenshot({ path: path.resolve(__dirname, '../target/ui/scene-filters.png') });
        await page.click('#savedLocationsTab');
        await page.locator('.locationCard').first().waitFor();
        await fs.mkdir(path.resolve(__dirname, '../target/ui'), { recursive: true });
        await page.waitForFunction(() => !document.querySelector('#locationsMessage').textContent.includes('Loading'));
        await page.screenshot({ path: path.resolve(__dirname, '../target/ui/locations.png') });
        await page.click('#addLocationTab');
        await page.screenshot({ path: path.resolve(__dirname, '../target/ui/add-location.png') });
        await page.click('#editorTab');
        await page.locator('#locationPanel').evaluate(element => { element.scrollTop = element.scrollHeight; });
        assert.ok(await page.locator('#tjunctionCheck').isVisible());
        await page.locator('#tjunctionCheck').click();
        await page.screenshot({ path: path.resolve(__dirname, '../target/ui/editor.png') });

        // Deleting a selected, unused template clears both selectors and its photo reference.
        const uploaded = locations.find(l => l.name === 'Uploaded location');
        await page.selectOption('#editorLocationSelect', uploaded.id);
        await page.waitForFunction(() => document.querySelector('#locName').value === 'Uploaded location');
        await page.click('#savedScenesTab');
        await page.selectOption('#sceneLocationFilter', uploaded.id);
        await page.click('#savedLocationsTab');
        const uploadedCard = page.locator(`.locationCard[data-location-id="${uploaded.id}"]`);
        await uploadedCard.waitFor();
        const deleteCount = requests.filter(r => r.method === 'DELETE').length;
        acceptDialogs = false;
        await uploadedCard.getByRole('button', { name: 'Delete Location', exact: true }).click();
        assert.equal(requests.filter(r => r.method === 'DELETE').length, deleteCount);
        assert.ok(locations.some(l => l.id === uploaded.id));
        acceptDialogs = true;
        await uploadedCard.getByRole('button', { name: 'Delete Location', exact: true }).click();
        await uploadedCard.waitFor({ state: 'detached' });
        await page.waitForFunction(() => document.querySelector('#locationsMessage').textContent.startsWith('Deleted:'));
        for (const select of ['editorLocationSelect', 'sceneLocationFilter']) {
            assert.equal(await page.locator(`#${select} option[value="${uploaded.id}"]`).count(), 0);
            assert.equal(await page.inputValue(`#${select}`), '');
        }
        assert.equal(await page.textContent('#locationsPageInfo'), 'Page 0 of 0');
        await page.click('#editorTab');
        assert.equal(await page.inputValue('#locName'), 'Uploaded location');
        assert.ok((await page.textContent('#vehicleList')).includes('V1'));
        await page.click('#saveDatabaseBtn');
        await page.waitForFunction(() => !document.querySelector('#saveDatabaseBtn').disabled);
        const resaved = requests.filter(r => r.pathname.endsWith('/full-scene')).at(-1).body;
        assert.equal(resaved.locationId, undefined);
        assert.equal(resaved.locationInfo.photoSourceLocationId, null);

        // A location still used by a saved accident remains visible after the API refuses deletion.
        const usedId = [...scenes.values()][0].locationId;
        await page.click('#savedLocationsTab');
        await page.fill('#locationsSearch', '');
        await page.click('#locationsSearchButton');
        const usedCard = page.locator(`.locationCard[data-location-id="${usedId}"]`);
        await usedCard.waitFor();
        await usedCard.getByRole('button', { name: 'Delete Location', exact: true }).click();
        await usedCard.getByRole('status').filter({ hasText: 'used by a saved accident' }).waitFor();
        assert.ok(locations.some(l => l.id === usedId));
        assert.equal(await usedCard.getByRole('button', { name: 'Delete Location', exact: true }).isEnabled(), true);
        await page.screenshot({ path: path.resolve(__dirname, '../target/ui/location-delete.png') });

        // Removing the only result on the last page returns to the preceding page.
        while (locations.length < 13) locations.push({ id: `Location:ui-${nextLocationId++}`, name: `Unused ${nextLocationId}`, roadLayoutType: 'INTERSECTION' });
        await page.click('#locationsSearchButton');
        await page.waitForFunction(() => document.querySelector('#locationsPageInfo').textContent === 'Page 1 of 2');
        await page.click('#locationsNext');
        await page.waitForFunction(() => document.querySelector('#locationsPageInfo').textContent === 'Page 2 of 2');
        await page.locator('.locationCard').getByRole('button', { name: 'Delete Location', exact: true }).click();
        await page.waitForFunction(() => document.querySelector('#locationsPageInfo').textContent === 'Page 1 of 1');
        assert.deepEqual(errors, []);
        // Both old JSON files and new exports load a scene name outside the location.
        await page.click('#editorTab');
        await page.setInputFiles('#loadInput', { name: 'legacy.json', mimeType: 'application/json',
            buffer: Buffer.from(JSON.stringify({ ...exported, fileName: undefined,
                location: { ...exported.location, file_name: 'Legacy imported scene' } })) });
        await page.waitForFunction(() => document.querySelector('#sceneFileName').value === 'Legacy imported scene');
        await page.setInputFiles('#loadInput', { name: 'new.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(exported)) });
        await page.waitForFunction(() => document.querySelector('#sceneFileName').value === 'Renamed accident report');
        assert.equal(await page.textContent('#sceneModeStatus'), 'Current mode: Edit');
        assert.deepEqual(errors, []);
        console.log('PASS: AI/edit labels and failure handling; scene file name save/load/search/export/import; all eight templates; location deletion and existing workflows.');
    } finally {
        if (browser) await browser.close();
        await new Promise(resolve => server.close(resolve));
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
