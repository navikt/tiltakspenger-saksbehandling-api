package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.NonEmptySet
import arrow.core.Tuple4
import arrow.core.nonEmptySetOf
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling.AUTOMATISK_SAKSBEHANDLER
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterSøknadsbehandlingAvslagKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterSøknadsbehandlingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilBeslutning
import no.nav.tiltakspenger.saksbehandling.objectmothers.toTiltak
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettRammevedtak
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
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    id: SøknadId = Søknad.randomId(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
    søknad: InnvilgbarSøknad =
        ObjectMother.nyInnvilgbarSøknad(
            periode = tiltaksOgVedtaksperiode,
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
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(tiltaksOgVedtaksperiode),
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
            vedtaksperiode = tiltaksOgVedtaksperiode,
            saksnummer = sak.saksnummer,
            saksbehandler = saksbehandler,
            sakId = sak.id,
            barnetillegg = barnetillegg,
            clock = clock,
        )
    behandlingRepo.lagre(sakMedBehandling.rammebehandlinger.singleOrNullOrThrow()!!)

    return Triple(
        sakRepo.hentForSakId(sakId)!!,
        sakMedBehandling.rammebehandlinger.singleOrNullOrThrow()!! as Søknadsbehandling,
        søknadRepo.hentForSøknadId(søknad.id)!!,
    )
}

internal fun TestDataHelper.persisterOpprettetAutomatiskSøknadsbehandling(
    sakId: SakId = SakId.random(),
    saksnummer: Saksnummer = this.saksnummerGenerator.neste(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    id: SøknadId = Søknad.randomId(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
    ),
    søknad: InnvilgbarSøknad =
        ObjectMother.nyInnvilgbarSøknad(
            periode = tiltaksOgVedtaksperiode,
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
): Triple<Sak, Søknadsbehandling, Søknad> {
    this.persisterSakOgSøknad(
        fnr = sak.fnr,
        søknad = søknad,
        sak = sak,
    )
    val (sakMedBehandling) =
        ObjectMother.sakMedOpprettetAutomatiskBehandling(
            søknad = søknad,
            fnr = sak.fnr,
            vedtaksperiode = tiltaksOgVedtaksperiode,
            saksnummer = sak.saksnummer,
            sakId = sak.id,
            clock = clock,
        )
    behandlingRepo.lagre(sakMedBehandling.rammebehandlinger.singleOrNullOrThrow()!!)

    return Triple(
        sakRepo.hentForSakId(sakId)!!,
        sakMedBehandling.rammebehandlinger.singleOrNullOrThrow()!! as Søknadsbehandling,
        søknadRepo.hentForSøknadId(søknad.id)!!,
    )
}

internal fun TestDataHelper.persisterAutomatiskSøknadsbehandlingUnderBeslutning(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: InnvilgbarSøknad = ObjectMother.nyInnvilgbarSøknad(
        periode = tiltaksOgVedtaksperiode,
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
    beslutter: Saksbehandler = ObjectMother.beslutter(),
): Pair<Sak, Rammebehandling> {
    val (_, behandling) = persisterOpprettetAutomatiskSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        tiltaksOgVedtaksperiode = tiltaksOgVedtaksperiode,
        sak = sak,
        id = id,
        søknad = søknad,
        clock = clock,
    )

    val klarTilBeslutning = behandling.oppdater(
        kommando = oppdaterSøknadsbehandlingInnvilgelseKommando(
            sakId = sakId,
            behandlingId = behandling.id,
            saksbehandler = AUTOMATISK_SAKSBEHANDLER,
            correlationId = CorrelationId.generate(),
            fritekstTilVedtaksbrev = null,
            begrunnelseVilkårsvurdering = null,
            automatiskSaksbehandlet = true,
            innvilgelsesperioder = listOf(
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = tiltaksOgVedtaksperiode,
                    antallDagerPerMeldeperiode = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
                    tiltaksdeltakelse = behandling.søknad.tiltak!!.toTiltak(),
                ),
            ),
            barnetillegg = Barnetillegg.utenBarnetillegg(tiltaksOgVedtaksperiode),
        ),
        clock = clock,
        utbetaling = null,
        omgjørRammevedtak = OmgjørRammevedtak.empty,
    ).getOrFail().tilBeslutning(saksbehandler = AUTOMATISK_SAKSBEHANDLER)

    behandlingRepo.lagre(klarTilBeslutning)

    val tilBeslutning = behandlingRepo.hent(behandling.id).taBehandling(beslutter, clock)

    behandlingRepo.taBehandlingBeslutter(tilBeslutning)

    return sakRepo.hentForSakId(sakId)!! to behandlingRepo.hent(behandling.id)
}

internal fun TestDataHelper.persisterKlarTilBeslutningSøknadsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: InnvilgbarSøknad = ObjectMother.nyInnvilgbarSøknad(
        periode = tiltaksOgVedtaksperiode,
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
    fritekstTilVedtaksbrev: String = "persisterKlarTilBeslutningSøknadsbehandling()",
    begrunnelseVilkårsvurdering: String = "persisterKlarTilBeslutningSøknadsbehandling()",
    correlationId: CorrelationId = CorrelationId.generate(),
    innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
        innvilgelsesperiodeKommando(
            innvilgelsesperiode = tiltaksOgVedtaksperiode,
            tiltaksdeltakelse = søknad.tiltak.toTiltak(),
        ),
    ),
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
    resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
    /** Brukt for å styre meldeperiode generering */
    clock: Clock = this.clock,
    automatiskSaksbehandlet: Boolean = false,
): Pair<Sak, Rammebehandling> {
    val (sak, søknadsbehandling) = persisterOpprettetSøknadsbehandling(
        sakId = sak.id,
        fnr = sak.fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVedtaksperiode = tiltaksOgVedtaksperiode,
        id = id,
        søknad = søknad,
        sak = sak,
        clock = clock,
    )

    val oppdatertSøknadsbehandling =
        søknadsbehandling
            .oppdater(
                when (resultat) {
                    SøknadsbehandlingsresultatType.INNVILGELSE -> oppdaterSøknadsbehandlingInnvilgelseKommando(
                        sakId = sak.id,
                        saksbehandler = saksbehandler,
                        behandlingId = søknadsbehandling.id,
                        correlationId = correlationId,
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                        innvilgelsesperioder = innvilgelsesperioder,
                        barnetillegg = Barnetillegg.utenBarnetillegg(tiltaksOgVedtaksperiode),
                        automatiskSaksbehandlet = automatiskSaksbehandlet,
                    )

                    SøknadsbehandlingsresultatType.AVSLAG -> oppdaterSøknadsbehandlingAvslagKommando(
                        sakId = sak.id,
                        saksbehandler = saksbehandler,
                        behandlingId = søknadsbehandling.id,
                        correlationId = correlationId,
                        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                        avslagsgrunner = avslagsgrunner!!,
                    )
                },
                clock = clock,
                utbetaling = null,
                omgjørRammevedtak = OmgjørRammevedtak.empty,
            ).getOrFail()

    val søknadsbehandlingKlarTilBeslutning = oppdatertSøknadsbehandling.tilBeslutning(
        saksbehandler = saksbehandler,
        clock = clock,
    )

    behandlingRepo.lagre(søknadsbehandlingKlarTilBeslutning)
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    return Pair(oppdatertSak, søknadsbehandlingKlarTilBeslutning)
}

