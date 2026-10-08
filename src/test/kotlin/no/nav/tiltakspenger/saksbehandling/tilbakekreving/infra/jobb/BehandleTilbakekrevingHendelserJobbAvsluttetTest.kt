package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tildelTilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.konsumerTilbakekrevingshendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Verifiserer at en behandling_endret-hendelse med status AVSLUTTET avslutter tilbakekrevingbehandlingen uansett hvilken status den har fra før.
 * Vi parameteriserer over den interne statusen, slik at også tildelte behandlinger (UNDER_*) dekkes.
 */
class BehandleTilbakekrevingHendelserJobbAvsluttetTest {

    @ParameterizedTest
    @EnumSource(
        value = TilbakekrevingBehandlingsstatusIntern::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["AVSLUTTET"],
    )
    fun `behandlingendret - avslutter tilbakekrevingbehandling fra status`(
        fraStatus: TilbakekrevingBehandlingsstatusIntern,
    ) {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!
            val eksternBehandlingId = sak.utbetalinger.first().id.uuidPart()
            val tilbakeBehandlingId = "tilbake-behandling-avsluttes-fra-$fraStatus"

            fun sendStatus(
                status: TilbakekrevingBehandlingsstatus,
                forrigeStatus: TilbakekrevingBehandlingsstatus?,
                hendelseOpprettet: LocalDateTime,
            ): TilbakekrevinghendelseId = sendBehandlingEndret(
                tac = tac,
                sak = sak,
                eksternBehandlingId = eksternBehandlingId,
                tilbakeBehandlingId = tilbakeBehandlingId,
                status = status,
                forrigeStatus = forrigeStatus,
                hendelseOpprettet = hendelseOpprettet,
            )

            fun nesteStatus(
                status: TilbakekrevingBehandlingsstatus,
                forrigeStatus: TilbakekrevingBehandlingsstatus,
            ) = sendStatus(status, forrigeStatus, hentBehandling(tac, sak).sistEndret.plusSeconds(10))

            suspend fun tildel(saksbehandler: Saksbehandler) {
                tildelTilbakekrevingBehandling(
                    tac = tac,
                    sakId = sak.id,
                    tilbakekrevingId = hentBehandling(tac, sak).id,
                    saksbehandler = saksbehandler,
                )!!
            }

            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTar")
            val beslutter = ObjectMother.beslutter("beslutterSomTar")

            sendStatus(TilbakekrevingBehandlingsstatus.OPPRETTET, null, nå(tac.clock))

            when (fraStatus) {
                TilbakekrevingBehandlingsstatusIntern.OPPRETTET -> Unit

                TilbakekrevingBehandlingsstatusIntern.TIL_FORHÅNDSVARSEL -> {
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL, TilbakekrevingBehandlingsstatus.OPPRETTET)
                }

                TilbakekrevingBehandlingsstatusIntern.UNDER_FORHÅNDSVARSLING -> {
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL, TilbakekrevingBehandlingsstatus.OPPRETTET)
                    tildel(saksbehandler)
                }

