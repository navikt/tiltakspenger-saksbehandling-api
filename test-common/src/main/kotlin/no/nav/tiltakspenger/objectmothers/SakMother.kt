package no.nav.tiltakspenger.objectmothers

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.objectmothers.ObjectMother.godkjentAttestering
import no.nav.tiltakspenger.objectmothers.ObjectMother.nySøknad
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.ObjectMother.søknadstiltak
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlinger
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Vedtaksliste
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LeggTilLivsoppholdSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LeggTilLivsoppholdSaksopplysningCommand.HarYtelseForPeriode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.leggTilLivsoppholdSaksopplysning
import no.nav.tiltakspenger.utbetaling.domene.Utbetalinger
import java.time.LocalDate

interface SakMother {
    fun nySak(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
    ): Sak = Sak(
        id = sakId,
        fnr = fnr,
        saksnummer = saksnummer,
        behandlinger = Behandlinger(emptyList()),
        vedtaksliste = Vedtaksliste.empty(),
        meldekortBehandlinger = MeldekortBehandlinger.empty(),
        utbetalinger = Utbetalinger(emptyList()),
        meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
        brukersMeldekort = emptyList(),
        soknader = emptyList(),
    )

    fun sakMedOpprettetBehandling(
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        iDag: LocalDate = LocalDate.of(2023, 1, 1),
        løpenummer: Int = 1001,
        saksnummer: Saksnummer = Saksnummer(iDag, løpenummer),
        vurderingsperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
        fødselsdato: LocalDate = ObjectMother.fødselsdato(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: Søknad =
            nySøknad(
                søknadstiltak =
                søknadstiltak(
                    deltakelseFom = vurderingsperiode.fraOgMed,
                    deltakelseTom = vurderingsperiode.tilOgMed,
                ),
            ),
        registrerteTiltak: List<Tiltaksdeltagelse> = listOf(søknad.tiltak.toTiltak()),
        saksopplysninger: Saksopplysninger = Saksopplysninger(
            fødselsdato = fødselsdato,
            tiltaksdeltagelse = registrerteTiltak.single(),
        ),
    ): Sak {
        val førstegangsbehandling =
            runBlocking {
                Behandling.opprettDeprecatedFørstegangsbehandling(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    søknad = søknad,
                    saksbehandler = saksbehandler,
                    hentSaksopplysninger = { saksopplysninger },
                ).getOrFail()
            }
        return Sak(
            id = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            behandlinger = Behandlinger(førstegangsbehandling),
            vedtaksliste = Vedtaksliste.empty(),
            meldekortBehandlinger = MeldekortBehandlinger.empty(),
            utbetalinger = Utbetalinger(emptyList()),
            meldeperiodeKjeder = MeldeperiodeKjeder(emptyList()),
            brukersMeldekort = emptyList(),
            soknader = listOf(søknad),
        )
    }

    fun sakMedInnvilgetVilkårssett(
        sakId: SakId = SakId.random(),
        iDag: LocalDate = LocalDate.of(2023, 1, 1),
        løpenummer: Int = 1001,
        saksnummer: Saksnummer = Saksnummer(iDag, løpenummer),
        saksbehandler: Saksbehandler = saksbehandler(),
        correlationId: CorrelationId = CorrelationId.generate(),
        vurderingsperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
    ): Sak {
        return sakMedOpprettetBehandling(
            iDag = iDag,
            løpenummer = løpenummer,
            sakId = sakId,
            saksnummer = saksnummer,
            saksbehandler = saksbehandler,
            vurderingsperiode = vurderingsperiode,
        ).let {
            val oppdatertFørstegangsbehandling = it.førstegangsbehandling!!.leggTilLivsoppholdSaksopplysning(
                LeggTilLivsoppholdSaksopplysningCommand(
                    behandlingId = it.førstegangsbehandling!!.id,
                    saksbehandler = saksbehandler,
                    harYtelseForPeriode = HarYtelseForPeriode(
                        periode = it.førstegangsbehandling!!.stansperiode!!,
                        harYtelse = false,
                    ),
                    correlationId = correlationId,

                ),
            ).getOrNull()!!
            require(it.behandlinger.size == 1)
            it.copy(behandlinger = Behandlinger(oppdatertFørstegangsbehandling))
        }
    }

    fun sakTilBeslutter(
        sakId: SakId = SakId.random(),
        iDag: LocalDate = LocalDate.of(2023, 1, 1),
        løpenummer: Int = 1001,
        saksnummer: Saksnummer = Saksnummer(iDag, løpenummer),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        vurderingsperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
    ): Sak {
        return sakMedInnvilgetVilkårssett(
            iDag = iDag,
            løpenummer = løpenummer,
            vurderingsperiode = vurderingsperiode,
            sakId = sakId,
            saksnummer = saksnummer,
            saksbehandler = saksbehandler,
        ).let {
            require(it.behandlinger.size == 1)
            it.copy(
                behandlinger = Behandlinger(
                    it.førstegangsbehandling!!.tilBeslutning(saksbehandler).taBehandling(beslutter),
                ),
            )
        }
    }

    fun sakMedRammevedtak(
        sakId: SakId = SakId.random(),
        iDag: LocalDate = LocalDate.of(2023, 1, 1),
        løpenummer: Int = 1001,
        saksnummer: Saksnummer = Saksnummer(iDag, løpenummer),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        vurderingsperiode: Periode = Periode(fraOgMed = 1.januar(2023), tilOgMed = 31.januar(2023)),
    ): Sak {
        return sakTilBeslutter(
            sakId = sakId,
            saksnummer = saksnummer,
            iDag = iDag,
            vurderingsperiode = vurderingsperiode,
            løpenummer = løpenummer,
            saksbehandler = saksbehandler,
        ).let {
            require(it.behandlinger.size == 1)
            val iverksattBehandling = it.førstegangsbehandling!!.iverksett(beslutter, godkjentAttestering())
            it.copy(
                behandlinger = Behandlinger(iverksattBehandling),
                vedtaksliste = Vedtaksliste(it.opprettVedtak(iverksattBehandling).second),
            )
        }
    }
}
