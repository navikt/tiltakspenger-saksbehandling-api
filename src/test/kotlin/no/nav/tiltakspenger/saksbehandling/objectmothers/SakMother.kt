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
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.virkningsperiode
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalinger
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettVedtak
import java.time.Clock
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
        saksnummer: Saksnummer = Saksnummer(
            iDag,
            løpenummer,
        ),
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
            periode = søknad.saksopplysningsperiode(),
            ytelser = emptyList(),
        ),
        barnetillegg: Barnetillegg? = null,
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = listOf(
            Pair(
                virkningsperiode,
                registrerteTiltak.first().eksternDeltagelseId,
            ),
        ),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        clock: Clock = fixedClock,
        antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode> = Periodisering(
            PeriodeMedVerdi(
                AntallDagerForMeldeperiode((MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)),
                virkningsperiode,
            ),
        ),
    ): Pair<Sak, Søknadsbehandling> {
        val søknadsbehandling =
            runBlocking {
                val behandling = Søknadsbehandling.opprett(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    søknad = søknad,
                    saksbehandler = saksbehandler,
                    hentSaksopplysninger = { saksopplysninger },
                    clock = clock,
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
                            barnetillegg = barnetillegg,
                            fritekstTilVedtaksbrev = null,
                            begrunnelseVilkårsvurdering = null,
                            behandlingsperiode = virkningsperiode,
                            tiltaksdeltakelser = valgteTiltaksdeltakelser,
                            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                            avslagsgrunner = avslagsgrunner,
                            resultat = resultat,
                        ),
                        clock = clock,
                    )
                }
            }
        return Sak(
            id = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            behandlinger = Behandlinger(søknadsbehandling),
            vedtaksliste = Vedtaksliste.empty(),
            meldekortBehandlinger = MeldekortBehandlinger.empty(),
            utbetalinger = Utbetalinger(emptyList()),
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            brukersMeldekort = emptyList(),
            soknader = listOf(søknad),
        ) to søknadsbehandling
    }

    fun nySakMedVedtak(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        saksbehandler: Saksbehandler = saksbehandler(),
        virkningsperiode: Periode = virkningsperiode(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        clock: Clock = fixedClock,
    ): Triple<Sak, Vedtak, Behandling> {
        val (sak, søknadsbehandling) = this.sakMedOpprettetBehandling(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            virkningsperiode = virkningsperiode,
            saksbehandler = saksbehandler,
        )

        val iverksattBehandling = søknadsbehandling.tilBeslutning(
            SendSøknadsbehandlingTilBeslutningKommando(
                sakId = sakId,
                behandlingId = søknadsbehandling.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                barnetillegg = null,
                fritekstTilVedtaksbrev = null,
                begrunnelseVilkårsvurdering = null,
                behandlingsperiode = virkningsperiode,
                tiltaksdeltakelser = søknadsbehandling.saksopplysninger.tiltaksdeltagelse.map {
                    Pair(virkningsperiode, it.eksternDeltagelseId)
                }.toList(),
                antallDagerPerMeldeperiode = Periodisering(PeriodeMedVerdi(AntallDagerForMeldeperiode(10), virkningsperiode)),
                avslagsgrunner = null,
                resultat = SøknadsbehandlingType.INNVILGELSE,
            ),
            clock = clock,
        ).taBehandling(beslutter)
            .iverksett(
                utøvendeBeslutter = beslutter,
                attestering = ObjectMother.godkjentAttestering(beslutter),
                clock = clock,
            )

        val sakMedIverksattBehandling = sak.copy(behandlinger = Behandlinger(iverksattBehandling))
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
        clock: Clock = fixedClock,
    ): Triple<Sak, Rammevedtak, Behandling> {
        val (sak, søknadsbehandling) = this.sakMedOpprettetBehandling(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            virkningsperiode = virkningsperiode,
            saksbehandler = saksbehandler,
        )

        val iverksattBehandling = søknadsbehandling.tilBeslutning(
            SendSøknadsbehandlingTilBeslutningKommando(
                sakId = sakId,
                behandlingId = søknadsbehandling.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                barnetillegg = null,
                fritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nySakMedAvslagsvedtak"),
                begrunnelseVilkårsvurdering = null,
                behandlingsperiode = virkningsperiode,
                tiltaksdeltakelser = søknadsbehandling.saksopplysninger.tiltaksdeltagelse.map {
                    Pair(virkningsperiode, it.eksternDeltagelseId)
                }.toList(),
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
                resultat = SøknadsbehandlingType.AVSLAG,
                antallDagerPerMeldeperiode = Periodisering(PeriodeMedVerdi(AntallDagerForMeldeperiode(10), virkningsperiode)),
            ),
            clock = clock,
        ).taBehandling(beslutter)
            .iverksett(
                utøvendeBeslutter = beslutter,
                attestering = ObjectMother.godkjentAttestering(beslutter),
                clock = clock,
            )

        val sakMedIverksattBehandling = sak.copy(behandlinger = Behandlinger(iverksattBehandling))
        val sakMedVedtak = sakMedIverksattBehandling.opprettVedtak(iverksattBehandling, clock)

        return Triple(sakMedVedtak.first, sakMedVedtak.second, iverksattBehandling)
    }
}
