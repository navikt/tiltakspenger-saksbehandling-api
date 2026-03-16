package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class TilbakekrevingConsumerTest {

    @Test
    fun `infobehov - deserialiseres og persistes korrekt`() {
        withTestApplicationContext { tac ->
            val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234")
            val sak = ObjectMother.nySak(saksnummer = saksnummer)
            tac.sakContext.sakRepo.opprettSak(sak)

            val key = "test-key-infobehov"
            val value = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "${saksnummer.verdi}",
                    "hendelseOpprettet": "2024-01-15T10:30:00",
                    "kravgrunnlagReferanse": "ref-12345"
                }
            """.trimIndent()

            TilbakekrevingConsumer.consume(
                key = key,
                value = value,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                sakRepo = tac.sakContext.sakRepo,
            )

            val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            hendelser.size shouldBe 1

            val hendelse = hendelser.first() as TilbakekrevingInfoBehovHendelse
            hendelse.eksternFagsakId shouldBe saksnummer.verdi
            hendelse.kravgrunnlagReferanse shouldBe "ref-12345"
            hendelse.sakId shouldBe sak.id
        }
    }

    @Test
    fun `behandling_endret - deserialiseres og persistes korrekt`() {
        withTestApplicationContext { tac ->
            val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234")
            val sak = ObjectMother.nySak(saksnummer = saksnummer)
            tac.sakContext.sakRepo.opprettSak(sak)

            val key = "test-key-behandling-endret"
            val value = """
                {
                    "hendelsestype": "behandling_endret",
                    "versjon": 1,
                    "eksternFagsakId": "${saksnummer.verdi}",
                    "hendelseOpprettet": "2024-01-15T10:30:00",
                    "eksternBehandlingId": "ekstern-behandling-123",
                    "tilbakekreving": {
                        "behandlingId": "tilbake-behandling-456",
                        "sakOpprettet": "2024-01-10T08:00:00",
                        "varselSendt": "2024-01-12",
                        "behandlingsstatus": "OPPRETTET",
                        "forrigeBehandlingsstatus": null,
                        "totaltFeilutbetaltBeløp": 1500.50,
                        "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/123",
                        "fullstendigPeriode": {
                            "fom": "2024-01-01",
                            "tom": "2024-01-31"
                        }
                    }
                }
            """.trimIndent()

            TilbakekrevingConsumer.consume(
                key = key,
                value = value,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                sakRepo = tac.sakContext.sakRepo,
            )

            val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            hendelser.size shouldBe 1

            val hendelse = hendelser.first() as TilbakekrevingBehandlingEndretHendelse
            hendelse.eksternFagsakId shouldBe saksnummer.verdi
            hendelse.eksternBehandlingId shouldBe "ekstern-behandling-123"
            hendelse.tilbakeBehandlingId shouldBe "tilbake-behandling-456"
            hendelse.behandlingsstatus shouldBe TilbakekrevingBehandlingsstatus.OPPRETTET
            hendelse.totaltFeilutbetaltBeløp shouldBe BigDecimal("1500.50")
            hendelse.url shouldBe "https://tilbakekreving.nav.no/behandling/123"
            hendelse.sakId shouldBe sak.id
        }
    }

    @Test
    fun `infosvar - deserialiseres men persistes ikke`() {
        withTestApplicationContext { tac ->
            val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234")
            val sak = ObjectMother.nySak(saksnummer = saksnummer)
            tac.sakContext.sakRepo.opprettSak(sak)

            val key = "test-key-infosvar"
            val value = """
                {
                    "hendelsestype": "fagsysteminfo_svar",
                    "versjon": 1,
                    "eksternFagsakId": "${saksnummer.verdi}",
                    "hendelseOpprettet": "2024-01-15T10:30:00",
                    "mottaker": {
                        "type": "PERSON",
                        "ident": "12345678911"
                    },
                    "revurdering": {
                        "behandlingId": "rev-123",
                        "årsak": "KORRIGERING",
                        "årsakTilFeilutbetaling": "Feil utregning",
                        "vedtaksdato": "2024-01-10"
                    },
                    "utvidPerioder": [],
                    "behandlendeEnhet": "4100"
                }
            """.trimIndent()

            TilbakekrevingConsumer.consume(
                key = key,
                value = value,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                sakRepo = tac.sakContext.sakRepo,
            )

            // infosvar skal ikke persistes
            val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            hendelser.size shouldBe 0
        }
    }

    @Test
    fun `null value - logger warning og returnerer`() {
        withTestApplicationContext { tac ->
            // Skal ikke kaste exception, bare logge warning og returnere
            TilbakekrevingConsumer.consume(
                key = "test-key",
                value = null,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                sakRepo = tac.sakContext.sakRepo,
            )

            val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            hendelser.size shouldBe 0
        }
    }

    @Test
    fun `ukjent hendelsestype - kaster exception`() {
        withTestApplicationContext { tac ->
            val key = "test-key"
            val value = """
                {
                    "hendelsestype": "ugyldig_hendelsestype",
                    "versjon": 1,
                    "eksternFagsakId": "202401011234",
                    "hendelseOpprettet": "2024-01-15T10:30:00"
                }
            """.trimIndent()

            assertThrows<Exception> {
                TilbakekrevingConsumer.consume(
                    key = key,
                    value = value,
                    tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                    sakRepo = tac.sakContext.sakRepo,
                )
            }

            val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            hendelser.size shouldBe 0
        }
    }

    @Test
    fun `manglende påkrevd felt - kaster exception`() {
        withTestApplicationContext { tac ->
            val key = "test-key"
            // mangler kravgrunnlagReferanse for infobehov
            val value = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "202401011234",
                    "hendelseOpprettet": "2024-01-15T10:30:00"
                }
            """.trimIndent()

            assertThrows<Exception> {
                TilbakekrevingConsumer.consume(
                    key = key,
                    value = value,
                    tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                    sakRepo = tac.sakContext.sakRepo,
                )
            }

            val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            hendelser.size shouldBe 0
        }
    }

    @Test
    fun `duplikat infobehov med samme kravgrunnlagReferanse - persistes ikke`() {
        withTestApplicationContext { tac ->
            val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234")
            val sak = ObjectMother.nySak(saksnummer = saksnummer)
            tac.sakContext.sakRepo.opprettSak(sak)

            val value = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "${saksnummer.verdi}",
                    "hendelseOpprettet": "2024-01-15T10:30:00",
                    "kravgrunnlagReferanse": "samme-ref"
                }
            """.trimIndent()

            // Første kall - skal persiste
            TilbakekrevingConsumer.consume(
                key = "key-1",
                value = value,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                sakRepo = tac.sakContext.sakRepo,
            )

            // Andre kall med samme kravgrunnlagReferanse - skal ikke persiste
            TilbakekrevingConsumer.consume(
                key = "key-2",
                value = value,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                sakRepo = tac.sakContext.sakRepo,
            )

            val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            hendelser.size shouldBe 1
        }
    }

    @Test
    fun `ukjent saksnummer - sakId settes til null`() {
        withTestApplicationContext { tac ->
            val ukjentSaksnummer = "202401019999"

            val key = "test-key"
            val value = """
                {
                    "hendelsestype": "fagsysteminfo_behov",
                    "versjon": 1,
                    "eksternFagsakId": "$ukjentSaksnummer",
                    "hendelseOpprettet": "2024-01-15T10:30:00",
                    "kravgrunnlagReferanse": "ref-12345"
                }
            """.trimIndent()

            TilbakekrevingConsumer.consume(
                key = key,
                value = value,
                tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                sakRepo = tac.sakContext.sakRepo,
            )

            val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
            hendelser.size shouldBe 1

            val hendelse = hendelser.first() as TilbakekrevingInfoBehovHendelse
            hendelse.sakId shouldBe null
        }
    }

    @Test
    fun `behandling_endret med alle statuser - deserialiseres korrekt`() {
        withTestApplicationContext { tac ->
            val saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234")
            val sak = ObjectMother.nySak(saksnummer = saksnummer)
            tac.sakContext.sakRepo.opprettSak(sak)

            val statuser = listOf(
                "OPPRETTET" to TilbakekrevingBehandlingsstatus.OPPRETTET,
                "TIL_BEHANDLING" to TilbakekrevingBehandlingsstatus.TIL_BEHANDLING,
                "TIL_GODKJENNING" to TilbakekrevingBehandlingsstatus.TIL_GODKJENNING,
                "AVSLUTTET" to TilbakekrevingBehandlingsstatus.AVSLUTTET,
            )

            statuser.forEachIndexed { index, (statusJson, expectedStatus) ->
                val key = "test-key-status-$index"
                val value = """
                    {
                        "hendelsestype": "behandling_endret",
                        "versjon": 1,
                        "eksternFagsakId": "${saksnummer.verdi}",
                        "hendelseOpprettet": "2024-01-15T10:30:0$index",
                        "eksternBehandlingId": "ekstern-$index",
                        "tilbakekreving": {
                            "behandlingId": "tilbake-$index",
                            "sakOpprettet": "2024-01-10T08:00:00",
                            "varselSendt": null,
                            "behandlingsstatus": "$statusJson",
                            "forrigeBehandlingsstatus": null,
                            "totaltFeilutbetaltBeløp": 1000,
                            "saksbehandlingURL": "https://tilbakekreving.nav.no/behandling/$index",
                            "fullstendigPeriode": {
                                "fom": "2024-01-01",
                                "tom": "2024-01-31"
                            }
                        }
                    }
                """.trimIndent()

                TilbakekrevingConsumer.consume(
                    key = key,
                    value = value,
                    tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
                    sakRepo = tac.sakContext.sakRepo,
                )

                val hendelser = tac.tilbakekrevingHendelseRepo.hentUbehandledeHendelser()
                    .filterIsInstance<TilbakekrevingBehandlingEndretHendelse>()
                    .filter { it.eksternBehandlingId == "ekstern-$index" }

                hendelser.size shouldBe 1
                hendelser.first().behandlingsstatus shouldBe expectedStatus
            }
        }
    }
}
