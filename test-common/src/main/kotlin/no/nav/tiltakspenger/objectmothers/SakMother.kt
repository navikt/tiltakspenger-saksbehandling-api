package no.nav.tiltakspenger.objectmothers

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.objectmothers.ObjectMother.nySøknad
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterBarnetilleggKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.utbetaling.domene.Utbetalinger
import java.time.LocalDate

interface SakMother {
    fun nySak(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
    ): Sak = Sak(
        id = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
        behandlinger = Behandlinger(emptyList()),
        vedtaksliste = Vedtaksliste.empty(),
        meldekortBehandlinger = MeldekortBehandlinger.empty(),
        utbetalinger = Utbetalinger(emptyList()),
        meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
        brukersMeldekort = emptyList(),
        soknader = emptyList(),
    )

    fun sakMedOpprettetBehandling(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        iDag: LocalDate = LocalDate.of(2023, 1, 1),
        løpenummer: Int = 1001,
        saksnummer: Saksnummer = Saksnummer(iDag, løpenummer),
        vurderingsperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: Søknad =
            nySøknad(
                søknadstiltak =
                søknadstiltak(
                    deltakelseFom = vurderingsperiode.fraOgMed,
                    deltakelseTom = vurderingsperiode.tilOgMed,
                ),
            ),
        registrerteTiltak: List<Tiltaksdeltagelse> = listOf(søknad.tiltak.toTiltak()),
        saksopplysninger: Saksopplysninger = Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltagelse = registrerteTiltak.single(),
        ),
        barnetillegg: Barnetillegg? = null,
    ): Sak {
        val førstegangsbehandling =
            runBlocking {
                val behandling = Behandling.opprettSøknadsbehandling(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    søknad = søknad,
                    saksbehandler = saksbehandler,
                    hentSaksopplysninger = { saksopplysninger },
                ).getOrFail()

                if (barnetillegg == null) {
                    behandling
                } else {
                    behandling.oppdaterBarnetillegg(
                        OppdaterBarnetilleggKommando(
                            sakId = sakId,
                            behandlingId = behandling.id,
                            barnetillegg = barnetillegg,
                            correlationId = CorrelationId.generate(),
                            saksbehandler = saksbehandler,
                        ),
                    )
                }
            }
        return Sak(
            id = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            behandlinger = Behandlinger(førstegangsbehandling),
            vedtaksliste = Vedtaksliste.empty(),
            meldekortBehandlinger = MeldekortBehandlinger.empty(),
            utbetalinger = Utbetalinger(emptyList()),
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            brukersMeldekort = emptyList(),
            soknader = listOf(søknad),
        )
    }
}
