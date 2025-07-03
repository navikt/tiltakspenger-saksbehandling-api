package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingStansTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
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
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger = { _, _, _ -> ObjectMother.saksopplysninger() },
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Sak = { s ->
        s ?: this.persisterIverksattSøknadsbehandling().first
    },
    revurderingType: RevurderingType = RevurderingType.STANS,
    navkontor: Navkontor = ObjectMother.navkontor(),
): Pair<Sak, Behandling> {
    val sakMedVedtak = genererSak(sak)

    return runBlocking {
        sakMedVedtak.startRevurdering(
            kommando = StartRevurderingKommando(
                sakId = sakMedVedtak.id,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                revurderingType = revurderingType,
            ),
            hentSaksopplysninger = hentSaksopplysninger,
            navkontor = navkontor,
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
internal fun TestDataHelper.persisterRevurderingTilBeslutning(
    s: Sak? = null,
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("TestDataHelper.persisterRevurderingTilBeslutning"),
    stansDato: LocalDate = ObjectMother.revurderingsperiode().fraOgMed,
    valgteHjemler: NonEmptyList<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Behandling> = { s -> this.persisterOpprettetRevurdering(s) },
): Pair<Sak, Behandling> {
    val (sakMedRevurdering, revurdering) = genererSak(s)

    return runBlocking {
        sakMedRevurdering.sendRevurderingTilBeslutning(
            kommando = RevurderingStansTilBeslutningKommando(
                sakId = sakMedRevurdering.id,
                behandlingId = revurdering.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                begrunnelse = begrunnelse,
                stansFraOgMed = stansDato,
                valgteHjemler = valgteHjemler,
                fritekstTilVedtaksbrev = FritekstTilVedtaksbrev("TestDataHelper.persisterRevurderingTilBeslutning"),
                sisteDagSomGirRett = null,
            ),
            clock = clock,
        )
    }.getOrNull()!!.let {
        behandlingRepo.lagre(it)
        sakRepo.hentForSakId(sakMedRevurdering.id)!! to it
    }
}

internal fun TestDataHelper.persisterRevurderingUnderBeslutning(
    sak: Sak? = null,
    opprettetAv: Saksbehandler = ObjectMother.saksbehandler(),
    beslutterAv: Saksbehandler = ObjectMother.beslutter(),
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("TestDataHelper.persisterRevurderingUnderBeslutning"),
    stansDato: LocalDate = ObjectMother.revurderingsperiode().fraOgMed,
    valgteHjemler: NonEmptyList<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Behandling> = { s ->
        this.persisterRevurderingTilBeslutning(
            s = s,
            saksbehandler = opprettetAv,
            begrunnelse = begrunnelse,
            stansDato = stansDato,
            valgteHjemler = valgteHjemler,
            clock = clock,
        )
    },
): Pair<Sak, Behandling> {
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
internal fun TestDataHelper.persisterIverksattRevurdering(
    sak: Sak? = null,
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("TestDataHelper.persisterRevurderingTilBeslutning"),
    stansDato: LocalDate = ObjectMother.revurderingsperiode().fraOgMed,
    valgteHjemler: Nel<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Behandling> = { s ->
        this.persisterRevurderingUnderBeslutning(
            sak = s,
            opprettetAv = saksbehandler,
            beslutterAv = beslutter,
            begrunnelse = begrunnelse,
            stansDato = stansDato,
            valgteHjemler = valgteHjemler,
            clock = clock,
        )
    },
): Triple<Sak, Rammevedtak, Behandling> {
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
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger = { _, _, _ -> ObjectMother.saksopplysninger() },
    clock: Clock = this.clock,
    genererSak: (Sak?) -> Pair<Sak, Behandling> = { s ->
        this.persisterOpprettetRevurdering(
            sak = s,
            saksbehandler = opprettetAv,
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    },
): Pair<Sak, Behandling> {
    val (sakMedOpprettetRevurdering, opprettetRevurdering) = genererSak(sak)

    val avbruttRevurdering = opprettetRevurdering.avbryt(
        avbruttAv = avbruttAv,
        begrunnelse = begrunnelse,
        tidspunkt = LocalDateTime.now(clock),
    )

    behandlingRepo.lagre(avbruttRevurdering)
    return Pair(sakRepo.hentForSakId(sakMedOpprettetRevurdering.id)!!, opprettetRevurdering)
}
