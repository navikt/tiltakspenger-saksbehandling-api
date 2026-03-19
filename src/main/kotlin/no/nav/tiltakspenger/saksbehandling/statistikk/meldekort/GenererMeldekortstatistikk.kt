@file:Suppress("PackageDirectoryMismatch")
// Egen mappe på grunn av domenetilhørighet, annen pakke pga. Kotlin's sealed interface regler

package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.saksbehandling.statistikk.meldekort.StatistikkMeldekortDTO

fun interface GenererMeldekortstatistikk : Statistikkhendelse {
    suspend fun genererMeldekortstatistikk(): StatistikkMeldekortDTO
}
