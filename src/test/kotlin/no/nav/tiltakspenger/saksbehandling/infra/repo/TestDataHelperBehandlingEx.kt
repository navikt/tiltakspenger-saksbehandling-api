package no.nav.tiltakspenger.saksbehandling.infra.repo

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendRevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingPostgresRepoTest.Companion.random
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

internal fun TestDataHelper.persisterOpprettetFørstegangsbehandling(
    sakId: SakId = SakId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
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
    barnetillegg: Barnetillegg? = null,
    clock: Clock = this.clock,
): Triple<Sak, Behandling, Søknad> {
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
            saksnummer = saksnummer,
            saksbehandler = saksbehandler,
            sakId = sak.id,
            barnetillegg = barnetillegg,
            clock = clock,
        )
    behandlingRepo.lagre(sakMedBehandling.behandlinger.singleOrNullOrThrow()!!)

    return Triple(
        sakRepo.hentForSakId(sakId)!!,
        sakMedBehandling.behandlinger.singleOrNullOrThrow()!!,
        søknadRepo.hentForSøknadId(søknad.id)!!,
    )
}

internal fun TestDataHelper.persisterKlarTilBeslutningFørstegangsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = random.nextInt().toString(),
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
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("persisterKlarTilBeslutningFørstegangsbehandling()"),
    begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("persisterKlarTilBeslutningFørstegangsbehandling()"),
    correlationId: CorrelationId = CorrelationId.generate(),
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
): Pair<Sak, Behandling> {
    val (sak, førstegangsbehandling) = persisterOpprettetFørstegangsbehandling(
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
    val oppdatertFørstegangsbehandling =
        førstegangsbehandling
            .tilBeslutning(
                SendSøknadsbehandlingTilBeslutningKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    behandlingId = førstegangsbehandling.id,
                    correlationId = correlationId,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    innvilgelsesperiode = tiltaksOgVurderingsperiode,
                    barnetillegg = null,
                    tiltaksdeltakelser = listOf(
                        Pair(
                            tiltaksOgVurderingsperiode,
                            førstegangsbehandling.saksopplysninger.tiltaksdeltagelse.first().eksternDeltagelseId,
                        ),
                    ),
                ),
                clock = clock,
            )

    behandlingRepo.lagre(oppdatertFørstegangsbehandling)
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    return Pair(oppdatertSak, oppdatertFørstegangsbehandling)
}

internal fun TestDataHelper.persisterUnderBeslutningFørstegangsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = random.nextInt().toString(),
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
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("persisterKlarTilBeslutningFørstegangsbehandling()"),
    begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("persisterKlarTilBeslutningFørstegangsbehandling()"),
    correlationId: CorrelationId = CorrelationId.generate(),
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
    beslutter: Saksbehandler = ObjectMother.beslutter(),
): Pair<Sak, Behandling> {
    val (sak, behandling) = persisterKlarTilBeslutningFørstegangsbehandling(
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
    )

    val tilBeslutning = behandling.taBehandling(beslutter)

    behandlingRepo.lagre(tilBeslutning)
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    return Pair(oppdatertSak, tilBeslutning)
}

internal fun TestDataHelper.persisterAvbruttFørstegangsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = random.nextInt().toString(),
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
    val (sakMedFørstegangsbehandling, _) = persisterOpprettetFørstegangsbehandling(
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
    val førstegangsbehandling = sakMedFørstegangsbehandling.behandlinger.singleOrNullOrThrow()!!
    val avbruttBehandling = førstegangsbehandling.avbryt(
        saksbehandler,
        "begrunnelse",
        avbruttTidspunkt,
    )
    behandlingRepo.lagre(avbruttBehandling)
    return sakRepo.hentForSakId(sakMedFørstegangsbehandling.id)!! to avbruttBehandling
}

