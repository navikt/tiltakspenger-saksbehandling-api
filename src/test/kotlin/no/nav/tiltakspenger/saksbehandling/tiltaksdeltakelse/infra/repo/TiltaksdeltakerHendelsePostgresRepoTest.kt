package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testStatusOpprettet
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
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
                  "tiltakstype": "ARBEIDSMARKEDSOPPLAERING",
                  "tiltakstypenavn": "Arbeidsmarkedsopplæring",
                  "tiltakskodeFraKilden": "ARBEIDSMARKEDSOPPLAERING",
                  "fraOgMed": "2026-01-01",
                  "tilOgMed": "2026-03-31",
                  "kildestatus": {
                    "kilde": "Komet",
                    "kodeIKontrakten": "DELTAR",
                    "årsak": null,
                    "opprettet": "$testStatusOpprettet"
                  },
                  "deltakelsesprosent": 50.0,
                  "dagerPerUke": 2.5,
                  "deltidsprosentPåGjennomføring": 80.0
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
                  "tiltakstype": "ARBEIDSMARKEDSOPPLAERING",
                  "tiltakstypenavn": "Arbeidsmarkedsopplæring",
                  "tiltakskodeFraKilden": "ARBEIDSMARKEDSOPPLAERING",
                  "fraOgMed": null,
                  "tilOgMed": null,
                  "kildestatus": {
                    "kilde": "Komet",
                    "kodeIKontrakten": "DELTAR",
                    "årsak": null,
                    "opprettet": "$testStatusOpprettet"
                  },
                  "deltakelsesprosent": 50.0,
                  "dagerPerUke": 2.5,
                  "deltidsprosentPåGjennomføring": 80.0
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
                nåtilstand = ObjectMother.tiltaksdeltakelseTac(
                    eksternTiltaksdeltakelseId = "ekstern-id",
                    typeKode = TiltakstypeSomGirRettDTO.ARBEIDSMARKEDSOPPLAERING,
                    typeNavn = "Arbeidsmarkedsopplæring",
                    eksternTiltaksgjennomføringsId = "gjennomføring-id",
                    fom = LocalDate.of(2026, 1, 1),
                    tom = LocalDate.of(2026, 3, 31),
                    status = TiltakDeltakerstatus.Deltar,
                    dagerPrUke = 2.5f,
                    prosent = 50f,
                    kilde = Tiltakskilde.Komet,
                    deltidsprosentGjennomforing = 80.0,
                ).copy(deltakelseFraOgMed = deltakelseFraOgMed, deltakelseTilOgMed = deltakelseTilOgMed).tilLibsDeltakelse(),
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
