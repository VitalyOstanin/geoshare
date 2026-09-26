// Whether this browser was signed in when it was last closed, put on the
// element before the first paint.
//
// The account comes back on its own, but only once the modules have loaded and
// the library has read its own storage, and until then the page used to offer
// a sign-in button to somebody already signed in. Where the page remembers a
// sign-in it shows that it is waiting instead, and app.js settles it one way
// or the other on the first answer. The key is spelled out here as well as in
// app.js because this file cannot import anything.
try {
    if (localStorage.getItem('geoshare.signedIn') === 'yes') {
        document.documentElement.dataset.signIn = 'waiting';

        // A page whose modules never arrive — no network, a blocked script —
        // would otherwise wait for good without a way to sign in.
        setTimeout(() => {
            delete document.documentElement.dataset.signIn;
        }, 15000);
    }
} catch {
    // A browser keeping no site data remembers no sign-in either, and the
    // button is what it gets.
}