/** Skal kun persistere en helt tom sak */
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
internal fun TestDataHelper.persisterIverksattFørstegangsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = random.nextInt().toString(),
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
    correlationId: CorrelationId = CorrelationId.generate(),
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("persisterIverksattFørstegangsbehandling()"),
    begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("persisterIverksattFørstegangsbehandling()"),
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
): Triple<Sak, Rammevedtak, Behandling> {
    val (sak, førstegangsbehandling) = persisterKlarTilBeslutningFørstegangsbehandling(
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
    )

    val oppdatertFørstegangsbehandling =
        førstegangsbehandling.taBehandling(beslutter).iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter), clock)
    behandlingRepo.lagre(oppdatertFørstegangsbehandling)
    val (sakMedVedtak, vedtak) = sak.opprettVedtak(oppdatertFørstegangsbehandling, clock)
    vedtakRepo.lagre(vedtak)
    sakRepo.oppdaterFørsteOgSisteDagSomGirRett(
        sakId = vedtak.sakId,
        førsteDagSomGirRett = sakMedVedtak.førsteDagSomGirRett,
        sisteDagSomGirRett = sakMedVedtak.sisteDagSomGirRett,
    )
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    val (_, meldeperioder) = oppdatertSak.genererMeldeperioder(clock)
    meldeperiodeRepo.lagre(meldeperioder)
    return Triple(sakRepo.hentForSakId(sakId)!!, vedtak, oppdatertFørstegangsbehandling)
}

/**
 * Persisterer førstegangsbehandling med tilhørende rammevedtak og starter en revurdering
 */
internal fun TestDataHelper.persisterOpprettetRevurderingDeprecated(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = ObjectMother.virkningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virkningsperiode().tilOgMed,
    journalpostId: String = random.nextInt().toString(),
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
        persisterIverksattFørstegangsbehandling(
            sakId = sakId,
            fnr = fnr,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            journalpostId = journalpostId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
            id = id,
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
            ),
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    }.getOrNull()!!.also {
        behandlingRepo.lagre(it.second)
    }
}

internal fun TestDataHelper.persisterOpprettetRevurdering(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = ObjectMother.virkningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virkningsperiode().tilOgMed,
    journalpostId: String = random.nextInt().toString(),
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
        persisterIverksattFørstegangsbehandling(
            sakId = sak.id,
            fnr = sak.fnr,
            deltakelseFom = deltakelseFom,
            deltakelseTom = deltakelseTom,
            journalpostId = journalpostId,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
            id = id,
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
            ),
            hentSaksopplysninger = hentSaksopplysninger,
            clock = clock,
        )
    }.getOrNull()!!.also {
        behandlingRepo.lagre(it.second)
    }
}

internal fun TestDataHelper.persisterBehandletRevurdering(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = ObjectMother.virkningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virkningsperiode().tilOgMed,
    journalpostId: String = random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    valgteHjemler: List<String> = listOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak.javaClass.simpleName),
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
            id = id,
            søknad = søknad,
            sak = sak,
            clock = clock,
        )
    }
    return runBlocking {
        sak.sendRevurderingTilBeslutning(
            kommando = SendRevurderingTilBeslutningKommando(
                sakId = sakId,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                begrunnelse = begrunnelse,
                stansDato = stansDato,
                valgteHjemler = valgteHjemler,
                fritekstTilVedtaksbrev = FritekstTilVedtaksbrev("fritekstTilVedtaksbrev"),
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
    journalpostId: String = random.nextInt().toString(),
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
): Pair<Sak, MeldekortBehandletManuelt> {
    val (sak) = persisterIverksattFørstegangsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVurderingsperiode = tiltaksOgVurderingsperiode,
        id = id,
        søknad = søknad,
        beslutter = beslutter,
        sak = sak,
        clock = clock,
    )
    val meldeperioder = sak.meldeperiodeKjeder.meldeperioder
    val behandletMeldekort = ObjectMother.meldekortBehandletManuelt(
        sakId = sak.id,
        fnr = sak.fnr,
        saksnummer = sak.saksnummer,
        meldeperiode = meldeperioder.first(),
        periode = meldeperioder.first().periode,
    )
    meldekortRepo.lagre(behandletMeldekort)
    return Pair(sakRepo.hentForSakId(sakId)!!, behandletMeldekort)
}
