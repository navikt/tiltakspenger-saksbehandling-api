package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.navkontor
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilBeslutning
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettVedtak
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Gir deg mulighet til å lage en revurdering på en eksisterende sak uten å lage ny søknad + vedtak (Disse må da eksistere på saken som sendes inn)
 *
 * @param sak optional sak som kan bygges på videre. Dersom den ikke sendes, får du en default sak
 * @param genererSak funksjon som genererer sak og revurdering. Den kan brukes til å lage en ny sak eller bruke en eksisterende.
 */
internal fun TestDataHelper.persisterOpprettetRevurdering(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _ -> ObjectMother.saksopplysninger() },
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Sak = { s ->
        s ?: this.persisterIverksattSøknadsbehandling().first
    },
    revurderingType: RevurderingType = RevurderingType.STANS,
    vedtakIdSomOmgjøres: VedtakId? = null,
): Pair<Sak, Revurdering> {
    val sakMedVedtak = genererSak(sak)

    return runBlocking {
        sakMedVedtak.startRevurdering(
            kommando = StartRevurderingKommando(
                sakId = sakMedVedtak.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                revurderingType = revurderingType,
                vedtakIdSomOmgjøres = vedtakIdSomOmgjøres,
            ),
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    }.also {
        behandlingRepo.lagre(it.second)
    }
}

/**
 * Gir deg mulighet til å lage en revurdering på en eksisterende sak uten å lage ny søknad + vedtak (Disse må da eksistere på saken som sendes inn)
 *
 * @param s optional sak som kan bygges på videre. Dersom den ikke sendes, får du en default sak
 * @param genererSak funksjon som genererer sak og revurdering. Den kan brukes til å lage en ny sak eller bruke en eksisterende.
 */
internal fun TestDataHelper.persisterRevurderingStansTilBeslutning(
    s: Sak? = null,
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("TestDataHelper.persisterRevurderingTilBeslutning"),
    stansFraOgMed: LocalDate? = s?.førsteDagSomGirRett ?: ObjectMother.revurderingVirkningsperiode().fraOgMed,
    stansTilOgMed: LocalDate? = s?.sisteDagSomGirRett ?: ObjectMother.revurderingVirkningsperiode().tilOgMed,
    valgteHjemler: NonEmptyList<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    utbetaling: BehandlingUtbetaling? = null,
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Revurdering> = { s -> this.persisterOpprettetRevurdering(s) },
): Pair<Sak, Revurdering> {
    val (sakMedRevurdering, revurdering) = genererSak(s)

    return runBlocking {
        revurdering.oppdaterStans(
            kommando = OppdaterRevurderingKommando.Stans(
                sakId = sakMedRevurdering.id,
                behandlingId = revurdering.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                begrunnelseVilkårsvurdering = begrunnelse,
                stansFraOgMed = OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed.create(stansFraOgMed),
                stansTilOgMed = OppdaterRevurderingKommando.Stans.ValgtStansTilOgMed.create(stansTilOgMed),
                valgteHjemler = valgteHjemler,
                fritekstTilVedtaksbrev = FritekstTilVedtaksbrev("TestDataHelper.persisterRevurderingTilBeslutning"),
            ),
            førsteDagSomGirRett = sakMedRevurdering.førsteDagSomGirRett!!,
            sisteDagSomGirRett = sakMedRevurdering.sisteDagSomGirRett!!,
            clock = clock,
            utbetaling = utbetaling,
        )
    }.getOrNull()!!.tilBeslutning().let {
        behandlingRepo.lagre(it)
        sakRepo.hentForSakId(sakMedRevurdering.id)!! to it as Revurdering
    }
}

internal fun TestDataHelper.persisterRevurderingStansUnderBeslutning(
    sak: Sak? = null,
    opprettetAv: Saksbehandler = ObjectMother.saksbehandler(),
    beslutterAv: Saksbehandler = ObjectMother.beslutter(),
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("TestDataHelper.persisterRevurderingUnderBeslutning"),
    stansFraOgMed: LocalDate? = ObjectMother.revurderingVirkningsperiode().fraOgMed,
    stansTilOgMed: LocalDate? = ObjectMother.revurderingVirkningsperiode().tilOgMed,
    valgteHjemler: NonEmptyList<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Rammebehandling> = { s ->
        this.persisterRevurderingStansTilBeslutning(
            s = s,
            saksbehandler = opprettetAv,
            begrunnelse = begrunnelse,
            stansFraOgMed = stansFraOgMed,
            stansTilOgMed = stansTilOgMed,
            valgteHjemler = valgteHjemler,
            clock = clock,
        )
    },
): Pair<Sak, Rammebehandling> {
    val (sakMedRevurderingTilBeslutning, revurderingTilBeslutning) = genererSak(sak)

    val revurderingUnderBeslutning = revurderingTilBeslutning.taBehandling(beslutterAv)
    behandlingRepo.lagre(revurderingUnderBeslutning)

    return Pair(sakRepo.hentForSakId(sakMedRevurderingTilBeslutning.id)!!, revurderingUnderBeslutning)
}

/**
 * Gir deg mulighet til å lage en revurdering på en eksisterende sak uten å lage ny søknad + vedtak (Disse må da eksistere på saken som sendes inn)
 *
 * @param sak optional sak som kan bygges på videre. Dersom den ikke sendes, får du en default sak
 * @param genererSak funksjon som genererer sak og revurdering. Den kan brukes til å lage en ny sak eller bruke en eksisterende.
 */
internal fun TestDataHelper.persisterIverksattRevurderingStans(
    sak: Sak? = null,
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("TestDataHelper.persisterRevurderingTilBeslutning"),
    stansFraOgMed: LocalDate = ObjectMother.revurderingVirkningsperiode().fraOgMed,
    stansTilOgMed: LocalDate = ObjectMother.revurderingVirkningsperiode().tilOgMed,
    valgteHjemler: Nel<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Rammebehandling> = { s ->
        this.persisterRevurderingStansUnderBeslutning(
            sak = s,
            opprettetAv = saksbehandler,
            beslutterAv = beslutter,
            begrunnelse = begrunnelse,
            stansFraOgMed = stansFraOgMed,
            stansTilOgMed = stansTilOgMed,
            valgteHjemler = valgteHjemler,
            clock = clock,
        )
    },
): Triple<Sak, Rammevedtak, Rammebehandling> {
    val (sakMedRevurderingTilBeslutning, revurderingTilBeslutning) = genererSak(sak)

    val iverksattRevurdering =
        revurderingTilBeslutning.iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter), clock)

    behandlingRepo.lagre(iverksattRevurdering)

    val (_, stansVedtak) = sakMedRevurderingTilBeslutning.opprettVedtak(iverksattRevurdering, clock)
    vedtakRepo.lagre(stansVedtak)

    return Triple(sakRepo.hentForSakId(sakMedRevurderingTilBeslutning.id)!!, stansVedtak, iverksattRevurdering)
}

