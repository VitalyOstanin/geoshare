// What tools/web-config.sh writes next to this file, with the values of the
// project taken out. The real one is not in the repository: it names the
// project, which is not the repository's business.
export const firebaseConfig = {
    projectId: 'the-project-id',
    appId: 'the-web-app-id',
    storageBucket: 'the-project-id.firebasestorage.app',
    apiKey: 'the-web-api-key',
    authDomain: 'the-project-id.firebaseapp.com',
    messagingSenderId: 'the-sender-id',
    // Optional, for a trial stand or snapshots against the Firebase emulators;
    // the project id must then start with `demo-`.
    // emulators: { auth: 'http://127.0.0.1:9099', firestore: { host: '127.0.0.1', port: 8080 } },
};
