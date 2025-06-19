package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.Nel
import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingStansTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettVedtak
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

internal fun TestDataHelper.persisterOpprettetSøknadsbehandling(
    sakId: SakId = SakId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    id: SøknadId = Søknad.randomId(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
    søknad: Søknad =
        ObjectMother.nySøknad(
            periode = tiltaksOgVurderingsperiode,
            journalpostId = journalpostId,
            personopplysninger =
            ObjectMother.personSøknad(
                fnr = fnr,
            ),
            id = id,
            søknadstiltak =
            ObjectMother.søknadstiltak(
                deltakelseFom = deltakelseFom,
                deltakelseTom = deltakelseTom,
            ),
            barnetillegg = listOf(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ),
    barnetillegg: Barnetillegg? = null,
    clock: Clock = this.clock,
): Triple<Sak, Søknadsbehandling, Søknad> {
    this.persisterSakOgSøknad(
        fnr = sak.fnr,
        søknad = søknad,
        sak = sak,
    )
    val (sakMedBehandling) =
        ObjectMother.sakMedOpprettetBehandling(
            søknad = søknad,
            fnr = sak.fnr,
            virkningsperiode = tiltaksOgVurderingsperiode,
            saksnummer = sak.saksnummer,
            saksbehandler = saksbehandler,
            sakId = sak.id,
            barnetillegg = barnetillegg,
            clock = clock,
        )
    behandlingRepo.lagre(sakMedBehandling.behandlinger.singleOrNullOrThrow()!!)

    return Triple(
        sakRepo.hentForSakId(sakId)!!,
        sakMedBehandling.behandlinger.singleOrNullOrThrow()!! as Søknadsbehandling,
        søknadRepo.hentForSøknadId(søknad.id)!!,
    )
}

internal fun TestDataHelper.persisterKlarTilBeslutningSøknadsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: Søknad = ObjectMother.nySøknad(
        periode = tiltaksOgVurderingsperiode,
        journalpostId = journalpostId,
        personopplysninger =
        ObjectMother.personSøknad(
            fnr = fnr,
        ),
        id = id,
        søknadstiltak =
        ObjectMother.søknadstiltak(
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
        ),
        barnetillegg = listOf(),
        sakId = sak.id,
        saksnummer = sak.saksnummer,
    ),
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("persisterKlarTilBeslutningSøknadsbehandling()"),
    begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("persisterKlarTilBeslutningSøknadsbehandling()"),
    correlationId: CorrelationId = CorrelationId.generate(),
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
    resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
    antallDagerPerMeldeperiode: Periodisering<AntallDagerForMeldeperiode> = Periodisering(
        PeriodeMedVerdi(
            AntallDagerForMeldeperiode((MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)),
            Periode(deltakelseFom, deltakelseTom),
        ),
    ),
): Pair<Sak, Behandling> {
    val (sak, søknadsbehandling) = persisterOpprettetSøknadsbehandling(
        sakId = sak.id,
        fnr = sak.fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
        id = id,
        søknad = søknad,
        sak = sak,
    )
    val oppdatertSøknadsbehandling =
        søknadsbehandling
            .tilBeslutning(
                SendSøknadsbehandlingTilBeslutningKommando(
                    sakId = sak.id,
                    saksbehandler = saksbehandler,
                    behandlingId = søknadsbehandling.id,
                    correlationId = correlationId,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    behandlingsperiode = tiltaksOgVurderingsperiode,
                    barnetillegg = null,
                    tiltaksdeltakelser = listOf(
                        Pair(
                            tiltaksOgVurderingsperiode,
                            søknadsbehandling.saksopplysninger.tiltaksdeltagelse.first().eksternDeltagelseId,
                        ),
                    ),
                    antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                    avslagsgrunner = avslagsgrunner,
                    resultat = resultat,
                ),
                clock = clock,
            )

    behandlingRepo.lagre(oppdatertSøknadsbehandling)
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    return Pair(oppdatertSak, oppdatertSøknadsbehandling)
}

internal fun TestDataHelper.persisterUnderBeslutningSøknadsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: Søknad = ObjectMother.nySøknad(
        periode = tiltaksOgVurderingsperiode,
        journalpostId = journalpostId,
        personopplysninger =
        ObjectMother.personSøknad(
            fnr = fnr,
        ),
        id = id,
        søknadstiltak =
        ObjectMother.søknadstiltak(
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
        ),
        barnetillegg = listOf(),
        sakId = sak.id,
        saksnummer = sak.saksnummer,
    ),
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("persisterKlarTilBeslutningSøknadsbehandling()"),
    begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("persisterKlarTilBeslutningSøknadsbehandling()"),
    correlationId: CorrelationId = CorrelationId.generate(),
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
    beslutter: Saksbehandler = ObjectMother.beslutter(),
): Pair<Sak, Behandling> {
    val behandling = persisterKlarTilBeslutningSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
        sak = sak,
        id = id,
        søknad = søknad,
        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
        correlationId = correlationId,
        clock = clock,
    ).second

    val tilBeslutning = behandling.taBehandling(beslutter)

    behandlingRepo.lagre(tilBeslutning)
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    return Pair(oppdatertSak, tilBeslutning)
}

