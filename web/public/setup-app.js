// Wires the setup page a project owner opens to finish their Firebase project:
// five screens, one at a time. The calls to Google are the steps of
// lib/setup-project.js and lib/setup-hosting.js; this file only holds the
// token, the screens and the order. Every step looks before it creates, so
// "Повторить" simply runs all of them again.

import {
    makeApi,
    listProjects,
    enableFirestoreApi,
    createDatabase,
    ensureWebApp,
    ensureAndroidApp,
    ensureFingerprints,
    deployRules,
} from './lib/setup-project.js';
import {
    ensureSite,
    appSettings,
    webConfigFile,
    deploySite,
    versionConfig,
    googleSignInEnabled,
    releasedPaths,
    servesGroup,
} from './lib/setup-hosting.js';

// The page is Russian only for now; a second language is a second object of
// the same keys, picked in `words` below.
const WORDS_RU = {
    kicker: (n) => `Geoshare · заведение группы · шаг ${n} из 5`,
    s1Title: 'Создайте проект Firebase',
    s1List: [
        'Откройте консоль Firebase под аккаунтом, которому будет принадлежать группа. ' +
            'Если вы там впервые — примите условия.',
        'Нажмите «Создать проект», назовите его, например, «Семья».',
        'Google Analytics можно выключить — группе он не нужен.',
        'Дождитесь «Проект готов» и вернитесь на эту вкладку.',
    ],
    s1Console: 'Открыть консоль Firebase',
    s1Plan: 'Тариф оставьте бесплатным (Spark). Платёжная карта не нужна.',
    s1Next: 'Проект создан — дальше',
    s2Title: 'Разрешите настроить проект',
    s2Text:
        'Google спросит, можно ли этой странице управлять вашими проектами Firebase и базой. ' +
        'Доступ действует около часа и нужен только на время настройки: страница ничего не ' +
        'хранит и никуда его не передаёт.',
    s2Auth: 'Войти через Google и разрешить',
    s2Got: 'Доступ получен',
    s2Refused: (why) => `Доступ не получен: ${why}`,
    s2NoLibrary: 'Не удалось загрузить вход Google. Проверьте сеть и обновите страницу.',
    s2NoConfig: 'Не удалось прочитать setup-config.json рядом со страницей.',
    s2Pick: 'Выберите проект группы',
    s2NoProjects: 'В этом аккаунте нет проектов Firebase. Вернитесь к шагу 1.',
    s2Go: (name) => `Настроить «${name}»`,
    s2IsGroup: (id) => `${id} · уже настроен как группа`,
    s2Go0: 'Настроить',
    s3Title: (name) => `Настраиваю «${name}»`,
    s3Text: 'Не закрывайте вкладку. Обычно 2–5 минут: база после включения ещё немного ' +
        'недоступна.',
    s3Retry: 'Повторить',
    s3Next: 'Дальше',
    s3Wait: 'Дальше — после настройки',
    s3Taken: (id) =>
        `Адрес ${id}.web.app уже занят чужим сайтом. Страница группы должна называться так же, ` +
        'как проект, поэтому другое имя не подойдёт. Создайте новый проект с другим ' +
        'идентификатором (при создании его можно изменить под названием) и начните заново.',
    steps: [
        'База Firestore включена',
        'База создана (Европа)',
        'Правила доступа выложены',
        'Приложение и страница зарегистрированы',
        'Отпечаток сборки добавлен',
        'Страница группы выложена',
        'Настройки для устройств выложены',
    ],
    s4Title: 'Два шага в консоли',
    s4Text: 'Их Google разрешает только самому владельцу. Ссылки уже ведут в ваш проект.',
    s4SignIn: '1. Вход через Google',
    s4On: '✓ включён',
    s4Off: 'ещё не включён',
    s4Checking: 'проверяю…',
    s4SignInHow: 'Get started → Google → Enable → почта поддержки → Save.',
    s4OpenProviders: 'Открыть способы входа',
    s4Redirect: '2. Адрес возврата страницы',
    s4RedirectHow:
        'Web client (auto created by Google Service) → Authorized redirect URIs → Add URI → ' +
        'вставить → Save.',
    s4Copy: 'Скопировать',
    s4Copied: 'Скопировано',
    s4OpenCredentials: 'Открыть клиентов OAuth',
    s4Tick: 'Адрес добавлен и сохранён',
    s4Note: 'Первый шаг страница видит сама. Второй проверить не может — проверит вход на ' +
        'странице группы в конце.',
    s4Next: 'Оба шага сделаны',
    s5Title: (name) => `Группа «${name}» готова`,
    s5Text: 'Подключите своё устройство первым: вы станете администратором группы и сможете ' +
        'приглашать остальных из приложения.',
    s5Open: 'Открыть в приложении geoshare',
    s5Camera: 'Страница открыта на компьютере? Откройте эту ссылку на устройстве:',
    s5NoQr: 'QR-код здесь не рисуется: ссылку можно отправить себе на устройство.',
    s5Foot: (site) =>
        `Карта группы на компьютере: ${site}. Доступ, выданный этой странице, можно отозвать ` +
        'в настройках аккаунта Google — группе он больше не нужен.',
};

