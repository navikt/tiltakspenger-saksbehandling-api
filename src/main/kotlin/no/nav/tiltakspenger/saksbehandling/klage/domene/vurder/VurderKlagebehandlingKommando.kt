package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

sealed interface VurderKlagebehandlingKommando {
    val sakId: SakId
    val klagebehandlingId: KlagebehandlingId
    val saksbehandler: Saksbehandler
    val correlationId: CorrelationId
}

/**
 * @param rammebehandlingId Genereres av systemet når klagen omgjøres til en rammebehandling.
 */
data class OmgjørKlagebehandlingKommando(
    override val sakId: SakId,
    override val klagebehandlingId: KlagebehandlingId,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
    val årsak: KlageOmgjøringsårsak,
    val begrunnelse: Begrunnelse,
    val rammebehandlingId: BehandlingId?,
) : VurderKlagebehandlingKommando {
    fun toResultat(): Klagebehandlingsresultat.Omgjør {
        return Klagebehandlingsresultat.Omgjør(
            årsak = årsak,
            begrunnelse = begrunnelse,
            rammebehandlingId = rammebehandlingId,
        )
    }
}
