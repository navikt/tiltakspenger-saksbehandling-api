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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.virkningsperiode
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
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
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
        virkningsperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: InnvilgbarSøknad =
            nyInnvilgbarSøknad(
                sakId = sakId,
                saksnummer = saksnummer,
                søknadstiltak =
                søknadstiltak(
                    deltakelseFom = virkningsperiode.fraOgMed,
                    deltakelseTom = virkningsperiode.tilOgMed,
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
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(virkningsperiode),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = listOf(
            Pair(
                virkningsperiode,
                registrerteTiltak.first().eksternDeltakelseId,
            ),
        ),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        clock: Clock = fixedClock,
        antallDagerPerMeldeperiode: List<Pair<Periode, AntallDagerForMeldeperiode>> = listOf(virkningsperiode to AntallDagerForMeldeperiode.default),
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
                )

                behandling.oppdater(
                    when (resultat) {
                        SøknadsbehandlingType.INNVILGELSE -> OppdaterSøknadsbehandlingKommando.Innvilgelse(
                            sakId = sakId,
                            behandlingId = behandling.id,
                            correlationId = CorrelationId.generate(),
                            saksbehandler = saksbehandler,
                            barnetillegg = barnetillegg,
                            fritekstTilVedtaksbrev = null,
                            begrunnelseVilkårsvurdering = null,
                            innvilgelsesperiode = virkningsperiode,
                            tiltaksdeltakelser = valgteTiltaksdeltakelser,
                            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                            automatiskSaksbehandlet = automatiskSaksbehandlet,
                        )

                        SøknadsbehandlingType.AVSLAG -> OppdaterSøknadsbehandlingKommando.Avslag(
                            sakId = sakId,
                            behandlingId = behandling.id,
                            correlationId = CorrelationId.generate(),
                            saksbehandler = saksbehandler,
                            fritekstTilVedtaksbrev = null,
                            begrunnelseVilkårsvurdering = null,
                            avslagsgrunner = avslagsgrunner!!,
                            automatiskSaksbehandlet = automatiskSaksbehandlet,
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
        virkningsperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        søknad: InnvilgbarSøknad =
            nyInnvilgbarSøknad(
                sakId = sakId,
                saksnummer = saksnummer,
                søknadstiltak =
                søknadstiltak(
                    deltakelseFom = virkningsperiode.fraOgMed,
                    deltakelseTom = virkningsperiode.tilOgMed,
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
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        saksbehandler: Saksbehandler = saksbehandler(),
        virkningsperiode: Periode = virkningsperiode(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(virkningsperiode),
        kanSendeInnHelgForMeldekort: Boolean = false,
        clock: Clock = fixedClock,
    ): Triple<Sak, Vedtak, Rammebehandling> {
        val (sak, søknadsbehandling) = this.sakMedOpprettetBehandling(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            virkningsperiode = virkningsperiode,
            saksbehandler = saksbehandler,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        )

        val iverksattBehandling: Rammebehandling = søknadsbehandling.oppdater(
            OppdaterSøknadsbehandlingKommando.Innvilgelse(
                sakId = sakId,
                behandlingId = søknadsbehandling.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                barnetillegg = barnetillegg,
                fritekstTilVedtaksbrev = null,
                begrunnelseVilkårsvurdering = null,
                innvilgelsesperiode = virkningsperiode,
                tiltaksdeltakelser = søknadsbehandling.saksopplysninger.tiltaksdeltakelser.map {
                    Pair(virkningsperiode, it.eksternDeltakelseId)
                }.toList(),
                antallDagerPerMeldeperiode = listOf(virkningsperiode to AntallDagerForMeldeperiode.default),
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
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        saksbehandler: Saksbehandler = saksbehandler(),
        virkningsperiode: Periode = virkningsperiode(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        kanSendeInnHelgForMeldekort: Boolean = false,
        clock: Clock = fixedClock,
    ): Triple<Sak, Rammevedtak, Rammebehandling> {
        val (sak, søknadsbehandling) = this.sakMedOpprettetBehandling(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            virkningsperiode = virkningsperiode,
            saksbehandler = saksbehandler,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        )

        val iverksattBehandling = søknadsbehandling.oppdater(
            OppdaterSøknadsbehandlingKommando.Avslag(
                sakId = sakId,
                behandlingId = søknadsbehandling.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = FritekstTilVedtaksbrev.create("nySakMedAvslagsvedtak"),
                begrunnelseVilkårsvurdering = null,
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
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
}
