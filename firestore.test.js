const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
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

function validTaskPayload(userId = ALICE_UID) {
  return {
    userId,
    title: "Grocery Shopping",
    description: "Buy milk and eggs",
    priority: "high",
    status: "todo",
    category: "Personal",
    isCompleted: false,
    tags: ["shopping", "home"],
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

test("Unauthenticated user: cannot read tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
});

test("Unauthenticated user: cannot create task", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID))
  );
});

test("Authenticated user: can create a valid task in their own collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID))
  );
});

test("Authenticated user: cannot create task in another user's collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("task_1").set(validTaskPayload(BOB_UID))
  );
});

test("Authenticated user: cannot create task with mismatched userId inside payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTaskPayload(BOB_UID))
  );
});

test("Authenticated user: cannot create task with invalid priority", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const invalidPayload = { ...validTaskPayload(ALICE_UID), priority: "super_critical" };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(invalidPayload)
  );
});

test("Authenticated user: cannot create task with ghost field (shadow update test)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ghostPayload = { ...validTaskPayload(ALICE_UID), isHacked: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(ghostPayload)
  );
});

test("Authenticated user: can list and read own tasks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get());
});

test("Authenticated user: cannot read another user's task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("tasks").doc("task_bob").set(validTaskPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("task_bob").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").get());
});

test("Authenticated user: can update own task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").update({
      title: "Updated Grocery Shopping",
      status: "in_progress",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot update immutable createdAt or userId", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").update({
      userId: BOB_UID,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can delete own task but not another user's task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_alice").set(validTaskPayload(ALICE_UID));
    await context.firestore().collection("users").doc(BOB_UID).collection("tasks").doc("task_bob").set(validTaskPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("task_bob").delete());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_alice").delete());
});
