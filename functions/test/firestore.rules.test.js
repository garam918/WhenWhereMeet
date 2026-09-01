"use strict";

const fs = require("node:fs");
const path = require("node:path");
const {after, before, beforeEach, test} = require("node:test");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} = require("@firebase/rules-unit-testing");
const {
  collection,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} = require("firebase/firestore");

const PROJECT_ID = "whenwheremeet-security-test";
const ROOM_ID = "ROOM01";
const HOST_UID = "host-user";
const MEMBER_UID = "member-user";
const OUTSIDER_UID = "outsider-user";
let testEnvironment;

before(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, "../../firestore.rules"), "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const firestore = context.firestore();
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID), roomData());
    await setDoc(doc(firestore, "roomCodes", ROOM_ID), {
      code: ROOM_ID,
      roomId: ROOM_ID,
      createdAt: "2026-08-20T10:00:00+09:00",
    });
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "participants", "host-participant"),
      participantData("host-participant", HOST_UID, true));
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "participants", "member-participant"),
      participantData("member-participant", MEMBER_UID, false));
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "members", HOST_UID),
      memberData(HOST_UID, "host-participant", "host"));
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "members", MEMBER_UID),
      memberData(MEMBER_UID, "member-participant", "participant"));
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "availabilities", "host-participant-2026-08-21"), {
      roomId: ROOM_ID,
      participantId: "host-participant",
      date: "2026-08-21",
      status: "AVAILABLE",
      updatedAt: "2026-08-20T10:00:00+09:00",
    });
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "startLocations", "host-participant"), {
      participantId: "host-participant",
      roomId: ROOM_ID,
      label: "서울역",
      address: "서울시 정확한 주소",
      latitude: 37.554722,
      longitude: 126.970833,
      privacyLevel: "EXACT_PRIVATE",
      updatedAt: "2026-08-20T10:00:00+09:00",
    });
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "startLocations", "member-participant"), {
      participantId: "member-participant",
      roomId: ROOM_ID,
      label: "강남역",
      address: "서울시 다른 정확한 주소",
      latitude: 37.497941,
      longitude: 127.027621,
      privacyLevel: "EXACT_PRIVATE",
      updatedAt: "2026-08-20T10:00:00+09:00",
    });
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "startLocationSummaries", "host-participant"),
      summaryData("host-participant", "서울역", 37.55, 126.97));
    await setDoc(doc(firestore, "meetingRooms", ROOM_ID, "startLocationSummaries", "member-participant"),
      summaryData("member-participant", "강남역", 37.50, 127.03));
  });
});

after(async () => {
  await testEnvironment.cleanup();
});

test("초대 코드는 방 단건 조회만 허용하고 외부인의 내부 데이터 열람은 거부한다", async () => {
  const firestore = authenticatedFirestore(OUTSIDER_UID);
  await assertSucceeds(getDoc(doc(firestore, "roomCodes", ROOM_ID)));
  await assertSucceeds(getDoc(doc(firestore, "meetingRooms", ROOM_ID)));
  await assertFails(getDocs(collection(firestore, "meetingRooms")));
  await assertFails(getDocs(collection(firestore, "meetingRooms", ROOM_ID, "participants")));
  await assertFails(getDocs(collection(firestore, "meetingRooms", ROOM_ID, "availabilities")));
  await assertFails(getDoc(doc(firestore, "meetingRooms", ROOM_ID, "startLocations", "host-participant")));
});

test("방 멤버도 다른 참가자의 정확한 위치는 읽지 못하고 요약 위치만 읽는다", async () => {
  const firestore = authenticatedFirestore(MEMBER_UID);
  await assertSucceeds(getDocs(collection(firestore, "meetingRooms", ROOM_ID, "participants")));
  await assertSucceeds(getDocs(collection(firestore, "meetingRooms", ROOM_ID, "availabilities")));
  await assertSucceeds(getDoc(doc(firestore, "meetingRooms", ROOM_ID, "startLocations", "member-participant")));
  await assertFails(getDoc(doc(firestore, "meetingRooms", ROOM_ID, "startLocations", "host-participant")));
  await assertSucceeds(getDocs(collection(firestore, "meetingRooms", ROOM_ID, "startLocationSummaries")));
});

