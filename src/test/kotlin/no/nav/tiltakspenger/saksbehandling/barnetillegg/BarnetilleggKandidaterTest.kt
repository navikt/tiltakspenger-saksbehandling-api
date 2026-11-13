package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggForBarnISøknaden
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggMedIdent
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggV2
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import org.junit.jupiter.api.Test

class BarnetilleggKandidaterTest {

    private val periode1 = 1.januar(2025) til 31.mars(2025)
    private val periode2 = 1.april(2025) til 30.juni(2025)

    private val ola = barnMedBarnetillegg(fornavn = "Ola", fødselsdato = 1.januar(2013))
    private val nils = barnMedBarnetillegg(fornavn = "Nils", fødselsdato = 1.januar(2021))
    private val kari = barnetilleggMedIdent(fornavn = "Kari", mellomnavn = null, fødselsdato = 1.januar(2015))
    private val per = barnetilleggMedIdent(fornavn = "Per", mellomnavn = null, fødselsdato = 1.januar(2017), oppholderSegIEØS = Søknad.JaNeiSpm.Nei)
    private val lise = barnetilleggMedIdent(fornavn = "Lise", mellomnavn = null, fødselsdato = 1.januar(2019))

    @Test
    fun `søknadsbehandling har barna fra søknaden, men ikke barn lagt til av saksbehandler`() {
        withTestApplicationContext { tac ->
            val deltakelse = tac.tiltaksdeltakelse(periode1)
            val (sak, søknad, behandlingId) = sendSøknadsbehandlingTilBeslutning(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(periode1, deltakelse),
                barnFraSøknad = listOf(per, kari),
                barnetilleggForSøknad = barnetilleggForBarnISøknaden(
                    innvilgelsesperioder = nonEmptyListOf(periode1),
                    velgBarn = { it.fornavn == "Kari" },
                    andreBarn = listOf(ola),
                ),
            )
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!

            oppdatertSak.barnetilleggKandidater(oppdatertSak.rammebehandlinger.hentRammebehandling(behandlingId)!!) shouldBe listOf(
                BarnetilleggKandidat(kari.tilBarnMedBarnetillegg(søknad.id), kari, tiltaksdeltakerIder = null, fraGjeldendeVedtak = false),
                BarnetilleggKandidat(per.tilBarnMedBarnetillegg(søknad.id), per, tiltaksdeltakerIder = null, fraGjeldendeVedtak = false),
            )
        }
    }

    @Test
    fun `revurdering har barna fra søknadene og gjeldende vedtak, og barn i flere kilder tas med én gang`() {
        withTestApplicationContext { tac ->
            val deltakelse1 = tac.tiltaksdeltakelse(periode1)
            val deltakelse2 = tac.tiltaksdeltakelse(periode2)
            val (sak, søknad1) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse1,
                innvilgelsesperioder = innvilgelsesperioder(periode1, deltakelse1),
                barnFraSøknad = listOf(kari, per),
                barnetilleggForSøknad = barnetilleggForBarnISøknaden(
                    innvilgelsesperioder = nonEmptyListOf(periode1),
                    velgBarn = { it.fornavn == "Kari" },
                    andreBarn = listOf(ola),
                ),
            )
            val (_, søknad2) = iverksettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                fnr = sak.fnr,
                tiltaksdeltakelse = deltakelse2,
                innvilgelsesperioder = innvilgelsesperioder(periode2, deltakelse2),
                barnFraSøknad = listOf(kari, lise),
            )

            val (sakMedRevurdering, revurdering, revurderingJson) = startRevurderingInnvilgelse(tac, sak.id)!!

            // Kari er søkt for i begge søknadene og gir barnetillegg i det første vedtaket, og hentes fra den nyeste søknaden.
            // Ola er lagt til av saksbehandler i det første vedtaket.
            sakMedRevurdering.barnetilleggKandidater(revurdering) shouldBe listOf(
                BarnetilleggKandidat(ola, søknadsopplysninger = null, tiltaksdeltakerIder = null, fraGjeldendeVedtak = true),
                BarnetilleggKandidat(kari.tilBarnMedBarnetillegg(søknad2.id), kari, tiltaksdeltakerIder = null, fraGjeldendeVedtak = true),
                BarnetilleggKandidat(per.tilBarnMedBarnetillegg(søknad1.id), per, setOf(deltakelse1.internDeltakelseId), fraGjeldendeVedtak = false),
                BarnetilleggKandidat(lise.tilBarnMedBarnetillegg(søknad2.id), lise, setOf(deltakelse2.internDeltakelseId), fraGjeldendeVedtak = false),
            )

            revurderingJson.getJSONArray("barnetilleggKandidater").getJSONObject(3).toString().shouldEqualJson(
                """
                {
                  "fnr": {"verdi": null, "erSladdet": false},
                  "fornavn": {"verdi": "Lise", "erSladdet": false},
                  "mellomnavn": {"verdi": null, "erSladdet": false},
                  "etternavn": {"verdi": "Etternavn Barn", "erSladdet": false},
                  "fødselsdato": {"verdi": "2019-01-01", "erSladdet": false},
                  "kilde": "SØKNAD",
                  "søknadId": "${søknad2.id}",
                  "søknadsopplysninger": {"oppholderSegIEØSSpm": {"svar": "JA"}, "oppholdssted": null},
                  "internDeltakelseIder": ["${deltakelse2.internDeltakelseId}"],
                  "fraGjeldendeVedtak": false
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `barn som er lagret på revurderingen er aktuelle uansett tiltaksdeltakelse, og barn lagt til av saksbehandler er ikke med`() {
        withTestApplicationContext { tac ->
            val deltakelse = tac.tiltaksdeltakelse(periode1)
            val (sak, søknad) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = deltakelse,
                innvilgelsesperioder = innvilgelsesperioder(periode1, deltakelse),
                barnFraSøknad = listOf(per),
            )
            val (_, revurdering) = startRevurderingInnvilgelse(tac, sak.id)!!
            val perMedValgtPeriode = per.tilBarnMedBarnetillegg(søknad.id).copy(perioder = nonEmptyListOf(1.februar(2025) til 28.februar(2025)))

            val (oppdatertSak, oppdatertRevurdering) = oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                innvilgelsesperioder = innvilgelsesperioder(periode1, deltakelse),
                barnetillegg = barnetilleggV2(innvilgelsesperioder = nonEmptyListOf(periode1), barn = listOf(perMedValgtPeriode, nils)),
            )

            oppdatertSak.barnetilleggKandidater(oppdatertRevurdering) shouldBe listOf(
                BarnetilleggKandidat(per.tilBarnMedBarnetillegg(søknad.id), per, tiltaksdeltakerIder = null, fraGjeldendeVedtak = false),
            )
        }
    }
}