internal fun TestDataHelper.persisterAvbruttSøknadsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    avbruttTidspunkt: LocalDateTime = førsteNovember24,
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: Søknad =
        ObjectMother.nySøknad(
            periode = tiltaksOgVurderingsperiode,
            journalpostId = journalpostId,
            personopplysninger =
            ObjectMother.personSøknad(
                fnr = fnr,
            ),
            id = id,
            søknadstiltak =
            ObjectMother.søknadstiltak(
                deltakelseFom = deltakelseFom,
                deltakelseTom = deltakelseTom,
            ),
            barnetillegg = listOf(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ),
    clock: Clock = this.clock,
): Pair<Sak, Behandling> {
    val (sakMedSøknadsbehandling, _) = persisterOpprettetSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
        id = id,
        søknad = søknad,
        sak = sak,
        clock = clock,
    )
    val søknadsbehandling =
        sakMedSøknadsbehandling.behandlinger.filter { it is Søknadsbehandling && it.søknad.id == id }
            .singleOrNullOrThrow()!!
    val avbruttBehandling = søknadsbehandling.avbryt(
        saksbehandler,
        "begrunnelse",
        avbruttTidspunkt,
    )
    behandlingRepo.lagre(avbruttBehandling)
    return sakRepo.hentForSakId(sakMedSøknadsbehandling.id)!! to avbruttBehandling
}

/** Skal kun persistere en helt tom sak */
@Suppress("unused")
internal fun TestDataHelper.persisterNySak(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
): Sak {
    return ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ).also {
        sakRepo.opprettSak(it)
    }
}

/**
 * Persisterer og et rammevedtak.
 */
internal fun TestDataHelper.persisterIverksattSøknadsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    søknadId: SøknadId = Søknad.randomId(),
    søknad: Søknad =
        ObjectMother.nySøknad(
            periode = tiltaksOgVurderingsperiode,
            journalpostId = journalpostId,
            personopplysninger =
            ObjectMother.personSøknad(
                fnr = fnr,
            ),
            id = søknadId,
            søknadstiltak =
            ObjectMother.søknadstiltak(
                deltakelseFom = deltakelseFom,
                deltakelseTom = deltakelseTom,
            ),
            barnetillegg = listOf(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ),
    correlationId: CorrelationId = CorrelationId.generate(),
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("persisterIverksattSøknadsbehandling()"),
    begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("persisterIverksattSøknadsbehandling()"),
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
): Triple<Sak, Rammevedtak, Behandling> {
    val (sak, søknadsbehandling) = persisterKlarTilBeslutningSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
        sak = sak,
        id = søknadId,
        søknad = søknad,
        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
        correlationId = correlationId,
        clock = clock,
    )

    val oppdatertBehandling = søknadsbehandling
        .taBehandling(beslutter)
        .iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter), clock)
    behandlingRepo.lagre(oppdatertBehandling)
    val vedtak = sak.opprettVedtak(oppdatertBehandling, clock).second
    vedtakRepo.lagre(vedtak)
    sakRepo.oppdaterSkalSendesTilMeldekortApi(
        sakId = vedtak.sakId,
        skalSendesTilMeldekortApi = true,
    )
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    val (_, meldeperioder) = oppdatertSak.genererMeldeperioder(clock)
    meldeperiodeRepo.lagre(meldeperioder)
    return Triple(sakRepo.hentForSakId(sakId)!!, vedtak, oppdatertBehandling)
}

