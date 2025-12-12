package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger

import arrow.core.right
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import org.junit.jupiter.api.Test

internal class OppdaterSaksopplysningerTest {
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
                tiltaksdeltakelse = Tiltaksdeltakelse(
                    eksternDeltakelseId = "TA12345",
                    gjennomføringId = null,
                    typeNavn = "Testnavn",
                    typeKode = TiltakstypeSomGirRett.JOBBKLUBB,
                    rettPåTiltakspenger = true,
                    deltakelseFraOgMed = behandling.saksopplysningsperiode!!.fraOgMed,
                    deltakelseTilOgMed = behandling.saksopplysningsperiode!!.tilOgMed,
                    deltakelseStatus = TiltakDeltakerstatus.Deltar,
                    deltakelseProsent = 100.0f,
                    antallDagerPerUke = 5.0f,
                    kilde = Tiltakskilde.Arena,
                    deltidsprosentGjennomforing = 100.0,
                ),
            )
            val (oppdatertSak, oppdatertBehandling, responseJson) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                behandling.id,
            )
            oppdatertBehandling.saksopplysninger.fødselsdato shouldBe 2.januar(2001)
        }
    }

    @Test
    fun `revurdering til omgjøring - kan oppdatere saksopplysninger`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!
            val forrigeTiltaksdeltakelse = revurdering!!.saksopplysninger.tiltaksdeltakelser.first()
            val avbruttTiltaksdeltakelse = forrigeTiltaksdeltakelse.copy(
                deltakelseFraOgMed = forrigeTiltaksdeltakelse.deltakelseFraOgMed!!,
                deltakelseTilOgMed = forrigeTiltaksdeltakelse.deltakelseTilOgMed!!.minusDays(1),
                deltakelseStatus = TiltakDeltakerstatus.Avbrutt,
            )
            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = avbruttTiltaksdeltakelse)
            val (_, oppdatertRevurdering: Rammebehandling) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            // Forventer at saksopplysningene er oppdatert og at resultatet har resatt seg.
            (oppdatertRevurdering as Revurdering).saksopplysninger.tiltaksdeltakelser.single() shouldBe avbruttTiltaksdeltakelse
            oppdatertRevurdering.resultat.right() shouldBe RevurderingResultat.Omgjøring.create(
                omgjørRammevedtak = sak.rammevedtaksliste.single(),
                saksopplysninger = oppdatertRevurdering.saksopplysninger,
            )
            oppdatertRevurdering.erFerdigutfylt() shouldBe true
            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = revurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }

    @Test
    fun `revurdering til omgjøring - skal nulle ut innvilgelsen dersom tiltaksdeltakelsen vi omgjør har blitt filtrert bort`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!

            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = null)
            val (_, oppdatertBehandling) = oppdaterSaksopplysningerForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
            )

            oppdatertBehandling.innvilgelsesperioder.shouldBeNull()
        }
    }
}
