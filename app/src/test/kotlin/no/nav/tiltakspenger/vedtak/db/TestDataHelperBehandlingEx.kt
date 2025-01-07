package no.nav.tiltakspenger.vedtak.db

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
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.startRevurdering
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LeggTilLivsoppholdSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LeggTilLivsoppholdSaksopplysningCommand.HarYtelseForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.leggTilLivsoppholdSaksopplysning
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
        ),
): Pair<Sak, Søknad> {
    this.persisterSøknad(
        søknad = søknad,
    )
    val sak =
        ObjectMother.sakMedOpprettetBehandling(
            søknad = søknad,
            fnr = fnr,
            vurderingsperiode = tiltaksOgVurderingsperiode,
            saksnummer = saksnummer,
            saksbehandler = saksbehandler,
            sakId = sakId,
        )
    søknadRepo.lagre(søknad)
    sakRepo.opprettSakOgFørstegangsbehandling(sak)

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
        ),
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
    )
    val førstegangsbehandling = sak.førstegangsbehandling
    val oppdatertFørstegangsbehandling =
        førstegangsbehandling
            .leggTilLivsoppholdSaksopplysning(
                LeggTilLivsoppholdSaksopplysningCommand(
                    behandlingId = førstegangsbehandling.id,
                    saksbehandler = saksbehandler,
                    harYtelseForPeriode =
                    HarYtelseForPeriode(
                        periode = førstegangsbehandling.vurderingsperiode,
                        harYtelse = false,
                    ),
                    årsakTilEndring = null,
                    correlationId = CorrelationId.generate(),
                ),
            ).getOrNull()!!
            .tilBeslutning(saksbehandler)
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
internal fun TestDataHelper.persisterOpprettetRevurdering(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
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
        ),
    revurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom.plusMonths(1), tilOgMed = deltakelseTom),
): Pair<Sak, Behandling> {
    val (sak, _) = persisterIverksattFørstegangsbehandling(
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
    )
    return sak.startRevurdering(
        kommando = StartRevurderingKommando(
            sakId = sakId,
            periode = revurderingsperiode,
            correlationId = CorrelationId.generate(),
            saksbehandler = saksbehandler,
        ),
    ).getOrNull()!!.also {
        behandlingRepo.lagre(it.second)
    }
}

internal fun TestDataHelper.persisterRammevedtakMedUtfyltMeldekort(
    sakId: SakId = SakId.random(),
    fnr: Fnr = Fnr.random(),
    deltakelseFom: LocalDate = 1.januar(2023),
    deltakelseTom: LocalDate = 31.mars(2023),
    journalpostId: String = random.nextInt().toString(),
    saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    beslutter: Saksbehandler = ObjectMother.beslutter(),
    tiltaksOgVurderingsperiode: Periode = Periode(fraOgMed = deltakelseFom, tilOgMed = deltakelseTom),
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
        ),
): Pair<Sak, MeldekortBehandling.UtfyltMeldekort> {
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
    )
    val utfyltMeldekort =
        ObjectMother.utfyltMeldekort(
            sakId = sak.id,
            rammevedtakId = vedtak.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
        )
    meldekortRepo.lagre(utfyltMeldekort)
    return Pair(sakRepo.hentForSakId(sakId)!!, utfyltMeldekort)
}
