package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import org.junit.jupiter.api.Test

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
                tiltaksdeltakelse = Tiltaksdeltakelse(
                    eksternDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.value.first().eksternDeltakelseId,
                    gjennomføringId = null,
                    typeNavn = "Testnavn",
                    typeKode = TiltakstypeSomGirRettDTO.JOBBKLUBB,
                    rettPåTiltakspenger = true,
                    deltakelseFraOgMed = behandling.saksopplysningsperiode!!.fraOgMed,
                    deltakelseTilOgMed = behandling.saksopplysningsperiode!!.tilOgMed,
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
    fun `revurdering til omgjøring - kan oppdatere saksopplysninger`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsvedtak, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                vedtaksperiode = søknadsvedtak.periode,
            )

            val forrigeTiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.first()
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

            (oppdatertRevurdering.resultat as OmgjøringInnvilgelse)
                .innvilgelsesperioder!!.valgteTiltaksdeltagelser
                .single().verdi shouldBe avbruttTiltaksdeltakelse

            oppdatertRevurdering.erFerdigutfylt() shouldBe true
            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = revurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }

    /**
     * En fersk omgjøring står som `OmgjøringIkkeValgt`, så innvilgelsen må velges før den kan nulles.
     * Kjører mot postgres fordi det er lagringen av det nullstilte resultatet som dekkes.
     */
    @Test
    fun `revurdering til omgjøring - skal nulle ut innvilgelsen dersom tiltaksdeltakelsen vi omgjør har blitt filtrert bort`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, søknadsvedtak, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                vedtaksperiode = søknadsvedtak.periode,
            )

            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = null)
            val (_, oppdatertBehandling) = oppdaterSaksopplysningerForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
            )

            val resultat = (oppdatertBehandling as Revurdering).resultat as OmgjøringInnvilgelse
            resultat.innvilgelsesperioder.shouldBeNull()
            resultat.barnetillegg.shouldBeNull()
        }
    }
}
