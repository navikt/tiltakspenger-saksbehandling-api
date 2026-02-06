package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.beregning.beregnInnvilgelse
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.navkontor
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterRevurderingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tilStartRevurderingType
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilBeslutning
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.tilTiltakstype
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettRammevedtak
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
    revurderingType: RevurderingsresultatType = RevurderingsresultatType.STANS,
    vedtakIdSomOmgjøres: VedtakId? = null,
): Pair<Sak, Revurdering> {
    val sakMedVedtak = genererSak(sak)

    return runBlocking {
        sakMedVedtak.startRevurdering(
            kommando = StartRevurderingKommando(
                sakId = sakMedVedtak.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                revurderingType = revurderingType.tilStartRevurderingType(),
                vedtakIdSomOmgjøres = vedtakIdSomOmgjøres,
                klagebehandlingId = null,
            ),
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    }.also {
        it.second.saksopplysninger.tiltaksdeltakelser.forEach { tiltaksdeltakelse ->
            if (tiltaksdeltakerRepo.hentInternId(tiltaksdeltakelse.eksternDeltakelseId) == null) {
                tiltaksdeltakerRepo.lagre(
                    id = tiltaksdeltakelse.internDeltakelseId,
                    eksternId = tiltaksdeltakelse.eksternDeltakelseId,
                    tiltakstype = tiltaksdeltakelse.typeKode.tilTiltakstype(),
                )
            }
        }
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
    begrunnelse: Begrunnelse = Begrunnelse.createOrThrow("TestDataHelper.persisterRevurderingTilBeslutning"),
    stansFraOgMed: LocalDate? = s?.førsteDagSomGirRett ?: ObjectMother.revurderingVedtaksperiode().fraOgMed,
    valgteHjemler: NonEmptySet<ValgtHjemmelForStans> = nonEmptySetOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    utbetaling: BehandlingUtbetaling? = null,
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Revurdering> = { s -> this.persisterOpprettetRevurdering(s) },
): Pair<Sak, Revurdering> {
    val (sakMedRevurdering, revurdering) = genererSak(s)
    val kommando = OppdaterRevurderingKommando.Stans(
        sakId = sakMedRevurdering.id,
        behandlingId = revurdering.id,
        saksbehandler = saksbehandler,
        correlationId = CorrelationId.generate(),
        begrunnelseVilkårsvurdering = begrunnelse,
        stansFraOgMed = OppdaterRevurderingKommando.Stans.ValgtStansFraOgMed.create(stansFraOgMed),
        valgteHjemler = valgteHjemler,
        fritekstTilVedtaksbrev = FritekstTilVedtaksbrev.create("TestDataHelper.persisterRevurderingTilBeslutning"),
    )
    val stansperiode = kommando.utledStansperiode(
        førsteDagSomGirRett = sakMedRevurdering.førsteDagSomGirRett!!,
        sisteDagSomGirRett = sakMedRevurdering.sisteDagSomGirRett!!,
    )
    return runBlocking {
        revurdering.oppdaterStans(
            kommando = kommando,
            førsteDagSomGirRett = sakMedRevurdering.førsteDagSomGirRett,
            sisteDagSomGirRett = sakMedRevurdering.sisteDagSomGirRett,
            clock = clock,
            utbetaling = utbetaling,
            omgjørRammevedtak = sakMedRevurdering.vedtaksliste.finnRammevedtakSomOmgjøres(stansperiode),
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
    begrunnelse: Begrunnelse = Begrunnelse.createOrThrow("TestDataHelper.persisterRevurderingUnderBeslutning"),
    stansFraOgMed: LocalDate? = ObjectMother.revurderingVedtaksperiode().fraOgMed,
    valgteHjemler: NonEmptySet<ValgtHjemmelForStans> = nonEmptySetOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Rammebehandling> = { s ->
        this.persisterRevurderingStansTilBeslutning(
            s = s,
            saksbehandler = opprettetAv,
            begrunnelse = begrunnelse,
            stansFraOgMed = stansFraOgMed,
            valgteHjemler = valgteHjemler,
            clock = clock,
        )
    },
): Pair<Sak, Rammebehandling> {
    val (sakMedRevurderingTilBeslutning, revurderingTilBeslutning) = genererSak(sak)

    val revurderingUnderBeslutning = revurderingTilBeslutning.taBehandling(beslutterAv, clock)
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
    begrunnelse: Begrunnelse = Begrunnelse.createOrThrow("TestDataHelper.persisterRevurderingTilBeslutning"),
    stansFraOgMed: LocalDate = ObjectMother.revurderingVedtaksperiode().fraOgMed,
    valgteHjemler: NonEmptySet<ValgtHjemmelForStans> = nonEmptySetOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    clock: Clock = this.clock,
    correlationId: CorrelationId = CorrelationId.generate(),
    genererSak: (Sak?) -> Pair<Sak, Rammebehandling> = { s ->
        this.persisterRevurderingStansUnderBeslutning(
            sak = s,
            opprettetAv = saksbehandler,
            beslutterAv = beslutter,
            begrunnelse = begrunnelse,
            stansFraOgMed = stansFraOgMed,
            valgteHjemler = valgteHjemler,
            clock = clock,
        )
    },
): Triple<Sak, Rammevedtak, Rammebehandling> {
    val (sakMedRevurderingTilBeslutning, revurderingTilBeslutning) = genererSak(sak)

    val iverksattRevurdering =
        revurderingTilBeslutning.iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter), correlationId, clock)

    val stansVedtak = sessionFactory.withTransactionContext { tx ->
        behandlingRepo.lagre(iverksattRevurdering, tx)

        val (sakMedNyttVedtak, stansVedtak) = sakMedRevurderingTilBeslutning.opprettRammevedtak(
            iverksattRevurdering,
            clock,
        )
        vedtakRepo.lagre(stansVedtak, tx)
        sakMedNyttVedtak.rammevedtaksliste.dropLast(1).forEach {
            vedtakRepo.oppdaterOmgjortAv(it.id, it.omgjortAvRammevedtak, tx)
        }
        stansVedtak
    }

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
        begrunnelse = begrunnelse.toNonBlankString(),
        tidspunkt = LocalDateTime.now(clock),
        skalAvbryteSøknad = false,
    )

    behandlingRepo.lagre(avbruttRevurdering)
    return Pair(sakRepo.hentForSakId(sakMedOpprettetRevurdering.id)!!, opprettetRevurdering)
}

internal fun TestDataHelper.persisterRevurderingInnvilgelseIverksatt(
    sak: Sak? = null,
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    begrunnelse: String = "TestDataHelper.persisterRevurderingTilBeslutning",
    fritekstTilVedtaksbrev: String = "TestDataHelper.persisterRevurderingTilBeslutning",
    innvilgelsesperiode: Periode? = null,
    barnetillegg: Barnetillegg? = null,
    clock: Clock = this.clock,
    correlationId: CorrelationId = CorrelationId.generate(),
    genererSak: (Sak?) -> Pair<Sak, Revurdering> = { s ->
        this.persisterOpprettetRevurdering(
            sak = s,
            revurderingType = RevurderingsresultatType.INNVILGELSE,
        )
    },
): Pair<Sak, Revurdering> {
    val (sakMedRevurdering, revurdering) = genererSak(sak)

    val periode = innvilgelsesperiode ?: Periode(
        sakMedRevurdering.førsteDagSomGirRett!!,
        sakMedRevurdering.sisteDagSomGirRett!!,
    )

    val barnetillegg = barnetillegg ?: Barnetillegg.utenBarnetillegg(periode)

    val kommando = oppdaterRevurderingInnvilgelseKommando(
        sakId = sakMedRevurdering.id,
        behandlingId = revurdering.id,
        saksbehandler = saksbehandler,
        begrunnelseVilkårsvurdering = begrunnelse,
        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        innvilgelsesperioder = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = periode,
                tiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.first(),
            ),
        ),
        barnetillegg = barnetillegg,
    )

    val utbetaling = sakMedRevurdering.beregnInnvilgelse(
        behandlingId = revurdering.id,
        vedtaksperiode = periode,
        innvilgelsesperioder = kommando.tilInnvilgelseperioder(revurdering),
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
            omgjørRammevedtak = sakMedRevurdering.vedtaksliste.finnRammevedtakSomOmgjøres(periode),
        )
    }.getOrNull()!!.tilBeslutning().taBehandling(
        saksbehandler = beslutter,
        clock = clock,
    ).iverksett(
        utøvendeBeslutter = beslutter,
        attestering = Attestering(
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        ),
        correlationId = correlationId,
        clock = clock,
    ).let {
        behandlingRepo.lagre(it)
        sakRepo.hentForSakId(sakMedRevurdering.id)!! to it as Revurdering
    }
}

internal fun TestDataHelper.persisterOpprettetOmgjøring(
    genererSak: Triple<Sak, Rammevedtak, Rammebehandling> = persisterIverksattSøknadsbehandling(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _ -> genererSak.second.rammebehandling.saksopplysninger },
    clock: Clock = this.clock,
): Pair<Sak, Revurdering> {
    val (sakMedVedtak, _, _) = genererSak

    return runBlocking {
        sakMedVedtak.startRevurdering(
            kommando = StartRevurderingKommando(
                sakId = sakMedVedtak.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                revurderingType = StartRevurderingType.OMGJØRING,
                vedtakIdSomOmgjøres = sakMedVedtak.rammevedtaksliste.single().id,
                klagebehandlingId = null,
            ),
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    }.also {
        behandlingRepo.lagre(it.second)
    }
}
