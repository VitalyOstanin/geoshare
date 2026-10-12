// Everything the page says in English.
//
// The application keeps its wording in strings.xml, and the page keeps one
// file per language. One source per language is what makes the two halves
// possible to keep in step, and what makes a second language a file to write
// rather than a search across seven modules.
//
// The words of index.html are here too, and the markup carries them as well:
// the markup is what the first paint shows, and a viewer reading in the other
// language gets these a moment later, once the modules have run.
export const EN = {
    // The strip under the map, and what stands in for it when it is empty.
    you: 'You',
    signedIn: 'Signed in',
    nobodyYet: 'Nobody has shared a position yet.',
    looking: 'Looking for the group…',
    unreachable: 'No connection to the group. Trying again.',
    linkLost: (since) => `No connection to the database since ${since}`,
    pausedSince: (since) => `Map paused since ${since}`,
    pausedNote: 'Nobody has looked at it for 5 minutes. Markers are grey. Touch the map or move the mouse to refresh.',
    linkKnownAt: (time) => `Showing what was known at ${time}. Markers are grey.`,
    linkKnownNever: 'The database has not answered yet. Markers are grey.',
    knownAt: (time) => `as of ${time}`,
    linkLostAction: 'No connection to the database: this would not reach the device. Try again once the connection is back.',
    unanswered: 'The database did not answer in 15 seconds. The write goes out once the connection is back; it cannot be called off.',
    outsideGroup: 'You are not in a group yet. The account menu makes one or joins one by '
        + 'invitation.',
    nobodySharing: 'Nobody is sharing a position with you right now.',
    oneMoreRefused: 'One more member is not sharing with you right now.',
    moreRefused: (count) => `${count} more members are not sharing with you right now.`,

    // The names of the five ways a device collects its position. Names rather
    // than sentences describing them: what each one asks for and how often it
    // sends belongs in the help the card opens, where all five can be read
    // against each other. The document carries the entry of the enumeration,
    // which is the key of the lookup rather than anything shown, so a
    // translation names the modes in its own language.
    modeIdle: 'Idle',
    modeStandby: 'Standby',
    modeMoving: 'Moving',
    modeViewing: 'Viewing',
    modeNight: 'Night',

    // The help the mode on a card opens. All five are described in one place
    // rather than a sentence at a time on the cards: what a mode means is only
    // clear against the other four, and a member reads it once.
    modeHelpTitle: 'How a device collects its position',
    modeHelpIntro:
        'The device chooses one of these five by itself. Somebody with the map open outranks '
        + 'everything else; the rest of the time it spends as little as it can and still be found.',
    modeHelpWhen: 'When',
    modeHelpAsks: 'What the device does',
    close: 'Close',
    modeWhenViewing: 'Somebody has the map open.',
    modeAsksViewing: 'Asks for the most exact position every 30 seconds, and writes it as soon as '
        + 'the marker would move.',
    modeWhenMoving: 'The device noticed walking, running, cycling or riding.',
    modeAsksMoving: 'Asks every 5 minutes.',
    modeWhenStandby: 'Away from where it last wrote, with nobody looking.',
    modeAsksStandby: 'Asks every 15 minutes.',
    modeWhenNight: 'Between 23:00 and 07:00, or the charge is below 15 per cent.',
    modeAsksNight: 'Asks every 20 minutes, in the cheapest way it has.',
    modeWhenIdle: 'Settled within 150 metres of the position it last wrote.',
    modeAsksIdle: 'Asks for nothing at all, and repeats the last position every 25 minutes so the '
        + 'card does not go grey.',

    // The card a marker opens.
    at: 'Taken at',
    online: 'Online',
    mode: 'Mode',
    position: 'Position',
    unknown: 'unknown',

    // How the device arrived at the answer, and how wide it called it. The two
    // rows stand together: a word without a width does not separate the flat
    // from the district, and a width without the word does not say whether the
    // circle was measured off the sky or looked up in somebody's database.
    foundBy: 'Found by',
    width: 'Width',
    foundSatellites: 'The satellites',
    foundAssistance: 'Towers and Wi-Fi',
    foundUnheard: 'Satellites with no measurements',
    foundOtherRadios: 'Towers and Wi-Fi',
    foundPretended: 'Another application on the device',
    foundUnsaid: 'Source not stated',

    // The summary under the name: what a card is opened for most often — how
    // far the member is, when their position was taken and when the device was
    // last online, how much charge is left. The distance is measured from the
    // freshest place of the viewer's own devices, and is missing when there is
    // nothing to measure from.
    fromYou: (distance) => `${distance} from you`,
    positionAgo: (age) => `position ${age}`,
    onlineAgo: (age) => `online ${age}`,

    // The warnings as one paragraph, each only while it is true: the device is
    // online and no new position has been accepted for half an hour or more;
    // positions are being rejected; the receiver has not settled since the
    // device was switched on and answers narrowly and wrongly.
    falsePlaceLead: 'The position is most likely false:',
    falsePlaceAndAfter: (count) => `This position and ${count} more after it are in`,
    falsePlaceWhere: 'a known spoofed area',
    stuckSentence: (age) => `Online ${age}, but no new location has been accepted.`,
    leftOutLead: (answers) => `Rejected ${answers}:`,
    settlingSentence: 'The receiver had not settled since the device was switched on.',

    // The rest behind a button: the two moments, mode, source, width, position.
    moreDetails: 'More details',
    fewerDetails: 'Fewer details',

    // How many answers were rejected since the last position written, and why.
    leftOutAnswers: (count) => (count === 1 ? '1 answer' : `${count} answers`),
    leftOutDrifted: 'Drift while the device lay still',
    leftOutHurried: 'A speed the device was never carried at',
    leftOutContradicted: 'The towers give another position',
    leftOutWidth: 'Too much doubt in the answer',
    leftOutSpeed: 'An impossible speed',
    leftOutContradiction: 'Two positions at one time',
    leftOutFalsePlace: 'A known spoofed area',
    leftOutUnmoved: 'The device was not moved',
    leftOutCircling: 'The track goes round a circle',
    leftOutOneBand: 'A fix without GLONASS',
    leftOutOneTransmitter: 'The signal is not from orbit',
    leftOutTimeApart: 'The satellites\' time stepped',
    leftOutOffOrbit: 'Satellites off their orbits',
    leftOutRepeating: 'The track repeats itself',
    leftOutSettling: 'The receiver has not settled',
    leftOutBySource: 'The source is switched off',
    leftOutBySourceNamed: (source) => `Source switched off: ${source}`,

    // The help the rejection reason on a card opens.
    reasonHelpTitle: 'Why a position is rejected',
    reasonHelpIntro:
        'The device holds every position against several rules and rejects it as soon as the '
        + 'first one speaks. A rejection is not a fault: where the signal is spoofed there are '
        + 'more rejected answers than accepted ones, and that is the work being done properly.',
    reasonHelpSign: 'What was seen',
    reasonHelpMeans: 'What it means',
    reasonSignWidth: 'The device named the radius it is somewhere inside, and the radius is over '
        + 'half a kilometre.',
    reasonMeansWidth: 'Such an answer names a district rather than a place. It is what the towers '
        + 'give while the device is indoors.',
    reasonSignSpeed: 'Reaching this place from the last one in the time between them would have '
        + 'taken over 200 km/h.',
    reasonMeansSpeed: 'One of the two is false. A spoof throws the marker far off rather than '
        + 'easing it along.',
    reasonSignContradiction: 'Two different positions arrived carrying the same moment.',
    reasonMeansContradiction: 'The source is contradicting itself, which is what happens when a '
        + 'receiver is caught between a transmitter on the ground and the satellites.',
    reasonSignFalsePlace: 'The answer landed in an area you marked as somewhere the spoof puts '
        + 'devices.',
    reasonMeansFalsePlace: 'A transmitter puts the marker there, not the satellites. The areas '
        + 'belong to the group, so marking one from any device is enough.',
    reasonSignUnmoved: 'The movement sensors say the device was not carried anywhere, and the '
        + 'position changed.',
    reasonMeansUnmoved: 'Only a false signal moves the marker of a device lying still: steps and '
        + 'journeys are counted by a chip of their own that a transmitter cannot reach.',
    reasonSignDrifted: 'The device is lying where it was put down, and the answers walk away from '
        + 'it a little at a time.',
    reasonMeansDrifted: 'A slow drift. A step of thirty metres never trips a comparison between '
        + 'neighbours, but within an hour the marker is somewhere else, so the distance is '
        + 'counted from where the device was put down.',
    reasonSignHurried: 'The device has not been touched, and the answer says it is travelling at '
        + 'the speed of a car.',
    reasonMeansHurried: 'The receiver reports a speed of its own alongside every position, and on '
        + 'a device lying still that speed is zero. A false signal draws a road and a speed along '
        + 'it, and the speed is inside the single answer — so this catches the first false '
        + 'position rather than the one after it.',
    reasonSignContradicted: 'The position from the satellites and the position from the towers '
        + 'and the Wi-Fi are kilometres apart.',
    reasonMeansContradicted: 'The towers are harder to imitate than the satellites. This is the '
        + 'one rule that does not compare an answer with an earlier one, so it catches both a '
        + 'slow drift and the very first false answer.',
    reasonSignCircling: 'The answers go round a closed circle at a steady speed.',
    reasonMeansCircling: 'Every stretch of such a track reads as ordinary driving. What gives it '
        + 'away is that the road closes, which is how a transmitter draws one and not how '
        + 'anybody drives.',
    reasonSignRepeating: 'The device has been through this same run of places before.',
    reasonMeansRepeating: 'A recorded false route is being played through again.',
    reasonSignOneBand: 'The fix rests on the satellites of the 1575 MHz band — GPS, Galileo, '
        + 'BeiDou — while GLONASS is heard and left out of it.',
    reasonMeansOneBand: 'A spoofing transmitter works in one band. GLONASS broadcasts at 1602 '
        + 'MHz, out of its reach, so the receiver could not reconcile it with the satellites it '
        + 'was handed.',
    reasonSignOneTransmitter: 'Every satellite arrives at the same strength however low it claims '
        + 'to be, and the far band is silent.',
    reasonMeansOneTransmitter: 'A real sky grows quieter the lower the satellite. One strength '
        + 'for all of them means one point on the ground.',
    reasonSignTimeApart: 'Between two epochs the time the satellites carry advanced by a '
        + 'different amount than the device\'s own clock did.',
    reasonMeansTimeApart: 'The device counts the seconds since it was switched on, and nothing '
        + 'transmitted can reach that count. A signal built to move the device has to move the '
        + 'time it carries, and the step shows up here.',
    reasonSignOffOrbit: 'Seen from where the towers and Wi-Fi place the device, a satellite it '
        + 'hears is below the horizon, or its signal arrived hundreds of kilometres sooner or '
        + 'later than its distance allows.',
    reasonMeansOffOrbit: 'Where every satellite is at every moment is published daily by '
        + 'stations around the world, out of reach of anything near the device. Signals that do '
        + 'not fit those orbits were made up for some other place.',
    reasonSignSettling: 'The first five minutes after the device was switched on, while the answer '
        + 'rests on fewer than four satellites.',
    reasonMeansSettling: 'In those minutes the receiver reports places the device has never been. '
        + 'Answers of that window are held to tighter limits: wider than a hundred metres, or '
        + 'faster than 5 m/s, and they are rejected.',
    reasonSignSource: 'You switched this source of a position off in the settings.',
    reasonMeansSource: 'Nothing is wrong with the answer: it is collected and logged, but it '
        + 'moves no marker and reaches nobody. That is what the switch is for.',

    // How wide the device said its answer is. The sign is what turns a distance
    // into a claim about where the marker might really be.
    widthMetres: (count) => `${count} m`,
    widthKilometres: (count) => `${count} km`,
    backToList: '← Back to the list',
    oneMember: '1 member',
    members: (count) => `${count} members`,

    // The follow button over the map, which cycles between not following,
    // one member and everybody. The "one" caption is the member's own name
    // rather than a word here, worked out on the page from who was chosen.
    followNone: 'Not following',
    followOne: (name) => `Following ${name}`,
    followAll: 'Following everyone',
    followTargetMissing: 'No current position for this member',
    followMenuNone: 'Don’t follow',
    followMenuNoneNote: 'The map stays where it is',
    followMenuOne: 'Follow a member',
    followMenuOneNamed: (name) => `Follow ${name}`,
    followMenuOneNote: 'Keeps the chosen member in the middle',
    followMenuAll: 'Follow everyone',
    followMenuAllNote: 'Everyone in view',
    followMenuHint: 'To choose whom to follow, tap their card below or their marker on the map.',
    followWait: 'Choose a member: tap their card or their marker on the map.',

    // Who else has the map open right now, named rather than counted, and said
    // once for the household rather than on a card: one open map puts every
    // device of the group into the same way of collecting, so naming it per
    // member would repeat the same list under every face.
    mapOpen: (names) => `Map open: ${names.join(', ')}`,
    thisPage: 'you (this page)',

    // Ages, which are the only words with a plural in them.
    justNow: 'just now',
    ago: (count, unit) => `${count} ${count === 1 ? unit.slice(0, -1) : unit} ago`,

    // What the sign-in screen says when it did not happen. The same four cases
    // the application tells apart.
    signInOffline: 'No connection. Sign in again once the network is back.',
    signInMisconfigured: 'This page is not set up for sign-in yet.',
    signInFailed: 'Sign-in did not go through. Try again.',

    // The words index.html is written with. Kept here as well as there so that
    // a page read in the other language has them: the markup is the first
    // paint, and these replace it as soon as the modules run.
    signInExplanation: 'Sign in with the Google account you share your position with the group under.',
    signInAction: 'Sign in with Google',
    signInWaiting: 'Signing in…',
    signOut: 'Sign out',
    appearance: 'Appearance',
    themeLight: 'Light',
    themeDark: 'Dark',
    themeSystem: 'System',

    // The language, written in the language it stands for: a viewer looking
    // for their own reads it before they read anything else on the page.
    language: 'Language',
    languageRussian: 'Русский',
    languageEnglish: 'English',
    languageSystem: 'System',

    // The window the language moved to, and the button that gives the map the
    // whole screen.
    settings: 'Settings',
    fullScreen: 'Full screen',
    fullScreenLeave: 'Leave full screen',

    // The group: who is in it, and what the person looking may do about it.
    // An invitation is a link (ADR-0017, ADR-0019) naming the group as well
    // as the code, or an older "group.code" string kept working for a link
    // made before invitations were links at all — either way, because
    // somebody joining has to reach a document nobody has named to them yet.
    group: 'Group',
    groupNone: 'You are not in a group yet. Make one and hand out an invitation, '
        + 'or paste in the invitation somebody sent you.',
    groupFound: 'Make a group',
    groupNameLabel: 'Group name',
    groupInvitation: 'Invitation link',
    groupJoin: 'Join',
    groupJoinForeignSite: 'This invitation is into another project\'s group. Open the link '
        + 'itself: it can only be used on its own page.',
    groupJoinWrongAccount: (to, signedInAs) => `This invitation is for ${to}, but you are `
        + `signed in as ${signedInAs}. Sign in with the account the invitation is for.`,
    groupMakeInvitation: 'Invite a new member to the group',
    groupMakeAnotherInvitation: 'Make another invitation',
    groupInviteTitle: (name) => `Invite to “${name}”`,
    groupInviteEmailLabel: "Member's Google address",
    groupInviteNote: 'Only whoever signs into the application with this address will be able '
        + 'to join. The invitation lasts a day.',
    groupInviteCreate: 'Create',
    groupInvitationTitle: (address) => `Invitation for ${address}`,
    groupInvitationNote: (until) => `Good until ${until}. Send the link to the member: it `
        + 'connects the application to the group\'s project and lets them into the group.',
    groupShare: 'Send',
    groupCopy: 'Copy',
    groupCopied: 'Invitation copied',
    groupRunsIt: 'admin',
    groupRemove: 'Remove from the group',
    groupMakeAdmin: 'Make admin',
    groupStopRunning: 'Remove admin role',
    groupGiveUpRunning: 'Give up your admin role? Only another admin can give it back.',
    groupRemoveTitle: (name) => `Remove ${name} from the group?`,
    groupRemoveNote:
        'They will no longer see the group, nor the group them. ' +
        'Only a new invitation brings them back.',
    groupRemoveGo: 'Remove',
    sirenHint: 'Siren for 22 seconds',
    myPhonesTitle: 'My devices',
    myPhonesNone: 'No device has shared its position from this account yet',
    phoneRemove: 'Remove device',
    helpOpen: 'Help',
    helpClose: 'Got it',
    helpTitleScreen: 'Buttons on this screen',
    helpTitleMap: 'Buttons on the map',
    helpSiren: 'Siren for 22 seconds: the device sounds loud, even with its sound off, so that it can be found',
    helpSearch: 'Open a search for the device: the whole group sees it on the map, even those it was never shared with',
    helpSearchStanding: 'A search for the device is on. Press it once it is found',
    helpRemove: 'Remove the device: it disappears from the map and the lists of everybody',
    helpReset: 'Reset the place: the device forgets its last place and takes the next one, when its marker is stuck',
    helpFollowNone: 'The map stays where it is',
    helpFollowOne: 'The map keeps the chosen member in the middle',
    helpFollowAll: 'Everybody in view',
    helpFullScreen: 'The map on the whole screen; press again to leave',
    helpThemeLight: 'In the menu: light appearance',
    helpThemeDark: 'In the menu: dark appearance',
    helpThemeSystem: 'In the menu: appearance as in the system',
    phoneRemoveTitle: (name) => `Remove “${name}”?`,
    phoneRemoveNote:
        'The device will disappear from the map and the lists of everybody in the group. ' +
        'The name you gave it goes too. ' +
        'If the device is on, it leaves the group and stops sharing its location.',
    phoneRemoveGo: 'Remove',
    sirenTitle: (member, phone) => `Switch on the siren: ${member} · ${phone}?`,
    sirenPhoneChip: (name, age) => `${name} · ${age}`,
    sirenNote:
        'The device will sound loudly for 22 seconds, even with its sound off. ' +
        'The siren can be stopped on the device itself or here.',
    sirenGo: 'Switch on',
    sirenStop: 'Stop',
    sirenWaiting: (phone) => `siren on ${phone} · waiting for the device`,
    sirenSounding: (phone, seconds) => `siren on ${phone} · ${seconds} s · the device answered`,
    sirenNoAnswer: (phone) => `${phone} did not answer the siren`,
    sirenStopped: (phone) => `siren on ${phone} stopped`,
    resetHint: 'Reset the place',
    resetTitle: (phone) => `Reset the place on ${phone}?`,
    resetNote:
        'The device forgets its last place and takes the next one, however far it is from the old one. ' +
        'It helps when a marker on the map is stuck and does not move.',
    resetGo: 'Reset',
    resetWaiting: (time) => `place reset at ${time} · waiting for the device`,
    resetDone: (time) => `place reset at ${time} · the device took it`,
    resetNoAnswer: 'the device did not answer the place reset',
    groupUnrun: 'Nobody runs this group, so its members are set from the Firebase console.',
    // Shown until a group is given a name of its own (ADR-0016).
    groupUnnamed: (name) => `${name}'s group`,
    groupRename: 'Rename',

    // The panel the member running the group opens: the household seen whole,
    // and the two acts that are running it — handing out an invitation and
    // taking somebody off the roster.
    adminTitle: 'Running the group',
    adminYou: 'you',
    // Said of a member whose devices this viewer may not read. Their devices are
    // named in their grant, which is closed to everybody they have not shared
    // with, so this is what is known here rather than what they own.
    adminNotShared: 'not sharing with you',
    adminPhonesShared: (count) => `${count} ${count === 1 ? 'device' : 'devices'} shared with you`,

    // What a refused write says. The rules weigh several things at once — a
    // code that has run out, a group already made, a member the last
    // administrator cannot be taken from — and which of them it was is not
    // something a refusal says.
    groupRefused: 'The database refused that. The invitation may have run out, or the group '
        + 'may already exist.',

    // Naming a device or a person for oneself, from the card.
    namePhone: 'Rename device',
    namePerson: 'Rename person',
    nameForYou: 'Only you see this name.',
    nameSave: 'Save',
    nameReset: 'Reset',
    nameCancel: 'Cancel',
    nameSaving: 'Saving…',
    nameFailed: (reason) => `The name was not saved: ${reason}`,
    unnamedDevice: 'Device',

    // A device in the panel that runs the group, and whether it is in touch.
    adminPhoneOnline: (name, age) => `${name}: in touch ${age}`,
    adminPhoneSilent: (name) => `${name}: not in touch yet`,
    adminPhoneCharging: 'charging',
    // The same in My devices, where the device's name is the line above.
    phoneOnline: (age) => `In touch ${age}`,
    phoneSilent: 'Not in touch yet',

    // The group looking for a phone: an administrator opens a search on it,
    // and while it stands everybody in the group sees where that phone is.
    searchMark: 'Searching',
    searchStart: 'Search',
    searchFound: 'Found',
    searchRenew: 'Extend',
    searchOpenTitle: (phoneName, memberName) =>
        `Show the whole group where the device "${phoneName}" (${memberName}) is?`,
    searchRenewTitle: (phoneName, memberName) =>
        `Extend the search for the device "${phoneName}" (${memberName})?`,
    searchNote: 'Everybody in the group will see this device on the map, even those it is not '
        + 'shared with. It will say who opened the search and until when.',
    searchHours: (hours) => ({ 6: '6 hours', 24: 'A day', 72: '3 days' })[hours] ?? `${hours} h`,
    searchUntil: (until) => `Until ${until}. "Found" ends it sooner.`,
    searchGo: 'Open',
    searchCancel: 'Cancel',
    soughtUntil: (until) => `searching until ${until}`,
    searchSummary: (phoneName, memberName, until) =>
        `${memberName}: device "${phoneName}", searching until ${until}`,
    searchOpenedBy: (name) => `Opened by ${name}.`,
    searchOpenedByYou: 'Opened by you.',
    soughtBanner: (phoneNames, until) =>
        `Everybody in the group sees where you are (${phoneNames}) until ${until}`,
    soughtBannerNote: (names) => `Opened by ${names}. If you are all right, end the search.`,
    soughtFound: "I'm found",
};
