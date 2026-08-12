package no.nav.tiltakspenger.saksbehandling.benk.domene

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
data class BenkVentestatus(
    val erSattPåVent: Boolean,
    val begrunnelse: String?,
    val frist: LocalDate?,
)

/**
 * Fellesfeltene alle rader i benken har, uavhengig av fane.
 */
data class BenkBehandlingsfelles(
    val sakId: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val startet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val saksbehandler: String?,
    val beslutter: String?,
    val erUnderkjent: Boolean,
    val ventestatus: BenkVentestatus,
)

/**
 * En rad i benken.
 * Hver fane har sin egen radtype, fordi kolonnene fanene viser er ulike.
 */
sealed interface BenkBehandling {
    val felles: BenkBehandlingsfelles

    val fnr: Fnr get() = felles.fnr
}

data class BenkSøknadsbehandling(
    override val felles: BenkBehandlingsfelles,
    val id: RammebehandlingId,
    val status: BenkBehandlingsstatus,
    val søknadstype: BenkSøknadstype,
    val kravtidspunkt: LocalDateTime,
    /** Basen lagrer null fram til noe er valgt — benken kaller det [BenkSøknadsbehandlingResultat.IKKE_VALGT]. */
    val resultat: BenkSøknadsbehandlingResultat,
) : BenkBehandling

data class BenkRevurdering(
    override val felles: BenkBehandlingsfelles,
    val id: RammebehandlingId,
    val status: BenkBehandlingsstatus,
    val resultat: BenkRevurderingResultat?,
) : BenkBehandling

data class BenkMeldekort(
    override val felles: BenkBehandlingsfelles,
    /** Både meldekortbehandlinger og brukers meldekort har [MeldekortId] — brukers meldekort får id-en sin fra meldekort-api. */
    val id: MeldekortId,
    val status: BenkBehandlingsstatus,
    val type: BenkMeldekortType,
    /**
     * Kjedene raden dekker, i kronologisk rekkefølge.
     * Én for innsendte og korrigerte meldekort, én eller flere for meldekortbehandlinger.
     */
    val meldeperioder: List<Periode>,
    /** Beregnet beløp for meldekortbehandlinger som er beregnet, ellers null. */
    val beløp: Int?,
) : BenkBehandling

data class BenkKlagebehandling(
    override val felles: BenkBehandlingsfelles,
    val id: KlagebehandlingId,
    val status: BenkBehandlingsstatus,
    val kravtidspunkt: LocalDateTime,
    val resultat: BenkKlagebehandlingResultat?,
) : BenkBehandling

data class BenkTilbakekreving(
    override val felles: BenkBehandlingsfelles,
    val id: TilbakekrevingId,
    val status: BenkTilbakekrevingStatus,
    val beløp: BigDecimal,
    val kilde: BenkTilbakekrevingKilde,
    val kravgrunnlagPeriode: Periode,
    /** Lenke til behandlingen i tilbakekrevingsløsningen, slik personoversikten bruker den. */
    val url: String,
) : BenkBehandling