internal fun TestDataHelper.persisterIverksattSøknadsbehandlingAvslag(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: Søknad = ObjectMother.nySøknad(
        periode = tiltaksOgVurderingsperiode,
        journalpostId = journalpostId,
        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
        id = id,
        søknadstiltak = ObjectMother.søknadstiltak(deltakelseFom = deltakelseFom, deltakelseTom = deltakelseTom),
        barnetillegg = listOf(),
        sakId = sak.id,
        saksnummer = sak.saksnummer,
    ),
    correlationId: CorrelationId = CorrelationId.generate(),
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("persisterIverksattSøknadsbehandlingAvslag()"),
    begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("persisterIverksattSøknadsbehandlingAvslag()"),
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
): Triple<Sak, Rammevedtak, Behandling> {
    val (sak, søknadsbehandling) = persisterKlarTilBeslutningSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
        sak = sak,
        id = id,
        søknad = søknad,
        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
        resultat = SøknadsbehandlingType.AVSLAG,
        avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
        correlationId = correlationId,
        clock = clock,
    )

    val oppdatertSøknadsbehandling =
        søknadsbehandling.taBehandling(beslutter)
            .iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter), clock)
    behandlingRepo.lagre(oppdatertSøknadsbehandling)
    val (sakMedVedtak, vedtak) = sak.opprettVedtak(oppdatertSøknadsbehandling, clock)
    vedtakRepo.lagre(vedtak)
    return Triple(sakMedVedtak, vedtak, oppdatertSøknadsbehandling)
}

/**
 * Persisterer søknadsbehandling med tilhørende rammevedtak og starter en revurdering
 */
