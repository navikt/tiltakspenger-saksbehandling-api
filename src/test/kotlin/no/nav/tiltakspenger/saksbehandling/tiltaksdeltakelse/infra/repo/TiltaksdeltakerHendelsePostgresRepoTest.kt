package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.TiltaksdeltakelseFraRegister
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TiltaksdeltakerHendelsePostgresRepoTest {

    /** Formatet låses her, siden ingen kode leser verdien tilbake og en endring ellers ville gått upåaktet hen. */
    @Test
    fun `nå-tilstanden lagres med eksplisitte felter`() {
        withTestApplicationContextAndPostgres { tac ->
            val verdi = lagreBehandletEndring(
                tac = tac,
                deltakelseFraOgMed = LocalDate.of(2026, 1, 1),
                deltakelseTilOgMed = LocalDate.of(2026, 3, 31),
            )

            verdi shouldEqualJson """
                {
                  "eksternDeltakelseId": "ekstern-id",
                  "gjennomføringId": "gjennomføring-id",
                  "typeNavn": "Arbeidsmarkedsopplæring",
                  "typeKode": "ARBEIDSMARKEDSOPPLAERING",
                  "rettPåTiltakspenger": true,
                  "deltakelseFraOgMed": "2026-01-01",
                  "deltakelseTilOgMed": "2026-03-31",
                  "deltakelseStatus": "Deltar",
                  "deltakelseProsent": 50.0,
                  "antallDagerPerUke": 2.5,
                  "kilde": "Komet",
                  "deltidsprosentGjennomforing": 80.0
                }
            """.trimIndent()
        }
    }

    @Test
    fun `nå-tilstand uten datoer lagres med null-datoer`() {
        withTestApplicationContextAndPostgres { tac ->
            val verdi = lagreBehandletEndring(
                tac = tac,
                deltakelseFraOgMed = null,
                deltakelseTilOgMed = null,
            )

            verdi shouldEqualJson """
                {
                  "eksternDeltakelseId": "ekstern-id",
                  "gjennomføringId": "gjennomføring-id",
                  "typeNavn": "Arbeidsmarkedsopplæring",
                  "typeKode": "ARBEIDSMARKEDSOPPLAERING",
                  "rettPåTiltakspenger": true,
                  "deltakelseFraOgMed": null,
                  "deltakelseTilOgMed": null,
                  "deltakelseStatus": "Deltar",
                  "deltakelseProsent": 50.0,
                  "antallDagerPerUke": 2.5,
                  "kilde": "Komet",
                  "deltidsprosentGjennomforing": 80.0
                }
            """.trimIndent()
        }
    }

    /** Lagrer en behandlet endring for en ekte sak og deltaker, og returnerer verdien slik den ble skrevet. */
    private suspend fun ApplicationTestBuilder.lagreBehandletEndring(
        tac: TestApplicationContextMedPostgres,
        deltakelseFraOgMed: LocalDate?,
        deltakelseTilOgMed: LocalDate?,
    ): String {
        val (sak) = iverksettSøknadsbehandling(tac = tac)
        val søknadstiltak = sak.søknader.single().tiltak.shouldNotBeNull()
        val hendelse = TiltaksdeltakerHendelse(
            id = TiltaksdeltakerHendelseId.random(),
            internDeltakerId = søknadstiltak.tiltaksdeltakerId,
            eksternDeltakerId = søknadstiltak.id,
            sakId = sak.id,
        )

        tac.sessionFactory.withTransactionContext { tx ->
            tac.tiltaksdeltakerHendelseRepo.lagreBehandletEndring(
                tiltaksdeltakerHendelse = hendelse,
                nåtilstand = TiltaksdeltakelseFraRegister(
                    eksternDeltakelseId = "ekstern-id",
                    gjennomføringId = "gjennomføring-id",
                    typeNavn = "Arbeidsmarkedsopplæring",
                    typeKode = TiltakstypeSomGirRettDTO.ARBEIDSMARKEDSOPPLAERING,
                    rettPåTiltakspenger = true,
                    deltakelseFraOgMed = deltakelseFraOgMed,
                    deltakelseTilOgMed = deltakelseTilOgMed,
                    deltakelseStatus = TiltakDeltakerstatus.Deltar,
                    deltakelseProsent = 50f,
                    antallDagerPerUke = 2.5f,
                    kilde = Tiltakskilde.Komet,
                    deltidsprosentGjennomforing = 80.0,
                ),
                endring = null,
                behandlingId = null,
                oppgaveId = null,
                sessionContext = tx,
            )
        }

        return tac.sessionFactory.hentTiltaksdeltakerEndringer(søknadstiltak.tiltaksdeltakerId)
            .single { it.id == hendelse.id.toString() }
            .verdi.shouldNotBeNull()
    }
}
