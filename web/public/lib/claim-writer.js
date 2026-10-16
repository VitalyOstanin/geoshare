/**
 * Writes and withdraws a claim, keeping the writes to one per refresh.
 *
 * Apart from the database so that it can be tried without one: `write` and
 * `remove` are the calls that reach it, and both return a promise.
 *
 * A write the server has not answered yet is waited for rather than repeated.
 * Without a network such a write never answers, and every check would queue
 * another one, all of them sent together when the network is back.
 *
 * A withdrawal ends the claim the unanswered write belongs to. A push after it
 * waits for that write and then makes its own, since the answer to the old one
 * says nothing about the label the withdrawal removed.
 */
export function claimWriter(write, remove, onError, refreshMs, now = Date.now) {
    // When a push last reached the server, never at first; a failed one leaves
    // it where it was, so the next check tries again rather than the next
    // refresh.
    let pushed = -Infinity;
    let pending = null;
    // Counts the withdrawals, so that a write started before one is told apart.
    let generation = 0;
    let pendingGeneration = 0;

    const push = () => {
        if (pending) {
            if (pendingGeneration === generation) {
                return pending;
            }

            // Made only if no withdrawal came while the old write was waited for.
            const wanted = generation;

            return pending.then(() => (wanted === generation ? push() : undefined));
        }

        const started = generation;

        pendingGeneration = started;
        pending = write()
            .then(() => {
                // An answer to a write made before a withdrawal must not
                // postpone the next one: the label is gone.
                if (started === generation) {
                    pushed = now();
                }
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
        withdraw: () => {
            generation++;
            pushed = -Infinity;

            return remove().catch(onError);
        },
    };
}