internal fun TestDataHelper.persisterUnderBeslutningSøknadsbehandling(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: InnvilgbarSøknad = ObjectMother.nyInnvilgbarSøknad(
        periode = tiltaksOgVedtaksperiode,
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
    fritekstTilVedtaksbrev: String = "persisterKlarTilBeslutningSøknadsbehandling()",
    begrunnelseVilkårsvurdering: String = "persisterKlarTilBeslutningSøknadsbehandling()",
    correlationId: CorrelationId = CorrelationId.generate(),
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
    beslutter: Saksbehandler = ObjectMother.beslutter(),
): Pair<Sak, Rammebehandling> {
    val behandling = persisterKlarTilBeslutningSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVedtaksperiode = tiltaksOgVedtaksperiode,
        sak = sak,
        id = id,
        søknad = søknad,
        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
        correlationId = correlationId,
        clock = clock,
    ).second

    val tilBeslutning = behandling.taBehandling(beslutter, clock)

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
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: InnvilgbarSøknad =
        ObjectMother.nyInnvilgbarSøknad(
            periode = tiltaksOgVedtaksperiode,
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
): Pair<Sak, Rammebehandling> {
    val (sakMedSøknadsbehandling, _) = persisterOpprettetSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVedtaksperiode = tiltaksOgVedtaksperiode,
        id = id,
        søknad = søknad,
        sak = sak,
        clock = clock,
    )
    val søknadsbehandling =
        sakMedSøknadsbehandling.rammebehandlinger.filter { it is Søknadsbehandling && it.søknad.id == id }
            .singleOrNullOrThrow()!!
    val avbruttBehandling = søknadsbehandling.avbryt(
        avbruttAv = saksbehandler,
        begrunnelse = "begrunnelse".toNonBlankString(),
        tidspunkt = avbruttTidspunkt,
        skalAvbryteSøknad = true,
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
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    søknadId: SøknadId = Søknad.randomId(),
    søknad: InnvilgbarSøknad =
        ObjectMother.nyInnvilgbarSøknad(
            periode = tiltaksOgVedtaksperiode,
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
    fritekstTilVedtaksbrev: String = "persisterIverksattSøknadsbehandling()",
    begrunnelseVilkårsvurdering: String = "persisterIverksattSøknadsbehandling()",
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
): Triple<Sak, Rammevedtak, Rammebehandling> {
    val (sak, søknadsbehandling) = persisterKlarTilBeslutningSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVedtaksperiode = tiltaksOgVedtaksperiode,
        sak = sak,
        id = søknadId,
        søknad = søknad,
        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
        correlationId = correlationId,
        clock = clock,
    )

    val oppdatertRammebehandling = søknadsbehandling
        .taBehandling(beslutter, clock)
        .iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter), correlationId, clock)
    behandlingRepo.lagre(oppdatertRammebehandling)
    val vedtak = sak.opprettRammevedtak(oppdatertRammebehandling, clock).second
    vedtakRepo.lagre(vedtak)
    sakRepo.oppdaterSkalSendeMeldeperioderTilDatadelingOgSkalSendesTilMeldekortApi(
        sakId = vedtak.sakId,
        skalSendesTilMeldekortApi = true,
        skalSendeMeldeperioderTilDatadeling = true,
    )
    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    val (_, meldeperioder) = oppdatertSak.genererMeldeperioder(clock)
    meldeperiodeRepo.lagre(meldeperioder)
    return Triple(sakRepo.hentForSakId(sakId)!!, vedtak, oppdatertRammebehandling)
}

