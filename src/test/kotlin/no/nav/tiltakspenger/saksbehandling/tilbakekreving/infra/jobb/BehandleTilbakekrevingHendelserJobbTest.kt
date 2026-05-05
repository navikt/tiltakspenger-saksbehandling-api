package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortDagStatusDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdaterMeldekortdagDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseType
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@Disabled
class BehandleTilbakekrevingHendelserJobbTest {

    @Test
    fun `infobehov - behandles og svar produseres når utbetaling finnes`() {
        withTestApplicationContext { tac ->
            val (sak, _, vedtak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val (_, opphørVedtak) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtak.id,
                vedtaksperiode = vedtak.periode,
            )

            val opphørUtbetaling = opphørVedtak.utbetaling!!
            tac.tilbakekrevingProducer.produserInfoBehovVedFeilutbetaling(opphørUtbetaling)

            val hendelse = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().single()

            hendelse.shouldBeInstanceOf<TilbakekrevingInfoBehovHendelse>()
            hendelse.hendelsestype shouldBe TilbakekrevinghendelseType.InfoBehov
            hendelse.eksternFagsakId shouldBe sak.saksnummer.verdi
            hendelse.kravgrunnlagReferanse shouldBe opphørUtbetaling.id.uuidPart()

            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // InfoBehov er behandlet, og en ny BehandlingEndret-hendelse er generert som svar
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
            behandletHendelse.feil shouldBe TilbakekrevinghendelseFeil.FantIkkeUtbetaling
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
            behandletHendelse.feil shouldBe TilbakekrevinghendelseFeil.FantIkkeSak
        }
    }