internal fun TestDataHelper.persisterAvbruttRevurdering(
    sak: Sak? = null,
    opprettetAv: Saksbehandler = ObjectMother.saksbehandler(),
    avbruttAv: Saksbehandler = ObjectMother.saksbehandler(),
    begrunnelse: String = "TestDataHelper.persisterAvbruttRevurdering",
    hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _ -> ObjectMother.saksopplysninger() },
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Rammebehandling> = { s ->
        this.persisterOpprettetRevurdering(
            sak = s!!,
            saksbehandler = opprettetAv,
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    },
): Pair<Sak, Rammebehandling> {
    val (sakMedOpprettetRevurdering, opprettetRevurdering) = genererSak(sak)

    val avbruttRevurdering = opprettetRevurdering.avbryt(
        avbruttAv = avbruttAv,
        begrunnelse = begrunnelse,
        tidspunkt = LocalDateTime.now(clock),
    )

    behandlingRepo.lagre(avbruttRevurdering)
    return Pair(sakRepo.hentForSakId(sakMedOpprettetRevurdering.id)!!, opprettetRevurdering)
}

internal fun TestDataHelper.persisterRevurderingInnvilgelseIverksatt(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("TestDataHelper.persisterRevurderingTilBeslutning"),
    innvilgelsesperiode: Periode? = null,
    barnetillegg: Barnetillegg? = null,
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Revurdering> = { s ->
        this.persisterOpprettetRevurdering(
            sak = s,
            revurderingType = RevurderingType.INNVILGELSE,
        )
    },
): Pair<Sak, Revurdering> {
    val (sakMedRevurdering, revurdering) = genererSak(sak)

    val periode = innvilgelsesperiode ?: Periode(
        sakMedRevurdering.førsteDagSomGirRett!!,
        sakMedRevurdering.sisteDagSomGirRett!!,
    )

    val barnetillegg = barnetillegg ?: Barnetillegg.utenBarnetillegg(periode)

    val kommando = OppdaterRevurderingKommando.Innvilgelse(
        sakId = sakMedRevurdering.id,
        behandlingId = revurdering.id,
        saksbehandler = saksbehandler,
        correlationId = CorrelationId.generate(),
        begrunnelseVilkårsvurdering = begrunnelse,
        fritekstTilVedtaksbrev = FritekstTilVedtaksbrev("TestDataHelper.persisterRevurderingTilBeslutning"),
        innvilgelsesperiode = periode,
        tiltaksdeltakelser = listOf(
            Pair(
                periode,
                revurdering.saksopplysninger.tiltaksdeltagelser.first().eksternDeltagelseId,
            ),
        ),
        antallDagerPerMeldeperiode = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            periode,
        ),
        barnetillegg = barnetillegg,
    )

    val utbetaling = sakMedRevurdering.beregnInnvilgelse(
        behandlingId = revurdering.id,
        virkningsperiode = periode,
        barnetilleggsperioder = barnetillegg.periodisering,
    )

    return runBlocking {
        revurdering.oppdaterInnvilgelse(
            kommando = kommando,
            clock = clock,
            utbetaling = utbetaling?.let {
                BehandlingUtbetaling(
                    beregning = it,
                    navkontor = navkontor(),
                    simulering = null,
                )
            },
        )
    }.getOrNull()!!.tilBeslutning().taBehandling(
        saksbehandler = beslutter,
    ).iverksett(
        utøvendeBeslutter = beslutter,
        attestering = Attestering(
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        ),
        clock = clock,
    ).let {
        behandlingRepo.lagre(it)
        sakRepo.hentForSakId(sakMedRevurdering.id)!! to it as Revurdering
    }
}
