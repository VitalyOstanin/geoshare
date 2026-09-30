// What the join page says in English.
//
// A dictionary of its own rather than a few more keys in words/en.js: this
// page shows neither the map nor the sign-in screen, only what a person
// needs before either — what the link connects to, and who it might be from.
export const JOIN_EN = {
    // A link with no invitation in it, only the project the app is pointed
    // at.
    joinTitle: 'Connect to geoshare',
    joinText: (site) => `This link connects the app to the project ${site}.`,

    // A link's invitation, captioned from what it carries. Both names on it
    // are optional, so the title falls back a step at a time rather than
    // showing a blank.
    joinInvited: (by, group) => `${by} invited you to the group “${group}”`,
    joinInvitedNoBy: (group) => `You have been invited to the group “${group}”`,
    joinInvitedNoGroup: (by) => `${by} invited you to a group`,
    joinInvitedBare: 'Invitation to a group',
    joinInvitedFor: (address) =>
        `The invitation is for ${address}: sign in to the app with this Google account.`,

    // What the two buttons do.
    joinOpen: 'Open in the app',
    joinDownload: 'Download the app',

    // Said once the link has gone to the clipboard instead of opening the
    // app: there is no app to open on a browser that is not Android's.
    joinLinkCopied: 'Link copied — open it on an Android device.',
};
