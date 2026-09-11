// What the security rules let each side do.
//
// Run through tools/rules.sh, which starts the Firestore emulator around this
// file. The emulator needs no project of our own: a project id starting with
// "demo-" never reaches the real service.
import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, it } from "node:test";

import {
    assertFails,
    assertSucceeds,
    initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { deleteDoc, doc, getDoc, setDoc } from "firebase/firestore";

const GROUP = "family";
const ALICE = "alice-uid";
const BOB = "bob-uid";
const STRANGER = "stranger-uid";

let testEnv;

const positionOf = (db, uid) => doc(db, `groups/${GROUP}/positions/${uid}`);
const watchOf = (db, uid) => doc(db, `groups/${GROUP}/watch/${uid}`);
const memberOf = (db, uid) => doc(db, `groups/${GROUP}/members/${uid}`);

const somePosition = {
    lat: 60.93,
    lon: 4.34,
    accuracy: 12,
    ts: new Date(),
    battery: 80,
    mode: "standby",
};

before(async () => {
    testEnv = await initializeTestEnvironment({
        projectId: "demo-geoshare",
        firestore: {
            rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8"),
        },
    });
});

after(async () => {
    await testEnv.cleanup();
});

// The group document itself is written by hand from the console, so the tests
// plant it the same way: with the rules out of the way.
beforeEach(async () => {
    await testEnv.clearFirestore();
    await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();

        await setDoc(doc(db, `groups/${GROUP}`), { members: [ALICE, BOB] });
        await setDoc(memberOf(db, ALICE), { name: "Alice", colour: "#1e88e5" });
        await setDoc(memberOf(db, BOB), { name: "Bob", colour: "#43a047" });
        await setDoc(positionOf(db, BOB), somePosition);
    });
});

describe("positions", () => {
    it("a member reads another member's position", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(positionOf(db, BOB)));
    });

    it("a member writes their own position", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(positionOf(db, ALICE), somePosition));
    });

    it("a member cannot write another member's position", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(positionOf(db, BOB), somePosition));
    });

    it("a stranger reads nothing", async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    it("a signed out client reads nothing", async () => {
        const db = testEnv.unauthenticatedContext().firestore();

        await assertFails(getDoc(positionOf(db, BOB)));
    });

    it("a stranger cannot write a position of their own", async () => {
        const db = testEnv.authenticatedContext(STRANGER).firestore();

        await assertFails(setDoc(positionOf(db, STRANGER), somePosition));
    });
});

describe("watch", () => {
    it("a member marks themselves as watching", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(setDoc(watchOf(db, ALICE), { until: new Date() }));
    });

    it("a member reads another member's marker", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(watchOf(db, BOB)));
    });

    it("a member cannot forge another member's marker", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(watchOf(db, BOB), { until: new Date() }));
    });

    // Closing the map deletes the marker, so the delete has to pass the same
    // check as the write.
    it("a member clears their own marker", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await setDoc(watchOf(db, ALICE), { until: new Date() });

        await assertSucceeds(deleteDoc(watchOf(db, ALICE)));
    });
});

describe("membership", () => {
    it("a member reads the roster", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertSucceeds(getDoc(memberOf(db, BOB)));
    });

    // The roster and the group document are maintained from the console. A
    // phone that could edit them could add itself to the group.
    it("a member cannot edit the roster", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(memberOf(db, ALICE), { name: "Someone else" }));
    });

    it("a member cannot add anybody to the group", async () => {
        const db = testEnv.authenticatedContext(ALICE).firestore();

        await assertFails(setDoc(doc(db, `groups/${GROUP}`), { members: [ALICE, BOB, STRANGER] }));
    });
});
