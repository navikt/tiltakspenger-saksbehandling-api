package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.NonBlankString
import java.time.LocalDateTime

/**
 * Wrapper for historikk over søknadshendelser.
 * Gir helpers og bevarer typen i domenet istedenfor rå List<T>.
 *
 * Historikken er append-only og kronologisk, og en søknad kan bare gjenåpnes etter at den er avbrutt.
 * Det gir én lovlig form: avbrutt, gjenåpnet, avbrutt, gjenåpnet, ...
 */
data class Søknadshendelser(
    private val _hendelser: List<Søknadshendelse> = emptyList(),
) : List<Søknadshendelse> by _hendelser {

    init {
        _hendelser.zipWithNext { forrige, neste ->
            require(neste.tidspunkt > forrige.tidspunkt) {
                "Søknadshendelsene må være i stigende kronologisk rekkefølge, men ${neste.tidspunkt} kom etter ${forrige.tidspunkt}."
            }
        }
        _hendelser.forEachIndexed { index, hendelse ->
            val skalVæreAvbrutt = index % 2 == 0
            require(hendelse is Søknadshendelse.Avbrutt == skalVæreAvbrutt) {
                "Søknadshendelsene må alternere mellom avbrutt og gjenåpnet, men hendelse nr. ${index + 1} var ${if (skalVæreAvbrutt) "gjenåpnet" else "avbrutt"}."
            }
        }
    }

    val erAvbrutt: Boolean = this.lastOrNull() is Søknadshendelse.Avbrutt
    val erGjenåpnet: Boolean = this.lastOrNull() is Søknadshendelse.Gjenåpnet

    operator fun plus(hendelse: Søknadshendelse): Søknadshendelser =
        Søknadshendelser(_hendelser + hendelse)

    companion object {
        fun empty() = Søknadshendelser()

        fun fromAvbrutt(tidspunkt: LocalDateTime, utførtAv: String, begrunnelse: NonBlankString) =
            Søknadshendelser(listOf(Søknadshendelse.Avbrutt(tidspunkt = tidspunkt, utførtAv = utførtAv, begrunnelse = begrunnelse)))
    }
}
