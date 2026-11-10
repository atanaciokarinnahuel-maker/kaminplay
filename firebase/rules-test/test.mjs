// Checks firebase/database.rules.json against the Realtime Database emulator.
// Run from the firebase/ folder:  npm --prefix rules-test install && npm --prefix rules-test test
import { readFileSync } from "node:fs";
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { get, push, ref, serverTimestamp, set, update } from "firebase/database";

const env = await initializeTestEnvironment({
  projectId: "demo-kaminplay",
  database: { rules: readFileSync("database.rules.json", "utf8"), host: "127.0.0.1", port: 9000 },
});
await env.clearDatabase();

const alice = env.authenticatedContext("alice").database();
const bob = env.authenticatedContext("bob").database();
const carol = env.authenticatedContext("carol").database();
const anon = env.unauthenticatedContext().database();
const ROOM = "rooms/KM7P2X";

const meta = (uid) => ({ filmId: "detour-1945", createdBy: uid, createdAt: serverTimestamp() });
const member = (name, online = true) => ({ name, online, lastSeen: serverTimestamp() });
const playback = (uid, playing = false, positionMs = 0) => ({ playing, positionMs, updatedAt: serverTimestamp(), by: uid, reason: "user" });
const message = (uid, text = "hola") => ({ uid, name: uid, text, at: serverTimestamp() });

let passed = 0;
const check = async (label, assertion) => {
  try { await assertion; passed++; console.log("ok   ", label); }
  catch (error) { console.log("FAIL ", label, "-", error.message); process.exitCode = 1; }
};

// Creating a room, in the order the app does it.
await check("signed-out users cannot read a room's meta", assertFails(get(ref(anon, `${ROOM}/meta`))));
await check("signed-out users cannot create a room", assertFails(set(ref(anon, `${ROOM}/meta`), meta("alice"))));
await check("a member entry cannot be written before the room exists", assertFails(set(ref(alice, `${ROOM}/members/alice`), member("Alice"))));
await check("a code outside the alphabet is rejected", assertFails(set(ref(alice, "rooms/abc/meta"), meta("alice"))));
await check("meta cannot name someone else as creator", assertFails(set(ref(alice, `${ROOM}/meta`), meta("bob"))));
await check("creator writes meta", assertSucceeds(set(ref(alice, `${ROOM}/meta`), meta("alice"))));
await check("playback cannot be written before joining", assertFails(set(ref(alice, `${ROOM}/playback`), playback("alice"))));
await check("creator adds herself as member", assertSucceeds(set(ref(alice, `${ROOM}/members/alice`), member("Alice"))));
await check("creator writes the initial playback state", assertSucceeds(set(ref(alice, `${ROOM}/playback`), playback("alice"))));
await check("meta cannot be overwritten, even by its creator", assertFails(set(ref(alice, `${ROOM}/meta`), meta("alice"))));
await check("meta cannot be overwritten by someone else", assertFails(set(ref(bob, `${ROOM}/meta`), meta("bob"))));

// Someone who has the code but has not joined yet.
await check("anyone signed in can check that a code exists", assertSucceeds(get(ref(bob, `${ROOM}/meta`))));
await check("non-members cannot read the room", assertFails(get(ref(bob, ROOM))));
await check("non-members cannot read playback", assertFails(get(ref(bob, `${ROOM}/playback`))));
await check("non-members cannot read messages", assertFails(get(ref(bob, `${ROOM}/messages`))));
await check("non-members cannot control playback", assertFails(set(ref(bob, `${ROOM}/playback`), playback("bob", true))));
await check("non-members cannot post messages", assertFails(set(push(ref(bob, `${ROOM}/messages`)), message("bob"))));
await check("nobody can add another person as member", assertFails(set(ref(bob, `${ROOM}/members/carol`), member("Carol"))));
await check("the list of rooms cannot be read", assertFails(get(ref(bob, "rooms"))));

// Joining and using the room.
await check("joining writes your own member entry", assertSucceeds(update(ref(bob, `${ROOM}/members/bob`), member("Bob"))));
await check("members can read the whole room", assertSucceeds(get(ref(bob, ROOM))));
await check("any member can control playback", assertSucceeds(set(ref(bob, `${ROOM}/playback`), playback("bob", true, 61500))));
await check("the other member can take over", assertSucceeds(set(ref(alice, `${ROOM}/playback`), playback("alice", false, 70000))));
await check("playback cannot be signed with another uid", assertFails(set(ref(bob, `${ROOM}/playback`), playback("alice", true))));
await check("playback needs the server timestamp", assertFails(set(ref(bob, `${ROOM}/playback`), { ...playback("bob"), updatedAt: 12345 })));
await check("playback position cannot be negative", assertFails(set(ref(bob, `${ROOM}/playback`), playback("bob", true, -5))));
await check("members can post messages", assertSucceeds(set(push(ref(bob, `${ROOM}/messages`)), message("bob"))));
await check("messages cannot be signed with another uid", assertFails(set(push(ref(bob, `${ROOM}/messages`)), message("alice"))));
await check("empty messages are rejected", assertFails(set(push(ref(bob, `${ROOM}/messages`)), message("bob", ""))));
await check("messages over 500 characters are rejected", assertFails(set(push(ref(bob, `${ROOM}/messages`)), message("bob", "x".repeat(501)))));
await check("going offline updates your own entry", assertSucceeds(update(ref(bob, `${ROOM}/members/bob`), member("Bob", false))));
await check("you cannot mark someone else offline", assertFails(update(ref(bob, `${ROOM}/members/alice`), { online: false })));
await check("a member entry needs a name", assertFails(set(ref(carol, `${ROOM}/members/carol`), { online: true })));
await check("a third person with the code can join", assertSucceeds(update(ref(carol, `${ROOM}/members/carol`), member("Carol"))));

console.log(`\n${passed} checks passed${process.exitCode ? ", with failures" : ""}`);
await env.cleanup();
