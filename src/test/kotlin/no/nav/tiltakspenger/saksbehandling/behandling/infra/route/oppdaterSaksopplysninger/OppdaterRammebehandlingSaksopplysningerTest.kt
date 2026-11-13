package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakstype
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.UgyldigInnvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.lukketEllerÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.medPeriode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppdaterRammebehandlingSaksopplysningerTest {
    @Test
    fun `søknadsbehandling - saksopplysninger blir oppdatert`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            behandling.saksopplysninger.fødselsdato shouldBe 1.januar(2001)
            val personopplysningerForBrukerFraPdl = ObjectMother.personopplysningKjedeligFyr(
                fnr = sak.fnr,
                fødselsdato = 2.januar(2001),
            )
            tac.leggTilPerson(
                fnr = sak.fnr,
                person = personopplysningerForBrukerFraPdl,
                tiltaksdeltakelse = TiltaksdeltakelseIntern(
                    eksternDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.value.first().eksternDeltakelseId,
                    gjennomføringId = null,
                    typeNavn = "Testnavn",
                    tittel = null,
                    tiltakstype = TiltakstypeSomGirRett.JOBBKLUBB,
                    rettPåTiltakspenger = true,
                    periode = lukketEllerÅpenPeriode(behandling.saksopplysningsperiode!!.fraOgMed, behandling.saksopplysningsperiode!!.tilOgMed),
                    deltakelseStatus = TiltakDeltakerstatus.Deltar,
                    deltakelseProsent = 100.0f,
                    antallDagerPerUke = 5.0f,
                    kilde = Tiltakskilde.Arena,
                    deltidsprosentGjennomforing = 100.0,
                    internDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.value.first().internDeltakelseId,
                ),
            )
            val (_, oppdatertBehandling, _) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                behandling.id,
            )
            oppdatertBehandling.saksopplysninger.fødselsdato shouldBe 2.januar(2001)
        }
    }

    @Test
    fun `søknadsbehandling - lønnstilskudd som overlapper saksopplysningsperioden vises, men gir ikke rett`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            behandling.saksopplysninger.tiltaksdeltakelserSomFratarRett.shouldBeEmpty()
            tac.leggTilTiltaksdeltakelseSomFratarRett(sak.fnr, lønnstilskudd(id = "TA1", fraOgMed = 1.februar(2023), tilOgMed = 30.juni(2023)))
            tac.leggTilTiltaksdeltakelseSomFratarRett(sak.fnr, lønnstilskudd(id = "TA2", fraOgMed = 1.mai(2023), tilOgMed = 30.juni(2023)))

            val (_, oppdatertBehandling, body) = oppdaterSaksopplysningerForBehandlingId(tac, sak.id, behandling.id)

            oppdatertBehandling.saksopplysninger.tiltaksdeltakelser shouldBe behandling.saksopplysninger.tiltaksdeltakelser
            oppdatertBehandling.saksopplysninger.tiltaksdeltakelserSomFratarRett.map { it.eksternDeltakelseId } shouldBe listOf("TA1")
            body.shouldContainJsonKeyValue("$.saksopplysninger.tiltaksdeltakelserSomFratarRett.length()", 1)
            body.shouldContainJsonKeyValue("$.saksopplysninger.tiltaksdeltakelserSomFratarRett[0].eksternDeltakelseId", "TA1")
            body.shouldContainJsonKeyValue("$.saksopplysninger.tiltaksdeltakelserSomFratarRett[0].tiltakskodeFraKilden", "MIDLONTIL")
            body.shouldContainJsonKeyValue("$.saksopplysninger.tiltaksdeltakelserSomFratarRett[0].deltakelseStatus", "Deltar")
        }
    }

    private fun lønnstilskudd(id: String, fraOgMed: LocalDate, tilOgMed: LocalDate) = testdeltakelse(
        id = id,
        tiltakstype = Tiltakstype.SomFratarRett(tiltakskodeFraKilden = "MIDLONTIL"),
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
    ) as Tiltaksdeltakelse.FratarRett

    @Test
    fun `revurdering til omgjøring - oppdaterer valgt tiltaksdeltakelse som fortsatt dekker innvilgelsen`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsvedtak, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                vedtaksperiode = søknadsvedtak.periode,
            )

            val forrigeTiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.first()
            val endretTiltaksdeltakelse = forrigeTiltaksdeltakelse.copy(deltakelseStatus = TiltakDeltakerstatus.Avbrutt)

            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = endretTiltaksdeltakelse)
            val (_, oppdatertRevurdering: Rammebehandling, body) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )

            body.shouldContainJsonKeyValue("$.innvilgelsesperioder[0].ugyldigTiltaksdeltakelse", null)
            (oppdatertRevurdering as Revurdering).saksopplysninger.tiltaksdeltakelser.single() shouldBe endretTiltaksdeltakelse
            val innvilgelsesperioder = (oppdatertRevurdering.resultat as OmgjøringInnvilgelse).innvilgelsesperioder!!
            innvilgelsesperioder.totalPeriode shouldBe søknadsvedtak.periode
            innvilgelsesperioder.valgteTiltaksdeltagelser.single().verdi.single() shouldBe endretTiltaksdeltakelse
            oppdatertRevurdering.erFerdigutfylt() shouldBe true
            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = revurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }

    @Test
    fun `revurdering til omgjøring - beholder innvilgelsen når tiltaksdeltakelsen har krympet`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsvedtak, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                vedtaksperiode = søknadsvedtak.periode,
            )

            val forrigeTiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.first()
            val avbruttTiltaksdeltakelse = forrigeTiltaksdeltakelse.medPeriode(fraOgMed = forrigeTiltaksdeltakelse.periode.fraOgMed!!, tilOgMed = forrigeTiltaksdeltakelse.periode.tilOgMed!!.minusDays(1)).copy(
                deltakelseStatus = TiltakDeltakerstatus.Avbrutt,
            )

            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = avbruttTiltaksdeltakelse)
            val (_, oppdatertRevurdering: Rammebehandling, body) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )

            body.shouldContainJsonKeyValue("$.innvilgelsesperioder[0].ugyldigTiltaksdeltakelse.type", "DEKKER_IKKE_PERIODEN")
            body.shouldContainJsonKeyValue(
                "$.innvilgelsesperioder[0].ugyldigTiltaksdeltakelse.deltakelsesperiode.tilOgMed",
                avbruttTiltaksdeltakelse.periode.tilOgMed.toString(),
            )
            (oppdatertRevurdering as Revurdering).saksopplysninger.tiltaksdeltakelser.single() shouldBe avbruttTiltaksdeltakelse
            val innvilgelsesperioder = (oppdatertRevurdering.resultat as OmgjøringInnvilgelse).innvilgelsesperioder!!
            innvilgelsesperioder.totalPeriode shouldBe søknadsvedtak.periode
            innvilgelsesperioder.valgteTiltaksdeltagelser.single().verdi.single() shouldBe forrigeTiltaksdeltakelse
            innvilgelsesperioder.ugyldigePerioder(oppdatertRevurdering.saksopplysninger.tiltaksdeltakelser) shouldBe nonEmptyListOf(
                UgyldigInnvilgelsesperiode.TiltaksdeltakelsenDekkerIkkePerioden(
                    periode = søknadsvedtak.periode,
                    internDeltakelseId = forrigeTiltaksdeltakelse.internDeltakelseId,
                    deltakelsesperiode = avbruttTiltaksdeltakelse.lukketPeriode!!,
                ),
            )
            tac.sakContext.sakService.hentForSakId(sakId = revurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).shouldContainJsonKeyValue("$.kode", "innvilgelsen_passer_ikke_med_tiltaksdeltakelsene")
        }
    }

    /** Kjører mot postgres fordi det er lagringen av en innvilgelse som ikke passer med saksopplysningene som dekkes. */
    @Test
    fun `revurdering til omgjøring - beholder innvilgelsen når tiltaksdeltakelsen vi omgjør har blitt filtrert bort`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, søknadsvedtak, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                vedtaksperiode = søknadsvedtak.periode,
            )
            val forrigeTiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.first()

            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = null)
            val (_, oppdatertBehandling, body) = oppdaterSaksopplysningerForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
            )

            body.shouldContainJsonKeyValue("$.innvilgelsesperioder[0].ugyldigTiltaksdeltakelse.type", "FINNES_IKKE")
            body.shouldContainJsonKeyValue("$.innvilgelsesperioder[0].ugyldigTiltaksdeltakelse.deltakelsesperiode", null)

            val resultat = (oppdatertBehandling as Revurdering).resultat as OmgjøringInnvilgelse
            resultat.innvilgelsesperioder!!.valgteTiltaksdeltagelser.single().verdi.single() shouldBe forrigeTiltaksdeltakelse
            resultat.innvilgelsesperioder.ugyldigePerioder(oppdatertBehandling.saksopplysninger.tiltaksdeltakelser) shouldBe nonEmptyListOf(
                UgyldigInnvilgelsesperiode.TiltaksdeltakelsenFinnesIkke(
                    periode = søknadsvedtak.periode,
                    internDeltakelseId = forrigeTiltaksdeltakelse.internDeltakelseId,
                ),
            )
            tac.sakContext.sakService.hentForSakId(sakId = revurdering.sakId).rammebehandlinger[1] shouldBe oppdatertBehandling
        }
    }
}
