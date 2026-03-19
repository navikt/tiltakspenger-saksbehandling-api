@file:Suppress("PackageDirectoryMismatch")
// Egen mappe på grunn av domenetilhørighet, annen pakke pga. Kotlin's sealed interface regler

package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk.StatistikkStønadDTO

fun interface GenererStønadsstatistikk : Statistikkhendelse {
    suspend fun genererStønadsstatistikk(): StatistikkStønadDTO
}
