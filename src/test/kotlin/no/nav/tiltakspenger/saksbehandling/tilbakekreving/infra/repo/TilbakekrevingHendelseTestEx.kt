package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse

/**
 * Oppslag mot `tilbakekreving_hendelse` som kun testene trenger.
 * Prodkoden henter ubehandlede hendelse-ider og hendelser per id; denne spørringen finnes for å se alt som ble skrevet for en sak.
 * Den hører derfor i testlaget, ikke som `@TestOnly` på [TilbakekrevingHendelsePostgresRepo].
 *
 * Mappingen gjenbrukes fra repoet — den brukes av prodspørringene og skal ikke dupliseres her.
 */
fun PostgresSessionFactory.hentTilbakekrevingshendelserForEksternFagsakId(eksternFagsakId: String): List<Tilbakekrevingshendelse> =
    withSession { session ->
        session.run(
            sqlQuery(
                """
                    SELECT *
                    FROM tilbakekreving_hendelse
                    WHERE ekstern_fagsak_id = :ekstern_fagsak_id
                    ORDER BY opprettet
                """.trimIndent(),
                "ekstern_fagsak_id" to eksternFagsakId,
            ).map { row -> row.tilTilbakekrevingshendelse() }.asList,
        )
    }
