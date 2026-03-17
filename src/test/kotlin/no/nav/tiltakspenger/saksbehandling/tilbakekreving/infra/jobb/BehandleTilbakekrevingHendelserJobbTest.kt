package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseType
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingConsumer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BehandleTilbakekrevingHendelserJobbTest {

    @Test
    fun `infobehov - behandles og svar produseres når utbetaling finnes`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val utbetaling = sak.utbetalinger.first()
            val kravgrunnlagReferanse = utbetaling.id.uuidPart()

            @Language("JSON")
            val hendelseJson = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "kravgrunnlagReferanse": "$kravgrunnlagReferanse"
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, hendelseJson)

            val hendelse = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().single()

            hendelse.shouldBeInstanceOf<TilbakekrevingInfoBehovHendelse>()
            hendelse.hendelsestype shouldBe TilbakekrevinghendelseType.InfoBehov
            hendelse.eksternFagsakId shouldBe sak.saksnummer.verdi
            hendelse.kravgrunnlagReferanse shouldBe kravgrunnlagReferanse

            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Hendelsen skal være behandlet (ikke lenger i ubehandlede)
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
                .single().hendelsestype shouldBe TilbakekrevinghendelseType.BehandlingEndret
        }
    }

    @Test
    fun `infobehov - markeres med feil når utbetaling ikke finnes`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            @Language("JSON")
            val hendelseJson = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "kravgrunnlagReferanse": "finnes-ikke-uuid"
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, hendelseJson)

            val hendelseId = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().single().id

            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Hendelsen skal være behandlet (markert med feil)
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            // Verifiser at feil-feltet er satt
            val behandletHendelse = tac.tilbakekrevingHendelseRepo.hentHendelse(hendelseId)
            behandletHendelse.shouldBeInstanceOf<TilbakekrevingInfoBehovHendelse>()
            behandletHendelse.feil.shouldNotBeNull()
        }
    }

    @Test
    fun `infobehov - markeres med feil når sakId er null`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            // Bruk et saksnummer som ikke finnes i databasen (gyldig format men ikke eksisterende)
            @Language("JSON")
            val hendelseJson = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "202501011001",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "kravgrunnlagReferanse": "ref-123"
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, hendelseJson)

            val hendelseId = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().single().id

            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Hendelsen skal være behandlet (markert med feil)
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            // Verifiser at feil-feltet er satt
            val behandletHendelse = tac.tilbakekrevingHendelseRepo.hentHendelse(hendelseId)
            behandletHendelse.shouldBeInstanceOf<TilbakekrevingInfoBehovHendelse>()
            behandletHendelse.feil.shouldNotBeNull()
        }
    }

    @Test
    fun `behandlingendret - oppretter ny tilbakekrevingbehandling når utbetaling finnes`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val utbetaling = sak.utbetalinger.first()
            val eksternBehandlingId = utbetaling.id.uuidPart()

            @Language("JSON")
            val hendelseJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "eksternBehandlingId": "$eksternBehandlingId",
                    "tilbakekreving": {
                        "behandlingId": "tilbake-behandling-123",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": "${LocalDate.now(tac.clock)}",
                        "behandlingsstatus": "OPPRETTET",
                        "forrigeBehandlingsstatus": null,
                        "totaltFeilutbetaltBeløp": 1500.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/123",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, hendelseJson)

            tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).size shouldBe 0

            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Hendelsen skal være behandlet
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            // TilbakekrevingBehandling skal være opprettet
            val tilbakekrevingBehandlinger = tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id)
            tilbakekrevingBehandlinger.size shouldBe 1

            val tilbakekrevingBehandling = tilbakekrevingBehandlinger.first()
            tilbakekrevingBehandling.sakId shouldBe sak.id
            tilbakekrevingBehandling.utbetalingId shouldBe utbetaling.id
            tilbakekrevingBehandling.tilbakeBehandlingId shouldBe "tilbake-behandling-123"
            tilbakekrevingBehandling.status shouldBe TilbakekrevingBehandlingsstatus.OPPRETTET
            tilbakekrevingBehandling.totaltFeilutbetaltBeløp shouldBe BigDecimal("1500.00")
        }
    }

    @Test
    fun `behandlingendret - oppdaterer eksisterende tilbakekrevingbehandling`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val utbetaling = sak.utbetalinger.first()
            val eksternBehandlingId = utbetaling.id.uuidPart()
            val tilbakeBehandlingId = "tilbake-behandling-456"

            // Først opprett en behandling
            @Language("JSON")
            val førsteHendelseJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "eksternBehandlingId": "$eksternBehandlingId",
                    "tilbakekreving": {
                        "behandlingId": "$tilbakeBehandlingId",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": null,
                        "behandlingsstatus": "OPPRETTET",
                        "forrigeBehandlingsstatus": null,
                        "totaltFeilutbetaltBeløp": 1000.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/456",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, førsteHendelseJson)
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            val førsteBehandling = tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).first()
            førsteBehandling.status shouldBe TilbakekrevingBehandlingsstatus.OPPRETTET

            // Så oppdater med ny hendelse
            @Language("JSON")
            val andreHendelseJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock).plusSeconds(10)}",
                    "eksternBehandlingId": "$eksternBehandlingId",
                    "tilbakekreving": {
                        "behandlingId": "$tilbakeBehandlingId",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": "${LocalDate.now(tac.clock)}",
                        "behandlingsstatus": "TIL_BEHANDLING",
                        "forrigeBehandlingsstatus": "OPPRETTET",
                        "totaltFeilutbetaltBeløp": 1200.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/456",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, andreHendelseJson)
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Skal fortsatt bare være én behandling (oppdatert)
            val behandlinger = tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id)
            behandlinger.size shouldBe 1

            val oppdatertBehandling = behandlinger.first()
            oppdatertBehandling.id shouldBe førsteBehandling.id
            oppdatertBehandling.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            oppdatertBehandling.totaltFeilutbetaltBeløp shouldBe BigDecimal("1200.00")
            oppdatertBehandling.varselSendt.shouldNotBeNull()
        }
    }

    @Test
    fun `behandlingendret - markeres med feil når utbetaling ikke finnes`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            @Language("JSON")
            val hendelseJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "eksternBehandlingId": "finnes-ikke-uuid",
                    "tilbakekreving": {
                        "behandlingId": "tilbake-behandling-789",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": null,
                        "behandlingsstatus": "OPPRETTET",
                        "forrigeBehandlingsstatus": null,
                        "totaltFeilutbetaltBeløp": 500.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/789",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, hendelseJson)

            val hendelse = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().first()

            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Hendelsen skal være behandlet (markert med feil)
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            // Ingen tilbakekrevingbehandling skal være opprettet
            tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).size shouldBe 0

            // Verifiser at feil-feltet er satt
            val behandletHendelse = tac.tilbakekrevingHendelseRepo.hentHendelse(hendelse.id)
            behandletHendelse.shouldBeInstanceOf<TilbakekrevingBehandlingEndretHendelse>()
            behandletHendelse.feil.shouldNotBeNull()
        }
    }

    @Test
    fun `behandlingendret - markeres med feil når sakId er null`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            // Bruk et saksnummer som ikke finnes i databasen (gyldig format men ikke eksisterende)
            @Language("JSON")
            val hendelseJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "202501011001",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "eksternBehandlingId": "ekstern-id",
                    "tilbakekreving": {
                        "behandlingId": "tilbake-behandling-999",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": null,
                        "behandlingsstatus": "OPPRETTET",
                        "forrigeBehandlingsstatus": null,
                        "totaltFeilutbetaltBeløp": 750.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/999",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, hendelseJson)

            val hendelse = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().first()

            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Hendelsen skal være behandlet (markert med feil)
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            // Verifiser at feil-feltet er satt
            val behandletHendelse = tac.tilbakekrevingHendelseRepo.hentHendelse(hendelse.id)
            behandletHendelse.shouldBeInstanceOf<TilbakekrevingBehandlingEndretHendelse>()
            behandletHendelse.feil.shouldNotBeNull()
        }
    }

    @Test
    fun `flere hendelser - behandler alle ubehandlede hendelser`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val utbetaling = sak.utbetalinger.first()
            val kravgrunnlagReferanse = utbetaling.id.uuidPart()

            // Opprett en infobehov-hendelse
            @Language("JSON")
            val infoBehovJson = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "kravgrunnlagReferanse": "$kravgrunnlagReferanse"
                }
            """.trimIndent()

            // Opprett en behandlingendret-hendelse
            @Language("JSON")
            val behandlingEndretJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "eksternBehandlingId": "$kravgrunnlagReferanse",
                    "tilbakekreving": {
                        "behandlingId": "tilbake-multi-123",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": null,
                        "behandlingsstatus": "OPPRETTET",
                        "forrigeBehandlingsstatus": null,
                        "totaltFeilutbetaltBeløp": 2000.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/multi",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, infoBehovJson)
            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, behandlingEndretJson)

            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 2

            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Begge hendelsene skal være behandlet, men infobehov genererer en ny BehandlingEndret hendelse
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
                .single().hendelsestype shouldBe TilbakekrevinghendelseType.BehandlingEndret

            // TilbakekrevingBehandling skal være opprettet fra behandlingendret-hendelsen
            tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).size shouldBe 1
        }
    }

    @Test
    fun `ingen ubehandlede hendelser - gjør ingenting`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0
            tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).size shouldBe 0

            // Skal ikke kaste feil
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0
            tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).size shouldBe 0
        }
    }

    @Test
    fun `behandlingendret - hopper over oppdatering når hendelse er utdatert`() {
        withTestApplicationContext { tac ->
            val (sak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val utbetaling = sak.utbetalinger.first()
            val eksternBehandlingId = utbetaling.id.uuidPart()
            val tilbakeBehandlingId = "tilbake-utdatert-123"

            // Opprett første hendelse med nyere timestamp
            @Language("JSON")
            val nyHendelseJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock).plusMinutes(10)}",
                    "eksternBehandlingId": "$eksternBehandlingId",
                    "tilbakekreving": {
                        "behandlingId": "$tilbakeBehandlingId",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": null,
                        "behandlingsstatus": "TIL_BEHANDLING",
                        "forrigeBehandlingsstatus": null,
                        "totaltFeilutbetaltBeløp": 1500.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/utdatert",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, nyHendelseJson)
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            val behandlingFørUtdatertHendelse = tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).first()
            behandlingFørUtdatertHendelse.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING

            // Opprett utdatert hendelse med eldre timestamp
            @Language("JSON")
            val utdatertHendelseJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "eksternBehandlingId": "$eksternBehandlingId",
                    "tilbakekreving": {
                        "behandlingId": "$tilbakeBehandlingId",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": null,
                        "behandlingsstatus": "OPPRETTET",
                        "forrigeBehandlingsstatus": null,
                        "totaltFeilutbetaltBeløp": 1000.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/utdatert",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sak.fnr.verdi, utdatertHendelseJson)
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Hendelsen skal være behandlet, men behandlingen skal ikke være oppdatert
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            val behandlingEtterUtdatertHendelse = tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).first()
            behandlingEtterUtdatertHendelse.status shouldBe TilbakekrevingBehandlingsstatus.TIL_BEHANDLING
            behandlingEtterUtdatertHendelse.totaltFeilutbetaltBeløp shouldBe BigDecimal("1500.00")
        }
    }

    @Test
    fun `postgres - full flyt fra InfoBehov til TilbakekrevingBehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _, _) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val utbetaling = sak.utbetalinger.first()
            val kravgrunnlagReferanse = utbetaling.id.uuidPart()

            // Simuler mottak av InfoBehov hendelse via Kafka consumer
            val key = "test-key-infobehov"

            @Language("JSON")
            val value = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "${sak.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "kravgrunnlagReferanse": "$kravgrunnlagReferanse"
                }
            """.trimIndent()

            TilbakekrevingConsumer.consume(
                key = key,
                value = value,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                sakRepo = tac.sakContext.sakRepo,
            )

            // Verifiser at InfoBehov hendelse er lagret og ubehandlet
            val ubehandledeFørFørsteKjøring = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            ubehandledeFørFørsteKjøring.size shouldBe 1
            ubehandledeFørFørsteKjøring.single().hendelsestype shouldBe TilbakekrevinghendelseType.InfoBehov

            // Første kjøring av jobben - håndterer InfoBehov hendelsen
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Verifiser at InfoBehov er behandlet og en ny BehandlingEndret hendelse er generert
            val ubehandledeEtterFørsteKjøring = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            ubehandledeEtterFørsteKjøring.size shouldBe 1
            ubehandledeEtterFørsteKjøring.single().hendelsestype shouldBe TilbakekrevinghendelseType.BehandlingEndret

            // Verifiser at det ennå ikke er opprettet noen TilbakekrevingBehandling
            tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id).size shouldBe 0

            // Andre kjøring av jobben - håndterer BehandlingEndret hendelsen
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Verifiser at alle hendelser nå er behandlet
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            // Verifiser at TilbakekrevingBehandling er opprettet i databasen
            val tilbakekrevingBehandlinger = tac.tilbakekrevingBehandlingRepo.hentForSakId(sak.id)
            tilbakekrevingBehandlinger.size shouldBe 1

            val tilbakekrevingBehandling = tilbakekrevingBehandlinger.first()
            tilbakekrevingBehandling.sakId shouldBe sak.id
            tilbakekrevingBehandling.status shouldBe TilbakekrevingBehandlingsstatus.OPPRETTET
        }
    }
}
