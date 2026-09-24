package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.angre

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.angreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning
import org.junit.jupiter.api.Test

class AngreMeldekortbehandlingRouteTest {

    @Test
    fun `en saksbehandler kan ikke angre en meldekortbehandling fra en annen sak`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, _, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac)!!
            val (_, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
            }

            angreMeldekortbehandling(
                tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                forventet = ForventetRespons.json(
                    status = 400,
                    json = """
                    {
                        "melding": "Kan ikke angre en meldekortbehandling som ikke finnes",
                        "kode":"meldekortbehandlingen_må_eksistere"
                    }
                    """.trimIndent(),
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
            }
        }
    }

    @Test
    fun `en meldekortbehandling allerede tatt av en beslutter kan ikke angres`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
                it.beslutter shouldBe "beslutter"
            }

            angreMeldekortbehandling(
                tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                forventet = ForventetRespons.json(
                    status = 400,
                    json = """
                      {
                        "melding": "Kan ikke angre en meldekortbehandling som ikke er klar til beslutning, status er UNDER_BESLUTNING",
                        "kode": "meldekortbehandlingen_må_være_klar_til_beslutning"
                      }
                    """.trimIndent(),
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
                it.beslutter shouldBe "beslutter"
            }
        }
    }

    @Test
    fun `en beslutter kan ikke angre en meldekortbehandling sendt til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
            }

            angreMeldekortbehandling(
                tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = ObjectMother.beslutter(),
                forventet = ForventetRespons.json(
                    status = 403,
                    json = """
                        {
                        "melding":"Saksbehandler B12345 mangler rollen SAKSBEHANDLER. Saksbehandlers roller: Saksbehandlerroller(value=[BESLUTTER])",
                        "kode":"tilgang_nektet_krev_rolle"}
                    """.trimIndent(),
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
            }
        }
    }

    @Test
    fun `en saksbehandler kan ikke angre en meldekortbehandling som ikke er sendt til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            }

            angreMeldekortbehandling(
                tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                forventet = ForventetRespons.json(
                    status = 400,
                    json = """
                      {
                        "melding": "Kan ikke angre en meldekortbehandling som ikke er klar til beslutning, status er UNDER_BEHANDLING",
                        "kode": "meldekortbehandlingen_må_være_klar_til_beslutning"
                      }
                    """.trimIndent(),
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `en saksbehandler kan ikke angre en meldekortbehandling som de ikke sendte til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
            }

            angreMeldekortbehandling(
                tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "A12345"),
                forventet = ForventetRespons.json(
                    status = 403,
                    json = """
                      {
                        "melding": "Du må være saksbehandleren som er tildelt meldekortbehandling for å angre.",
                        "kode": "maa_vaere_saksbehandler_for_meldekortbehandlingen"
                      }
                    """.trimIndent(),
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
                it.saksbehandler shouldBe "Z12345"
            }
        }
    }

    @Test
    fun `en saksbehandler kan angre en meldekortbehandling sendt til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling, _) = iverksettSøknadsbehandlingOgSendMeldekortbehandlingTilBeslutning(tac)!!

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.beslutter shouldBe null
            }

            angreMeldekortbehandling(
                tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
            )!!.also { (_, angretMeldekortbehandling, _) ->
                angretMeldekortbehandling?.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
                angretMeldekortbehandling?.sendtTilBeslutning shouldBe null
                tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortId = meldekortbehandling.id)!!.also {
                    it.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
                    it.sendtTilBeslutning shouldBe null
                }
            }
        }
    }
}