    @Test
    fun `behandlingendret - oppdaterer eksisterende tilbakekrevingbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, vedtak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val (sakMedOpphør, opphørVedtak) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtak.id,
                vedtaksperiode = vedtak.periode,
            )

            // Trigg infobehov + behandling-endret flyten via fake-produseren slik at en
            // TilbakekrevingBehandling med status OPPRETTET opprettes.
            tac.tilbakekrevingProducer.produserInfoBehovVedFeilutbetaling(opphørVedtak.utbetaling!!)
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            val førsteBehandling = tac.tilbakekrevingBehandlingRepo.hentForSakId(sakMedOpphør.id).single()
            førsteBehandling.status shouldBe TilbakekrevingBehandlingsstatus.OPPRETTET

            // Send en oppdatering med samme tilbakeBehandlingId men ny status og beløp.
            // Vi bygger denne som rå JSON fordi fake-produseren alltid genererer en ny
            // tilbakeBehandlingId, og vi vil verifisere at en eksisterende behandling oppdateres.
            val tilbakeBehandlingId = førsteBehandling.tilbakeBehandlingId
            val eksternBehandlingId = opphørVedtak.behandlingId

            @Language("JSON")
            val oppdateringJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sakMedOpphør.saksnummer.verdi}",
                    "hendelseOpprettet": "${nå(tac.clock)}",
                    "eksternBehandlingId": "$eksternBehandlingId",
                    "tilbakekreving": {
                        "behandlingId": "$tilbakeBehandlingId",
                        "sakOpprettet": "${nå(tac.clock)}",
                        "varselSendt": "${LocalDate.now(tac.clock)}",
                        "behandlingsstatus": "TIL_BEHANDLING",
                        "forrigeBehandlingsstatus": "OPPRETTET",
                        "totaltFeilutbetaltBeløp": 1200.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/oppdatert",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sakMedOpphør.fnr.verdi, oppdateringJson)
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            val behandlinger = tac.tilbakekrevingBehandlingRepo.hentForSakId(sakMedOpphør.id)
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
            behandletHendelse.feil shouldBe TilbakekrevinghendelseFeil.FantIkkeUtbetaling
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
            behandletHendelse.feil shouldBe TilbakekrevinghendelseFeil.FantIkkeSak
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
            val (sak, _, vedtak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val (sakMedOpphør, opphørVedtak) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtak.id,
                vedtaksperiode = vedtak.periode,
            )

            tac.tilbakekrevingProducer.produserInfoBehovVedFeilutbetaling(opphørVedtak.utbetaling!!)
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            val behandlingFørUtdatertHendelse = tac.tilbakekrevingBehandlingRepo.hentForSakId(sakMedOpphør.id).single()
            val opprinneligStatus = behandlingFørUtdatertHendelse.status
            val opprinneligBeløp = behandlingFørUtdatertHendelse.totaltFeilutbetaltBeløp
            val tilbakeBehandlingId = behandlingFørUtdatertHendelse.tilbakeBehandlingId
            val eksternBehandlingId = opphørVedtak.behandlingId
            val utdatertTimestamp = behandlingFørUtdatertHendelse.sistEndret.minusMinutes(10)

            // Send en utdatert hendelse (eldre timestamp) med samme tilbakeBehandlingId
            @Language("JSON")
            val utdatertHendelseJson = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${sakMedOpphør.saksnummer.verdi}",
                    "hendelseOpprettet": "$utdatertTimestamp",
                    "eksternBehandlingId": "$eksternBehandlingId",
                    "tilbakekreving": {
                        "behandlingId": "$tilbakeBehandlingId",
                        "sakOpprettet": "$utdatertTimestamp",
                        "varselSendt": null,
                        "behandlingsstatus": "TIL_BEHANDLING",
                        "forrigeBehandlingsstatus": "OPPRETTET",
                        "totaltFeilutbetaltBeløp": 1.00,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/utdatert",
                        "fullstendigPeriode": {
                            "fom": "${LocalDate.now(tac.clock).minusMonths(1)}",
                            "tom": "${LocalDate.now(tac.clock)}"
                        }
                    }
                }
            """.trimIndent()

            tac.tilbakekrevingConsumer.consume(sakMedOpphør.fnr.verdi, utdatertHendelseJson)
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Hendelsen skal være behandlet, men behandlingen skal ikke være oppdatert
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            val behandlingEtterUtdatertHendelse =
                tac.tilbakekrevingBehandlingRepo.hentForSakId(sakMedOpphør.id).single()
            behandlingEtterUtdatertHendelse.status shouldBe opprinneligStatus
            behandlingEtterUtdatertHendelse.totaltFeilutbetaltBeløp shouldBe opprinneligBeløp
        }
    }

    @Test
    fun `behandlingendret - oppretter tilbakekrevingbehandling for omgjøring til opphør`() {
        withTestApplicationContext { tac ->
            val (sak, _, vedtak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            // Lag en stans-revurdering på saken og bruk dens rammebehandlingId for hendelsen
            val (sakMedOpphør, opphørVedtak) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtak.id,
                vedtaksperiode = vedtak.periode,
            )

            tac.tilbakekrevingProducer.produserInfoBehovVedFeilutbetaling(opphørVedtak.utbetaling!!)

            // Håndterer info-behov hendelse som følge av feilutbetaling
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Håndterer behandling endret hendelse som følge av info-svar
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            val forventetUtbetaling =
                sakMedOpphør.utbetalinger.hentUtbetalingForRammebehandling(opphørVedtak.behandlingId)!!

            val tilbakekrevingBehandlinger = tac.tilbakekrevingBehandlingRepo.hentForSakId(sakMedOpphør.id)
            tilbakekrevingBehandlinger.size shouldBe 1

            val tilbakekrevingBehandling = tilbakekrevingBehandlinger.first()
            tilbakekrevingBehandling.sakId shouldBe sakMedOpphør.id
            tilbakekrevingBehandling.utbetalingId shouldBe forventetUtbetaling.id
        }
    }

    @Test
    fun `behandlingendret - oppretter tilbakekrevingbehandling for korrigert meldekort med feilutbetaling`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val (sakMedKorrigering, korrigeringVedtak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = meldekortbehandling.kjedeId,
                meldeperioder = listOf(
                    OppdatertMeldeperiodeDTO(
                        kjedeId = meldekortbehandling.kjedeId.verdi,
                        dager = meldekortbehandling.dager.map {
                            OppdaterMeldekortdagDTO(
                                dato = it.dato,
                                status = if (it.status === MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                                    MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
                                } else {
                                    MeldekortDagStatusDTO.IKKE_TILTAKSDAG
                                },
                            )
                        },
                    ),
                ),
            )!!

            tac.tilbakekrevingProducer.produserInfoBehovVedFeilutbetaling(korrigeringVedtak.utbetaling)

            // Håndterer info-behov hendelse som følge av feilutbetaling
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Håndterer behandling endret hendelse som følge av info-svar
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            val forventetUtbetaling =
                sakMedKorrigering.utbetalinger.hentUtbetalingForMeldekort(korrigeringVedtak.meldekortId)!!

            val tilbakekrevingBehandlinger = tac.tilbakekrevingBehandlingRepo.hentForSakId(sakMedKorrigering.id)
            tilbakekrevingBehandlinger.size shouldBe 1

            val tilbakekrevingBehandling = tilbakekrevingBehandlinger.first()
            tilbakekrevingBehandling.sakId shouldBe sakMedKorrigering.id
            tilbakekrevingBehandling.utbetalingId shouldBe forventetUtbetaling.id
        }
    }

    @Test
    fun `postgres - full flyt fra InfoBehov til TilbakekrevingBehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, vedtak) = iverksettSøknadsbehandlingOgMeldekortbehandling(tac = tac)!!

            val (sakMedOpphør, opphørVedtak) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtak.id,
                vedtaksperiode = vedtak.periode,
            )

            // Trigg InfoBehov via fake-produseren (som følge av feilutbetaling i opphør-utbetalingen)
            tac.tilbakekrevingProducer.produserInfoBehovVedFeilutbetaling(opphørVedtak.utbetaling!!)

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
            tac.tilbakekrevingBehandlingRepo.hentForSakId(sakMedOpphør.id).size shouldBe 0

            // Andre kjøring av jobben - håndterer BehandlingEndret hendelsen
            tac.behandleTilbakekrevingHendelserJobb.håndterUbehandledeHendelser()

            // Verifiser at alle hendelser nå er behandlet
            tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser().size shouldBe 0

            // Verifiser at TilbakekrevingBehandling er opprettet i databasen
            val tilbakekrevingBehandlinger = tac.tilbakekrevingBehandlingRepo.hentForSakId(sakMedOpphør.id)
            tilbakekrevingBehandlinger.size shouldBe 1

            val tilbakekrevingBehandling = tilbakekrevingBehandlinger.first()
            tilbakekrevingBehandling.sakId shouldBe sakMedOpphør.id
            tilbakekrevingBehandling.status shouldBe TilbakekrevingBehandlingsstatus.OPPRETTET
        }
    }
}
