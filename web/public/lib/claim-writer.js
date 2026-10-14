/**
 * Writes and withdraws a claim, keeping the writes to one per refresh.
 *
 * Apart from the database so that it can be tried without one: `write` and
 * `remove` are the calls that reach it, and both return a promise.
 *
 * A write the server has not answered yet is waited for rather than repeated.
 * Without a network such a write never answers, and every check would queue
 * another one, all of them sent together when the network is back.
 */
export function claimWriter(write, remove, onError, refreshMs, now = Date.now) {
    // When a push last reached the server, never at first; a failed one leaves
    // it where it was, so the next check tries again rather than the next
    // refresh.
    let pushed = -Infinity;
    let pending = null;

    const push = () => {
        if (pending) {
            return pending;
        }

        pending = write()
            .then(() => {
                pushed = now();
            })
            .catch(onError)
            .finally(() => {
                pending = null;
            });

        return pending;
    };

    return {
        push,
        pushWhenDue: () => {
            if (now() - pushed >= refreshMs) {
                push();
            }
        },
        withdraw: () => remove().catch(onError),
    };
}
