package no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger

import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import java.time.LocalDate

/**
 * Et sett med opplysninger som er relevante for saksbehandlingen. En [no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling] vil på sikt ha en referanse til [Saksopplysninger].
 * TODO John + Anders: Vurder om vi skal flytte nullable søknad inn hit. Den vil eksistere for førstegangsbehandling/søknadsbehandling, men ikke for revurdering nødvendigvis.
 */
data class Saksopplysninger(
    val fødselsdato: LocalDate,
    /** TODO John + Anders: Vurder på hvilket tidspunkt denne kan gjøres om til en liste. Kan det vente til vi har slettet vilkårssettet? */
    val tiltaksdeltagelse: Tiltaksdeltagelse,
)
