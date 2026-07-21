package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import tools.jackson.databind.JsonNode
import java.time.LocalDate

/**
 * Henter rammebehandlingen med [id] fra `behandlinger`-arrayen i sakens respons-JSON.
 * Lar tester asserte på den relevante behandlingen uten å sammenligne hele saken.
 */
fun SakDTOJson.rammebehandlingJson(id: RammebehandlingId): JsonNode =
    get("behandlinger").single { behandling -> behandling.get("id").asString() == id.toString() }

/**
 * Henter klagebehandlingen med [id] fra `klageBehandlinger`-arrayen i sakens respons-JSON.
 * Lar tester asserte på den relevante klagebehandlingen uten å sammenligne hele saken.
 */
fun SakDTOJson.klagebehandlingJson(id: KlagebehandlingId): JsonNode =
    get("klageBehandlinger").single { behandling -> behandling.get("id").asString() == id.toString() }

/**
 * Asserter siste hendelse i behandlingens `ventestatus`-array felt for felt; `tidspunkt` ignoreres bevisst.
 * [forventetAntallHendelser] asserter i tillegg lengden på arrayen når den er satt.
 */
fun JsonNode.shouldHaSisteVentestatus(
    sattPåVentAv: String,
    begrunnelse: String,
    status: String,
    frist: LocalDate?,
    erSattPåVent: Boolean = true,
    forventetAntallHendelser: Int? = null,
): JsonNode {
    val ventestatus = get("ventestatus")
    if (forventetAntallHendelser != null) {
        withClue("antall hendelser i ventestatus") { ventestatus.size() shouldBe forventetAntallHendelser }
    }
    val hendelse = ventestatus.last()
    withClue("sattPåVentAv i siste ventestatus-hendelse") { hendelse.get("sattPåVentAv").asString() shouldBe sattPåVentAv }
    withClue("begrunnelse i siste ventestatus-hendelse") { hendelse.get("begrunnelse").asString() shouldBe begrunnelse }
    withClue("erSattPåVent i siste ventestatus-hendelse") { hendelse.get("erSattPåVent").asBoolean() shouldBe erSattPåVent }
    withClue("status i siste ventestatus-hendelse") { hendelse.get("status").asString() shouldBe status }
    withClue("frist i siste ventestatus-hendelse") {
        if (frist == null) {
            hendelse.get("frist").isNull shouldBe true
        } else {
            hendelse.get("frist").asString() shouldBe frist.toString()
        }
    }
    return this
}
