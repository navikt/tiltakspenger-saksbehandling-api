package no.nav.tiltakspenger.vedtak.db

import no.nav.tiltakspenger.felles.Saksbehandler
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
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
    sakRepo.lagre(sak)

    return Pair(
        sakRepo.hentForSakId(sakId)!!,
        søknadRepo.hentForSøknadId(søknad.id),
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
): Sak {
    val (sak, _) =
        persisterOpprettetFørstegangsbehandling(
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
    oppdatertFørstegangsbehandling.opprettVedtak().also {
        vedtakRepo.lagre(it)
    }
    behandlingRepo.lagre(oppdatertFørstegangsbehandling)
    return sakRepo.hentForSakId(sakId)!!
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
): Pair<Sak, Meldekort.UtfyltMeldekort> {
    val sak =
        persisterIverksattFørstegangsbehandling(
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
            rammevedtakId = sak.rammevedtak!!.id,
            fnr = sak.fnr,
            saksnummer = sak.saksnummer,
        )
    meldekortRepo.lagre(utfyltMeldekort)
    return Pair(sakRepo.hentForSakId(sakId)!!, utfyltMeldekort)
}
