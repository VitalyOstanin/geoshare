// The words of the setup page, one object per language with the same keys.
// They live apart from setup-app.js so a test can compare the two; the page
// picks one by the language the browser asks for.

export const WORDS_RU = {
    title: 'Geoshare: заведение группы',
    kicker: (n) => `Geoshare · заведение группы · шаг ${n} из 5`,
    s1Title: 'Создайте проект Firebase',
    s1List: [
        'Откройте консоль Firebase под аккаунтом, которому будет принадлежать группа. ' +
            'Если вы там впервые — примите условия. С 18 октября 2026 года консоль пускает ' +
            'только аккаунт с включённой двухэтапной проверкой.',
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
    s2Got: (email) => (email ? `Доступ получен: ${email}` : 'Доступ получен'),
    s2Refused: (why) => `Доступ не получен: ${why}`,
    s2NoLibrary: 'Не удалось загрузить вход Google. Проверьте сеть и обновите страницу.',
    errNetwork: 'Нет связи с Google. Проверьте сеть и повторите.',
    errDenied: 'У аккаунта нет прав на это действие. Войдите тем аккаунтом, которому принадлежит проект.',
    errTimeout: 'Google не успел закончить операцию. Повторите через минуту.',
    errMissing: 'Не найден нужный файл или ресурс. Обновите страницу и повторите.',
    errOther: 'Не удалось выполнить шаг. Повторите; если повторяется, подробности в консоли браузера.',
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
    privacy: 'Политика конфиденциальности',
    s4SignIn: '1. Вход через Google',
    s4On: '✓ включён',
    s4Off: 'ещё не включён',
    s4Checking: 'проверяю…',
    s4SignInHow: 'Get started → Google → Enable → название (например, «Семья») и почта поддержки → ' +
        'Save. Окно «An identical OAuth client already exists» ожидаемо: приложение входит ' +
        'через браузер, вход остаётся включённым — просто закройте его.',
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
    s5Camera: 'Страница открыта на компьютере? Наведите камеру устройства:',
    s5Foot: (site) =>
        `Карта группы на компьютере: ${site}. Доступ, выданный этой странице, можно отозвать ` +
        'в настройках аккаунта Google — группе он больше не нужен.',
};

export const WORDS_EN = {
    title: 'Geoshare: setting up a group',
    kicker: (n) => `Geoshare · setting up a group · step ${n} of 5`,
    s1Title: 'Create a Firebase project',
    s1List: [
        'Open the Firebase console with the account that will own the group. ' +
            'If this is your first time there, accept the terms. From 18 October 2026 the ' +
            'console only lets in an account with 2-Step Verification turned on.',
        'Start a new Firebase project and give it a name, for example “Family”.',
        'You can turn Google Analytics off — the group does not need it.',
        'Wait until the console says the project is ready, then come back to this tab.',
    ],
    s1Console: 'Open the Firebase console',
    s1Plan: 'Stay on the free plan (Spark). No payment card is needed.',
    s1Next: 'Project created — next',
    s2Title: 'Allow the setup',
    s2Text:
        'Google will ask whether this page may manage your Firebase projects and database. ' +
        'The access lasts about an hour and is only needed while you set up: the page ' +
        'stores nothing and sends it nowhere.',
    s2Auth: 'Sign in with Google and allow',
    s2Got: (email) => (email ? `Access granted: ${email}` : 'Access granted'),
    s2Refused: (why) => `Access not granted: ${why}`,
    s2NoLibrary: 'Could not load Google sign-in. Check your connection and reload the page.',
    errNetwork: 'Cannot reach Google. Check the network and try again.',
    errDenied: 'The account has no rights for this action. Sign in with the account that owns the project.',
    errTimeout: 'Google did not finish the operation in time. Try again in a minute.',
    errMissing: 'A needed file or resource was not found. Reload the page and try again.',
    errOther: 'The step failed. Try again; if it repeats, the details are in the browser console.',
    s2NoConfig: 'Could not read setup-config.json next to the page.',
    s2Pick: 'Choose the group’s project',
    s2NoProjects: 'This account has no Firebase projects. Go back to step 1.',
    s2Go: (name) => `Set up “${name}”`,
    s2IsGroup: (id) => `${id} · already set up as a group`,
    s2Go0: 'Set up',
    s3Title: (name) => `Setting up “${name}”`,
    s3Text: 'Keep this tab open. It usually takes 2–5 minutes: the database is still ' +
        'unavailable for a little while after it is turned on.',
    s3Retry: 'Retry',
    s3Next: 'Next',
    s3Wait: 'Next — once the setup is done',
    s3Taken: (id) =>
        `The address ${id}.web.app is already taken by someone else’s site. The group page ` +
        'has to be named after the project, so another name will not do. Create a new ' +
        'project with a different ID (you can change it under the name when you create ' +
        'it) and start over.',
    steps: [
        'Firestore is turned on',
        'Database created (Europe)',
        'Access rules deployed',
        'App and page registered',
        'Build fingerprint added',
        'Group page deployed',
        'Device settings deployed',
    ],
    s4Title: 'Two steps in the console',
    s4Text: 'Google lets only the project owner do these. The links already lead to your project.',
    privacy: 'Privacy policy',
    s4SignIn: '1. Sign-in with Google',
    s4On: '✓ turned on',
    s4Off: 'not turned on yet',
    s4Checking: 'checking…',
    s4SignInHow: 'Get started → Google → Enable → a name (for example, “Family”) and a support ' +
        'email → Save. A dialog saying “An identical OAuth client already exists” is ' +
        'expected: the app signs in through the browser and sign-in stays on — just close it.',
    s4OpenProviders: 'Open sign-in methods',
    s4Redirect: '2. The page’s redirect address',
    s4RedirectHow:
        'Web client (auto created by Google Service) → Authorized redirect URIs → Add URI → ' +
        'paste → Save.',
    s4Copy: 'Copy',
    s4Copied: 'Copied',
    s4OpenCredentials: 'Open OAuth clients',
    s4Tick: 'The address is added and saved',
    s4Note: 'The page sees the first step on its own. It cannot check the second — the ' +
        'sign-in on the group page at the end will.',
    s4Next: 'Both steps done',
    s5Title: (name) => `Group “${name}” is ready`,
    s5Text: 'Connect your own device first: you become the group’s administrator and can invite ' +
        'the others from the app.',
    s5Open: 'Open in the geoshare app',
    s5Camera: 'Is this page open on a computer? Point your device’s camera at it:',
    s5Foot: (site) =>
        `The group’s map on a computer: ${site}. The access given to this page can be ` +
        'revoked in your Google account settings — the group no longer needs it.',
};
