// Everything the page says in Russian.
//
// A translation of words/en.js rather than a second wording of the page: the
// two files carry the same keys, and a test holds them to that. Where English
// tells one from many with a stray "s", Russian has three forms of a counted
// word, so the counts here go through Intl.PluralRules rather than through a
// comparison with one.

const FORMS = new Intl.PluralRules('ru');

/**
 * The form a counted word takes: 1 участник, 2 участника, 5 участников.
 *
 * Written out for every word rather than derived: the endings follow the
 * count, but which endings they are is a property of the word itself.
 */
function counted(count, one, few, many) {
    const form = FORMS.select(count);

    if (form === 'one') {
        return one;
    }

    return form === 'few' ? few : many;
}

/** The units an age is said in, in the case "N ... назад" puts them in. */
const AGES = {
    minutes: ['минуту', 'минуты', 'минут'],
    hours: ['час', 'часа', 'часов'],
    days: ['день', 'дня', 'дней'],
};

export const RU = {
    // The strip under the map, and what stands in for it when it is empty.
    you: 'Вы',
    signedIn: 'Вход выполнен',
    nobodyYet: 'Никто ещё не поделился своим местом.',
    looking: 'Ищем группу…',
    unreachable: 'Нет связи с группой. Пробуем ещё раз.',
    nobodySharing: 'Сейчас никто не делится с вами своим местом.',
    oneMoreRefused: 'Ещё один участник сейчас не делится с вами.',
    moreRefused: (count) => `Ещё ${count} `
        + `${counted(count, 'участник', 'участника', 'участников')} сейчас не `
        + `${counted(count, 'делится', 'делятся', 'делятся')} с вами.`,

    // The names of the five ways a phone collects its position. Names rather
    // than sentences describing them: what each one asks for and how often it
    // sends belongs in the help the card opens.
    modeIdle: 'На месте',
    modeStandby: 'Ожидание',
    modeMoving: 'Движение',
    modeViewing: 'Просмотр',
    modeNight: 'Ночь',

    // The help the mode on a card opens.
    modeHelpTitle: 'Как телефон определяет своё место',
    modeHelpIntro:
        'Телефон сам выбирает один из этих пяти способов. Открытая у кого-то карта важнее '
        + 'всего остального; в остальное время телефон тратит столько, сколько нужно, чтобы его '
        + 'можно было найти.',
    modeHelpWhen: 'Когда',
    modeHelpAsks: 'Что делает телефон',
    close: 'Закрыть',
    modeWhenViewing: 'Кто-то смотрит карту.',
    modeAsksViewing: 'Запрашивает самое точное место каждые 15 секунд и записывает его, как '
        + 'только метка сдвинулась бы.',
    modeWhenMoving: 'Телефон заметил ходьбу, бег, велосипед или поездку.',
    modeAsksMoving: 'Запрашивает каждые 5 минут.',
    modeWhenStandby: 'Телефон не там, где записал место в прошлый раз, и карту никто не смотрит.',
    modeAsksStandby: 'Запрашивает каждые 15 минут.',
    modeWhenNight: 'С 23:00 до 07:00 или заряд ниже 15 процентов.',
    modeAsksNight: 'Запрашивает каждые 20 минут самым экономным способом.',
    modeWhenIdle: 'Телефон стоит в 150 метрах от места, которое записал последним.',
    modeAsksIdle: 'Не запрашивает ничего и повторяет последнее место каждые 25 минут, чтобы '
        + 'карточка не помечалась как устаревшая.',

    // The card a marker opens.
    lastSeen: 'Обновлено',
    at: 'Время позиции',
    online: 'В сети',
    mode: 'Режим',
    battery: 'Заряд',
    position: 'Координаты',
    unknown: 'неизвестно',

    // Откуда взялась позиция и насколько она точна. Две строки рядом:
    // слово без точности не отличает квартиру от района, а точность без
    // слова не говорит, измерено это по небу или найдено в чьей-то базе.
    foundBy: 'Откуда позиция',
    width: 'Точность',
    foundSatellites: 'Спутники',
    foundAssistance: 'Вышки и Wi-Fi',
    foundUnheard: 'Спутники без измерений',
    foundOtherRadios: 'Вышки и Wi-Fi',
    foundPretended: 'Другое приложение на телефоне',
    foundUnsaid: 'Источник не назван',

    // Что телефон сказал о приёмнике после включения. Строка появляется
    // только тогда, когда это верно, и стоит под точностью: приёмник, который
    // ещё не устоялся, отвечает узко и неверно.
    settling: 'Приёмник',
    settlingNote: 'Не устоялся после включения телефона',

    // Что телефон отбросил с последней записанной позиции. Строки появляются
    // только тогда, когда отказы идут: телефон, у которого позиции проходят,
    // объяснять нечего, а две строки с нулём стояли бы на каждой карточке.
    leftOut: 'Отброшено',
    leftOutReason: 'Последняя причина',
    leftOutAnswers: (count) => `${count} ${counted(count, 'позиция', 'позиции', 'позиций')}`,
    leftOutDrifted: 'Увод при неподвижном телефоне',
    leftOutContradicted: 'Вышки говорят другое',
    leftOutWidth: 'Ответ шире района',
    leftOutSpeed: 'Такая скорость невозможна',
    leftOutContradiction: 'Два места в одно время',
    leftOutFalsePlace: 'Известное ложное место',
    leftOutUnmoved: 'Телефон не двигали',
    leftOutCircling: 'Ответы ходят по кругу',
    leftOutOneBand: 'Слышна одна полоса',
    leftOutOneTransmitter: 'Небо от одного передатчика',
    leftOutRepeating: 'Ответ повторяется',
    leftOutSettling: 'Приёмник ещё не устоялся',
    leftOutBySource: 'Источник выключен на устройстве',
    leftOutBySourceNamed: (source) => `Источник выключен на устройстве: ${source}`,

    // How wide the phone said its answer is. The sign is what turns a distance
    // into a claim about where the marker might really be.
    widthMetres: (count) => `${count} м`,
    widthKilometres: (count) => `${count} км`,
    backToList: '← К списку',
    oneMember: '1 участник',
    members: (count) => `${count} ${counted(count, 'участник', 'участника', 'участников')}`,

    // Who else has the map open right now, named rather than counted.
    mapOpen: (names) => `Карту смотрят: ${names.join(', ')}`,

    // Ages, which are the only words with a plural in them.
    justNow: 'только что',
    ago: (count, unit) => `${count} ${counted(count, ...AGES[unit])} назад`,

    // What the sign-in screen says when it did not happen.
    signInOffline: 'Нет связи. Войдите ещё раз, когда сеть появится.',
    signInMisconfigured: 'Эта страница пока не настроена для входа.',
    signInFailed: 'Войти не удалось. Попробуйте ещё раз.',

    // The words index.html is written with.
    signInExplanation: 'Войдите в тот аккаунт Google, под которым семья делится своими местами.',
    signInAction: 'Войти через Google',
    signOut: 'Выйти',
    appearance: 'Оформление',
    themeLight: 'Светлое',
    themeDark: 'Тёмное',
    themeSystem: 'Как в системе',

    // The language, written in the language it stands for.
    language: 'Язык',
    languageRussian: 'Русский',
    languageEnglish: 'English',
    languageSystem: 'Как в системе',
};
