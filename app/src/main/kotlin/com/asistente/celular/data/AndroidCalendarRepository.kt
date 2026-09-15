package com.asistente.celular.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.asistente.celular.nlu.calendar.CalendarEventItem
import com.asistente.celular.nlu.calendar.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

/**
 * Implementación de CalendarRepository que interactúa directamente con el Provider de Calendario nativo de Android.
 */
class AndroidCalendarRepository(
    private val context: Context
) : CalendarRepository {

    override fun hasCalendarPermission(): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted
    }

    override suspend fun getEvents(startMillis: Long, endMillis: Long): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()

        val events = mutableListOf<CalendarEventItem>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )

        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?) AND (${CalendarContract.Events.DELETED} != 1)"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val startCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val locCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                val allDayCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
                val calNameCol = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Sin título"
                    val desc = cursor.getString(descCol) ?: ""
                    val start = cursor.getLong(startCol)
                    val end = cursor.getLong(endCol).let { if (it > 0) it else start + 3600000L }
                    val loc = cursor.getString(locCol) ?: ""
                    val isAllDay = cursor.getInt(allDayCol) == 1
                    val calName = cursor.getString(calNameCol) ?: ""

                    events.add(
                        CalendarEventItem(
                            id = id,
                            title = title,
                            description = desc,
                            startMillis = start,
                            endMillis = end,
                            location = loc,
                            isAllDay = isAllDay,
                            calendarName = calName
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return@withContext events
    }

    override suspend fun getUpcomingEvents(limit: Int): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val future = now + 7L * 24 * 3600 * 1000 // 7 días hacia adelante
        return@withContext getEvents(now, future).take(limit)
    }

    override suspend fun createEvent(
        title: String,
        startMillis: Long,
        durationMinutes: Int,
        description: String,
        location: String
    ): Long? = withContext(Dispatchers.IO) {
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        if (!writeGranted) return@withContext null

        val endMillis = startMillis + (durationMinutes * 60 * 1000L)
        val defaultCalendarId = getPrimaryCalendarId() ?: 1L

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, defaultCalendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, location)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return@withContext uri?.lastPathSegment?.toLongOrNull()
        } catch (_: Exception) {
            return@withContext null
        }
    }

    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
