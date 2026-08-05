package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

import arrow.core.right
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.oppdaterOversendtKlageinstansFeilet
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Pinner rutingen i [genererBrev]: hvilke tilstander som gir forhåndsvisning (null saksbehandler mulig) og hvilke som gir endelig brev.
 */
class KlagebehandlingBrevExTest {

    private val respons = PdfOgJson(PdfA("pdf".toByteArray()), "{}").right()

    @Test
    fun `klar til behandling gir forhåndsvisning med null saksbehandler`() {
        runTest {
            val behandling = ObjectMother.opprettKlagebehandling(erKlagenSignert = false)
                .copy(saksbehandler = null, status = KLAR_TIL_BEHANDLING)

            var fangetNavIdent: String? = "ikke kalt"
            var fangetForhåndsvisning: Boolean? = null
            behandling.genererBrev(
                kommando = KlagebehandlingBrevKommando(
                    sakId = behandling.sakId,
                    klagebehandlingId = behandling.id,
                    saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
                    correlationId = CorrelationId.generate(),
                    brevtekster = Brevtekster.empty,
                ),
                genererAvvisningsbrev = { _, _, navIdent, _, forhåndsvisning ->
                    fangetNavIdent = navIdent
                    fangetForhåndsvisning = forhåndsvisning
                    respons
                },
                genererKlageInnstillingsbrev = { _, _, _, _, _, _, _ -> error("Avvist klage skal ikke generere innstillingsbrev") },
                vedtaksdato = null,
            ) shouldBe respons

            fangetNavIdent shouldBe null
            fangetForhåndsvisning shouldBe true
        }
    }

    @Test
    fun `oversendelse feilet gir endelig innstillingsbrev`() {
        runTest {
            val behandling = ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse()
                .oppdaterOversendtKlageinstansFeilet(nå(fixedClock))

            var fangetNavIdent: String? = null
            var fangetForhåndsvisning: Boolean? = null
            behandling.genererBrev(
                kommando = KlagebehandlingBrevKommando(
                    sakId = behandling.sakId,
                    klagebehandlingId = behandling.id,
                    saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
                    correlationId = CorrelationId.generate(),
                    brevtekster = Brevtekster.empty,
                ),
                genererAvvisningsbrev = { _, _, _, _, _ -> error("Opprettholdt klage skal ikke generere avvisningsbrev") },
                genererKlageInnstillingsbrev = { _, _, navIdent, _, forhåndsvisning, _, _ ->
                    fangetNavIdent = navIdent
                    fangetForhåndsvisning = forhåndsvisning
                    respons
                },
                vedtaksdato = LocalDate.now(fixedClock),
            ) shouldBe respons

            fangetNavIdent shouldBe behandling.saksbehandler
            fangetForhåndsvisning shouldBe false
        }
    }
}
