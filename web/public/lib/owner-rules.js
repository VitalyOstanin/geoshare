/**
 * Binds the first founding of a group to the project owner (ADR-0038).
 *
 * `firebase/firestore.rules` is written for any project, so it cannot name
 * the account that owns one. Its `foundingAllowed()` carries a marker line
 * that lets every signed-in account found the group, as before; whoever
 * deploys the rules to a project, the setup page or `tools/project.sh`, puts
 * the owner's address in its place. Rules left as they are in the repository
 * keep working, and only a project they were not bound for stays open to
 * whoever signs in first.
 */

const MARKER = /^([ \t]*)return true; \/\/ @project-owner$/m;

// What a Google account is signed in with. Anything that could close the
// string literal in the rules, or add to it, is not an address.
const ADDRESS = /^[^\s'"\\@]+@[^\s'"\\@]+$/;

/**
 * Returns the rules with the founding bound to [email]. The address is the
 * one Google reports for the account that signed in to deploy: lower-cased
 * here, and compared with the lower-cased address in the token, which has to
 * be verified.
 */
export function rulesForOwner(rules, email) {
    const address = typeof email === 'string' ? email.trim().toLowerCase() : '';

    if (!ADDRESS.test(address)) {
        throw new Error('the project owner has no usable e-mail address');
    }

    if (!MARKER.test(rules)) {
        throw new Error('the rules have no project owner marker');
    }

    return rules.replace(
        MARKER,
        (_, indent) => `${indent}return request.auth.token.email_verified == true &&\n` +
            `${indent}       request.auth.token.email.lower() == '${address}';`,
    );
}
