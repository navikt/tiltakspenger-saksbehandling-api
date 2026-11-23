@file:Suppress("PackageDirectoryMismatch")
// Egen mappe på grunn av domenetilhørighet, annen pakke pga. Kotlin's sealed interface regler

package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.Skjermingsoppslag

fun interface GenererSaksstatistikk : Statistikkhendelse {
    suspend fun genererSaksstatistikk(
        skjermingsoppslag: Skjermingsoppslag,
        versjon: String,
    ): SaksstatistikkDTO
}
