package no.nav.tiltakspenger.saksbehandling.benk.v2.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Ventestatusen slik benken viser den: er den satt på vent, og i så fall hvorfor og til når.
 */
data class BenkV2Ventestatus(
    val erSattPåVent: Boolean,
    val begrunnelse: String?,
    val frist: LocalDate?,
)

/**
 * Fellesfeltene alle rader i benken har, uavhengig av fane.
 */
data class BenkV2Behandlingsfelles(
    val sakId: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val startet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val saksbehandler: String?,
    val beslutter: String?,
    val erUnderkjent: Boolean,
    val ventestatus: BenkV2Ventestatus,
)

/**
 * En rad i benken.
 * Hver fane har sin egen radtype, fordi kolonnene fanene viser er ulike.
 */
sealed interface BenkV2Behandling {
    val felles: BenkV2Behandlingsfelles

    val fnr: Fnr get() = felles.fnr
}

data class BenkSøknadsbehandling(
    override val felles: BenkV2Behandlingsfelles,
    val id: RammebehandlingId,
    val status: BenkV2Behandlingsstatus,
    val søknadstype: BenkSøknadstype,
    val kravtidspunkt: LocalDateTime,
    val resultat: BenkSøknadsbehandlingResultat?,
) : BenkV2Behandling

data class BenkRevurdering(
    override val felles: BenkV2Behandlingsfelles,
    val id: RammebehandlingId,
    val status: BenkV2Behandlingsstatus,
    val resultat: BenkRevurderingResultat?,
) : BenkV2Behandling

data class BenkMeldekort(
    override val felles: BenkV2Behandlingsfelles,
    /** Både meldekortbehandlinger og brukers meldekort har [MeldekortId] — brukers meldekort får id-en sin fra meldekort-api. */
    val id: MeldekortId,
    val status: BenkV2Behandlingsstatus,
    val type: BenkMeldekortType,
    val periode: Periode,
    /** Beregnet beløp for meldekortbehandlinger som er beregnet, ellers null. */
    val beløp: Int?,
    /** Tidspunktet bruker sendte inn meldekortet, kun for innsendte og korrigerte meldekort. */
    val mottattTidspunkt: LocalDateTime?,
) : BenkV2Behandling

data class BenkKlagebehandling(
    override val felles: BenkV2Behandlingsfelles,
    val id: KlagebehandlingId,
    val status: BenkV2Behandlingsstatus,
    val kravtidspunkt: LocalDateTime,
    val resultat: BenkKlagebehandlingResultat?,
) : BenkV2Behandling

data class BenkTilbakekreving(
    override val felles: BenkV2Behandlingsfelles,
    val id: TilbakekrevingId,
    val status: BenkTilbakekrevingStatus,
    val beløp: BigDecimal,
    val kilde: BenkTilbakekrevingKilde,
    val kravgrunnlagPeriode: Periode,
    /** Lenke til behandlingen i tilbakekrevingsløsningen, slik personoversikten bruker den. */
    val url: String,
) : BenkV2Behandling