test("날짜 응답은 본인만 쓰고 약속 확정은 방장만 할 수 있다", async () => {
  const memberFirestore = authenticatedFirestore(MEMBER_UID);
  await assertFails(setDoc(
    doc(memberFirestore, "meetingRooms", ROOM_ID, "availabilities", "host-participant-2026-08-22"),
    availabilityData("host-participant", "2026-08-22"),
  ));
  await assertSucceeds(setDoc(
    doc(memberFirestore, "meetingRooms", ROOM_ID, "availabilities", "member-participant-2026-08-22"),
    availabilityData("member-participant", "2026-08-22"),
  ));
  await assertFails(updateDoc(doc(memberFirestore, "meetingRooms", ROOM_ID), {
    status: "DATE_CONFIRMED",
    confirmedDate: "2026-08-22",
    updatedAt: "2026-08-20T11:00:00+09:00",
  }));

  const hostFirestore = authenticatedFirestore(HOST_UID);
  await assertSucceeds(updateDoc(doc(hostFirestore, "meetingRooms", ROOM_ID), {
    status: "DATE_CONFIRMED",
    confirmedDate: "2026-08-22",
    updatedAt: "2026-08-20T11:00:00+09:00",
  }));
});

test("공개 위치 요약에는 소수점 둘째 자리보다 정밀한 좌표를 저장할 수 없다", async () => {
  const firestore = authenticatedFirestore(MEMBER_UID);
  await assertFails(setDoc(
    doc(firestore, "meetingRooms", ROOM_ID, "startLocationSummaries", "member-participant"),
    summaryData("member-participant", "강남역", 37.4979, 127.0276),
  ));
  await assertSucceeds(setDoc(
    doc(firestore, "meetingRooms", ROOM_ID, "startLocationSummaries", "member-participant"),
    summaryData("member-participant", "강남역", 37.50, 127.03),
  ));
});

test("신규 참가자는 참가자와 멤버십 문서를 같은 원자적 쓰기로 생성해야 한다", async () => {
  const firestore = authenticatedFirestore(OUTSIDER_UID);
  const batch = writeBatch(firestore);
  batch.update(doc(firestore, "meetingRooms", ROOM_ID), {
    participantCount: 3,
    updatedAt: "2026-08-20T12:00:00+09:00",
  });
  batch.set(doc(firestore, "meetingRooms", ROOM_ID, "participants", "outsider-participant"),
    participantData("outsider-participant", OUTSIDER_UID, false));
  batch.set(doc(firestore, "meetingRooms", ROOM_ID, "members", OUTSIDER_UID),
    memberData(OUTSIDER_UID, "outsider-participant", "participant"));
  batch.set(doc(firestore, "meetingRooms", ROOM_ID, "nicknameKeys", "외부인"), {
    participantId: "outsider-participant",
  });
  await assertSucceeds(batch.commit());
  await assertSucceeds(getDocs(collection(firestore, "meetingRooms", ROOM_ID, "participants")));
});

test("방 생성 시 방장·초대 참가자의 멤버십을 원자적으로 함께 생성한다", async () => {
  const roomId = "NEWROOM";
  const invitedUid = "invited-user";
  const firestore = authenticatedFirestore(OUTSIDER_UID);
  const batch = writeBatch(firestore);
  batch.set(doc(firestore, "meetingRooms", roomId), {
    ...roomData(),
    id: roomId,
    hostParticipantId: "new-host-participant",
    hostAuthUid: OUTSIDER_UID,
    participantCount: 2,
  });
  batch.set(doc(firestore, "roomCodes", roomId), {
    code: roomId,
    roomId,
    createdAt: "2026-08-20T13:00:00+09:00",
  });
  batch.set(doc(firestore, "meetingRooms", roomId, "participants", "new-host-participant"), {
    ...participantData("new-host-participant", OUTSIDER_UID, true),
    roomId,
  });
  batch.set(doc(firestore, "meetingRooms", roomId, "members", OUTSIDER_UID),
    memberData(OUTSIDER_UID, "new-host-participant", "host"));
  batch.set(doc(firestore, "meetingRooms", roomId, "participants", "invited-participant"), {
    ...participantData("invited-participant", invitedUid, false),
    roomId,
    isInvited: true,
  });
  batch.set(doc(firestore, "meetingRooms", roomId, "members", invitedUid), {
    ...memberData(invitedUid, "invited-participant", "participant"),
    joined: false,
  });
  batch.set(doc(firestore, "meetingRooms", roomId, "nicknameKeys", "방장"), {
    participantId: "new-host-participant",
  });
  batch.set(doc(firestore, "meetingRooms", roomId, "nicknameKeys", "초대자"), {
    participantId: "invited-participant",
  });
  await assertSucceeds(batch.commit());
});

