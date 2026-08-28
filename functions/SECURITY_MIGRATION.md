# Firestore security migration

The hardened rules use `meetingRooms/{roomId}/members/{authUid}` as the room authorization source and split private start locations from coarse room-visible summaries.

Run this migration before deploying `firestore.rules` to a project that already contains meeting rooms.

## Verification

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" npm run test:emulator
```

## Production rollout order

1. Deploy `deleteAccountData` and `cleanupDeletedMeetingRoom` with the other Functions.
2. Inspect the migration count without writes:

   ```bash
   npm run backfill:room-security -- --dry-run
   ```

3. Backfill member documents and coarse location summaries:

   ```bash
   npm run backfill:room-security
   ```

4. Deploy the new Firestore Rules and compatible Android, iOS, and web clients in the same release window.
5. Verify room creation, invited-user acceptance, code-based joining, leaving, room deletion, and account deletion against the production project.

Do not deploy the final rules before the backfill. Older clients do not create member documents and cannot create or join rooms under the hardened rules.
