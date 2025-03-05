package no.nav.tiltakspenger.saksbehandling.domene.sak

import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.felles.min
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.utbetaling.domene.Utbetalinger
import java.time.LocalDate

data class Sak(
    val id: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val behandlinger: Behandlinger,
    val vedtaksliste: Vedtaksliste,
    val meldekortBehandlinger: MeldekortBehandlinger,
    val meldeperiodeKjeder: MeldeperiodeKjeder,
    val brukersMeldekort: List<BrukersMeldekort>,
    val utbetalinger: Utbetalinger,
    val soknader: List<Søknad>,
) {
    /** Nåtilstand. Dette er sakens totale vedtaksperiode. Vær veldig obs når du bruker denne, fordi den sier ikke noe om antall perioder, om de gir rett eller ikke. */
    val vedtaksperiode: Periode? = vedtaksliste.vedtaksperiode

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    val førsteDagSomGirRett = vedtaksliste.førsteDagSomGirRett

    /** Nåtilstand. Tar utgangspunkt i tidslinja på saken og henter den siste innvilget dagen. */
    @Suppress("unused")
    val sisteDagSomGirRett = vedtaksliste.sisteDagSomGirRett

    /**
     * En sak kan kun ha en førstegangsbehandling, dersom perioden til den vedtatte førstegangsbehandlingen skal utvides eller minskes (den må fortsatt være sammenhengende) må vi revurdere/omgjøre, ikke førstegangsbehandle på nytt.
     * Dersom den nye søknaden ikke overlapper eller tilstøter den gamle perioden, må vi opprette en ny sak som får en ny førstegangsbehandling.
     */
    val førstegangsbehandling: Behandling? = behandlinger.førstegangsbehandling
    val revurderinger = behandlinger.revurderinger

    val saksopplysningsperiode: Periode? = førstegangsbehandling?.saksopplysningsperiode

    /** Henter fra siste godkjente meldekort */
    @Suppress("unused")
    val sisteNavkontor: Navkontor? by lazy {
        meldekortBehandlinger.sisteGodkjenteMeldekort?.navkontor
    }

    val barnetilleggsperioder: Periodisering<AntallBarn> by lazy { vedtaksliste.barnetilleggsperioder }

    val tiltaksdeltagelseperioder: Periodisering<Tiltaksdeltagelse> by lazy { vedtaksliste.valgteTiltaksdeltakelser }

    val tiltakstypeperioder: Periodisering<TiltakstypeSomGirRett> by lazy { vedtaksliste.tiltakstypeperioder }

    fun antallBarnForDag(dag: LocalDate): AntallBarn {
        return vedtaksliste.antallBarnForDag(dag)
    }

    fun hentMeldekortBehandlingForMeldekortBehandlingId(meldekortId: MeldekortId): MeldekortBehandling? {
        return meldekortBehandlinger.hentMeldekortBehandlingForMeldekortBehandlingId(meldekortId)
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    @Suppress("unused")
    fun hentMeldekortBehandlingForMeldeperiodeId(meldeperiodeId: MeldeperiodeId): List<MeldekortBehandling> {
        return meldekortBehandlinger.hentMeldekortBehandlingForMeldeperiodeId(meldeperiodeId)
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingForMeldeperiodeKjedeId(id: MeldeperiodeKjedeId): List<MeldekortBehandling> {
        return meldekortBehandlinger.hentMeldekortBehandlingForMeldeperiodeKjedeId(id)
    }

    fun hentMeldeperiode(id: MeldeperiodeId): Meldeperiode? {
        return meldeperiodeKjeder.hentMeldeperiode(id)
    }

    fun hentMeldeperiodeForKjedeId(meldeperiodeKjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(meldeperiodeKjedeId)
    }

    fun hentMeldekortUnderBehandling(): MeldekortBehandling? = meldekortBehandlinger.meldekortUnderBehandling

    /** Den er kun trygg inntil vi revurderer antall dager. */
    fun hentAntallDager(): Int? = vedtaksliste.førstegangsvedtak?.behandling?.maksDagerMedTiltakspengerForPeriode
    fun hentTynnSak(): TynnSak = TynnSak(this.id, this.fnr, this.saksnummer)

    fun hentTiltaksnavnForPeriode(periode: Periode): String? =
        vedtaksliste.valgteTiltaksdeltakelserForForstegangsvedtakOgPeriode(periode).firstOrNull()?.typeNavn

    fun hentBehandling(behandlingId: BehandlingId): Behandling? = behandlinger.hentBehandling(behandlingId)

    fun sisteUtbetalteMeldekortDag(): LocalDate? = meldekortBehandlinger.sisteUtbetalteMeldekortDag

    /**
     * Vil være innenfor [vedtaksperiode]. Dersom vi ikke har et vedtak, vil den være null.
     */
    fun førsteLovligeStansdato(): LocalDate? = sisteUtbetalteMeldekortDag()?.plusDays(1)?.let {
        min(it, vedtaksperiode!!.tilOgMed)
    } ?: vedtaksperiode?.fraOgMed

    fun erSisteVersjonAvMeldeperiode(meldeperiode: Meldeperiode): Boolean {
        return meldeperiodeKjeder.erSisteVersjonAvMeldeperiode(meldeperiode)
    }
}