const words = WORDS_RU;

const SCOPES = [
    'https://www.googleapis.com/auth/firebase',
    'https://www.googleapis.com/auth/datastore',
    'https://www.googleapis.com/auth/service.management',
].join(' ');
const GIS_URL = 'https://accounts.google.com/gsi/client';
const SIGN_IN_POLL_MS = 5000;
// Files the setup page cannot take from this origin: the group's own.
const GENERATED = ['/geoshare.json', '/firebase-config.js'];

const $ = (id) => document.getElementById(id);
const state = { config: null, token: null, api: null, project: null, polling: null };

for (const element of document.querySelectorAll('[data-text]')) {
    element.textContent = words[element.dataset.text];
}

for (const element of document.querySelectorAll('[data-kicker]')) {
    element.textContent = words.kicker(element.dataset.kicker);
}

for (const text of words.s1List) {
    const item = document.createElement('li');

    item.textContent = text;
    $('s1-list').append(item);
}

function show(step) {
    for (let n = 1; n <= 5; n++) {
        $(`step-${n}`).hidden = n !== step;
    }

    window.scrollTo(0, 0);
}

function fail(id, text) {
    $(id).textContent = text;
    $(id).hidden = false;
}

function loadGis() {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');

        script.src = GIS_URL;
        script.onload = resolve;
        script.onerror = reject;
        document.head.append(script);
    });
}

async function readJson(path) {
    const response = await fetch(path);

    if (!response.ok) {
        throw new Error(`${path} -> ${response.status}`);
    }

    return response.json();
}

async function readBytes(path) {
    const response = await fetch(path);

    if (!response.ok) {
        throw new Error(`${path} -> ${response.status}`);
    }

    return new Uint8Array(await response.arrayBuffer());
}

// Screen 1.
$('s1-next').addEventListener('click', () => show(2));

// Screen 2: the token, then the projects of the account.
function projectLabel(project) {
    return project.displayName || project.projectId;
}

function renderProjects(projects) {
    const box = $('choices');

    box.replaceChildren();

    if (projects.length === 0) {
        const none = document.createElement('p');

        none.className = 'bad';
        none.textContent = words.s2NoProjects;
        box.append(none);
        $('s2-projects').hidden = false;

        return;
    }

    // Nothing is chosen for the owner: the list may hold a group already in
    // use, and setting it up again redeploys its rules and page.
    projects.forEach((project) => {
        const label = document.createElement('label');
        const radio = document.createElement('input');
        const text = document.createElement('span');
        const name = document.createElement('span');
        const id = document.createElement('span');

        label.className = 'choice';
        radio.type = 'radio';
        radio.name = 'project';
        text.className = 'choice-text';
        name.style.fontWeight = '600';
        name.textContent = projectLabel(project);
        id.className = 'small muted';
        id.textContent = project.projectId;
        text.append(name, id);
        label.append(radio, text);
        radio.addEventListener('change', () => choose(project));
        box.append(label);
        servesGroup(state.api, project.projectId)
            .then((group) => {
                if (group) {
                    id.textContent = words.s2IsGroup(project.projectId);
                }
            })
            .catch(() => {
                // Unknown is told as nothing: the mark warns, it does not guard.
            });
    });

    $('s2-projects').hidden = false;
    $('s2-go').disabled = true;
}

