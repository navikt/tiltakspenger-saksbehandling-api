package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagehjemmel
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

/**
 * Overordnet kommando for å vurdere en klagebehandling.
 *  Vi har gått for å en mer detaljert kommando som er explisitt i sin intensjon - istedenfor en generell intensjon.
 */
sealed interface VurderKlagebehandlingKommando {
    val sakId: SakId
    val klagebehandlingId: KlagebehandlingId
    val saksbehandler: Saksbehandler
    val correlationId: CorrelationId
}

/**
 * rammebehandlingId genereres av systemet når klagen omgjøres til en rammebehandling.
 */
data class OmgjørKlagebehandlingKommando(
    override val sakId: SakId,
    override val klagebehandlingId: KlagebehandlingId,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
    val årsak: KlageOmgjøringsårsak,
    val begrunnelse: Begrunnelse,
) : VurderKlagebehandlingKommando {
    /**
     * Brukes bare initielt.
     */
    fun tilResultatUtenRammebehandlingId(): Klagebehandlingsresultat.Omgjør {
        return Klagebehandlingsresultat.Omgjør(
            årsak = årsak,
            begrunnelse = begrunnelse,
            rammebehandlingId = null,
        )
    }
}

data class OpprettholdKlagebehandlingKommando(
    override val sakId: SakId,
    override val klagebehandlingId: KlagebehandlingId,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
    val hjemler: NonEmptySet<Klagehjemmel>,
) : VurderKlagebehandlingKommando {
    fun tilResultat(): Klagebehandlingsresultat.Opprettholdt =
        Klagebehandlingsresultat.Opprettholdt(hjemler = hjemler, brevtekst = null)
}
