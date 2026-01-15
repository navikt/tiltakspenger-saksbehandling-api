package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.repo.persisterOpprettetKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.infra.repo.withMigratedDb
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingBrevtekst
import org.junit.jupiter.api.Test

class KlagebehandlingPostgresRepoTest {

    @Test
    fun `lagre og hente en klagebehandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, klagebehandling, _) = opprettSakOgOppdaterKlagebehandlingBrevtekst(
                tac = tac,
            )!!
            klagebehandling shouldBe Klagebehandling(
                id = klagebehandling.id,
                sakId = sak.id,
                opprettet = klagebehandling.opprettet,
                sistEndret = klagebehandling.sistEndret,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                status = Klagebehandlingsstatus.UNDER_BEHANDLING,
                journalpostId = JournalpostId("12345"),
                journalpostOpprettet = klagebehandling.journalpostOpprettet,
                saksbehandler = "Z12345",
                resultat = Klagebehandlingsresultat.AVVIST,
                formkrav = KlageFormkrav(
                    erKlagerPartISaken = true,
                    klagesDetPåKonkreteElementerIVedtaket = true,
                    erKlagefristenOverholdt = true,
                    erKlagenSignert = true,
                    vedtakDetKlagesPå = null,
                ),
                brevtekst = Brevtekster(
                    listOf(
                        TittelOgTekst(
                            tittel = "Avvisning av klage",
                            tekst = "Din klage er dessverre avvist.",
                        ),
                    ),
                ),
            )
        }
    }
}
