package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet

/**
 * Prioriteringen av en rammebehandling, slik den vises på behandlingen og i benken.
 * Begrunnelsen er saksbehandlers fritekst og kan sladdes, mens det at behandlingen er prioritert alltid vises.
 */
data class PrioriteringDTO(
    val begrunnelse: SladdbarVerdi<String>,
)

/**
 * Prioriteringen slik saksbehandler sender den inn fra behandlingsskjemaet.
 * En blank begrunnelse avvises ved deserialisering, slik at kallet får 400.
 */
data class OppdaterPrioriteringDTO(
    val begrunnelse: String,
) {
    @JsonIgnore
    private val prioritering: Prioritering = requireNotNull(Prioritering.create(begrunnelse)) {
        "Begrunnelse for prioritering kan ikke være blank"
    }

    fun tilDomene(): Prioritering = prioritering
}

fun Prioritering.tilDTO(): PrioriteringDTO = PrioriteringDTO(
    begrunnelse = begrunnelse.verdi.ikkeSladdet(),
)

fun PrioriteringDTO.sladdet(): PrioriteringDTO = this.copy(
    begrunnelse = SladdetVerdi,
)
