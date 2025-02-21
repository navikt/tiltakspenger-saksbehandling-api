package no.nav.tiltakspenger.saksbehandling.domene.sak

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.felles.min
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
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
    val sisteDagSomGirRett = vedtaksliste.sisteDagSomGirRett

    /**
     * En sak kan kun ha en førstegangsbehandling, dersom perioden til den vedtatte førstegangsbehandlingen skal utvides eller minskes (den må fortsatt være sammenhengende) må vi revurdere/omgjøre, ikke førstegangsbehandle på nytt.
     * Dersom den nye søknaden ikke overlapper eller tilstøter den gamle perioden, må vi opprette en ny sak som får en ny førstegangsbehandling.
     */
    val førstegangsbehandling: Behandling? = behandlinger.førstegangsbehandling
    val revurderinger = behandlinger.revurderinger

    val saksopplysningsperiode: Periode? = førstegangsbehandling?.saksopplysningsperiode

    /** Henter fra siste godkjente meldekort */
    val sisteNavkontor: Navkontor? by lazy {
        meldekortBehandlinger.sisteGodkjenteMeldekort?.navkontor
    }

    /** null dersom det ikke finnes behandlinger */
    val erNyFlyt: Boolean? = førstegangsbehandling?.erNyFlyt

    fun hentMeldekortBehandlingForMeldekortBehandlingId(meldekortId: MeldekortId): MeldekortBehandling? {
        return meldekortBehandlinger.hentMeldekortBehandlingForMeldekortBehandlingId(meldekortId)
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingForMeldeperiodeId(id: HendelseId): List<MeldekortBehandling> {
        return meldekortBehandlinger.hentMeldekortBehandlingForMeldeperiodeId(id)
    }

    /** Flere behandlinger kan være knyttet til samme versjon av meldeperioden. */
    fun hentMeldekortBehandlingForMeldeperiodeKjedeId(id: MeldeperiodeKjedeId): List<MeldekortBehandling> {
        return meldekortBehandlinger.hentMeldekortBehandlingForMeldeperiodeKjedeId(id)
    }

    fun hentMeldeperiode(id: HendelseId): Meldeperiode? {
        return meldeperiodeKjeder.hentMeldeperiode(id)
    }

    fun hentMeldeperiodeForKjedeId(meldeperiodeKjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.hentSisteMeldeperiodeForKjedeId(meldeperiodeKjedeId)
    }

    fun hentMeldekortUnderBehandling(): MeldekortBehandling? = meldekortBehandlinger.meldekortUnderBehandling

    /** Den er kun trygg inntil vi revurderer antall dager. */
    fun hentAntallDager(): Int? = vedtaksliste.førstegangsvedtak?.behandling?.maksDagerMedTiltakspengerForPeriode
    fun hentTynnSak(): TynnSak = TynnSak(this.id, this.fnr, this.saksnummer)

    /** Den er kun trygg inntil vi støtter mer enn ett tiltak på én sak. */
    fun hentTiltaksnavn(): String? = vedtaksliste.førstegangsvedtak?.behandling?.tiltaksnavn

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
