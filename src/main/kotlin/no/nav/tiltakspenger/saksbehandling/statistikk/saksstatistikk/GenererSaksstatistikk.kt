@file:Suppress("PackageDirectoryMismatch")
// Egen mappe på grunn av domenetilhørighet, annen pakke pga. Kotlin's sealed interface regler

package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO

fun interface GenererSaksstatistikk : Statistikkhendelse {
    suspend fun genererSaksstatistikk(
        gjelderKode6: suspend (Fnr) -> Boolean,
        versjon: String,
        clock: java.time.Clock,
    ): SaksstatistikkDTO
}
