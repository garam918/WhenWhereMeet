"use strict";

const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");

initializeApp();

async function main() {
  const dryRun = process.argv.includes("--dry-run");
  const firestore = getFirestore("default");
  const participants = await firestore.collectionGroup("participants").get();
  const membershipWrites = [];
  const roomSnapshots = new Map();

  for (const participantDocument of participants.docs) {
    const participant = participantDocument.data();
    const roomRef = participantDocument.ref.parent.parent;
    if (!roomRef || typeof participant.authUid !== "string" || !participant.authUid) continue;
    let roomSnapshot = roomSnapshots.get(roomRef.path);
    if (!roomSnapshot) {
      roomSnapshot = await roomRef.get();
      roomSnapshots.set(roomRef.path, roomSnapshot);
    }
    if (!roomSnapshot.exists) continue;
    const isHost = roomSnapshot.data().hostParticipantId === participantDocument.id;
    membershipWrites.push({
      ref: roomRef.collection("members").doc(participant.authUid),
      data: {
        authUid: participant.authUid,
        participantId: participantDocument.id,
        role: isHost ? "host" : "participant",
        joined: participant.isInvited !== true,
        updatedAt: participant.joinedAt || new Date().toISOString(),
      },
    });
    if (isHost && roomSnapshot.data().hostAuthUid !== participant.authUid) {
      membershipWrites.push({
        ref: roomRef,
        data: {hostAuthUid: participant.authUid},
        merge: true,
      });
    }
  }

  const locations = await firestore.collectionGroup("startLocations").get();
  const summaryWrites = [];
  locations.docs.forEach((locationDocument) => {
    const location = locationDocument.data();
    const roomRef = locationDocument.ref.parent.parent;
    if (!roomRef || typeof location.participantId !== "string" || !location.participantId) return;
    summaryWrites.push({
      ref: roomRef.collection("startLocationSummaries").doc(location.participantId),
      data: {
        participantId: location.participantId,
        roomId: roomRef.id,
        label: typeof location.label === "string" ? location.label : "출발 지역",
        approximateLatitude: roundToAreaPrecision(location.latitude),
        approximateLongitude: roundToAreaPrecision(location.longitude),
        updatedAt: location.updatedAt || new Date().toISOString(),
      },
    });
  });

  if (!dryRun) {
    await writeInChunks(firestore, [...membershipWrites, ...summaryWrites]);
  }
  console.log(JSON.stringify({
    dryRun,
    membershipWriteCount: membershipWrites.length,
    locationSummaryWriteCount: summaryWrites.length,
  }));
}

async function writeInChunks(firestore, writes) {
  for (let start = 0; start < writes.length; start += 450) {
    const batch = firestore.batch();
    writes.slice(start, start + 450).forEach((write) => {
      if (write.merge) {
        batch.set(write.ref, write.data, {merge: true});
      } else {
        batch.set(write.ref, write.data);
      }
    });
    await batch.commit();
  }
}

function roundToAreaPrecision(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return 0;
  return Math.round(number * 100) / 100;
}

main().catch((error) => {
  console.error(error && error.message ? error.message : error);
  process.exitCode = 1;
});
