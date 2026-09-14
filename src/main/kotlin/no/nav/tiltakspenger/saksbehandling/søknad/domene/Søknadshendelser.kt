package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.NonBlankString
import java.time.LocalDateTime

/**
 * Wrapper for historikk over søknadshendelser.
 * Gir helpers og bevarer typen i domenet istedenfor rå List<T>.
 */
data class Søknadshendelser(
    private val _hendelser: List<Søknadshendelse> = emptyList(),
) : List<Søknadshendelse> by _hendelser {

    val erAvbrutt: Boolean = this.lastOrNull() is Søknadshendelse.Avbrutt
    val erGjenopprettet: Boolean = this.lastOrNull() is Søknadshendelse.Gjenopprettet

    operator fun plus(hendelse: Søknadshendelse): Søknadshendelser =
        Søknadshendelser(_hendelser + hendelse)

    fun toList(): List<Søknadshendelse> = _hendelser

    companion object {
        fun empty() = Søknadshendelser()

        fun fromAvbrutt(tidspunkt: LocalDateTime, utførtAv: String, begrunnelse: NonBlankString) =
            Søknadshendelser(listOf(Søknadshendelse.Avbrutt(tidspunkt = tidspunkt, utførtAv = utførtAv, begrunnelse = begrunnelse)))

        fun fromGjenopprettet(tidspunkt: LocalDateTime, utførtAv: String, begrunnelse: NonBlankString?) =
            Søknadshendelser(listOf(Søknadshendelse.Gjenopprettet(tidspunkt = tidspunkt, utførtAv = utførtAv, begrunnelse = begrunnelse)))
    }
}
