const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_esim_123";
const BOB_UID = "bob_esim_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validEsimPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    label: "Japan Voyager 5G",
    carrierName: "Sakura Mobile",
    countryCode: "JP",
    countryName: "Japan",
    planType: "TRAVEL",
    status: "ACTIVE",
    iccid: "8981000012345678901",
    smdpAddress: "smdp.esimstudio.net",
    activationCode: "LPA-JP-9F4A-2201",
    apn: "sakura.5g",
    mccMnc: "44010",
    dataLimitMb: 10240,
    dataUsedMb: 1280,
    validityDays: 30,
    roamingEnabled: true,
    fiveGEnabled: true,
    unlimitedData: false,
    colorAccent: "CYAN",
    notes: "Tokyo & Kyoto trip",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user cannot read or write virtual eSIMs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/esims`).get());
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/esims`)
      .doc("esim_1")
      .set(validEsimPayload(ALICE_UID))
  );
});

test("Owner can create, list, partial-update, and delete valid virtual eSIMs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/esims`).doc("esim_jp_1");

  await assertSucceeds(docRef.set(validEsimPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/esims`).orderBy("createdAt", "desc").get()
  );

  // Action 1: status / usage / radio partial update
  await assertSucceeds(
    docRef.update({
      status: "STANDBY",
      dataUsedMb: 2048,
      roamingEnabled: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action 2: profile metadata edit partial update
  await assertSucceeds(
    docRef.update({
      label: "Japan Business 5G",
      apn: "biz.sakura.5g",
      notes: "Updated APN",
      updatedAt: new Date(Date.now() - 250),
    })
  );

  await assertSucceeds(docRef.delete());
});

test("Cross-user read, list, create, update, and delete are denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection(`users/${BOB_UID}/esims`)
      .doc("bob_sim_1")
      .set(validEsimPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/esims`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/esims`).doc("bob_sim_1").get());
  await assertFails(
    aliceDb
      .collection(`users/${BOB_UID}/esims`)
      .doc("bob_sim_1")
      .update({ status: "EXPIRED", updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(
    aliceDb
      .collection(`users/${BOB_UID}/esims`)
      .doc("alice_inject")
      .set(validEsimPayload(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/esims`).doc("bob_sim_1").delete()
  );
});

test("Rejects ghost fields, @DocumentId id field, invalid ICCID, immutable createdAt/userId mutation, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection(`users/${ALICE_UID}/esims`);

  // @DocumentId `id` key rejected
  await assertFails(col.doc("sim_bad_1").set(validEsimPayload(ALICE_UID, { id: "sim_bad_1" })));

  // Ghost field rejected
  await assertFails(col.doc("sim_bad_2").set(validEsimPayload(ALICE_UID, { isAdmin: true })));

  // Invalid ICCID (non-digits or too short) rejected
  await assertFails(col.doc("sim_bad_3").set(validEsimPayload(ALICE_UID, { iccid: "ABC123" })));

  // Future timestamp rejected
  await assertFails(
    col
      .doc("sim_bad_4")
      .set(validEsimPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Create valid doc and try mutating immutable createdAt or iccid
  await assertSucceeds(col.doc("sim_good").set(validEsimPayload(ALICE_UID)));
  await assertFails(
    col.doc("sim_good").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    col.doc("sim_good").update({
      iccid: "8981000012345678909",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