test("일반 참가자는 자신의 멤버십과 관련 데이터만 정리하며 방장은 나갈 수 없다", async () => {
  const memberFirestore = authenticatedFirestore(MEMBER_UID);
  const batch = writeBatch(memberFirestore);
  batch.update(doc(memberFirestore, "meetingRooms", ROOM_ID), {
    participantCount: 1,
    updatedAt: "2026-08-20T14:00:00+09:00",
  });
  batch.delete(doc(memberFirestore, "meetingRooms", ROOM_ID, "members", MEMBER_UID));
  batch.delete(doc(memberFirestore, "meetingRooms", ROOM_ID, "participants", "member-participant"));
  batch.delete(doc(memberFirestore, "meetingRooms", ROOM_ID, "startLocations", "member-participant"));
  batch.delete(doc(memberFirestore, "meetingRooms", ROOM_ID, "startLocationSummaries", "member-participant"));
  await assertSucceeds(batch.commit());

  const hostFirestore = authenticatedFirestore(HOST_UID);
  await assertFails(updateDoc(doc(hostFirestore, "meetingRooms", ROOM_ID), {
    participantCount: 0,
    updatedAt: "2026-08-20T14:00:00+09:00",
  }));
});

test("익명 인증 사용자는 방 코드조차 읽을 수 없다", async () => {
  const firestore = testEnvironment.authenticatedContext("anonymous-user", {
    firebase: {sign_in_provider: "anonymous"},
  }).firestore();
  await assertFails(getDoc(doc(firestore, "roomCodes", ROOM_ID)));
});

test("알림 기기는 본인 경로에 lastLoginAt을 포함한 허용 필드만 저장할 수 있다", async () => {
  const firestore = authenticatedFirestore(HOST_UID);
  const deviceData = {
    token: "fcm-token",
    platform: "android",
    lastLoginAt: "2026-09-01T12:00:00Z",
    updatedAt: "2026-09-01T12:00:00Z",
  };

  await assertSucceeds(setDoc(
    doc(firestore, "users", HOST_UID, "notificationDevices", "installation-id"),
    deviceData,
  ));
  await assertFails(setDoc(
    doc(firestore, "users", MEMBER_UID, "notificationDevices", "installation-id"),
    deviceData,
  ));
  await assertFails(setDoc(
    doc(firestore, "users", HOST_UID, "notificationDevices", "unexpected-field"),
    {...deviceData, exactLocation: "저장하면 안 되는 위치"},
  ));
});

test("소셜 로그인 사용자는 자신의 기본 회원 문서만 생성하고 읽을 수 있다", async () => {
  const firestore = authenticatedFirestore(HOST_UID);
  await assertSucceeds(setDoc(doc(firestore, "users", HOST_UID), userProfileData()));
  await assertSucceeds(getDoc(doc(firestore, "users", HOST_UID)));
  await assertFails(setDoc(doc(firestore, "users", MEMBER_UID), userProfileData()));

  const memberFirestore = authenticatedFirestore(MEMBER_UID);
  await assertFails(getDoc(doc(memberFirestore, "users", HOST_UID)));
});

