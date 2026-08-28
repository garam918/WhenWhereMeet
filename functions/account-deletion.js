"use strict";

const RECENT_LOGIN_MAX_AGE_SECONDS = 15 * 60;

function bearerTokenFrom(request) {
  const authorization = request.get("Authorization") || "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  if (!match || !match[1].trim()) {
    throw publicError(401, "로그인 인증 정보가 필요합니다.");
  }
  return match[1].trim();
}

function assertRecentLogin(decodedToken, nowSeconds = Math.floor(Date.now() / 1000)) {
  const authTime = Number(decodedToken && decodedToken.auth_time);
  if (!Number.isFinite(authTime) || nowSeconds - authTime > RECENT_LOGIN_MAX_AGE_SECONDS) {
    throw publicError(401, "보안을 위해 다시 로그인한 뒤 회원 탈퇴를 시도해주세요.");
  }
  const provider = decodedToken && decodedToken.firebase && decodedToken.firebase.sign_in_provider;
  if (!provider || provider === "anonymous") {
    throw publicError(403, "Google 또는 Apple 계정으로 로그인해주세요.");
  }
}

async function deleteUserData({firestore, auth, uid}) {
  const participantsSnapshot = await firestore
    .collectionGroup("participants")
    .where("authUid", "==", uid)
    .get();
  const hostedRoomsSnapshot = await firestore
    .collection("meetingRooms")
    .where("hostAuthUid", "==", uid)
    .get();

  const hostedRoomRefs = new Map(hostedRoomsSnapshot.docs.map((document) => [document.id, document.ref]));
  const participantEntries = [];
  for (const participantDocument of participantsSnapshot.docs) {
    const roomRef = participantDocument.ref.parent.parent;
    if (!roomRef) continue;
    const roomSnapshot = await roomRef.get();
    if (!roomSnapshot.exists) continue;
    const room = roomSnapshot.data();
    if (room.hostAuthUid === uid || room.hostParticipantId === participantDocument.id) {
      hostedRoomRefs.set(roomRef.id, roomRef);
    }
    participantEntries.push({participantDocument, roomRef, room});
  }

  for (const roomRef of hostedRoomRefs.values()) {
    const roomCodes = await firestore
      .collection("roomCodes")
      .where("roomId", "==", roomRef.id)
      .get();
    await deleteReferences(firestore, roomCodes.docs.map((document) => document.ref));
    await firestore.recursiveDelete(roomRef);
  }

  const participantCountsByRoom = new Map();
  const participantRefsToDelete = [];
  let deletedMembershipCount = 0;
  for (const entry of participantEntries) {
    if (hostedRoomRefs.has(entry.roomRef.id)) continue;
    deletedMembershipCount += 1;
    const participant = entry.participantDocument.data();
    const availability = await entry.roomRef
      .collection("availabilities")
      .where("participantId", "==", entry.participantDocument.id)
      .get();
    participantRefsToDelete.push(
      ...availability.docs.map((document) => document.ref),
      entry.roomRef.collection("startLocations").doc(entry.participantDocument.id),
      entry.roomRef.collection("startLocationSummaries").doc(entry.participantDocument.id),
      entry.roomRef.collection("destinationStationProposals").doc(entry.participantDocument.id),
      entry.roomRef.collection("destinationStationVotes").doc(entry.participantDocument.id),
      entry.roomRef.collection("members").doc(uid),
      entry.participantDocument.ref,
    );
    if (typeof participant.nickname === "string" && participant.nickname.trim()) {
      participantRefsToDelete.push(
        entry.roomRef.collection("nicknameKeys").doc(normalizeNicknameKey(participant.nickname)),
      );
    }
    participantCountsByRoom.set(
      entry.roomRef.path,
      (participantCountsByRoom.get(entry.roomRef.path) || 0) + 1,
    );
  }
  await deleteReferences(firestore, participantRefsToDelete);

  for (const [roomPath, removedCount] of participantCountsByRoom.entries()) {
    const roomRef = firestore.doc(roomPath);
    await firestore.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(roomRef);
      if (!snapshot.exists) return;
      const participantCount = Number(snapshot.data().participantCount) || 1;
      transaction.update(roomRef, {
        participantCount: Math.max(1, participantCount - removedCount),
        updatedAt: new Date().toISOString(),
      });
    });
  }

  const connections = await firestore
    .collection("meetingFriendConnections")
    .where("userIds", "array-contains", uid)
    .get();
  const friendRefs = [];
  connections.docs.forEach((document) => {
    friendRefs.push(document.ref);
    const userIds = Array.isArray(document.data().userIds) ? document.data().userIds : [];
    userIds
      .filter((userId) => typeof userId === "string" && userId && userId !== uid)
      .forEach((otherUserId) => {
        friendRefs.push(firestore.collection("users").doc(otherUserId).collection("friends").doc(uid));
      });
  });
  await deleteReferences(firestore, friendRefs);

  await firestore.recursiveDelete(firestore.collection("users").doc(uid));
  try {
    await auth.deleteUser(uid);
  } catch (error) {
    if (!error || error.code !== "auth/user-not-found") throw error;
  }

  return {
    deletedHostedRoomCount: hostedRoomRefs.size,
    deletedMembershipCount,
  };
}

async function deleteReferences(firestore, references) {
  const uniqueReferences = [...new Map(references.map((reference) => [reference.path, reference])).values()];
  for (let start = 0; start < uniqueReferences.length; start += 450) {
    const batch = firestore.batch();
    uniqueReferences.slice(start, start + 450).forEach((reference) => batch.delete(reference));
    await batch.commit();
  }
}

function normalizeNicknameKey(value) {
  return String(value).trim().toLowerCase();
}

function publicError(statusCode, message) {
  const error = new Error(message);
  error.statusCode = statusCode;
  error.publicMessage = message;
  return error;
}

module.exports = {
  RECENT_LOGIN_MAX_AGE_SECONDS,
  assertRecentLogin,
  bearerTokenFrom,
  deleteUserData,
  normalizeNicknameKey,
};
