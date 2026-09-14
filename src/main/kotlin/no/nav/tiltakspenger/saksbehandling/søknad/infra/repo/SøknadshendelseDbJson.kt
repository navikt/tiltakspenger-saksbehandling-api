package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse
import java.time.LocalDateTime

/**
 * Lagret form av [Søknadshendelse].
 * Vi diskriminerer på [type] framfor Jackson-polymorfi, slik at den lagrede json-en er lett å lese og migrere.
 */
private data class SøknadshendelseDbJson(
    val type: Type,
    val tidspunkt: String,
    val utførtAv: String,
    val begrunnelse: String?,
) {
    enum class Type {
        AVBRUTT,
        GJENOPPRETTET,
    }

    fun toSøknadshendelse(): Søknadshendelse = when (type) {
        Type.AVBRUTT -> Søknadshendelse.Avbrutt(
            tidspunkt = LocalDateTime.parse(tidspunkt),
            utførtAv = utførtAv,
            // Et avbrudd kan ikke lagres uten begrunnelse, så `!!` kan ikke feile for rader vi selv har skrevet.
            begrunnelse = begrunnelse!!.toNonBlankString(),
        )

        Type.GJENOPPRETTET -> Søknadshendelse.Gjenopprettet(
            tidspunkt = LocalDateTime.parse(tidspunkt),
            utførtAv = utførtAv,
            begrunnelse = begrunnelse?.toNonBlankString(),
        )
    }
}

private fun Søknadshendelse.toDbJson(): SøknadshendelseDbJson = SøknadshendelseDbJson(
    type = when (this) {
        is Søknadshendelse.Avbrutt -> SøknadshendelseDbJson.Type.AVBRUTT
        is Søknadshendelse.Gjenopprettet -> SøknadshendelseDbJson.Type.GJENOPPRETTET
    },
    tidspunkt = tidspunkt.toString(),
    utførtAv = utførtAv,
    begrunnelse = begrunnelse?.value,
)

fun no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser.toDbJson(): String =
    serialize(this.toList().map { it.toDbJson() })

fun String.toSøknadshendelser(): no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser =
    no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelser(
        deserializeList<SøknadshendelseDbJson>(this).map { it.toSøknadshendelse() },
    )
