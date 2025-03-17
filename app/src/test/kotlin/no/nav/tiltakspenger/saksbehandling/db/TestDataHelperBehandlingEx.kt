package no.nav.tiltakspenger.saksbehandling.db

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingRepoTest.Companion.random
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.SendRevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.startRevurdering
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.opprettVedtak
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
): Pair<Sak, Søknad> {
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
        )
    behandlingRepo.lagre(sakMedBehandling.ikkeAvbruttFørstegangsbehandlinger.singleOrNullOrThrow()!!)

    return Pair(
        sakRepo.hentForSakId(sakId)!!,
        søknadRepo.hentForSøknadId(søknad.id)!!,
    )
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
    )
    val førstegangsbehandling = sakMedFørstegangsbehandling.ikkeAvbruttFørstegangsbehandlinger.singleOrNullOrThrow()!!
    val avbruttBehandling = førstegangsbehandling.avbryt(
        saksbehandler,
        "begrunnelse",
        avbruttTidspunkt,
    )
    behandlingRepo.lagre(avbruttBehandling)
    return sakRepo.hentForSakId(sakMedFørstegangsbehandling.id)!! to avbruttBehandling
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
): Pair<Sak, Rammevedtak> {
    val (sak, _) = persisterOpprettetFørstegangsbehandling(
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
    val førstegangsbehandling = sak.ikkeAvbruttFørstegangsbehandlinger.singleOrNullOrThrow()!!
    val oppdatertFørstegangsbehandling =
        førstegangsbehandling
            .tilBeslutning(
                SendSøknadsbehandlingTilBeslutningKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    behandlingId = førstegangsbehandling.id,
                    correlationId = correlationId,
                    fritekstTilVedtaksbrev = FritekstTilVedtaksbrev("fritekstTilVedtaksbrev"),
                    begrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("begrunnelseVilkårsvurdering"),
                    innvilgelsesperiode = tiltaksOgVurderingsperiode,
                    barnetillegg = null,
                    tiltaksdeltakelser = listOf(Pair(tiltaksOgVurderingsperiode, førstegangsbehandling.saksopplysninger.tiltaksdeltagelse.first().eksternDeltagelseId)),
                ),
            )
            .taBehandling(beslutter)
            .iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter))
    behandlingRepo.lagre(oppdatertFørstegangsbehandling)
    val vedtak = sak.opprettVedtak(oppdatertFørstegangsbehandling).second
    vedtakRepo.lagre(vedtak)

    val oppdatertSak = sakRepo.hentForSakId(sakId)!!
    val (_, meldeperioder) = oppdatertSak.genererMeldeperioder()
    meldeperioder.forEach {
        meldeperiodeRepo.lagre(it)
    }

    return sakRepo.hentForSakId(sakId)!! to vedtak
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
    stansDato: LocalDate = ObjectMother.revurderingsperiode().fraOgMed,
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("fordi"),
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
            ),
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
): Pair<Sak, MeldekortBehandling.MeldekortBehandlet> {
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
    )
    val (_, meldeperioder) = sak.genererMeldeperioder()
    val behandletMeldekort = ObjectMother.meldekortBehandlet(
        sakId = sak.id,
        fnr = sak.fnr,
        saksnummer = sak.saksnummer,
        meldeperiode = meldeperioder.first(),
        periode = meldeperioder.first().periode,
    )
    meldeperioder.forEach {
        meldeperiodeRepo.lagre(it)
    }
    meldekortRepo.lagre(behandletMeldekort)
    return Pair(sakRepo.hentForSakId(sakId)!!, behandletMeldekort)
}
