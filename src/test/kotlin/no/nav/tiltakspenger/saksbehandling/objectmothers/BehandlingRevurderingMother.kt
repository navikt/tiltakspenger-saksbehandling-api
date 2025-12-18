package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando.Innvilgelse.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.godkjentAttestering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.navkontor
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyRammevedtakInnvilgelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyVedtattSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterRevurderingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

interface BehandlingRevurderingMother : MotherOfAllMothers {
    fun revurderingVedtaksperiode() = 2.januar(2023) til 31.mars(2023)

    fun nyOpprettetRevurderingStans(
        clock: Clock = this.clock,
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        saksopplysningsperiode: Periode = revurderingVedtaksperiode(),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
                clock = clock,
            )
        },
    ): Revurdering {
        return runBlocking {
            Revurdering.opprettStans(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = hentSaksopplysninger(saksopplysningsperiode),
                clock = clock,
            ).copy(id = id)
        }
    }

    fun nyRevurderingStansKlarTilBeslutning(
        clock: Clock = this.clock,
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev.createOrThrow("nyRevurderingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: Begrunnelse = Begrunnelse.createOrThrow("nyRevurderingKlarTilBeslutning()"),
        vedtaksperiode: Periode = revurderingVedtaksperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
            clock = clock,
        ),
        valgteHjemler: NonEmptyList<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        stansFraOgMed: LocalDate?,
        stansTilOgMed: LocalDate?,
        førsteDagSomGirRett: LocalDate,
        sisteDagSomGirRett: LocalDate,
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
        kommando: OppdaterRevurderingKommando.Stans = OppdaterRevurderingKommando.Stans(
            sakId = sakId,
            behandlingId = id,
            saksbehandler = saksbehandler,
            correlationId = CorrelationId.generate(),
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            valgteHjemler = valgteHjemler,
            stansFraOgMed = OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed.create(stansFraOgMed),
            stansTilOgMed = OppdaterRevurderingKommando.Stans.ValgtStansTilOgMed.create(stansTilOgMed),
        ),
        utbetaling: BehandlingUtbetaling? = null,
    ): Revurdering {
        return this.nyOpprettetRevurderingStans(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            saksopplysningsperiode = vedtaksperiode,
            hentSaksopplysninger = { saksopplysninger },
            clock = clock,
        ).oppdaterStans(
            kommando = kommando,
            førsteDagSomGirRett = førsteDagSomGirRett,
            sisteDagSomGirRett = sisteDagSomGirRett,
            clock = clock,
            utbetaling = utbetaling,
            omgjørRammevedtak = omgjørRammevedtak,
        ).getOrFail().tilBeslutning(saksbehandler) as Revurdering
    }

    fun nyVedtattRevurderingStans(
        clock: Clock = this.clock,
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev.createOrThrow("nyRevurderingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: Begrunnelse = Begrunnelse.createOrThrow("nyRevurderingKlarTilBeslutning()"),
        vedtaksperiode: Periode = revurderingVedtaksperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
            clock = clock,
        ),
        valgteHjemler: Nel<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        attestering: Attestering = godkjentAttestering(beslutter, clock),
        stansFraOgMed: LocalDate?,
        stansTilOgMed: LocalDate?,
        førsteDagSomGirRett: LocalDate,
        sisteDagSomGirRett: LocalDate,
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
    ): Revurdering {
        return nyRevurderingStansKlarTilBeslutning(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            sendtTilBeslutning = sendtTilBeslutning,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            vedtaksperiode = vedtaksperiode,
            saksopplysninger = saksopplysninger,
            valgteHjemler = valgteHjemler,
            stansFraOgMed = stansFraOgMed,
            stansTilOgMed = stansTilOgMed,
            førsteDagSomGirRett = førsteDagSomGirRett,
            sisteDagSomGirRett = sisteDagSomGirRett,
            omgjørRammevedtak = omgjørRammevedtak,
            clock = clock,
        ).taBehandling(beslutter, clock).iverksett(
            utøvendeBeslutter = beslutter,
            attestering = attestering,
            clock = clock,
        ) as Revurdering
    }

    /**
     * Revurdering starter med tomt resultat - dvs. uten vedtaksperiode/innvilgelsesperiode
     */
    fun nyOpprettetRevurderingInnvilgelse(
        clock: Clock = this.clock,
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        saksopplysningsperiode: Periode = revurderingVedtaksperiode(),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = { saksopplysningsperiode ->
            saksopplysninger(
                fom = saksopplysningsperiode.fraOgMed,
                tom = saksopplysningsperiode.tilOgMed,
                clock = clock,
            )
        },
    ): Revurdering {
        return runBlocking {
            Revurdering.opprettInnvilgelse(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = hentSaksopplysninger(saksopplysningsperiode),
                clock = clock,
            ).copy(id = id)
        }
    }

    /**
     * @param innvilgelsesperioder vil default utledes fra [saksopplysningsperiode]. Hvis du overstyrer denne, bør du også overstyre [saksopplysningsperiode].
     */
    fun nyRevurderingInnvilgelseKlarTilBeslutning(
        clock: Clock = this.clock,
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        fritekstTilVedtaksbrev: String = "nyRevurderingKlarTilBeslutning()",
        begrunnelseVilkårsvurdering: String = "nyRevurderingKlarTilBeslutning()",
        saksopplysningsperiode: Periode = revurderingVedtaksperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = saksopplysningsperiode.fraOgMed,
            tom = saksopplysningsperiode.tilOgMed,
            clock = clock,
        ),
        navkontor: Navkontor = navkontor(),
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = saksopplysningsperiode,
                tiltaksdeltakelseId = saksopplysninger.tiltaksdeltakelser.first().eksternDeltakelseId,
            ),
        ),
        barnetillegg: Barnetillegg = barnetillegg(
            antallBarn = AntallBarn.ZERO,
            periode = Periode(
                innvilgelsesperioder.first().periode.fraOgMed,
                innvilgelsesperioder.last().periode.tilOgMed,
            ),
        ),
        beregning: Beregning? = null,
        simulering: Simulering? = null,
    ): Revurdering {
        val kommando = oppdaterRevurderingInnvilgelseKommando(
            sakId = sakId,
            behandlingId = id,
            saksbehandler = saksbehandler,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetillegg = barnetillegg,
        )

        return this.nyOpprettetRevurderingInnvilgelse(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            saksopplysningsperiode = saksopplysningsperiode,
            hentSaksopplysninger = { saksopplysninger },
            clock = clock,
        ).oppdaterInnvilgelse(
            kommando = kommando,
            clock = clock,
            utbetaling = if (beregning == null) {
                null
            } else {
                BehandlingUtbetaling(
                    beregning = beregning,
                    navkontor = navkontor,
                    simulering = simulering,
                )
            },
            omgjørRammevedtak = OmgjørRammevedtak.empty,
        ).getOrFail().tilBeslutning(saksbehandler = saksbehandler, clock = clock) as Revurdering
    }

    /**
     * @param innvilgelsesperioder vil default utledes fra [saksopplysningsperiode]. Hvis du overstyrer denne, bør du også overstyre [saksopplysningsperiode].
     */
    fun nyVedtattRevurderingInnvilgelse(
        clock: Clock = this.clock,
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: String = "nyRevurderingKlarTilBeslutning()",
        begrunnelseVilkårsvurdering: String = "nyRevurderingKlarTilBeslutning()",
        saksopplysningsperiode: Periode = revurderingVedtaksperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = saksopplysningsperiode.fraOgMed,
            tom = saksopplysningsperiode.tilOgMed,
            clock = clock,
        ),
        attestering: Attestering = godkjentAttestering(beslutter, clock),
        navkontor: Navkontor = navkontor(),
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = saksopplysningsperiode,
                tiltaksdeltakelseId = saksopplysninger.tiltaksdeltakelser.first().eksternDeltakelseId,
            ),
        ),
        barnetillegg: Barnetillegg = barnetillegg(
            antallBarn = AntallBarn.ZERO,
            periode = Periode(
                innvilgelsesperioder.first().periode.fraOgMed,
                innvilgelsesperioder.last().periode.tilOgMed,
            ),
        ),
        beregning: Beregning? = null,
    ): Revurdering {
        return nyRevurderingInnvilgelseKlarTilBeslutning(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            saksopplysningsperiode = saksopplysningsperiode,
            saksopplysninger = saksopplysninger,
            navkontor = navkontor,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetillegg = barnetillegg,
            beregning = beregning,
            clock = clock,
        ).taBehandling(beslutter, clock).iverksett(
            utøvendeBeslutter = beslutter,
            attestering = attestering,
            clock = clock,
        ) as Revurdering
    }

    fun nyOpprettetRevurderingOmgjøring(
        clock: Clock = this.clock,
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknadsbehandlingInnvilgelsesperiode: Periode = revurderingVedtaksperiode(),
        omgjøringInnvilgelsesperiode: Periode = revurderingVedtaksperiode(),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
                clock = clock,
            )
        },
        innvilgetSøknadsbehandling: Rammebehandling = nyVedtattSøknadsbehandling(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksopplysningsperiode = søknadsbehandlingInnvilgelsesperiode,
            saksopplysninger = hentSaksopplysninger(omgjøringInnvilgelsesperiode),
        ),
        vedtattInnvilgetSøknadsbehandling: Rammevedtak = nyRammevedtakInnvilgelse(
            sakId = sakId,
            fnr = fnr,
            behandling = innvilgetSøknadsbehandling,
        ),
    ): Revurdering {
        return Revurdering.opprettOmgjøring(
            saksbehandler = saksbehandler,
            saksopplysninger = hentSaksopplysninger(omgjøringInnvilgelsesperiode),
            omgjørRammevedtak = vedtattInnvilgetSøknadsbehandling,
            clock = clock,
        ).getOrFail().copy(id = id)
    }

    fun nyRevurderingOmgjøringUnderTilBeslutning(
        clock: Clock = this.clock,
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        søknadsbehandlingInnvilgelsesperiode: Periode = revurderingVedtaksperiode(),
        omgjøringInnvilgelsesperiode: Periode = revurderingVedtaksperiode(),
        omgjørBehandling: Rammebehandling = nyVedtattSøknadsbehandling(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksopplysningsperiode = søknadsbehandlingInnvilgelsesperiode,
        ),
        omgjørRammevedtak: Rammevedtak = nyRammevedtakInnvilgelse(
            sakId = sakId,
            fnr = fnr,
            behandling = omgjørBehandling,
        ),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
                clock = clock,
            )
        },
    ): Rammebehandling {
        return nyOpprettetRevurderingOmgjøring(
            clock = clock,
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
            omgjøringInnvilgelsesperiode = omgjøringInnvilgelsesperiode,
            innvilgetSøknadsbehandling = omgjørBehandling,
            vedtattInnvilgetSøknadsbehandling = omgjørRammevedtak,
            hentSaksopplysninger = hentSaksopplysninger,
        ).tilBeslutning(
            saksbehandler = saksbehandler,
            correlationId = CorrelationId.generate(),
            clock = clock,
        ).taBehandling(beslutter, clock)
    }

    fun nyIverksattRevurderingOmgjøring(
        clock: Clock = this.clock,
        iverksettendeBeslutter: Saksbehandler = beslutter(),
        attestering: Attestering = godkjentAttestering(),
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        beslutter: Saksbehandler = beslutter(),
        søknadsbehandlingInnvilgelsesperiode: Periode = revurderingVedtaksperiode(),
        omgjøringInnvilgelsesperiode: Periode = revurderingVedtaksperiode(),
        omgjørBehandling: Rammebehandling = nyVedtattSøknadsbehandling(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksopplysningsperiode = søknadsbehandlingInnvilgelsesperiode,
        ),
        omgjørRammevedtak: Rammevedtak = nyRammevedtakInnvilgelse(
            sakId = sakId,
            fnr = fnr,
            behandling = omgjørBehandling,
        ),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
                clock = clock,
            )
        },
    ): Rammebehandling = nyRevurderingOmgjøringUnderTilBeslutning(
        clock = clock,
        id = id,
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        beslutter = beslutter,
        søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
        omgjøringInnvilgelsesperiode = omgjøringInnvilgelsesperiode,
        omgjørBehandling = omgjørBehandling,
        omgjørRammevedtak = omgjørRammevedtak,
        hentSaksopplysninger = hentSaksopplysninger,
    ).iverksett(
        utøvendeBeslutter = iverksettendeBeslutter,
        attestering = attestering,
        clock = clock,
    )
}
