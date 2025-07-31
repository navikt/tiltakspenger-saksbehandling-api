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
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Innvilgelse.Utbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.beregning.BehandlingBeregning
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.godkjentAttestering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.navkontor
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDate
import java.time.LocalDateTime

interface BehandlingRevurderingMother : MotherOfAllMothers {
    fun revurderingVirkningsperiode() = 2.januar(2023) til 31.mars(2023)

    fun nyOpprettetRevurderingStans(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        virkningsperiode: Periode = revurderingVirkningsperiode(),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
            )
        },
    ): Revurdering {
        return runBlocking {
            Revurdering.opprettStans(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = hentSaksopplysninger(virkningsperiode),
                clock = clock,
            )
        }
    }

    fun nyRevurderingStansKlarTilBeslutning(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyRevurderingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("nyRevurderingKlarTilBeslutning()"),
        virkningsperiode: Periode = revurderingVirkningsperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        valgteHjemler: NonEmptyList<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        stansDato: LocalDate,
        sisteDagSomGirRett: LocalDate,
        kommando: RevurderingTilBeslutningKommando.Stans = RevurderingTilBeslutningKommando.Stans(
            sakId = sakId,
            behandlingId = id,
            saksbehandler = saksbehandler,
            correlationId = CorrelationId.generate(),
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            valgteHjemler = valgteHjemler,
            stansFraOgMed = stansDato,
            sisteDagSomGirRett = sisteDagSomGirRett,
        ),
    ): Revurdering {
        return this.nyOpprettetRevurderingStans(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            virkningsperiode = virkningsperiode,
            hentSaksopplysninger = { saksopplysninger },
        ).stansTilBeslutning(
            kommando = kommando,
            clock = clock,
        ).getOrFail()
    }

    fun nyVedtattRevurderingStans(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyRevurderingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("nyRevurderingKlarTilBeslutning()"),
        virkningsperiode: Periode = revurderingVirkningsperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        valgteHjemler: Nel<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        attestering: Attestering = godkjentAttestering(beslutter),
        stansDato: LocalDate,
        sisteDagSomGirRett: LocalDate,
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteHjemler = valgteHjemler,
            stansDato = stansDato,
            sisteDagSomGirRett = sisteDagSomGirRett,
        ).taBehandling(beslutter).iverksett(
            utøvendeBeslutter = beslutter,
            attestering = attestering,
            clock = clock,
        ) as Revurdering
    }

    fun nyOpprettetRevurderingInnvilgelse(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        virkningsperiode: Periode = revurderingVirkningsperiode(),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
            )
        },
    ): Revurdering {
        return runBlocking {
            Revurdering.opprettInnvilgelse(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysninger = hentSaksopplysninger(virkningsperiode),
                clock = clock,
            )
        }
    }

    fun nyRevurderingInnvilgelseKlarTilBeslutning(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyRevurderingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("nyRevurderingKlarTilBeslutning()"),
        virkningsperiode: Periode = revurderingVirkningsperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        navkontor: Navkontor = navkontor(),
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            virkningsperiode,
        ),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltagelse.map {
            Pair(virkningsperiode, it.eksternDeltagelseId)
        },
        barnetillegg: Barnetillegg? = null,
        beregning: BehandlingBeregning? = null,
    ): Revurdering {
        val kommando = RevurderingTilBeslutningKommando.Innvilgelse(
            sakId = sakId,
            behandlingId = id,
            saksbehandler = saksbehandler,
            correlationId = CorrelationId.generate(),
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            innvilgelsesperiode = virkningsperiode,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            tiltaksdeltakelser = valgteTiltaksdeltakelser,
            barnetillegg = barnetillegg,
        )

        return this.nyOpprettetRevurderingInnvilgelse(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            virkningsperiode = virkningsperiode,
            hentSaksopplysninger = { saksopplysninger },
        ).tilBeslutning(
            kommando = kommando,
            clock = clock,
            utbetaling = if (beregning == null) {
                null
            } else {
                Utbetaling(
                    beregning = beregning,
                    navkontor = navkontor,
                )
            },
        ).getOrFail()
    }

    fun nyVedtattRevurderingInnvilgelse(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyRevurderingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("nyRevurderingKlarTilBeslutning()"),
        virkningsperiode: Periode = revurderingVirkningsperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        attestering: Attestering = godkjentAttestering(beslutter),
        navkontor: Navkontor = navkontor(),
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            virkningsperiode,
        ),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltagelse.map {
            Pair(virkningsperiode, it.eksternDeltagelseId)
        },
        barnetillegg: Barnetillegg? = null,
        beregning: BehandlingBeregning? = null,
    ): Revurdering {
        return nyRevurderingInnvilgelseKlarTilBeslutning(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            navkontor = navkontor,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            barnetillegg = barnetillegg,
            beregning = beregning,
        ).taBehandling(beslutter).iverksett(
            utøvendeBeslutter = beslutter,
            attestering = attestering,
            clock = clock,
        ) as Revurdering
    }
}
