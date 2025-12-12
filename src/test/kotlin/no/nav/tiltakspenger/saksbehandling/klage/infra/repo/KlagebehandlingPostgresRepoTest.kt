package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import org.junit.jupiter.api.Test

class KlagebehandlingPostgresRepoTest {

    @Test
    fun `lagre og hente en klagebehandling`() {
        withMigratedDb { testDataHelper ->
            val klagebehandlingId: KlagebehandlingId = KlagebehandlingId.random()
            val (sak, actual) = testDataHelper.persisterOpprettetKlagebehandlingTilAvvisning(
                klagebehandlingId = klagebehandlingId,
            )
            actual shouldBe Klagebehandling(
                id = actual.id,
                sakId = sak.id,
                opprettet = actual.opprettet,
                sistEndret = actual.sistEndret,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                journalpostId = JournalpostId("journalpostId"),
                journalpostOpprettet = actual.journalpostOpprettet,
                saksbehandler = "Z12345",
                resultat = Klagebehandlingsresultat.AVVIST,
                formkrav = KlageFormkrav(
                    erKlagerPartISaken = true,
                    klagesDetPåKonkreteElementerIVedtaket = true,
                    erKlagefristenOverholdt = true,
                    erKlagenSignert = true,
                    vedtakDetKlagesPå = null,
                ),
            )
        }
    }
}
