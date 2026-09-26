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
    leftOut: 'Отказ точки',
    leftOutReason: 'Причина отказа',
    leftOutAnswers: (count) => `${count} ${counted(count, 'позиция', 'позиции', 'позиций')}`,
    leftOutDrifted: 'Увод при лежащем телефоне',
    leftOutHurried: 'Скорость, с которой телефон не носили',
    leftOutContradicted: 'От вышек другая позиция',
    leftOutWidth: 'Слишком большая погрешность',
    leftOutSpeed: 'Нереальная скорость',
    leftOutContradiction: 'Две одновременные позиции',
    leftOutFalsePlace: 'Известная область подмены',
    leftOutUnmoved: 'Телефон не двигали',
    leftOutCircling: 'Трек по кругу',
    leftOutOneBand: 'Позиция без ГЛОНАСС',
    leftOutOneTransmitter: 'Сигнал не с орбиты',
    leftOutTimeApart: 'Время спутников сместилось',
    leftOutRepeating: 'Трек повторяется',
    leftOutSettling: 'Приёмник не устоялся',
    leftOutBySource: 'Источник выключен',
    leftOutBySourceNamed: (source) => `Источник выключен: ${source}`,

    // The help the rejection reason on a card opens.
    reasonHelpTitle: 'Почему точка отброшена',
    reasonHelpIntro:
        'Телефон проверяет каждую позицию несколькими правилами и отказывает ей, как только '
        + 'сработало первое. Отказ не значит поломку: в районе с подменой сигнала отброшенных '
        + 'точек больше, чем принятых, и это правильная работа.',
    reasonHelpSign: 'Признак',
    reasonHelpMeans: 'О чём говорит',
    reasonSignWidth: 'Телефон сам назвал радиус, в котором он где-то находится, и радиус больше '
        + 'полукилометра.',
    reasonMeansWidth: 'Такая точка показывает не место, а район целиком. Чаще всего так отвечают '
        + 'вышки, когда телефон в помещении.',
    reasonSignSpeed: 'Чтобы попасть из прошлой точки в эту за прошедшее время, пришлось бы ехать '
        + 'быстрее 200 км/ч.',
    reasonMeansSpeed: 'Одна из двух точек ложная. Подмена почти всегда бросает метку далеко в '
        + 'сторону, а не сдвигает её понемногу.',
    reasonSignContradiction: 'На один и тот же момент времени пришли две разные позиции.',
    reasonMeansContradiction: 'Источник противоречит сам себе. Так бывает, когда рядом работает '
        + 'передатчик подмены и приёмник мечется между ним и спутниками.',
    reasonSignFalsePlace: 'Точка попала в область, которую вы отметили как место, куда подмена '
        + 'уводит телефоны.',
    reasonMeansFalsePlace: 'Метку туда ставит передатчик, а не спутники. Области задаются на '
        + 'группу, поэтому их достаточно отметить один раз с любого телефона.',
    reasonSignUnmoved: 'Датчики движения говорят, что телефон никуда не несли, а позиция '
        + 'сменилась.',
    reasonMeansUnmoved: 'Сдвинуть метку у неподвижного телефона может только ложный сигнал: шаги '
        + 'и поездки считает отдельный чип, до которого подмена не достаёт.',
    reasonSignDrifted: 'Телефон лежит на месте, а точки понемногу уходят в сторону, каждая '
        + 'недалеко от предыдущей.',
    reasonMeansDrifted: 'Медленный увод. Шаг в тридцать метров не ловится сравнением соседних '
        + 'точек, но за час метка оказывается в другом месте, поэтому счёт идёт от места, где '
        + 'телефон положили.',
    reasonSignHurried: 'Телефон никто не трогал, а ответ говорит, что он едет со скоростью '
        + 'автомобиля.',
    reasonMeansHurried: 'Приёмник сообщает свою оценку скорости вместе с каждой позицией, и у '
        + 'лежащего телефона она равна нулю. Ложный сигнал рисует дорогу и скорость вдоль неё, и '
        + 'эта скорость находится внутри одного ответа — поэтому ловится первая же ложная '
        + 'точка, а не следующая за ней.',
    reasonSignContradicted: 'Позиция по спутникам и позиция по вышкам и Wi-Fi расходятся на '
        + 'километры.',
    reasonMeansContradicted: 'Вышки подделать труднее, чем спутники. Это единственное правило, '
        + 'которое не сравнивает точку с предыдущей, поэтому оно видит и медленный увод, и первую '
        + 'же ложную точку.',
    reasonSignCircling: 'Точки идут по замкнутому кругу с ровной скоростью.',
    reasonMeansCircling: 'Каждый отрезок такого трека выглядит как обычная поездка. Выдаёт его '
        + 'то, что дорога замыкается: так рисует маршрут передатчик, а не человек.',
    reasonSignRepeating: 'Телефон уже проходил ровно этот набор точек раньше.',
    reasonMeansRepeating: 'Ложный маршрут прокручивают заново по записи.',
    reasonSignOneBand: 'Решение построено на спутниках полосы 1575 МГц — GPS, Galileo, BeiDou, — '
        + 'а ГЛОНАСС слышно, и в решение он не вошёл.',
    reasonMeansOneBand: 'Передатчик подмены работает в одной полосе. ГЛОНАСС вещает на 1602 МГц, '
        + 'туда передатчик не достаёт, и приёмник не смог согласовать его со своими ложными '
        + 'спутниками.',
    reasonSignOneTransmitter: 'Все спутники слышны одинаково громко, как бы низко над горизонтом '
        + 'они ни были, а дальняя полоса молчит.',
    reasonMeansOneTransmitter: 'Настоящее небо звучит тем тише, чем ниже спутник. Ровная '
        + 'громкость означает, что всё пришло из одной точки на земле.',
    reasonSignTimeApart: 'За время между двумя измерениями время, которое несут спутники, '
        + 'продвинулось не на столько же, на сколько собственные часы телефона.',
    reasonMeansTimeApart: 'Телефон считает секунды с момента включения, и до этого счёта не '
        + 'дотянуться ничем переданным по радио. Сигнал, который уводит телефон в другое место, '
        + 'вынужден сдвинуть переносимое им время — сдвиг здесь и виден.',
    reasonSignSettling: 'Первые пять минут после включения телефона, пока решение строится меньше '
        + 'чем на четырёх спутниках.',
    reasonMeansSettling: 'В эти минуты приёмник выдаёт места, где телефон никогда не был. Точки '
        + 'этого окна проверяются строже обычного: шире ста метров или быстрее 5 м/с — отказ.',
    reasonSignSource: 'Вы выключили этот источник позиции в настройках телефона.',
    reasonMeansSource: 'С точкой всё в порядке: она собрана и записана в журнал, но метку не '
        + 'двигает и никому не уходит. Так и было задумано.',

    // How wide the phone said its answer is. The sign is what turns a distance
    // into a claim about where the marker might really be.
    widthMetres: (count) => `${count} м`,
    widthKilometres: (count) => `${count} км`,
    backToList: '← К списку',
    oneMember: '1 участник',
    members: (count) => `${count} ${counted(count, 'участник', 'участника', 'участников')}`,

    // The follow button over the map, which cycles between not following,
    // one member and everybody. The "one" caption is the member's own name
    // rather than a word here, worked out on the page from who was chosen.
    followNone: 'Не следовать',
    followOne: (name) => `Следовать за: ${name}`,
    followAll: 'Следовать за всеми',
    followTargetMissing: 'У этого участника сейчас нет позиции',

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
    signInExplanation: 'Войдите в тот аккаунт Google, под которым семья делится своими геопозициями.',
    signInAction: 'Войти через Google',
    signInWaiting: 'Входим в аккаунт…',
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

    // The group: who is in it, and what the person looking may do about it.
    group: 'Группа',
    groupNone: 'Вы пока не в группе. Создайте её и раздайте приглашение или вставьте '
        + 'приглашение, которое вам прислали.',
    groupFound: 'Создать группу',
    groupInvitation: 'Приглашение',
    groupJoin: 'Вступить',
    groupMakeInvitation: 'Создать приглашение',
    groupMakeAnotherInvitation: 'Создать другое приглашение',
    groupCodeLasts: 'Приглашение действует сутки и впускает всех, до кого дойдёт за это '
        + 'время. Передавайте его целиком: в нём названа и группа, и код.',
    groupCopy: 'Скопировать',
    groupCopied: 'Приглашение скопировано',
    groupRunsIt: 'ведёт группу',
    groupRemove: 'Убрать',
    groupUnrun: 'Группу никто не ведёт, её состав задаётся в консоли Firebase.',

    // The panel the member running the group opens.
    adminTitle: 'Управление группой',
    adminYou: 'это вы',
    adminNotShared: 'не делится с вами',
    adminPhonesShared: (count) => `${count} `
        + `${counted(count, 'телефон', 'телефона', 'телефонов')} `
        + `${counted(count, 'доступен', 'доступны', 'доступны')} вам`,

    // What a refused write says.
    groupRefused: 'База отказала. Возможно, приглашение истекло или группа уже создана.',

    // Naming a phone or a person for oneself, from the card.
    namePhone: 'Переименовать устройство',
    namePerson: 'Переименовать человека',
    nameForYou: 'Это имя видите только вы.',
    nameSave: 'Сохранить',
    nameReset: 'Сбросить',
    nameCancel: 'Отмена',

    // A phone in the panel that runs the group, and whether it is in touch.
    adminPhoneOnline: (name, age) => `${name}: на связи ${age}`,
    adminPhoneSilent: (name) => `${name}: ещё не выходил на связь`,
    adminPhoneCharging: 'на зарядке',
};
