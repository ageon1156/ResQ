

package com.geeksville.mesh.android

import java.util.*

object DateUtils {
    fun dateUTC(year: Int, month: Int, day: Int): Date {
        val cal = GregorianCalendar(TimeZone.getTimeZone("GMT"))
        cal.set(year, month, day, 0, 0, 0);
        return Date(cal.getTime().getTime())
    }
}
