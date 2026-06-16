package com.garam.whenwheremeet.platform

import kotlinx.datetime.LocalDate
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun currentLocalDate(): LocalDate {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd"
        locale = NSLocale.currentLocale
    }
    return LocalDate.parse(formatter.stringFromDate(NSDate()))
}
