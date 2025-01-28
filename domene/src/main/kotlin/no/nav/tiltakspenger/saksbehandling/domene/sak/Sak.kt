package no.nav.tiltakspenger.saksbehandling.domene.sak

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.felles.exceptions.TilgangException
import no.nav.tiltakspenger.felles.min
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeOppretteBehandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltak
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
) {
    /** Dette er sakens totale vedtaksperiode. Per tidspunkt er den sammenhengende, men hvis vi lar en sak gjelde på tvers av tiltak, vil den kunne ha hull. */
    val vedtaksperiode: Periode? = vedtaksliste.vedtaksperiode

    val sisteInnvilgetDato = vedtaksliste.sisteInnvilgetDato

    /**
     * En sak kan kun ha en førstegangsbehandling, dersom perioden til den vedtatte førstegangsbehandlingen skal utvides eller minskes (den må fortsatt være sammenhengende) må vi revurdere/omgjøre, ikke førstegangsbehandle på nytt.
     * Dersom den nye søknaden ikke overlapper eller tilstøter den gamle perioden, må vi opprette en ny sak som får en ny førstegangsbehandling.
     */
    val førstegangsbehandling: Behandling = behandlinger.førstegangsbehandling
    val revurderinger = behandlinger.revurderinger

    /** Henter fra siste godkjente meldekort */
    val sisteNavkontor: Navkontor? by lazy {
        meldekortBehandlinger.sisteGodkjenteMeldekort?.navkontor
    }

    fun hentMeldekortBehandling(meldekortId: MeldekortId): MeldekortBehandling? {
        return meldekortBehandlinger.hentMeldekortBehandling(meldekortId)
    }

    fun hentMeldekortBehandling(hendelseId: HendelseId): MeldekortBehandling? {
        return meldekortBehandlinger.hentMeldekortBehandling(hendelseId)
    }

    fun hentMeldeperiode(hendelseId: HendelseId): Meldeperiode? {
        return meldeperiodeKjeder.hentMeldeperiode(hendelseId)
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

    companion object {
        fun lagSak(
            sakId: SakId = SakId.random(),
            saksnummer: Saksnummer,
            søknad: Søknad,
            fødselsdato: LocalDate,
            saksbehandler: Saksbehandler,
            registrerteTiltak: List<Tiltak>,
        ): Either<KanIkkeOppretteBehandling, Sak> {
            if (!saksbehandler.erSaksbehandler()) {
                throw TilgangException("Saksbehandler ${saksbehandler.navIdent} må ha rollen SAKSBEHANDLER. søknadId: ${søknad.id} roller: ${saksbehandler.roller}")
            }
            val fnr = søknad.fnr
            val førstegangsbehandling =
                Behandling.opprettFørstegangsbehandling(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    søknad = søknad,
                    fødselsdato = fødselsdato,
                    saksbehandler = saksbehandler,
                    registrerteTiltak = registrerteTiltak,
                ).getOrElse { return it.left() }
            return Sak(
                id = sakId,
                fnr = fnr,
                saksnummer = saksnummer,
                behandlinger = Behandlinger(førstegangsbehandling),
                vedtaksliste = Vedtaksliste.empty(),
                meldekortBehandlinger = MeldekortBehandlinger.empty(førstegangsbehandling.tiltakstype),
                utbetalinger = Utbetalinger(emptyList()),
                meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
                brukersMeldekort = emptyList(),
            ).right()
        }
    }
}
