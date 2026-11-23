package no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId

/**
 * Konverterer en revurdering under behandling til en annen revurderingstype.
 * Resultatet nullstilles til det en ny revurdering av typen ville startet med.
 */
sealed interface KonverterRevurderingKommando {
    val sakId: SakId
    val behandlingId: RammebehandlingId
    val saksbehandler: Saksbehandler

    /**
     * Antallet automatisk opprettede grunner saksbehandler fikk vist da konverteringen ble valgt.
     */
    val antallVisteAutomatiskOpprettedeGrunner: Int

    data class TilStans(
        override val sakId: SakId,
        override val behandlingId: RammebehandlingId,
        override val saksbehandler: Saksbehandler,
        override val antallVisteAutomatiskOpprettedeGrunner: Int,
    ) : KonverterRevurderingKommando

    data class TilInnvilgelse(
        override val sakId: SakId,
        override val behandlingId: RammebehandlingId,
        override val saksbehandler: Saksbehandler,
        override val antallVisteAutomatiskOpprettedeGrunner: Int,
    ) : KonverterRevurderingKommando

    /** Under-resultatet til omgjøringen velges av saksbehandler i behandlingen, slik som når en omgjøring startes. */
    data class TilOmgjøring(
        override val sakId: SakId,
        override val behandlingId: RammebehandlingId,
        override val saksbehandler: Saksbehandler,
        override val antallVisteAutomatiskOpprettedeGrunner: Int,
    ) : KonverterRevurderingKommando

    data class TilForlengelse(
        override val sakId: SakId,
        override val behandlingId: RammebehandlingId,
        override val saksbehandler: Saksbehandler,
        override val antallVisteAutomatiskOpprettedeGrunner: Int,
        val vedtakIdSomForlenges: VedtakId,
    ) : KonverterRevurderingKommando
}
