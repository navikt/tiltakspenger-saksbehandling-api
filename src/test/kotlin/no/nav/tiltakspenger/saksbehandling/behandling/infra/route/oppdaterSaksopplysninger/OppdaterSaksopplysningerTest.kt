package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
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
                    eksternDeltagelseId = "TA12345",
                    gjennomføringId = null,
                    typeNavn = "Testnavn",
                    typeKode = TiltakstypeSomGirRett.JOBBKLUBB,
                    rettPåTiltakspenger = true,
                    deltagelseFraOgMed = behandling.saksopplysningsperiode!!.fraOgMed,
                    deltagelseTilOgMed = behandling.saksopplysningsperiode!!.tilOgMed,
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
            val (sak, _, _, revurdering) = startRevurderingOmgjøring(tac)
            val forrigeTiltaksdeltagelse = revurdering!!.saksopplysninger.tiltaksdeltakelser.first()
            val avbruttTiltaksdeltagelse = forrigeTiltaksdeltagelse.copy(
                deltagelseFraOgMed = forrigeTiltaksdeltagelse.deltagelseFraOgMed!!,
                deltagelseTilOgMed = forrigeTiltaksdeltagelse.deltagelseTilOgMed!!.minusDays(1),
                deltakelseStatus = TiltakDeltakerstatus.Avbrutt,
            )
            tac.oppdaterTiltaksdeltagelse(fnr = sak.fnr, tiltaksdeltakelse = avbruttTiltaksdeltagelse)
            val (_, oppdatertRevurdering: Rammebehandling) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            // Forventer at saksopplysningene er oppdatert og at resultatet har resatt seg.
            (oppdatertRevurdering as Revurdering).saksopplysninger.tiltaksdeltakelser.single() shouldBe avbruttTiltaksdeltagelse
            oppdatertRevurdering.resultat shouldBe RevurderingResultat.Omgjøring.create(
                omgjørRammevedtak = sak.rammevedtaksliste.single(),
                saksopplysninger = oppdatertRevurdering.saksopplysninger,
            )
            oppdatertRevurdering.erFerdigutfylt() shouldBe true
            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = revurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }

    @Test
    fun `revurdering til omgjøring - kan ikke oppdatere saksopplysninger dersom tiltaksdeltagelsen vi omgjør har blitt filtrert bort`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingOmgjøring(tac)

            tac.oppdaterTiltaksdeltagelse(fnr = sak.fnr, tiltaksdeltakelse = null)
            val (_, _) = oppdaterSaksopplysningerForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering!!.id,
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }
}