                TilbakekrevingBehandlingsstatusIntern.TIL_BEHANDLING -> {
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL, TilbakekrevingBehandlingsstatus.OPPRETTET)
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_BEHANDLING, TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL)
                }

                TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING -> {
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL, TilbakekrevingBehandlingsstatus.OPPRETTET)
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_BEHANDLING, TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL)
                    tildel(saksbehandler)
                }

                TilbakekrevingBehandlingsstatusIntern.TIL_GODKJENNING -> {
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL, TilbakekrevingBehandlingsstatus.OPPRETTET)
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_BEHANDLING, TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL)
                    tildel(saksbehandler)
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_GODKJENNING, TilbakekrevingBehandlingsstatus.TIL_BEHANDLING)
                }

                TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING -> {
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL, TilbakekrevingBehandlingsstatus.OPPRETTET)
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_BEHANDLING, TilbakekrevingBehandlingsstatus.TIL_FORHÅNDSVARSEL)
                    tildel(saksbehandler)
                    nesteStatus(TilbakekrevingBehandlingsstatus.TIL_GODKJENNING, TilbakekrevingBehandlingsstatus.TIL_BEHANDLING)
                    tildel(beslutter)
                }

                TilbakekrevingBehandlingsstatusIntern.AVSLUTTET -> error("AVSLUTTET er ekskludert fra testen")
            }

            val behandlingFørAvslutning = hentBehandling(tac, sak)
            behandlingFørAvslutning.statusIntern shouldBe fraStatus

            val avsluttetTidspunkt = behandlingFørAvslutning.sistEndret.plusMinutes(1)
            val avsluttetHendelseId = sendStatus(
                status = TilbakekrevingBehandlingsstatus.AVSLUTTET,
                forrigeStatus = behandlingFørAvslutning.status,
                hendelseOpprettet = avsluttetTidspunkt,
            )

            val avsluttetBehandling = hentBehandling(tac, sak)
            avsluttetBehandling.id shouldBe behandlingFørAvslutning.id
            avsluttetBehandling.status shouldBe TilbakekrevingBehandlingsstatus.AVSLUTTET
            avsluttetBehandling.statusIntern shouldBe TilbakekrevingBehandlingsstatusIntern.AVSLUTTET
            avsluttetBehandling.sistEndret shouldBe avsluttetTidspunkt

            val avsluttetHendelse = tac.tilbakekrevingHendelseRepo.hentHendelse(avsluttetHendelseId)
            avsluttetHendelse.shouldBeInstanceOf<TilbakekrevingBehandlingEndretHendelse>()
            avsluttetHendelse.behandlet.shouldNotBeNull()
            avsluttetHendelse.feil shouldBe null
        }
    }

    private fun hentBehandling(tac: TestApplicationContext, sak: Sak): TilbakekrevingBehandling =
        tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).single()

    /**
     * Konsumerer en behandling_endret-hendelse og kjører jobben for akkurat denne hendelsen.
     * Jobbens kø-spørring går på tvers av saker, så vi kjører kun vår egen hendelse.
     */
    private fun sendBehandlingEndret(
        tac: TestApplicationContext,
        sak: Sak,
        eksternBehandlingId: String,
        tilbakeBehandlingId: String,
        status: TilbakekrevingBehandlingsstatus,
        forrigeStatus: TilbakekrevingBehandlingsstatus?,
        hendelseOpprettet: LocalDateTime,
    ): TilbakekrevinghendelseId {
        val varselSendt = if (status == TilbakekrevingBehandlingsstatus.OPPRETTET) "null" else "\"${LocalDate.now(tac.clock)}\""
        val forrigeBehandlingsstatus = forrigeStatus?.let { "\"$it\"" } ?: "null"

        @Language("JSON")
        val hendelseJson = """
            {
                "hendelsestype": "behandling_endret",
                "versjon": 1,
                "eksternFagsakId": "${sak.saksnummer.verdi}",
                "hendelseOpprettet": "$hendelseOpprettet",
                "eksternBehandlingId": "$eksternBehandlingId",
                "tilbakekreving": {
                    "behandlingId": "$tilbakeBehandlingId",
                    "sakOpprettet": "${LocalDate.now(tac.clock).atStartOfDay()}",
                    "varselSendt": $varselSendt,
                    "behandlingsstatus": "$status",
                    "forrigeBehandlingsstatus": $forrigeBehandlingsstatus,
                    "totaltFeilutbetaltBeløp": 1000.00,
                    "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/$tilbakeBehandlingId",
                    "fullstendigPeriode": {
                        "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                        "tom": "${LocalDate.now(tac.clock)}"
                    }
                }
            }
        """.trimIndent()

        val hendelseId = konsumerTilbakekrevingshendelse(
            key = sak.fnr.verdi,
            value = hendelseJson,
            tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
            clock = tac.clock,
        ).shouldNotBeNull()

        tac.behandleTilbakekrevingHendelserJobb.håndterHendelse(hendelseId)

        return hendelseId
    }
}