function choose(project) {
    state.project = project;
    $('s2-go').textContent = words.s2Go(projectLabel(project));
    $('s2-go').disabled = false;
}

async function gotToken(answer) {
    if (answer.error) {
        fail('s2-error', words.s2Refused(answer.error));

        return;
    }

    state.token = answer.access_token;
    state.api = makeApi(fetch, state.token);
    $('s2-error').hidden = true;
    $('s2-got').textContent = words.s2Got;
    $('s2-got').hidden = false;

    try {
        renderProjects(await listProjects(state.api));
    } catch (error) {
        fail('s2-error', error.message);
    }
}

$('s2-go').textContent = words.s2Go0;
$('s2-auth').addEventListener('click', async () => {
    $('s2-error').hidden = true;

    try {
        state.config ??= await readJson('setup-config.json');
    } catch {
        fail('s2-error', words.s2NoConfig);

        return;
    }

    try {
        if (!window.google?.accounts) {
            await loadGis();
        }
    } catch {
        fail('s2-error', words.s2NoLibrary);

        return;
    }

    const client = window.google.accounts.oauth2.initTokenClient({
        client_id: state.config.clientId,
        scope: SCOPES,
        callback: gotToken,
        error_callback: (error) => fail('s2-error', words.s2Refused(error.type)),
    });

    client.requestAccessToken();
});

// Screen 3: the steps as a checklist.
const stepLines = [];

function renderSteps() {
    $('steps').replaceChildren();
    stepLines.length = 0;

    for (const label of words.steps) {
        const line = document.createElement('div');
        const mark = document.createElement('span');
        const text = document.createElement('span');
        const why = document.createElement('span');

        line.className = 'line pending';
        mark.className = 'mark';
        text.style.display = 'flex';
        text.style.flexDirection = 'column';
        why.className = 'why';
        why.hidden = true;
        text.append(label, why);
        line.append(mark, text);
        $('steps').append(line);
        stepLines.push({ line, mark, why });
    }
}

function mark(index, status, why) {
    const { line, mark: sign, why: reason } = stepLines[index];

    line.className = `line ${status}`;
    sign.textContent = { done: '✓', running: '…', failed: '!', pending: '' }[status];
    reason.hidden = !why;
    reason.textContent = why || '';
}

async function runSteps() {
    const { api, project, config } = state;
    const projectId = project.projectId;
    const context = {};
    const stepFns = [
        () => enableFirestoreApi(api, project.projectNumber),
        () => createDatabase(api, projectId, config.location),
        async () => {
            const response = await fetch('firestore.rules');

            if (!response.ok) {
                throw new Error(`firestore.rules -> ${response.status}`);
            }

            await deployRules(api, projectId, await response.text());
        },
        async () => {
            context.web = await ensureWebApp(api, projectId);
            context.android = await ensureAndroidApp(api, projectId, config.packageName);
        },
        () => ensureFingerprints(api, context.android.name, config.fingerprints),
        async () => {
            await ensureSite(api, projectId, context.web.appId);
            context.manifest = await readJson('setup-files.json');
            context.files = {};

            for (const path of context.manifest.paths) {
                context.files[path] = await readBytes(`group${path}`);
            }

            // The page and its settings go out in one release: a page released
            // without them would not open for a member who reloads it meanwhile.
            const settings = await appSettings(api, context.android.name, projectId);

            context.files['/geoshare.json'] = JSON.stringify(settings, null, 2) + '\n';
            context.files['/firebase-config.js'] = await webConfigFile(
                api,
                context.web.name,
                projectId,
            );

            await deploySite(
                api,
                projectId,
                context.files,
                versionConfig(context.manifest.hosting),
            );
        },
        async () => {
            const served = await releasedPaths(api, projectId);

            for (const path of GENERATED) {
                if (!served.has(path)) {
                    throw new Error(`${path} is missing`);
                }
            }
        },
    ];

    for (let index = 0; index < stepFns.length; index++) {
        mark(index, 'running');

        try {
            await stepFns[index]();
        } catch (error) {
            mark(index, 'failed', error.message);

            return error;
        }

        mark(index, 'done');
    }

    return null;
}

