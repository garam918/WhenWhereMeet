package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomSyncPlannerTest {
    @Test
    fun missingAppliedRevisionsRequireFullSync() {
        assertEquals(RoomSyncPlan.Full, RoomSyncPlanner.plan(RoomSyncRevisions(), applied = null))
    }

    @Test
    fun missingRevisionFieldsAreTreatedAsZero() {
        val plan = RoomSyncPlanner.plan(
            remote = RoomSyncRevisions(
                collections = mapOf(RoomSyncCollection.PARTICIPANTS.key to 0L),
                availabilities = mapOf("host" to 0L),
            ),
            applied = RoomSyncRevisions(),
        )

        assertTrue(plan.isUpToDate)
    }

    @Test
    fun onlyChangedCollectionsAndParticipantsAreReloaded() {
        val applied = RoomSyncRevisions(
            collections = mapOf(
                RoomSyncCollection.PARTICIPANTS.key to 2L,
                RoomSyncCollection.DESTINATION_STATION_VOTES.key to 1L,
            ),
            availabilities = mapOf("host" to 3L, "guest" to 1L),
        )
        val remote = applied
            .incremented(RoomSyncCollection.DESTINATION_STATION_VOTES)
            .incrementedAvailability("guest")

        val plan = RoomSyncPlanner.plan(remote, applied)

        assertEquals(setOf(RoomSyncCollection.DESTINATION_STATION_VOTES), plan.collections)
        assertEquals(setOf("guest"), plan.availabilityParticipantIds)
        assertEquals(false, plan.reloadAllAvailabilities)
    }

    @Test
    fun decreasedOrRemovedRevisionsStillTriggerReload() {
        val applied = RoomSyncRevisions(
            collections = mapOf(RoomSyncCollection.PARTICIPANTS.key to 5L),
            availabilities = mapOf("left-participant" to 2L),
        )
        val remote = RoomSyncRevisions(collections = mapOf(RoomSyncCollection.PARTICIPANTS.key to 1L))

        val plan = RoomSyncPlanner.plan(remote, applied)

        assertEquals(setOf(RoomSyncCollection.PARTICIPANTS), plan.collections)
        assertEquals(setOf("left-participant"), plan.availabilityParticipantIds)
    }
}

class AvailabilityChangesTest {
    private val day1 = LocalDate(2026, 10, 1)
    private val day2 = LocalDate(2026, 10, 2)
    private val day3 = LocalDate(2026, 10, 3)

    @Test
    fun identicalAvailabilityHasNoChanges() {
        val values = mapOf(day1 to AvailabilityStatus.AVAILABLE, day2 to AvailabilityStatus.MAYBE)

        assertTrue(AvailabilityChanges.between(values, values).isEmpty)
    }

    @Test
    fun onlyChangedAddedAndRemovedDatesAreWritten() {
        val previous = mapOf(
            day1 to AvailabilityStatus.AVAILABLE,
            day2 to AvailabilityStatus.MAYBE,
        )
        val next = mapOf(
            day1 to AvailabilityStatus.AVAILABLE,
            day3 to AvailabilityStatus.UNAVAILABLE,
        )

        val changes = AvailabilityChanges.between(previous, next)

        assertEquals(mapOf(day3 to AvailabilityStatus.UNAVAILABLE), changes.upserts)
        assertEquals(setOf(day2), changes.deletedDates)
    }

    @Test
    fun statusChangeIsWrittenAsUpsert() {
        val changes = AvailabilityChanges.between(
            previous = mapOf(day1 to AvailabilityStatus.MAYBE),
            next = mapOf(day1 to AvailabilityStatus.AVAILABLE),
        )

        assertEquals(mapOf(day1 to AvailabilityStatus.AVAILABLE), changes.upserts)
        assertTrue(changes.deletedDates.isEmpty())
    }
}
