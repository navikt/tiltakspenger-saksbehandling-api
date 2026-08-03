package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.konsumerTilbakekrevingshendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

/**
 * Tester for [TilbakekrevingHendelsePostgresRepo] som ikke går på køspørringen — den har sin egen [TilbakekrevingHendelseAggregatTest].
 *
 * Kafka gir oss «minst én gang»-levering, så dedupliseringen er en del av repoets kontrakt og testes her.
 * Feilmarkeringen med sak er den andre halvdelen: jobben når den bare når saken faktisk ble funnet, og det er en annen feil enn de som slår ut tidlig.
 */
class TilbakekrevingHendelsePostgresRepoTest {

    /**
     * `on conflict (ekstern_fagsak_id, hendelse_type, opprettet) do nothing` gjør en gjentatt levering til en no-op.
     * Consumeren gir da `null` tilbake, slik at kalleren ikke starter behandling av en hendelse som allerede ligger der.
     */
    @Test
    fun `samme hendelse levert to ganger lagres bare én gang`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac = tac)

            @Language("JSON")
            val json = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "kravgrunnlagReferanse": "ref-duplikat"
                }
            """.trimIndent()

            fun konsumer() = konsumerTilbakekrevingshendelse(
                key = sak.fnr.verdi,
                value = json,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                clock = tac.clock,
            )

            konsumer().shouldNotBeNull()
            konsumer() shouldBe null
        }
    }

    /**
     * En hendelse som feiler etter at saken er funnet, lagres med både `sak_id` og `behandlet_feil`.
     * Jobbens tidlige feil ([TilbakekrevinghendelseFeil.UgyldigSaksnummer]) har ingen sak, så den varianten dekkes av aggregat-testen.
     */
    @Test
    fun `hendelse markert med feil og sak leses tilbake med begge deler`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac = tac)
            val repo = tac.tilbakekrevingHendelseRepo

            @Language("JSON")
            val json = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "kravgrunnlagReferanse": "ref-med-sak"
                }
            """.trimIndent()

            val hendelseId = konsumerTilbakekrevingshendelse(
                key = sak.fnr.verdi,
                value = json,
                tilbakekrevingHendelseRepo = repo,
                clock = tac.clock,
            )!!

            repo.markerSomBehandletMedFeil(
                hendelseId = hendelseId,
                sakId = sak.id,
                feil = TilbakekrevinghendelseFeil.FantIkkeUtbetaling,
            )

            repo.hentHendelse(hendelseId)!!.also {
                it.sakId shouldBe sak.id
                it.feil shouldBe TilbakekrevinghendelseFeil.FantIkkeUtbetaling
            }
        }
    }
}
