const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read user profiles or conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("conversations").get());
});

test("Authenticated user: can create, read, and update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice Wonderland",
    email: "alice@example.com",
    dailyMessageCount: 0,
    quotaResetEpochDay: 20000,
    createdAt: now,
    updatedAt: now,
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    dailyMessageCount: 1,
    updatedAt: now,
  }));
});

test("Cross-user isolation: Bob cannot read or write Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const now = new Date();
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      dailyMessageCount: 0,
      quotaResetEpochDay: 20000,
      createdAt: now,
      updatedAt: now,
    });
    await context.firestore().collection("users").doc(ALICE_UID).collection("conversations").doc("c1").set({
      userId: ALICE_UID,
      title: "Secret Chat",
      modelId: "gemini-3.8-flash",
      isPinned: false,
      isArchived: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("conversations").doc("c1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("conversations").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("conversations").doc("c1").update({
    title: "Hacked",
    updatedAt: new Date(),
  }));
});

test("Authenticated user: can create conversation and message in own hierarchy", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1").set({
    userId: ALICE_UID,
    title: "Exploring Kotlin",
    modelId: "gemini-3.8-flash",
    isPinned: false,
    isArchived: false,
    temperature: 0.7,
    maxOutputTokens: 2048,
    createdAt: now,
    updatedAt: now,
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1").collection("messages").doc("msg_1").set({
    userId: ALICE_UID,
    conversationId: "conv_1",
    role: "user",
    content: "Explain coroutines",
    createdAt: now,
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("conversations").doc("conv_1").collection("messages").get());
});
