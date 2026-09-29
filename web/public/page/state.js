// What every part of the page shares: the elements it draws into and its state.

import { NO_LABELS } from '../lib/labels.js';

/** Where the page remembers a sign-in; boot-sign-in.js spells it out too. */
export const SIGNED_IN_KEY = 'geoshare.signedIn';

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
export const AGE_TICK_MS = 30 * 1000;

export const elements = {
    signIn: document.querySelector('#sign-in'),
    signInExplanation: document.querySelector('#sign-in-explanation'),
    signInButton: document.querySelector('#sign-in-button'),
    signInWaiting: document.querySelector('#sign-in-waiting-text'),
    signInError: document.querySelector('#sign-in-error'),
    map: document.querySelector('#map'),
    card: document.querySelector('#card'),
    followButton: document.querySelector('#follow-button'),
    followCaption: document.querySelector('#follow-caption'),
    followMenu: document.querySelector('#follow-menu'),
    followMenuButtons: document.querySelectorAll('#follow-menu button'),
    followHint: document.querySelector('#follow-hint'),
    fullScreenButton: document.querySelector('#fullscreen-button'),
    followWait: document.querySelector('#follow-wait'),
    panel: document.querySelector('#panel'),
    panelNote: document.querySelector('#panel-note'),
    panelWatching: document.querySelector('#panel-watching'),
    members: document.querySelector('#members'),
    account: document.querySelector('#account'),
    accountButton: document.querySelector('#account-button'),
    accountPicture: document.querySelector('#account-picture'),
    accountInitial: document.querySelector('#account-initial'),
    accountMenu: document.querySelector('#account-menu'),
    accountName: document.querySelector('#account-name'),
    signOutButton: document.querySelector('#sign-out-button'),
    themeLabel: document.querySelector('#theme-label'),
    themeButtons: document.querySelectorAll('#theme-choice button'),
    languageLabel: document.querySelector('#language-label'),
    languageButtons: document.querySelectorAll('#language-choice button'),
    settings: document.querySelector('#settings'),
    settingsButton: document.querySelector('#settings-button'),
    settingsTitle: document.querySelector('#settings-title'),
    settingsClose: document.querySelector('#settings-close'),
    groupLabel: document.querySelector('#group-label'),
    groupNote: document.querySelector('#group-note'),
    groupMembers: document.querySelector('#group-members'),
    groupFound: document.querySelector('#group-found'),
    groupAdmin: document.querySelector('#group-admin'),
    groupPhones: document.querySelector('#group-phones'),
    phones: document.querySelector('#phones'),
    phonesClose: document.querySelector('#phones-close'),
    phonesHelp: document.querySelector('#phones-help'),
    help: document.querySelector('#help'),
    helpTitle: document.querySelector('#help-title'),
    helpList: document.querySelector('#help-list'),
    helpClose: document.querySelector('#help-close'),
    mapHelp: document.querySelector('#map-help'),
    phonesTitle: document.querySelector('#phones-title'),
    phonesList: document.querySelector('#phones-list'),
    groupJoin: document.querySelector('#group-join'),
    groupTyped: document.querySelector('#group-typed'),
    groupJoinButton: document.querySelector('#group-join-button'),
    groupSaid: document.querySelector('#group-said'),
    groupError: document.querySelector('#group-error'),
    admin: document.querySelector('#admin'),
    adminClose: document.querySelector('#admin-close'),
    adminHelp: document.querySelector('#admin-help'),
    adminTitle: document.querySelector('#admin-title'),
    adminMembers: document.querySelector('#admin-members'),
    adminInvitation: document.querySelector('#admin-invitation'),
    adminCode: document.querySelector('#admin-code'),
    adminCodeLasts: document.querySelector('#admin-code-lasts'),
    adminMakeInvitation: document.querySelector('#admin-make-invitation'),
    adminCopy: document.querySelector('#admin-copy'),
    adminSaid: document.querySelector('#admin-said'),
    adminError: document.querySelector('#admin-error'),
    naming: document.querySelector('#naming'),
    namingTitle: document.querySelector('#naming-title'),
    namingNote: document.querySelector('#naming-note'),
    namingTyped: document.querySelector('#naming-typed'),
    namingSave: document.querySelector('#naming-save'),
    namingReset: document.querySelector('#naming-reset'),
    namingCancel: document.querySelector('#naming-cancel'),
    adminSearches: document.querySelector('#admin-searches'),
    search: document.querySelector('#search'),
    searchTitle: document.querySelector('#search-title'),
    searchNote: document.querySelector('#search-note'),
    searchHours: document.querySelector('#search-hours'),
    searchUntil: document.querySelector('#search-until'),
    searchError: document.querySelector('#search-error'),
    searchGo: document.querySelector('#search-go'),
    searchCancel: document.querySelector('#search-cancel'),
    siren: document.querySelector('#siren'),
    sirenTitle: document.querySelector('#siren-title'),
    sirenPhones: document.querySelector('#siren-phones'),
    sirenNote: document.querySelector('#siren-note'),
    sirenError: document.querySelector('#siren-error'),
    sirenGo: document.querySelector('#siren-go'),
    sirenCancel: document.querySelector('#siren-cancel'),
    confirm: document.querySelector('#confirm'),
    confirmTitle: document.querySelector('#confirm-title'),
    confirmNote: document.querySelector('#confirm-note'),
    confirmGo: document.querySelector('#confirm-go'),
    confirmCancel: document.querySelector('#confirm-cancel'),
    soughtBanner: document.querySelector('#sought-banner'),
    soughtText: document.querySelector('#sought-text'),
    soughtNote: document.querySelector('#sought-note'),
    soughtFound: document.querySelector('#sought-found'),
};

