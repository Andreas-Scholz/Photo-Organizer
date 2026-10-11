package io.github.andreasscholz.photoorganizer.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/** The locale the UI is shown in, which honors a per-app language unlike [Locale.getDefault]. */
@Composable
@ReadOnlyComposable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

private fun format(value: TemporalAccessor, skeleton: String, locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return DateTimeFormatter.ofPattern(pattern, locale).format(value)
}

/** Day and month in the user's locale, e.g. "March 3" or "3. März". */
@Composable
@ReadOnlyComposable
fun formatDayOfYear(date: TemporalAccessor): String = format(date, "dMMMM", currentLocale())

/** Full date in the user's locale, e.g. "March 3, 2025" or "3. März 2025". */
@Composable
@ReadOnlyComposable
fun formatFullDate(date: TemporalAccessor): String = format(date, "dMMMMy", currentLocale())

/** Time of day honoring the device's 12/24-hour setting. */
@Composable
@ReadOnlyComposable
fun formatTime(time: TemporalAccessor, is24Hour: Boolean): String =
    format(time, if (is24Hour) "Hm" else "hm", currentLocale())