internal fun TestDataHelper.persisterIverksattSøknadsbehandlingAvslag(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    id: SøknadId = Søknad.randomId(),
    søknad: InnvilgbarSøknad = ObjectMother.nyInnvilgbarSøknad(
        periode = tiltaksOgVedtaksperiode,
        journalpostId = journalpostId,
        personopplysninger = ObjectMother.personSøknad(fnr = fnr),
        id = id,
        søknadstiltak = ObjectMother.søknadstiltak(deltakelseFom = deltakelseFom, deltakelseTom = deltakelseTom),
        barnetillegg = listOf(),
        sakId = sak.id,
        saksnummer = sak.saksnummer,
    ),
    correlationId: CorrelationId = CorrelationId.generate(),
    fritekstTilVedtaksbrev: String = "persisterIverksattSøknadsbehandlingAvslag()",
    begrunnelseVilkårsvurdering: String = "persisterIverksattSøknadsbehandlingAvslag()",
    /**
     * Brukt for å styre meldeperiode generering
     */
    clock: Clock = this.clock,
): Triple<Sak, Rammevedtak, Rammebehandling> {
    val (sak, søknadsbehandling) = persisterKlarTilBeslutningSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVedtaksperiode = tiltaksOgVedtaksperiode,
        sak = sak,
        id = id,
        søknad = søknad,
        fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
        resultat = SøknadsbehandlingsresultatType.AVSLAG,
        avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
        correlationId = correlationId,
        clock = clock,
    )

    val oppdatertSøknadsbehandling =
        søknadsbehandling.taBehandling(beslutter, clock)
            .iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter), correlationId, clock)
    behandlingRepo.lagre(oppdatertSøknadsbehandling)
    val (sakMedVedtak, vedtak) = sak.opprettRammevedtak(oppdatertSøknadsbehandling, clock)
    vedtakRepo.lagre(vedtak)
    return Triple(sakMedVedtak, vedtak, oppdatertSøknadsbehandling)
}

/**
 * Persisterer behandlingen, rammevedtaket og utbetalingen
 */
internal fun TestDataHelper.persisterVedtattInnvilgetSøknadsbehandlingMedBehandletMeldekort(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 2.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    id: SøknadId = Søknad.randomId(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    søknad: InnvilgbarSøknad =
        ObjectMother.nyInnvilgbarSøknad(
            periode = tiltaksOgVedtaksperiode,
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
): Tuple4<Sak, Rammevedtak, Meldekortvedtak, MeldekortBehandletManuelt> {
    val (sak, rammevedtak) = persisterIverksattSøknadsbehandling(
        sakId = sakId,
        fnr = fnr,
        deltakelseFom = deltakelseFom,
        deltakelseTom = deltakelseTom,
        journalpostId = journalpostId,
        saksbehandler = saksbehandler,
        tiltaksOgVedtaksperiode = tiltaksOgVedtaksperiode,
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
        clock = clock,
    )
    val meldekortvedtak =
        behandletMeldekort.opprettVedtak(
            sak.utbetalinger.lastOrNull(),
            clock,
        )
    meldekortRepo.lagre(behandletMeldekort, null)
    meldekortvedtakRepo.lagre(meldekortvedtak)
    return Tuple4(sakRepo.hentForSakId(sakId)!!, rammevedtak, meldekortvedtak, behandletMeldekort)
}

internal fun TestDataHelper.persisterRammevedtakAvslag(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 2.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = Random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVedtaksperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
    søknadId: SøknadId = Søknad.randomId(),
    sak: Sak = ObjectMother.nySak(
        sakId = sakId,
        fnr = fnr,
        saksnummer = this.saksnummerGenerator.neste(),
    ),
    søknad: InnvilgbarSøknad = ObjectMother.nyInnvilgbarSøknad(
        periode = tiltaksOgVedtaksperiode,
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
        tiltaksOgVedtaksperiode = tiltaksOgVedtaksperiode,
        id = søknadId,
        søknad = søknad,
        beslutter = beslutter,
        sak = sak,
        clock = clock,
    )

    return Pair(sak, rammevedtak)
}