export const state = {
    member: null,
    positions: [],
    // Null until the first answer arrives, which is what tells "nobody has a
    // profile" apart from "the answer is still on its way". Writing before it
    // lands would put the same profile in again on every visit.
    profiles: null,
    // The profiles as the members wrote them, before this viewer's own names
    // for them are put in: the profile this viewer publishes is compared with
    // their own words, not with what they call themselves here.
    accountProfiles: null,
    // What this viewer calls the phones and the people, for themselves.
    labels: NO_LABELS,
    stopLabels: null,
    // The name being asked for: which of the two maps, and under what key.
    naming: null,
    // What the members give out, by member: the phones to subscribe to, and
    // nothing at all for a member who shares with somebody else.
    grants: new Map(),
    // The positions as the database holds them, before what their phones said
    // about themselves since is laid over them.
    written: [],
    view: null,
    stopPositions: null,
    // The documents the current position subscriptions cover, joined into one
    // string: the profiles change for reasons that leave the devices alone,
    // and rebuilding the subscriptions for those would cost a read each time.
    positionDocuments: null,
    stopProfiles: null,
    stopGrants: null,
    // The members the current grant subscriptions cover, joined into one
    // string: a renamed account changes the profiles without changing who is
    // in the household.
    grantMembers: null,
    stopWatching: null,
    // The claims of the other members to be looking, as they arrived: the
    // names are worked out again on every draw, because a claim runs out with
    // the clock rather than with an answer from the database.
    watchers: [],
    // Whether this page itself is claiming to look, and so holding the
    // phones of the group in the viewing mode.
    pageLooking: false,
    stopWatchers: null,
    // How many of the documents the profiles name are being refused to us. A
    // refusal is a member no longer sharing, an expired permission or a rule
    // saying no, and the panel should say so rather than quietly lose a face.
    refused: 0,
    // Whether the group itself is out of reach: the profiles are what every
    // other subscription is built from, so their failure is the connection.
    reachable: true,
    // Whether the database refused the group outright, which is an account
    // on no roster rather than a connection gone.
    outsideGroup: false,
    // The name of whoever the follow button last had a target for. The map
    // only remembers a position id, which stops meaning anything the moment
    // that member goes quiet; the name is kept here so the button still has
    // something to say about them once their position drops out of the set.
    followTargetName: null,
    // The follow mode last drawn, to tell a change from a redrawing; null
    // before the first drawing after a sign-in.
    shownFollowMode: null,
    followAnnounceEnds: null,
    followWaitEnds: null,
    // Whose cards the strip highlighted on the last drawing, so that render
    // can tell a highlight that has actually changed from the same one drawn
    // again — the strip redraws on every clock tick regardless, and scrolling
    // it on every one of those would drag the view out from under a reader's
    // thumb for no reason at all.
    shownPositionIds: new Set(),
    // The strip's entries by position id, kept from one drawing to the next:
    // a clock tick changes an age and nothing else, and building every entry
    // again for that would throw away the elements and their listeners with
    // it.
    stripEntries: new Map(),
    // Who is in the group, or nothing at all — which is both a group that has
    // not been made and a group this account is not on, since neither can be
    // read.
    roster: null,
    stopRoster: null,
    // Where a false signal is known to put phones: a position there is drawn
    // grey and measured from by nobody.
    falsePlaces: [],
    stopFalsePlaces: null,
    // The invitation just made, held until the menu is closed. Nothing reads
    // it back from the database: nobody may read that collection at all.
    invitation: null,
    // What a refused write said, in a sentence. The rules weigh several things
    // at once and a refusal names none of them, so the page says what it can.
    groupTrouble: null,
    // What went right and is worth one line: the invitation reaching the
    // clipboard, which leaves nothing on screen to show for itself.
    groupSaid: null,
    // The group's searches by phone, as the database holds them, standing or
    // run out: which of them still stand is worked out against the clock.
    searches: new Map(),
    stopSearches: null,
    // The moment the next standing search runs out, when nothing arrives to
    // say so.
    searchEnds: null,
    // The search being opened or renewed in the window: which phone, whose,
    // the name it goes by, whether this renews a standing one, and the
    // length picked.
    searchAsk: null,
    // What the siren of every phone shown with a siren button says, followed
    // while the panel showing it is open: a call rung from another screen, or
    // before this page was loaded, is sounding all the same, and whoever may
    // ring it has to see so. The phones drawn with a button this time round,
    // the siren of each and the subscription that brings it.
    sirensShown: new Set(),
    sirens: new Map(),
    sirenWatches: new Map(),
    sirenTick: null,
    // The call being asked about in the window: whose, their phones, the one
    // picked.
    sirenAsk: null,
};