test("회원 기본 정보는 허용된 필드만 저장하고 가입일은 변경할 수 없다", async () => {
  const firestore = authenticatedFirestore(HOST_UID);
  const userReference = doc(firestore, "users", HOST_UID);
  await assertSucceeds(setDoc(userReference, userProfileData()));
  await assertSucceeds(updateDoc(userReference, {
    email: "changed@example.com",
    lastLoginAt: "2026-08-21T10:00:00Z",
    updatedAt: "2026-08-21T10:00:00Z",
  }));
  await assertFails(updateDoc(userReference, {
    createdAt: "2026-08-21T10:00:00Z",
  }));
  await assertFails(updateDoc(userReference, {
    exactHomeAddress: "저장하면 안 되는 주소",
  }));
});

test("기존 UTC 가입일은 같은 ISO 형식의 한국 시간으로 한 번 변환할 수 있다", async () => {
  const firestore = authenticatedFirestore(HOST_UID);
  const userReference = doc(firestore, "users", HOST_UID);
  await assertSucceeds(setDoc(userReference, userProfileData()));
  await assertSucceeds(updateDoc(userReference, {
    createdAt: "2026-08-20T19:00:00+09:00",
    lastLoginAt: "2026-09-01T21:00:00+09:00",
    updatedAt: "2026-09-01T21:00:00+09:00",
  }));
  await assertFails(updateDoc(userReference, {
    createdAt: "2026-08-21T19:00:00+09:00",
  }));
});

test("필드가 없던 기존 users 부모 문서에는 기본 정보를 보완할 수 있다", async () => {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users", HOST_UID), {});
  });
  const firestore = authenticatedFirestore(HOST_UID);
  await assertSucceeds(setDoc(doc(firestore, "users", HOST_UID), userProfileData()));
});

test("익명 인증 사용자는 회원 기본 문서를 만들 수 없다", async () => {
  const firestore = testEnvironment.authenticatedContext("anonymous-user", {
    firebase: {sign_in_provider: "anonymous"},
  }).firestore();
  await assertFails(setDoc(doc(firestore, "users", "anonymous-user"), userProfileData()));
});

function authenticatedFirestore(uid) {
  return testEnvironment.authenticatedContext(uid, {
    firebase: {sign_in_provider: "google.com"},
  }).firestore();
}

function userProfileData() {
  return {
    email: "member@example.com",
    displayName: "테스트 회원",
    providerId: "google.com",
    createdAt: "2026-08-20T10:00:00Z",
    lastLoginAt: "2026-08-20T10:00:00Z",
    updatedAt: "2026-08-20T10:00:00Z",
  };
}

function roomData() {
  return {
    id: ROOM_ID,
    title: "테스트 약속",
    meetingType: "MEAL",
    dateRangeStart: "2026-08-21",
    dateRangeEnd: "2026-08-31",
    minParticipants: 1,
    maxParticipants: 8,
    participantCount: 2,
    hostParticipantId: "host-participant",
    hostAuthUid: HOST_UID,
    status: "COLLECTING_AVAILABILITY",
    createdAt: "2026-08-20T10:00:00+09:00",
    updatedAt: "2026-08-20T10:00:00+09:00",
  };
}

function participantData(participantId, authUid, isHost) {
  return {
    id: participantId,
    roomId: ROOM_ID,
    nickname: participantId,
    isHost,
    authUid,
    isInvited: false,
    joinedAt: "2026-08-20T10:00:00+09:00",
  };
}

function memberData(authUid, participantId, role) {
  return {
    authUid,
    participantId,
    role,
    joined: true,
    updatedAt: "2026-08-20T10:00:00+09:00",
  };
}

function availabilityData(participantId, date) {
  return {
    roomId: ROOM_ID,
    participantId,
    date,
    status: "AVAILABLE",
    updatedAt: "2026-08-20T11:00:00+09:00",
  };
}

function summaryData(participantId, label, latitude, longitude) {
  return {
    participantId,
    roomId: ROOM_ID,
    label,
    approximateLatitude: latitude,
    approximateLongitude: longitude,
    updatedAt: "2026-08-20T10:00:00+09:00",
  };
}