internal fun TestDataHelper.persisterOpprettetRevurderingDeprecated(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = ObjectMother.virkningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virkningsperiode().tilOgMed,
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: Søknad =
        ObjectMother.nySøknad(
            periode = tiltaksOgVurderingsperiode,
            journalpostId = journalpostId,
            personopplysninger =
            ObjectMother.personSøknad(
                fnr = fnr,
            ),
            id = id,
            søknadstiltak =
            ObjectMother.søknadstiltak(
                deltakelseFom = deltakelseFom,
                deltakelseTom = deltakelseTom,
            ),
            barnetillegg = listOf(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ),
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger = { _, _, _ -> ObjectMother.saksopplysninger() },
    clock: Clock = this.clock,
): Pair<Sak, Behandling> {
    val (sak, _) = runBlocking {
        persisterIverksattSøknadsbehandling(
            sakId = sakId,
            fnr = fnr,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            journalpostId = journalpostId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
            søknadId = id,
            søknad = søknad,
            sak = sak,
            clock = clock,
        )
    }
    return runBlocking {
        sak.startRevurdering(
            kommando = StartRevurderingKommando(
                sakId = sakId,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                revurderingType = RevurderingType.STANS,
            ),
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    }.also {
        behandlingRepo.lagre(it.second)
    }
}

internal fun TestDataHelper.persisterOpprettetRevurdering(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = ObjectMother.virkningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virkningsperiode().tilOgMed,
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: Søknad =
        ObjectMother.nySøknad(
            periode = tiltaksOgVurderingsperiode,
            journalpostId = journalpostId,
            personopplysninger = ObjectMother.personSøknad(fnr = fnr),
            id = id,
            søknadstiltak = ObjectMother.søknadstiltak(deltakelseFom = deltakelseFom, deltakelseTom = deltakelseTom),
            barnetillegg = listOf(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ),
    hentSaksopplysninger: suspend (fnr: Fnr, correlationId: CorrelationId, saksopplysningsperiode: Periode) -> Saksopplysninger = { _, _, _ -> ObjectMother.saksopplysninger() },
    clock: Clock = this.clock,
    revurderingType: RevurderingType = RevurderingType.STANS,
): Pair<Sak, Behandling> {
    val (sak, _) = runBlocking {
        persisterIverksattSøknadsbehandling(
            sakId = sak.id,
            fnr = sak.fnr,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            journalpostId = journalpostId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
            søknadId = id,
            søknad = søknad,
            sak = sak,
            clock = clock,
        )
    }
    return runBlocking {
        sak.startRevurdering(
            kommando = StartRevurderingKommando(
                sakId = sakId,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                revurderingType = revurderingType,
            ),
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    }.also {
        behandlingRepo.lagre(it.second)
    }
}

internal fun TestDataHelper.persisterRevurderingTilBeslutning(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = ObjectMother.virkningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virkningsperiode().tilOgMed,
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    valgteHjemler: Nel<ValgtHjemmelForStans> = nonEmptyListOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    søknadId: SøknadId = Søknad.randomId(),
    søknad: Søknad =
        ObjectMother.nySøknad(
            periode = tiltaksOgVurderingsperiode,
            journalpostId = journalpostId,
            personopplysninger = ObjectMother.personSøknad(fnr = fnr),
            id = søknadId,
            søknadstiltak = ObjectMother.søknadstiltak(deltakelseFom = deltakelseFom, deltakelseTom = deltakelseTom),
            barnetillegg = listOf(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ),
    stansDato: LocalDate = ObjectMother.revurderingsperiode().fraOgMed,
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("fordi"),
    clock: Clock = this.clock,
): Pair<Sak, Behandling> {
    val (sak, behandling) = runBlocking {
        persisterOpprettetRevurdering(
            sakId = sak.id,
            fnr = sak.fnr,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            journalpostId = journalpostId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
            id = søknadId,
            søknad = søknad,
            sak = sak,
            clock = clock,
        )
    }
    return runBlocking {
        sak.sendRevurderingTilBeslutning(
            kommando = RevurderingStansTilBeslutningKommando(
                sakId = sakId,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                begrunnelse = begrunnelse,
                stansFraOgMed = stansDato,
                valgteHjemler = valgteHjemler,
                fritekstTilVedtaksbrev = FritekstTilVedtaksbrev("fritekstTilVedtaksbrev"),
                sisteDagSomGirRett = null,
            ),
            clock = clock,
        )
    }.getOrNull()!!.let {
        behandlingRepo.lagre(it)
        sakRepo.hentForSakId(sakId)!! to it
    }
}

internal fun TestDataHelper.persisterRammevedtakMedBehandletMeldekort(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 2.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    id: SøknadId = Søknad.randomId(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    søknad: Søknad =
        ObjectMother.nySøknad(
            periode = tiltaksOgVurderingsperiode,
            journalpostId = journalpostId,
            personopplysninger =
            ObjectMother.personSøknad(
                fnr = fnr,
            ),
            id = id,
            søknadstiltak =
            ObjectMother.søknadstiltak(
                deltakelseFom = deltakelseFom,
                deltakelseTom = deltakelseTom,
            ),
            barnetillegg = listOf(),
            sakId = sak.id,
            saksnummer = sak.saksnummer,
        ),
    clock: Clock = this.clock,
): Triple<Sak, MeldekortBehandletManuelt, Rammevedtak> {
    val (sak, rammevedtak) = persisterIverksattSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
        søknadId = id,
        søknad = søknad,
        beslutter = beslutter,
        sak = sak,
        clock = clock,
    )
    val meldeperioder = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede
    val behandletMeldekort = ObjectMother.meldekortBehandletManuelt(
        sakId = sak.id,
        fnr = sak.fnr,
        saksnummer = sak.saksnummer,
        meldeperiode = meldeperioder.first(),
        periode = meldeperioder.first().periode,
    )
    meldekortRepo.lagre(behandletMeldekort, null)
    return Triple(sakRepo.hentForSakId(sakId)!!, behandletMeldekort, rammevedtak)
}

internal fun TestDataHelper.persisterRammevedtakAvslag(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 2.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    søknadId: SøknadId = Søknad.randomId(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    søknad: Søknad = ObjectMother.nySøknad(
        periode = tiltaksOgVurderingsperiode,
        journalpostId = journalpostId,
        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
        id = søknadId,
        søknadstiltak = ObjectMother.søknadstiltak(deltakelseFom = deltakelseFom, deltakelseTom = deltakelseTom),
        barnetillegg = listOf(),
        sakId = sak.id,
        saksnummer = sak.saksnummer,
    ),
    clock: Clock = this.clock,
): Pair<Sak, Rammevedtak> {
    val (sak, rammevedtak) = persisterIverksattSøknadsbehandlingAvslag(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
        id = søknadId,
        søknad = søknad,
        beslutter = beslutter,
        sak = sak,
        clock = clock,
    )

    return Pair(sak, rammevedtak)
}
