"use strict";

const assert = require("node:assert/strict");
const {after, before, beforeEach, test} = require("node:test");
const {deleteApp, initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {
  assertRecentLogin,
  bearerTokenFrom,
  deleteUserData,
} = require("../account-deletion");

const PROJECT_ID = "whenwheremeet-security-test";
const DELETE_UID = "delete-user";
const OTHER_UID = "other-user";
const HOST_ROOM_ID = "DELETE-HOST-ROOM";
const MEMBER_ROOM_ID = "DELETE-MEMBER-ROOM";
let app;
let firestore;

before(async () => {
  assert.ok(process.env.FIRESTORE_EMULATOR_HOST, "Firestore Emulator에서 실행해야 합니다.");
  app = initializeApp({projectId: PROJECT_ID}, "account-deletion-test");
  firestore = getFirestore(app);
});

beforeEach(async () => {
  await clearFirestoreEmulator();
  await seedAccountData();
});

after(async () => {
  await clearFirestoreEmulator();
  await deleteApp(app);
});

test("최근 로그인과 Bearer 토큰을 검증한다", () => {
  assert.equal(bearerTokenFrom({get: () => "Bearer sample-token"}), "sample-token");
  assert.doesNotThrow(() => assertRecentLogin({
    auth_time: 10_000,
    firebase: {sign_in_provider: "google.com"},
  }, 10_100));
  assert.throws(() => assertRecentLogin({
    auth_time: 1,
    firebase: {sign_in_provider: "google.com"},
  }, 10_000), /다시 로그인/);
  assert.throws(() => assertRecentLogin({
    auth_time: 10_000,
    firebase: {sign_in_provider: "anonymous"},
  }, 10_100), /Google 또는 Apple/);
});

test("회원 탈퇴 시 호스트 방과 본인 참가 데이터, 위치, 친구, 알림 토큰, Auth 계정을 삭제한다", async () => {
  const deletedAuthUsers = [];
  await deleteUserData({
    firestore,
    auth: {deleteUser: async (uid) => deletedAuthUsers.push(uid)},
    uid: DELETE_UID,
  });

  assert.deepEqual(deletedAuthUsers, [DELETE_UID]);
  assert.equal((await firestore.doc(`meetingRooms/${HOST_ROOM_ID}`).get()).exists, false);
  assert.equal((await firestore.doc(`roomCodes/${HOST_ROOM_ID}`).get()).exists, false);
  assert.equal((await firestore.doc(`meetingRooms/${HOST_ROOM_ID}/startLocations/delete-host-participant`).get()).exists, false);

  const memberRoom = await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}`).get();
  assert.equal(memberRoom.exists, true);
  assert.equal(memberRoom.data().participantCount, 1);
  assert.equal((await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/participants/delete-member-participant`).get()).exists, false);
  assert.equal((await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/participants/other-host-participant`).get()).exists, true);
  assert.equal((await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/availabilities/delete-member-participant-2026-08-21`).get()).exists, false);
  assert.equal((await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/startLocations/delete-member-participant`).get()).exists, false);
  assert.equal((await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/startLocationSummaries/delete-member-participant`).get()).exists, false);
  assert.equal((await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/members/${DELETE_UID}`).get()).exists, false);
  assert.equal((await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/nicknameKeys/탈퇴자`).get()).exists, false);

  assert.equal((await firestore.doc(`users/${DELETE_UID}/notificationDevices/device-1`).get()).exists, false);
  assert.equal((await firestore.doc(`users/${DELETE_UID}/friends/${OTHER_UID}`).get()).exists, false);
  assert.equal((await firestore.doc(`users/${OTHER_UID}/friends/${DELETE_UID}`).get()).exists, false);
  assert.equal((await firestore.doc("meetingFriendConnections/connection-1").get()).exists, false);
});

async function seedAccountData() {
  await firestore.doc(`meetingRooms/${HOST_ROOM_ID}`).set(roomData(
    HOST_ROOM_ID,
    DELETE_UID,
    "delete-host-participant",
    1,
  ));
  await firestore.doc(`roomCodes/${HOST_ROOM_ID}`).set({code: HOST_ROOM_ID, roomId: HOST_ROOM_ID});
  await firestore.doc(`meetingRooms/${HOST_ROOM_ID}/participants/delete-host-participant`).set({
    id: "delete-host-participant",
    roomId: HOST_ROOM_ID,
    nickname: "방장",
    authUid: DELETE_UID,
    isHost: true,
  });
  await firestore.doc(`meetingRooms/${HOST_ROOM_ID}/startLocations/delete-host-participant`).set({
    participantId: "delete-host-participant",
    latitude: 37.123456,
    longitude: 127.123456,
  });

  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}`).set(roomData(
    MEMBER_ROOM_ID,
    OTHER_UID,
    "other-host-participant",
    2,
  ));
  await firestore.doc(`roomCodes/${MEMBER_ROOM_ID}`).set({code: MEMBER_ROOM_ID, roomId: MEMBER_ROOM_ID});
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/participants/other-host-participant`).set({
    id: "other-host-participant",
    roomId: MEMBER_ROOM_ID,
    nickname: "다른방장",
    authUid: OTHER_UID,
    isHost: true,
  });
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/participants/delete-member-participant`).set({
    id: "delete-member-participant",
    roomId: MEMBER_ROOM_ID,
    nickname: "탈퇴자",
    authUid: DELETE_UID,
    isHost: false,
  });
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/members/${DELETE_UID}`).set({
    authUid: DELETE_UID,
    participantId: "delete-member-participant",
  });
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/availabilities/delete-member-participant-2026-08-21`).set({
    participantId: "delete-member-participant",
  });
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/startLocations/delete-member-participant`).set({
    participantId: "delete-member-participant",
    latitude: 37.654321,
    longitude: 127.654321,
  });
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/startLocationSummaries/delete-member-participant`).set({
    participantId: "delete-member-participant",
    approximateLatitude: 37.65,
    approximateLongitude: 127.65,
  });
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/destinationStationProposals/delete-member-participant`).set({
    participantId: "delete-member-participant",
  });
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/destinationStationVotes/delete-member-participant`).set({
    participantId: "delete-member-participant",
  });
  await firestore.doc(`meetingRooms/${MEMBER_ROOM_ID}/nicknameKeys/탈퇴자`).set({
    participantId: "delete-member-participant",
  });

  await firestore.doc(`users/${DELETE_UID}/notificationDevices/device-1`).set({token: "token"});
  await firestore.doc(`users/${DELETE_UID}/friends/${OTHER_UID}`).set({userId: OTHER_UID});
  await firestore.doc(`users/${OTHER_UID}/friends/${DELETE_UID}`).set({userId: DELETE_UID});
  await firestore.doc("meetingFriendConnections/connection-1").set({
    roomId: MEMBER_ROOM_ID,
    userIds: [DELETE_UID, OTHER_UID],
  });
}

function roomData(roomId, hostAuthUid, hostParticipantId, participantCount) {
  return {
    id: roomId,
    hostAuthUid,
    hostParticipantId,
    participantCount,
    updatedAt: "2026-08-20T10:00:00+09:00",
  };
}

async function clearFirestoreEmulator() {
  const response = await fetch(
    `http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`,
    {method: "DELETE"},
  );
  assert.equal(response.ok, true, await response.text());
}
