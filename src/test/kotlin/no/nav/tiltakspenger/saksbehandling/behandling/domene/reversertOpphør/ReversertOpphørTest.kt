package no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskStans.utledAutomatiskStans
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelseForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.VurdertTiltaksdeltakerEndring.IngenEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.finnEndringerForDeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.medPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tester reversert opphør gjennom prodstien, fra omgjøringen til endringsjobben og automatisk stans.
 */
class ReversertOpphørTest {

    private val januarTilMars = 1.januar(2025) til 31.mars(2025)
    private val januarTilFebruar = 1.januar(2025) til 28.februar(2025)
    private val mars = 1.mars(2025) til 31.mars(2025)

    /**
     * Innvilger januar til mars, stanser fra mars og forkorter deretter tiltaksdeltakelsen til februar.
     * Mars er da opphørt, men har ikke lenger gyldig tiltaksdeltakelse.
     */
    private suspend fun ApplicationTestBuilder.innvilgStansOgForkortDeltakelsen(
        tac: TestApplicationContext,
    ): Triple<Sak, Rammevedtak, TiltaksdeltakelseIntern> {
        val opprinneligDeltakelse = tiltaksdeltakelse(fom = januarTilMars.fraOgMed, tom = januarTilMars.tilOgMed)
        val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
            tac = tac,
            innvilgelsesperioder = innvilgelsesperioder(januarTilMars, opprinneligDeltakelse),
            tiltaksdeltakelse = opprinneligDeltakelse,
        )
        iverksettRevurderingStans(tac = tac, sakId = sak.id, stansFraOgMed = mars.fraOgMed)
        tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = opprinneligDeltakelse.medPeriode(tilOgMed = januarTilFebruar.tilOgMed))
        return Triple(sak, søknadsvedtak, opprinneligDeltakelse)
    }

    @Test
    fun `kan innvilge en stanset periode på nytt som reversert opphør ved omgjøring`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknadsvedtak, opprinneligDeltakelse) = innvilgStansOgForkortDeltakelsen(tac)

            val (_, omgjøring, startJson) = startRevurderingOmgjøring(tac, sak.id, søknadsvedtak.id)!!
            startJson.getJSONArray("perioderForReversertOpphør").toString() shouldEqualJson """
                [
                  {
                    "internDeltakelseId": "${opprinneligDeltakelse.internDeltakelseId}",
                    "periode": { "fraOgMed": "2025-03-01", "tilOgMed": "2025-03-31" },
                    "typeNavn": "${opprinneligDeltakelse.typeNavn}"
                  }
                ]
            """.trimIndent()

            val (_, oppdatert, oppdaterJson) = oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = januarTilMars,
                innvilgelsesperioder = innvilgelsesperioder(
                    innvilgelsesperiode(januarTilFebruar, opprinneligDeltakelse),
                    innvilgelsesperiode(mars, opprinneligDeltakelse, reversertOpphør = ReversertOpphør(søknadsvedtak.id)),
                ),
            )
            oppdaterJson shouldContain """"erReversertOpphør":true"""

            val innvilgelsesperioder = oppdatert.innvilgelsesperioder!!
            innvilgelsesperioder.perioder shouldBe listOf(januarTilFebruar, mars)
            innvilgelsesperioder.hentVerdiForDag(januarTilFebruar.fraOgMed)!!.reversertOpphør shouldBe null
            innvilgelsesperioder.hentVerdiForDag(mars.fraOgMed)!!.reversertOpphør shouldBe ReversertOpphør(søknadsvedtak.id)
            innvilgelsesperioder.hentVerdiForDag(mars.fraOgMed)!!.valgtTiltaksdeltakelse shouldBe opprinneligDeltakelse

            val (_, omgjøringsvedtak) = iverksettOmgjøringInnvilgelseForBehandlingId(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadsvedtak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = januarTilMars,
                innvilgelsesperioder = innvilgelsesperioder,
            )
            omgjøringsvedtak.innvilgelsesperioder!!.hentVerdiForDag(mars.fraOgMed)!!.reversertOpphør shouldBe ReversertOpphør(søknadsvedtak.id)
        }
    }

    @Test
    fun `kan ikke innvilge reversert opphør utenfor den opphørte delen av den opprinnelige innvilgelsen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknadsvedtak, opprinneligDeltakelse) = innvilgStansOgForkortDeltakelsen(tac)
            val (_, omgjøring) = startRevurderingOmgjøring(tac, sak.id, søknadsvedtak.id)!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = januarTilMars,
                innvilgelsesperioder = innvilgelsesperioder(
                    // Februar er fortsatt innvilget med gyldig tiltaksdeltakelse, og kan ikke reverseres.
                    innvilgelsesperiode(1.januar(2025) til 31.januar(2025), opprinneligDeltakelse),
                    innvilgelsesperiode(1.februar(2025) til 31.mars(2025), opprinneligDeltakelse, reversertOpphør = ReversertOpphør(søknadsvedtak.id)),
                ),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "perioden_kan_ikke_reverseres"
            }
        }
    }

    @Test
    fun `kan ikke innvilge reversert opphør ved revurdering til innvilgelse`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknadsvedtak, opprinneligDeltakelse) = innvilgStansOgForkortDeltakelsen(tac)
            val (_, revurdering) = startRevurderingInnvilgelse(tac, sak.id)!!

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                innvilgelsesperioder = innvilgelsesperioder(
                    innvilgelsesperiode(mars, opprinneligDeltakelse, reversertOpphør = ReversertOpphør(søknadsvedtak.id)),
                ),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "reversert_opphør_kun_ved_omgjøring"
            }
        }
    }

    @Test
    fun `endret tiltaksdeltakelse gir ingen endring for perioder innvilget som reversert opphør`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, opprinneligDeltakelse) = iverksettOmgjøringMedReversertOpphørIMars(tac)

            sak.finnEndringerForDeltakelse(
                tiltaksdeltakerId = opprinneligDeltakelse.internDeltakelseId,
                oppdatertDeltakelse = opprinneligDeltakelse.medPeriode(tilOgMed = januarTilFebruar.tilOgMed).tilLibsDeltakelse(),
                clock = tac.clock,
            ) shouldBe IngenEndring
        }
    }

    @Test
    fun `automatisk stans overlates til saksbehandler når reversert opphør er innvilget etter sluttdatoen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, opprinneligDeltakelse) = iverksettOmgjøringMedReversertOpphørIMars(tac)

            sak.utledAutomatiskStans(
                tiltaksdeltakerId = opprinneligDeltakelse.internDeltakelseId,
                nåtilstand = opprinneligDeltakelse.medPeriode(tilOgMed = januarTilFebruar.tilOgMed).copy(
                    deltakelseStatus = TiltakDeltakerstatus.HarSluttet,
                ),
                iDag = LocalDate.now(tac.clock),
            ).leftOrNull()!! shouldContain ManueltBehandlesGrunn.STANS_REVERSERT_OPPHOR_ETTER_SLUTTDATO
        }
    }

    /** Innvilger januar og februar med gyldig tiltaksdeltakelse, og mars som reversert opphør. */
    private suspend fun ApplicationTestBuilder.iverksettOmgjøringMedReversertOpphørIMars(
        tac: TestApplicationContext,
    ): Pair<Sak, TiltaksdeltakelseIntern> {
        val (sak, søknadsvedtak, opprinneligDeltakelse) = innvilgStansOgForkortDeltakelsen(tac)
        val (oppdatertSak) = iverksettOmgjøringInnvilgelse(
            tac = tac,
            sakId = sak.id,
            rammevedtakIdSomOmgjøres = søknadsvedtak.id,
            vedtaksperiode = januarTilMars,
            innvilgelsesperioder = innvilgelsesperioder(
                innvilgelsesperiode(januarTilFebruar, opprinneligDeltakelse),
                innvilgelsesperiode(mars, opprinneligDeltakelse, reversertOpphør = ReversertOpphør(søknadsvedtak.id)),
            ),
        )
        return oppdatertSak to opprinneligDeltakelse
    }
}
