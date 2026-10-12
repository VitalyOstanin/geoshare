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
    linkLost: (since) => `Нет связи с базой с ${since}`,
    pausedSince: (since) => `Карта на паузе с ${since}`,
    pausedNote: 'Никто не смотрел в неё 5 минут. Метки серые. Коснитесь карты или двиньте мышью, чтобы обновить.',
    linkKnownAt: (time) => `Показано то, что было известно на ${time}. Метки серые.`,
    linkKnownNever: 'База ещё не ответила ни разу. Метки серые.',
    knownAt: (time) => `по данным на ${time}`,
    linkLostAction: 'Нет связи с базой: до устройства это не дойдёт. Повторите, когда связь вернётся.',
    unanswered: 'База не ответила за 15 секунд. Запись уйдёт, когда связь вернётся; отменить её нельзя.',
    outsideGroup: 'Вы пока не в группе. Создать группу или вступить по приглашению можно '
        + 'в меню аккаунта.',
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
    modeHelpTitle: 'Как устройство определяет своё место',
    modeHelpIntro:
        'Устройство само выбирает один из этих пяти способов. Открытая у кого-то карта важнее '
        + 'всего остального; в остальное время устройство тратит столько, сколько нужно, чтобы его '
        + 'можно было найти.',
    modeHelpWhen: 'Когда',
    modeHelpAsks: 'Что делает устройство',
    close: 'Закрыть',
    modeWhenViewing: 'Кто-то смотрит карту.',
    modeAsksViewing: 'Запрашивает самое точное место каждые 30 секунд и записывает его, как '
        + 'только метка сдвинулась бы.',
    modeWhenMoving: 'Устройство заметило ходьбу, бег, велосипед или поездку.',
    modeAsksMoving: 'Запрашивает каждые 5 минут.',
    modeWhenStandby: 'Устройство не там, где записало место в прошлый раз, и карту никто не смотрит.',
    modeAsksStandby: 'Запрашивает каждые 15 минут.',
    modeWhenNight: 'С 23:00 до 07:00 или заряд ниже 15 процентов.',
    modeAsksNight: 'Запрашивает каждые 20 минут самым экономным способом.',
    modeWhenIdle: 'Устройство стоит в 150 метрах от места, которое записало последним.',
    modeAsksIdle: 'Не запрашивает ничего и повторяет последнее место каждые 25 минут, чтобы '
        + 'карточка не помечалась как устаревшая.',

    // The card a marker opens.
    at: 'Время позиции',
    online: 'В сети',
    mode: 'Режим',
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
    foundPretended: 'Другое приложение на устройстве',
    foundUnsaid: 'Источник не назван',

    // Сводка под именем: то, ради чего карточку открывают чаще всего, —
    // как далеко участник, когда была его позиция и когда устройство было в сети,
    // сколько заряда. Расстояние меряется от самого свежего места своих
    // устройств; когда мерить не от чего, его нет.
    fromYou: (distance) => `${distance} от вас`,
    positionAgo: (age) => `позиция ${age}`,
    onlineAgo: (age) => `в сети ${age}`,

    // Предупреждения одним абзацем, и только пока они верны: устройство на
    // связи, а новое место не принято полчаса и дольше; позиции получают
    // отказ; приёмник не устоялся после включения и отвечает узко и неверно.
    falsePlaceLead: 'Позиция, скорее всего, ложная:',
    falsePlaceAndAfter: (count) => `Эта позиция и ещё ${count} после неё — в`,
    falsePlaceWhere: 'известной области подмены',
    stuckSentence: (age) => `В сети ${age}, но новое место не принято.`,
    leftOutLead: (answers) => `Отброшено: ${answers}, причина:`,
    settlingSentence: 'Приёмник не устоялся после включения устройства.',

    // Остальное — по кнопке: времена, режим, источник, точность, координаты.
    moreDetails: 'Подробнее',
    fewerDetails: 'Свернуть',

    // Сколько позиций получили отказ с последней записанной и почему.
    leftOutAnswers: (count) => `${count} ${counted(count, 'позиция', 'позиции', 'позиций')}`,
    leftOutDrifted: 'Увод при лежащем устройстве',
    leftOutHurried: 'Скорость, с которой устройство не носили',
    leftOutContradicted: 'От вышек другая позиция',
    leftOutWidth: 'Слишком большая погрешность',
    leftOutSpeed: 'Нереальная скорость',
    leftOutContradiction: 'Две одновременные позиции',
    leftOutFalsePlace: 'Известная область подмены',
    leftOutUnmoved: 'Устройство не двигали',
    leftOutCircling: 'Трек по кругу',
    leftOutOneBand: 'Позиция без ГЛОНАСС',
    leftOutOneTransmitter: 'Сигнал не с орбиты',
    leftOutTimeApart: 'Время спутников сместилось',
    leftOutOffOrbit: 'Спутники не на своих орбитах',
    leftOutRepeating: 'Трек повторяется',
    leftOutSettling: 'Приёмник не устоялся',
    leftOutBySource: 'Источник выключен',
    leftOutBySourceNamed: (source) => `Источник выключен: ${source}`,

    // The help the rejection reason on a card opens.
    reasonHelpTitle: 'Почему точка отброшена',
    reasonHelpIntro:
        'Устройство проверяет каждую позицию несколькими правилами и отказывает ей, как только '
        + 'сработало первое. Отказ не значит поломку: в районе с подменой сигнала отброшенных '
        + 'точек больше, чем принятых, и это правильная работа.',
    reasonHelpSign: 'Признак',
    reasonHelpMeans: 'О чём говорит',
    reasonSignWidth: 'Устройство само назвало радиус, в котором оно где-то находится, и радиус больше '
        + 'полукилометра.',
    reasonMeansWidth: 'Такая точка показывает не место, а район целиком. Чаще всего так отвечают '
        + 'вышки, когда устройство в помещении.',
    reasonSignSpeed: 'Чтобы попасть из прошлой точки в эту за прошедшее время, пришлось бы ехать '
        + 'быстрее 200 км/ч.',
    reasonMeansSpeed: 'Одна из двух точек ложная. Подмена почти всегда бросает метку далеко в '
        + 'сторону, а не сдвигает её понемногу.',
    reasonSignContradiction: 'На один и тот же момент времени пришли две разные позиции.',
    reasonMeansContradiction: 'Источник противоречит сам себе. Так бывает, когда рядом работает '
        + 'передатчик подмены и приёмник мечется между ним и спутниками.',
    reasonSignFalsePlace: 'Точка попала в область, которую вы отметили как место, куда подмена '
        + 'уводит устройства.',
    reasonMeansFalsePlace: 'Метку туда ставит передатчик, а не спутники. Области задаются на '
        + 'группу, поэтому их достаточно отметить один раз с любого устройства.',
    reasonSignUnmoved: 'Датчики движения говорят, что устройство никуда не несли, а позиция '
        + 'сменилась.',
    reasonMeansUnmoved: 'Сдвинуть метку у неподвижного устройства может только ложный сигнал: шаги '
        + 'и поездки считает отдельный чип, до которого подмена не достаёт.',
    reasonSignDrifted: 'Устройство лежит на месте, а точки понемногу уходят в сторону, каждая '
        + 'недалеко от предыдущей.',
    reasonMeansDrifted: 'Медленный увод. Шаг в тридцать метров не ловится сравнением соседних '
        + 'точек, но за час метка оказывается в другом месте, поэтому счёт идёт от места, где '
        + 'устройство положили.',
    reasonSignHurried: 'Устройство никто не трогал, а ответ говорит, что оно едет со скоростью '
        + 'автомобиля.',
    reasonMeansHurried: 'Приёмник сообщает свою оценку скорости вместе с каждой позицией, и у '
        + 'лежащего устройства она равна нулю. Ложный сигнал рисует дорогу и скорость вдоль неё, и '
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
    reasonSignRepeating: 'Устройство уже проходило ровно этот набор точек раньше.',
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
        + 'продвинулось не на столько же, на сколько собственные часы устройства.',
    reasonMeansTimeApart: 'Устройство считает секунды с момента включения, и до этого счёта не '
        + 'дотянуться ничем переданным по радио. Сигнал, который уводит устройство в другое место, '
        + 'вынужден сдвинуть переносимое им время — сдвиг здесь и виден.',
    reasonSignOffOrbit: 'Из места, где устройство по вышкам и Wi-Fi, слышимый спутник находится '
        + 'под горизонтом, или его сигнал пришёл на сотни километров раньше или позже, чем '
        + 'позволяет расстояние до него.',
    reasonMeansOffOrbit: 'Где находится каждый спутник в каждый момент, ежедневно публикуют '
        + 'станции по всему миру, и ничто рядом с устройством до этих данных не дотянется. '
        + 'Сигналы, которые с этими орбитами не сходятся, сделаны для другого места.',
    reasonSignSettling: 'Первые пять минут после включения устройства, пока решение строится меньше '
        + 'чем на четырёх спутниках.',
    reasonMeansSettling: 'В эти минуты приёмник выдаёт места, где устройство никогда не было. Точки '
        + 'этого окна проверяются строже обычного: шире ста метров или быстрее 5 м/с — отказ.',
    reasonSignSource: 'Вы выключили этот источник позиции в настройках приложения на устройстве.',
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
    // The menu the follow button opens: what each mode does, and how the
    // member followed is picked, which is not in the menu itself.
    followMenuNone: 'Не следовать',
    followMenuNoneNote: 'Карта стоит на месте',
    followMenuOne: 'За участником',
    followMenuOneNamed: (name) => `За участником: ${name}`,
    followMenuOneNote: 'Карта держит в центре выбранного участника',
    followMenuAll: 'За всеми',
    followMenuAllNote: 'Все участники в кадре',
    followMenuHint: 'Чтобы выбрать, за кем следовать, нажмите на его карточку внизу или на метку на карте.',
    followWait: 'Выберите участника: нажмите на его карточку или метку на карте.',

    // Who else has the map open right now, named rather than counted.
    mapOpen: (names) => `Карту смотрят: ${names.join(', ')}`,
    thisPage: 'вы (эта страница)',

    // Ages, which are the only words with a plural in them.
    justNow: 'только что',
    ago: (count, unit) => `${count} ${counted(count, ...AGES[unit])} назад`,

    // What the sign-in screen says when it did not happen.
    signInOffline: 'Нет связи. Войдите ещё раз, когда сеть появится.',
    signInMisconfigured: 'Эта страница пока не настроена для входа.',
    signInFailed: 'Войти не удалось. Попробуйте ещё раз.',

    // The words index.html is written with.
    signInExplanation: 'Войдите в тот аккаунт Google, под которым вы делитесь геопозицией с группой.',
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

    // The window the language moved to, and the button that gives the map the
    // whole screen.
    settings: 'Настройки',
    fullScreen: 'Во весь экран',
    fullScreenLeave: 'Выйти из полноэкранного режима',

    // The group: who is in it, and what the person looking may do about it.
    group: 'Группа',
    groupNone: 'Вы пока не в группе. Создайте её и раздайте приглашение или вставьте '
        + 'приглашение, которое вам прислали.',
    groupFound: 'Создать группу',
    groupNameLabel: 'Название группы',
    groupInvitation: 'Ссылка-приглашение',
    groupJoin: 'Вступить',
    groupJoinForeignSite: 'Это приглашение в группу другого проекта. Откройте саму ссылку: '
        + 'вступить по ней можно только на её странице.',
    groupJoinWrongAccount: (to, signedInAs) => `Приглашение для ${to}, а вы вошли как `
        + `${signedInAs}. Войдите аккаунтом, для которого приглашение.`,
    groupMakeInvitation: 'Пригласить нового участника в группу',
    groupMakeAnotherInvitation: 'Создать другое приглашение',
    groupInviteTitle: (name) => `Пригласить в группу «${name}»`,
    groupInviteEmailLabel: 'Адрес Google участника',
    groupInviteNote: 'Вступить сможет только тот, кто войдёт в приложение с этим адресом. '
        + 'Приглашение действует сутки.',
    groupInviteCreate: 'Создать',
    groupInvitationTitle: (address) => `Приглашение для ${address}`,
    groupInvitationNote: (until) => `Действует до ${until}. Отправьте ссылку участнику: она `
        + 'подключает приложение к проекту группы и впускает в группу.',
    groupShare: 'Отправить',
    groupCopy: 'Скопировать',
    groupCopied: 'Приглашение скопировано',
    groupRunsIt: 'админ',
    groupRemove: 'Удалить из группы',
    groupMakeAdmin: 'Сделать админом',
    groupStopRunning: 'Снять роль админа',
    groupGiveUpRunning: 'Снять с себя роль админа? Вернуть её сможет только другой админ.',
    groupRemoveTitle: (name) => `Удалить из группы: ${name}?`,
    groupRemoveNote:
        'Участник перестанет видеть группу, а группа — его. ' +
        'Вернуться в группу можно будет только по новому приглашению.',
    groupRemoveGo: 'Удалить',
    sirenHint: 'Сирена на 22 секунды',
    myPhonesTitle: 'Мои устройства',
    myPhonesNone: 'С этого аккаунта ещё ни одно устройство не делилось позицией',
    phoneRemove: 'Удалить устройство',
    helpOpen: 'Справка',
    helpClose: 'Понятно',
    helpTitleScreen: 'Кнопки на этом экране',
    helpTitleMap: 'Кнопки на карте',
    helpSiren: 'Сирена на 22 секунды: устройство громко звучит, даже если на нём выключен звук, — чтобы его нашли',
    helpSearch: 'Открыть поиск устройства: вся группа увидит его на карте, даже те, кому оно не открыто',
    helpSearchStanding: 'Поиск устройства идёт. Нажмите, когда его нашли',
    helpRemove: 'Удалить устройство: оно исчезнет с карты и из списков у всех участников',
    helpReset: 'Сбросить место: устройство забудет последнее место и примет следующее, когда метка застряла',
    helpFollowNone: 'Карта стоит на месте',
    helpFollowOne: 'Карта держит в центре выбранного участника',
    helpFollowAll: 'Все участники в кадре',
    helpFullScreen: 'Карта во весь экран; нажмите ещё раз, чтобы выйти',
    helpThemeLight: 'В меню: светлое оформление',
    helpThemeDark: 'В меню: тёмное оформление',
    helpThemeSystem: 'В меню: оформление как в системе',
    phoneRemoveTitle: (name) => `Удалить «${name}»?`,
    phoneRemoveNote:
        'Устройство исчезнет с карты и из списков у всех участников группы. ' +
        'Имя, которое вы ему дали, тоже удалится. ' +
        'Если устройство включено, оно выйдет из группы и перестанет передавать место.',
    phoneRemoveGo: 'Удалить',
    sirenTitle: (member, phone) => `Включить сирену: ${member} · ${phone}?`,
    sirenPhoneChip: (name, age) => `${name} · ${age}`,
    sirenNote:
        'Устройство будет громко звучать 22 секунды, даже если на нём выключен звук. ' +
        'Выключить сирену можно на самом устройстве или здесь.',
    sirenGo: 'Включить',
    sirenStop: 'Выключить',
    sirenWaiting: (phone) => `сирена на ${phone} · ждём ответа устройства`,
    sirenSounding: (phone, seconds) => `сирена на ${phone} · ${seconds} с · устройство приняло`,
    sirenNoAnswer: (phone) => `${phone}: нет ответа на сирену`,
    sirenStopped: (phone) => `сирена на ${phone} выключена`,
    resetHint: 'Сбросить место',
    resetTitle: (phone) => `Сбросить место на устройстве ${phone}?`,
    resetNote:
        'Устройство забудет последнее место и примет следующее, даже если оно далеко от прежнего. ' +
        'Помогает, когда метка на карте застряла и не двигается.',
    resetGo: 'Сбросить',
    resetWaiting: (time) => `место сброшено в ${time} · ждём ответа устройства`,
    resetDone: (time) => `место сброшено в ${time} · устройство приняло`,
    resetNoAnswer: 'устройство не ответило на сброс места',
    groupUnrun: 'Группу никто не ведёт, её состав задаётся в консоли Firebase.',
    // Показывается, пока у группы нет собственного имени (ADR-0016).
    groupUnnamed: (name) => `Группа · ${name}`,
    groupRename: 'Переименовать',

    // The panel the member running the group opens.
    adminTitle: 'Управление группой',
    adminYou: 'это вы',
    adminNotShared: 'не делится с вами',
    adminPhonesShared: (count) => `${count} `
        + `${counted(count, 'устройство', 'устройства', 'устройств')} `
        + `${counted(count, 'доступно', 'доступны', 'доступны')} вам`,

    // What a refused write says.
    groupRefused: 'База отказала. Возможно, приглашение истекло или группа уже создана.',

    // Naming a phone or a person for oneself, from the card.
    namePhone: 'Переименовать устройство',
    namePerson: 'Переименовать человека',
    nameForYou: 'Это имя видите только вы.',
    nameSave: 'Сохранить',
    nameReset: 'Сбросить',
    nameCancel: 'Отмена',
    nameSaving: 'Сохраняем…',
    nameFailed: (reason) => `Имя не сохранено: ${reason}`,
    unnamedDevice: 'Устройство',

    // A phone in the panel that runs the group, and whether it is in touch.
    adminPhoneOnline: (name, age) => `${name}: на связи ${age}`,
    adminPhoneSilent: (name) => `${name}: ещё не выходил на связь`,
    adminPhoneCharging: 'на зарядке',
    // То же в окне «Мои устройства», где имя устройства стоит строкой выше.
    phoneOnline: (age) => `На связи ${age}`,
    phoneSilent: 'Ещё не выходило на связь',

    // Группа ищет устройство: администратор открывает поиск на нём, и пока
    // он идёт, место этого устройства видно всей группе.
    searchMark: 'Ищем',
    searchStart: 'Искать',
    searchFound: 'Нашли',
    searchRenew: 'Продлить',
    searchOpenTitle: (phoneName, memberName) =>
        `Показать всей группе, где устройство «${phoneName}» (${memberName})?`,
    searchRenewTitle: (phoneName, memberName) =>
        `Продлить поиск устройства «${phoneName}» (${memberName})?`,
    searchNote: 'Все участники группы увидят это устройство на карте, даже те, кому оно не '
        + 'открыто. На нём появится сообщение: кто открыл поиск и до какого времени.',
    searchHours: (hours) => ({ 6: '6 часов', 24: 'Сутки', 72: '3 суток' })[hours] ?? `${hours} ч`,
    searchUntil: (until) => `До ${until}. Снять раньше можно кнопкой «Нашли».`,
    searchGo: 'Открыть',
    searchCancel: 'Отмена',
    soughtUntil: (until) => `ищем до ${until}`,
    searchSummary: (phoneName, memberName, until) =>
        `${memberName}: устройство «${phoneName}», ищем до ${until}`,
    searchOpenedBy: (name) => `Открыли поиск: ${name}.`,
    searchOpenedByYou: 'Открыли вы.',
    soughtBanner: (phoneNames, until) =>
        `Вашу позицию (${phoneNames}) видит вся группа до ${until}`,
    soughtBannerNote: (names) =>
        `Открыли поиск: ${names}. Если с вами всё в порядке, закройте поиск.`,
    soughtFound: 'Меня нашли',
};
