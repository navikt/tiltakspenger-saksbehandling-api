package no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.rawRequestString
import no.nav.tiltakspenger.libs.httpklient.rawResponseString
import no.nav.tiltakspenger.libs.httpklient.tidsstempler
import java.time.Clock
import java.time.LocalDateTime

data class OversendtKlageTilKabalMetadata(
    val request: String,
    val response: String,
    val statusKode: Int,
    val oversendtTidspunkt: LocalDateTime,
)

fun HttpKlientMetadata.tilOversendtKlageTilKabalMetadata(
    clock: Clock,
): OversendtKlageTilKabalMetadata {
    return OversendtKlageTilKabalMetadata(
        request = this.rawRequestString,
        response = this.rawResponseString!!,
        statusKode = this.statusCode!!,
        oversendtTidspunkt = this.tidsstempler.responsMottatt ?: nå(clock),
    )
}

fun HttpKlientError.ResponsMottatt.tilOversendtKlageTilKabalMetadata(
    clock: Clock,
): OversendtKlageTilKabalMetadata {
    return OversendtKlageTilKabalMetadata(
        request = this.rawRequestString,
        response = this.rawResponseString!!,
        statusKode = this.statusCode,
        oversendtTidspunkt = this.tidsstempler.responsMottatt ?: nå(clock),
    )
}