async function startSetup() {
    const name = projectLabel(state.project);

    $('s3-title').textContent = words.s3Title(name);
    $('s3-retry').hidden = true;
    $('s3-taken').hidden = true;
    $('s3-next').hidden = false;
    $('s3-next').disabled = true;
    $('s3-next').textContent = words.s3Wait;
    renderSteps();
    show(3);

    const error = await runSteps();

    if (error === null) {
        $('s3-next').textContent = words.s3Next;
        $('s3-next').disabled = false;

        return;
    }

    $('s3-next').hidden = true;
    $('s3-retry').hidden = false;

    if (error.code === 'site-name-taken') {
        $('s3-taken').textContent = words.s3Taken(state.project.projectId);
        $('s3-taken').hidden = false;
    }
}

$('s2-go').addEventListener('click', startSetup);
$('s3-retry').addEventListener('click', startSetup);

// Screen 4: sign-in is polled, the redirect address is ticked by hand.
function refreshStep4Button() {
    $('s4-next').disabled = !$('s4-tick').checked;
}

async function pollSignIn() {
    try {
        const on = await googleSignInEnabled(state.api, state.project.projectId);

        $('s4-state').textContent = on ? words.s4On : words.s4Off;
        $('s4-state').className = on ? 'small ok' : 'small muted';
    } catch {
        // The next poll tries again; a screen that shows nothing new is not wrong.
    }
}

function startStep4() {
    const id = state.project.projectId;
    const consoleBase = state.config.consoleUrl.replace(/\/?$/, '/');

    // The group's page signs in through its own site (authDomain), named after the project.
    $('s4-uri').textContent = `https://${id}.web.app/__/auth/handler`;
    $('s4-providers').href = `${consoleBase}project/${id}/authentication/providers`;
    $('s4-credentials').href = `https://console.cloud.google.com/apis/credentials?project=${id}`;
    $('s4-state').textContent = words.s4Checking;
    show(4);
    pollSignIn();
    state.polling = setInterval(pollSignIn, SIGN_IN_POLL_MS);
}

$('s3-next').addEventListener('click', startStep4);
$('s4-tick').addEventListener('change', refreshStep4Button);
$('s4-copy').addEventListener('click', async () => {
    try {
        await navigator.clipboard.writeText($('s4-uri').textContent);
        $('s4-copy').textContent = words.s4Copied;
    } catch {
        // Clipboard refused: the address is on screen to be selected by hand.
    }
});

// Screen 5.
$('s4-next').addEventListener('click', () => {
    clearInterval(state.polling);

    const id = state.project.projectId;
    const link = `https://${id}.web.app/join`;

    $('s5-title').textContent = words.s5Title(projectLabel(state.project));
    $('s5-open').href = link;
    $('s5-link').textContent = link;
    $('s5-foot').textContent = words.s5Foot(`${id}.web.app`);
    show(5);
});

$('s1-console').textContent = words.s1Console;
fetch('setup-config.json')
    .then((response) => (response.ok ? response.json() : null))
    .then((config) => {
        state.config = config;
        $('s1-console').href = config?.consoleUrl || 'https://console.firebase.google.com/';
    })
    .catch(() => {
        $('s1-console').href = 'https://console.firebase.google.com/';
    });
