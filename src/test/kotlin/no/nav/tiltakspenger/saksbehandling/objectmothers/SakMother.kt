package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterSøknadsbehandlingAvslagKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterSøknadsbehandlingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.vedtaksperiode
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettVedtak
import java.time.Clock
import java.time.LocalDate
import kotlin.Pair

interface SakMother {
    fun nySak(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        clock: Clock = KlokkeMother.clock,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        søknader: List<Søknad> = emptyList(),
        behandlinger: Rammebehandlinger = Rammebehandlinger.empty(),
        kanSendeInnHelgForMeldekort: Boolean = false,
    ): Sak = Sak(
        id = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
        behandlinger = Behandlinger(
            rammebehandlinger = behandlinger,
            meldekortbehandlinger = Meldekortbehandlinger.empty(),
            klagebehandlinger = Klagebehandlinger.empty(),
        ),
        vedtaksliste = Vedtaksliste.empty(),
        meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
        brukersMeldekort = emptyList(),
        søknader = søknader,
        kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
    )

    fun sakMedOpprettetBehandling(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        iDag: LocalDate = LocalDate.of(2023, 1, 1),
        løpenummer: Int = 1001,
        saksnummer: Saksnummer = Saksnummer(
            iDag,
            løpenummer,
        ),
        vedtaksperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: InnvilgbarSøknad =
            nyInnvilgbarSøknad(
                sakId = sakId,
                saksnummer = saksnummer,
                søknadstiltak =
                søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                ),
            ),
        registrerteTiltak: Tiltaksdeltakelser = Tiltaksdeltakelser(listOf(søknad.tiltak.toTiltak())),
        saksopplysninger: Saksopplysninger = Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltakelser = registrerteTiltak,
            ytelser = Ytelser.fromList(emptyList(), registrerteTiltak.totalPeriode!!, iDag.atStartOfDay()),
            tiltakspengevedtakFraArena = TiltakspengevedtakFraArena.fromList(
                emptyList(),
                registrerteTiltak.totalPeriode!!,
                iDag.atStartOfDay(),
            ),
            oppslagstidspunkt = iDag.atStartOfDay(),
        ),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(vedtaksperiode),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = vedtaksperiode,
                tiltaksdeltakelse = registrerteTiltak.first(),
            ),
        ),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        clock: Clock = fixedClock,
        kanSendeInnHelgForMeldekort: Boolean = false,
        sak: Sak = ObjectMother.nySak(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            søknader = listOf(søknad),
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        ),
        correlationId: CorrelationId = CorrelationId.generate(),
        automatiskSaksbehandlet: Boolean = false,
        klagebehandling: Klagebehandling? = null,
    ): Pair<Sak, Søknadsbehandling> {
        val søknadsbehandling =
            runBlocking {
                val behandling = Søknadsbehandling.opprett(
                    sak = sak,
                    søknad = søknad,
                    saksbehandler = saksbehandler,
                    hentSaksopplysninger = { _, _, _, _, _ -> saksopplysninger },
                    clock = clock,
                    correlationId = correlationId,
                    klagebehandling = klagebehandling,
                )

                behandling.oppdater(
                    when (resultat) {
                        SøknadsbehandlingType.INNVILGELSE -> oppdaterSøknadsbehandlingInnvilgelseKommando(
                            sakId = sakId,
                            behandlingId = behandling.id,
                            correlationId = CorrelationId.generate(),
                            saksbehandler = saksbehandler,
                            barnetillegg = barnetillegg,
                            fritekstTilVedtaksbrev = null,
                            begrunnelseVilkårsvurdering = null,
                            innvilgelsesperioder = innvilgelsesperioder,
                            automatiskSaksbehandlet = automatiskSaksbehandlet,
                        )

                        SøknadsbehandlingType.AVSLAG -> oppdaterSøknadsbehandlingAvslagKommando(
                            sakId = sakId,
                            behandlingId = behandling.id,
                            correlationId = CorrelationId.generate(),
                            saksbehandler = saksbehandler,
                            fritekstTilVedtaksbrev = null,
                            begrunnelseVilkårsvurdering = null,
                            avslagsgrunner = avslagsgrunner!!,
                        )
                    },
                    clock = clock,
                    utbetaling = null,
                    omgjørRammevedtak = OmgjørRammevedtak.empty,
                ).getOrFail()
            }

        return Sak(
            id = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            behandlinger = Behandlinger(
                rammebehandlinger = Rammebehandlinger(søknadsbehandling),
                meldekortbehandlinger = Meldekortbehandlinger.empty(),
                klagebehandlinger = Klagebehandlinger.empty(),
            ),
            vedtaksliste = Vedtaksliste.empty(),
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            brukersMeldekort = emptyList(),
            søknader = listOf(søknad),
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        ) to søknadsbehandling
    }

    fun sakMedOpprettetAutomatiskBehandling(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        iDag: LocalDate = LocalDate.of(2023, 1, 1),
        løpenummer: Int = 1001,
        saksnummer: Saksnummer = Saksnummer(
            iDag,
            løpenummer,
        ),
        vedtaksperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        søknad: InnvilgbarSøknad =
            nyInnvilgbarSøknad(
                sakId = sakId,
                saksnummer = saksnummer,
                søknadstiltak =
                søknadstiltak(
                    deltakelseFom = vedtaksperiode.fraOgMed,
                    deltakelseTom = vedtaksperiode.tilOgMed,
                ),
            ),
        registrerteTiltak: Tiltaksdeltakelser = Tiltaksdeltakelser(listOf(søknad.tiltak.toTiltak())),
        saksopplysninger: Saksopplysninger = Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltakelser = registrerteTiltak,
            ytelser = Ytelser.fromList(emptyList(), registrerteTiltak.totalPeriode!!, iDag.atStartOfDay()),
            tiltakspengevedtakFraArena = TiltakspengevedtakFraArena.fromList(
                emptyList(),
                registrerteTiltak.totalPeriode!!,
                iDag.atStartOfDay(),
            ),
            oppslagstidspunkt = iDag.atStartOfDay(),
        ),
        kanSendeInnHelgForMeldekort: Boolean = false,
        clock: Clock = fixedClock,
        correlationId: CorrelationId = CorrelationId.generate(),
        sak: Sak = nySak(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            søknader = listOf(søknad),
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        ),
    ): Pair<Sak, Søknadsbehandling> {
        val søknadsbehandling =
            runBlocking {
                Søknadsbehandling.opprettAutomatiskBehandling(
                    søknad = søknad,
                    hentSaksopplysninger = { _, _, _, _, _ -> saksopplysninger },
                    clock = clock,
                    sak = sak,
                    correlationId = correlationId,
                )
            }
        return Sak(
            id = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            behandlinger = Behandlinger(
                rammebehandlinger = Rammebehandlinger(søknadsbehandling),
                meldekortbehandlinger = Meldekortbehandlinger.empty(),
                klagebehandlinger = Klagebehandlinger.empty(),
            ),
            vedtaksliste = Vedtaksliste.empty(),
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            brukersMeldekort = emptyList(),
            søknader = listOf(søknad),
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        ) to søknadsbehandling
    }

    fun nySakMedVedtak(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        clock: Clock = fixedClock,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        saksbehandler: Saksbehandler = saksbehandler(),
        vedtaksperiode: Periode = vedtaksperiode(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(vedtaksperiode),
        kanSendeInnHelgForMeldekort: Boolean = false,
    ): Triple<Sak, Vedtak, Rammebehandling> {
        val (sak, søknadsbehandling) = this.sakMedOpprettetBehandling(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            vedtaksperiode = vedtaksperiode,
            saksbehandler = saksbehandler,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        )

        val iverksattBehandling: Rammebehandling = søknadsbehandling.oppdater(
            oppdaterSøknadsbehandlingInnvilgelseKommando(
                sakId = sakId,
                behandlingId = søknadsbehandling.id,
                saksbehandler = saksbehandler,
                barnetillegg = barnetillegg,
                innvilgelsesperioder = listOf(
                    innvilgelsesperiodeKommando(
                        innvilgelsesperiode = vedtaksperiode,
                        tiltaksdeltakelse = søknadsbehandling.saksopplysninger.tiltaksdeltakelser.first(),
                    ),
                ),
                automatiskSaksbehandlet = søknadsbehandling.automatiskSaksbehandlet,
            ),
            clock = clock,
            utbetaling = null,
            omgjørRammevedtak = OmgjørRammevedtak.empty,
        ).getOrFail().tilBeslutning(
            saksbehandler = saksbehandler,
            clock = clock,
        ).taBehandling(beslutter, clock)
            .iverksett(
                utøvendeBeslutter = beslutter,
                attestering = ObjectMother.godkjentAttestering(beslutter),
                clock = clock,
            )

        val sakMedIverksattBehandling = sak.oppdaterRammebehandling(iverksattBehandling)
        val sakMedVedtak = sakMedIverksattBehandling.opprettVedtak(iverksattBehandling, clock)

        return Triple(sakMedVedtak.first, sakMedVedtak.second, iverksattBehandling)
    }

    fun nySakMedAvslagsvedtak(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        clock: Clock = fixedClock,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        saksbehandler: Saksbehandler = saksbehandler(),
        avslagsperiode: Periode = vedtaksperiode(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        kanSendeInnHelgForMeldekort: Boolean = false,
    ): Triple<Sak, Rammevedtak, Rammebehandling> {
        val (sak, søknadsbehandling) = this.sakMedOpprettetBehandling(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            vedtaksperiode = avslagsperiode,
            saksbehandler = saksbehandler,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        )

        val iverksattBehandling = søknadsbehandling.oppdater(
            oppdaterSøknadsbehandlingAvslagKommando(
                sakId = sakId,
                behandlingId = søknadsbehandling.id,
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = "nySakMedAvslagsvedtak",
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
            ),
            clock = clock,
            utbetaling = null,
            omgjørRammevedtak = OmgjørRammevedtak.empty,
        ).getOrFail().tilBeslutning(
            saksbehandler = saksbehandler,
            clock = clock,
        ).taBehandling(beslutter, clock)
            .iverksett(
                utøvendeBeslutter = beslutter,
                attestering = ObjectMother.godkjentAttestering(beslutter),
                clock = clock,
            )

        val sakMedIverksattBehandling = sak.oppdaterRammebehandling(iverksattBehandling)
        val sakMedVedtak = sakMedIverksattBehandling.opprettVedtak(iverksattBehandling, clock)

        return Triple(sakMedVedtak.first, sakMedVedtak.second, iverksattBehandling)
    }
}
