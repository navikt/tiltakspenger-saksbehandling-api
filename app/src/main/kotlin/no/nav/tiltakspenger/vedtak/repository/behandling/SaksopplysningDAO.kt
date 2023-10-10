package no.nav.tiltakspenger.vedtak.repository.behandling

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.domene.saksopplysning.Kilde
import no.nav.tiltakspenger.domene.saksopplysning.Saksopplysning
import no.nav.tiltakspenger.domene.saksopplysning.TypeSaksopplysning
import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.vedtak.db.DataSource
import org.intellij.lang.annotations.Language

internal class SaksopplysningDAO {
    fun hent(behandlingId: BehandlingId): List<Saksopplysning> {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                txSession.run(
                    queryOf(
                        sqlHentSaksopplysninger,
                        mapOf(
                            "behandlingId" to behandlingId.toString(),
                        ),
                    ).map { row ->
                        row.toSaksopplysning()
                    }.asList,
                )
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, saksopplysninger: List<Saksopplysning>) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                slett(behandlingId, txSession)
                saksopplysninger.forEach { saksopplysning ->
                    lagre(behandlingId, saksopplysning, txSession)
                }
            }
        }
    }

    private fun lagre(behandlingId: BehandlingId, saksopplysning: Saksopplysning, txSession: TransactionalSession) {
        txSession.run(
            queryOf(
                sqlLagreSaksopplysning,
                mapOf(
                    "behandlingId" to behandlingId.toString(),
                    "fom" to saksopplysning.fom,
                    "tom" to saksopplysning.tom,
                    "kilde" to saksopplysning.kilde.name,
                    "vilkar" to saksopplysning.vilkår.tittel, // her burde vi kanskje lage en when over vilkår i stedet for å bruke tittel?
                    "detaljer" to saksopplysning.detaljer,
                    "typeSaksopplysning" to saksopplysning.typeSaksopplysning.name,
                ),
            ).asUpdate,
        )
    }

    private fun slett(behandlingId: BehandlingId, txSession: TransactionalSession) {
        txSession.run(
            queryOf(
                sqlSlettSaksopplysninger,
                mapOf("behandlingId" to behandlingId.toString()),
            ).asUpdate,
        )
    }

    private fun Row.toSaksopplysning(): Saksopplysning {
        val vilkår = hentVilkår(string("vilkår"))
        return Saksopplysning(
            fom = localDate("fom"),
            tom = localDate("tom"),
            kilde = Kilde.valueOf(string("kilde")),
            vilkår = vilkår,
            detaljer = string("detaljer"),
            typeSaksopplysning = TypeSaksopplysning.valueOf(string("typeSaksopplysning")),
        )
    }

    private val sqlHentSaksopplysninger = """
        select * from saksopplysning where behandlingId = :behandlingId
    """.trimIndent()

    private val sqlSlettSaksopplysninger = """
        delete from saksopplysning where behandlingId = :behandlingId
    """.trimIndent()

    @Language("SQL")
    private val sqlLagreSaksopplysning = """
        insert into saksbehandling (
                behandlingId,
                fom,
                tom,
                kilde,
                vilkår,
                detaljer,
                typeSaksopplysning
            ) values (
                :behandlingId,
                :fom,
                :tom,
                :kilde,
                :vilkar,
                :detaljer,
                :typeSaksopplysning
            )
    """.trimIndent()
}
