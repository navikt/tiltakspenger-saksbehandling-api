package no.nav.tiltakspenger.objectmothers

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother.nySøknad
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.vedtak.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.vedtak.felles.januar
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.vedtak.utbetaling.domene.Utbetalinger
import java.time.LocalDate

interface SakMother {
    fun nySak(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        søknader: List<Søknad> = emptyList(),
        behandlinger: Behandlinger = Behandlinger(emptyList()),
    ): Sak = Sak(
        id = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
        behandlinger = behandlinger,
        vedtaksliste = Vedtaksliste.empty(),
        meldekortBehandlinger = MeldekortBehandlinger.empty(),
        utbetalinger = Utbetalinger(emptyList()),
        meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
        brukersMeldekort = emptyList(),
        soknader = søknader,
    )

    fun sakMedOpprettetBehandling(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        iDag: LocalDate = LocalDate.of(2023, 1, 1),
        løpenummer: Int = 1001,
        saksnummer: Saksnummer = Saksnummer(iDag, løpenummer),
        virkningsperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: Søknad =
            nySøknad(
                sakId = sakId,
                saksnummer = saksnummer,
                søknadstiltak =
                søknadstiltak(
                    deltakelseFom = virkningsperiode.fraOgMed,
                    deltakelseTom = virkningsperiode.tilOgMed,
                ),
            ),
        registrerteTiltak: List<Tiltaksdeltagelse> = listOf(søknad.tiltak.toTiltak()),
        saksopplysninger: Saksopplysninger = Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltagelse = registrerteTiltak,
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
                    behandling.tilBeslutning(
                        SendSøknadsbehandlingTilBeslutningKommando(
                            sakId = sakId,
                            behandlingId = behandling.id,
                            correlationId = CorrelationId.generate(),
                            saksbehandler = saksbehandler,
                            begrunnelse = barnetillegg.begrunnelse,
                            perioder = barnetillegg.periodisering.perioderMedVerdi.map { it.periode to it.verdi },
                            fritekstTilVedtaksbrev = null,
                            begrunnelseVilkårsvurdering = null,
                            innvilgelsesperiode = virkningsperiode,
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
