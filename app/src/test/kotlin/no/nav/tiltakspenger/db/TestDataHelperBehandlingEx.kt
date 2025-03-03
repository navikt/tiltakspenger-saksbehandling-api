package no.nav.tiltakspenger.db

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.opprettFørsteMeldeperiode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.domene.behandling.SendRevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.domene.behandling.startRevurdering
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.opprettVedtak
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingRepoTest.Companion.random
import java.time.LocalDate

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
            sak = sak,
        ),
    barnetillegg: Barnetillegg? = null,
): Pair<Sak, Søknad> {
    this.persisterSakOgSøknad(
        søknad = søknad,
        sak = sak,
    )
    val sakMedBehandling =
        ObjectMother.sakMedOpprettetBehandling(
            søknad = søknad,
            fnr = fnr,
            virkningsperiode = tiltaksOgVurderingsperiode,
            saksnummer = saksnummer,
            saksbehandler = saksbehandler,
            sakId = sakId,
            barnetillegg = barnetillegg,
        )
    behandlingRepo.lagre(sakMedBehandling.førstegangsbehandling!!)

    return Pair(
        sakRepo.hentForSakId(sakId)!!,
        søknadRepo.hentForSøknadId(søknad.id)!!,
    )
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
            sak = sak,
        ),
    correlationId: CorrelationId = CorrelationId.generate(),
): Pair<Sak, Rammevedtak> {
    val (sak, _) = persisterOpprettetFørstegangsbehandling(
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
    val førstegangsbehandling = sak.førstegangsbehandling
    val oppdatertFørstegangsbehandling =
        førstegangsbehandling!!
            .tilBeslutning(
                SendSøknadsbehandlingTilBeslutningKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    behandlingId = førstegangsbehandling.id,
                    correlationId = correlationId,
                    fritekstTilVedtaksbrev = FritekstTilVedtaksbrev("fritekstTilVedtaksbrev"),
                    begrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("begrunnelseVilkårsvurdering"),
                    innvilgelsesperiode = tiltaksOgVurderingsperiode,
                    begrunnelse = null,
                    perioder = null,
                ),
            )
            .taBehandling(beslutter)
            .iverksett(beslutter, ObjectMother.godkjentAttestering(beslutter))
    behandlingRepo.lagre(oppdatertFørstegangsbehandling)
    val vedtak = sak.opprettVedtak(oppdatertFørstegangsbehandling).second
    vedtakRepo.lagre(vedtak)
    return sakRepo.hentForSakId(sakId)!! to vedtak
}

/**
 * Persisterer førstegangsbehandling med tilhørende rammevedtak og starter en revurdering
 */
internal fun TestDataHelper.persisterOpprettetRevurderingDeprecated(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = ObjectMother.virningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virningsperiode().tilOgMed,
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
            sak = sak,
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
    deltakelseFom: LocalDate = ObjectMother.virningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virningsperiode().tilOgMed,
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
            sak = sak,
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

internal fun TestDataHelper.persisterBehandletRevurdering(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = ObjectMother.virningsperiode().fraOgMed,
    deltakelseTom: LocalDate = ObjectMother.virningsperiode().tilOgMed,
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
            sak = sak,
        ),
    stansDato: LocalDate = ObjectMother.revurderingsperiode().fraOgMed,
    begrunnelse: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("fordi"),
): Pair<Sak, Behandling> {
    val (sak, behandling) = runBlocking {
        persisterOpprettetRevurdering(
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
    deltakelseFom: LocalDate = 1.januar(2023),
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
            sak = sak,
        ),
): Pair<Sak, MeldekortBehandling.MeldekortBehandlet> {
    val (sak, vedtak) = persisterIverksattFørstegangsbehandling(
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
    val førsteMeldeperiode = sak.opprettFørsteMeldeperiode()
    val behandletMeldekort = ObjectMother.meldekortBehandlet(
        sakId = sak.id,
        fnr = sak.fnr,
        saksnummer = sak.saksnummer,
        antallDagerForMeldeperiode = vedtak.antallDagerPerMeldeperiode,
        meldeperiode = førsteMeldeperiode,
        periode = førsteMeldeperiode.periode,
    )
    meldeperiodeRepo.lagre(behandletMeldekort.meldeperiode)
    meldekortRepo.lagre(behandletMeldekort)
    return Pair(sakRepo.hentForSakId(sakId)!!, behandletMeldekort)
}
