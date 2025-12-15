@file:Suppress("unused", "ktlint")

package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresTransactionContext.Companion.withSession
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.tilAntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.tilInnvilgelsesperioderDbJson
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.tilValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.toSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V170__migrer_innvilgelsesperioder : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        sessionFactory.withTransactionContext { tx ->
            tx.withSession { session ->
                val medInnvilgelse = hentInnvilgelsesperioder(session)
                logger.info { "Fant ${medInnvilgelse.size} behandlinger med innvilgelse" }

                val medFeil: List<BehandlingId> = medInnvilgelse.mapNotNull { (id, innvilgelsesperioder) ->
                    if (innvilgelsesperioder != null) {
                        lagreInnvilgelsesperioder(id, innvilgelsesperioder, session)
                    }

                    try {
                        BehandlingPostgresRepo.hentOrNull(id, session)!!
                        return@mapNotNull null
                    } catch (e: Exception) {
                        logger.error(e) { "Kunne ikke hente behandling $id etter migrering" }
                        return@mapNotNull id
                    }
                }

                if (medFeil.isNotEmpty()) {
                    throw IllegalStateException("${medFeil.size} behandlinger kunne ikke hentes opp etter migrering, avbryter")
                }
            }
        }
    }

    fun lagreInnvilgelsesperioder(id: BehandlingId, innvilgelsesperioder: Innvilgelsesperioder, session: Session) {
        session.run(
            sqlQuery(
                """
                    update behandling
                    set innvilgelsesperioder = to_jsonb(:innvilgelsesperioder::jsonb)
                    where id = :id                    
                """.trimIndent(),
                "id" to id.toString(),
                "innvilgelsesperioder" to innvilgelsesperioder.tilInnvilgelsesperioderDbJson(),
            ).asUpdate,
        )
    }

    fun hentInnvilgelsesperioder(session: Session): List<Pair<BehandlingId, Innvilgelsesperioder?>> {
        return session.run(
            sqlQuery(
                """
                    select * from behandling where resultat in ('INNVILGELSE', 'REVURDERING_INNVILGELSE', 'OMGJØRING')
                """,
            ).map {
                val id = it.string("id").let { BehandlingId.fromString(it) }
                val resultat = it.tilInnvilgelsesperioder()

                Pair(id, resultat)
            }.asList,
        )
    }

    fun Row.tilInnvilgelsesperioder(): Innvilgelsesperioder? {
        val virkningsperiodeFraOgMed = localDateOrNull("virkningsperiode_fra_og_med")
        val virkningsperiodeTilOgMed = localDateOrNull("virkningsperiode_til_og_med")

        if ((virkningsperiodeFraOgMed == null).xor(virkningsperiodeTilOgMed == null)) {
            throw IllegalStateException("Både fra og med og til og med for virkningsperiode må være satt, eller ingen av dem")
        }

        val virkningsperiode =
            virkningsperiodeFraOgMed?.let { Periode(virkningsperiodeFraOgMed, virkningsperiodeTilOgMed!!) }

        val innvilgelsesperiode = stringOrNull("innvilgelsesperiode")?.let {
            deserialize<PeriodeDbJson>(it).toDomain()
        }

        val saksopplysninger = string("saksopplysninger").toSaksopplysninger()

        val valgteTiltaksdeltakelser = stringOrNull("valgte_tiltaksdeltakelser")
            ?.tilValgteTiltaksdeltakelser()

        val antallDagerPerMeldeperiode = stringOrNull("antall_dager_per_meldeperiode")
            ?.tilAntallDagerForMeldeperiode()

        if (valgteTiltaksdeltakelser == null || antallDagerPerMeldeperiode == null) {
            return null
        }

        val resultatType = stringOrNull("resultat")

        return when (resultatType) {
            "INNVILGELSE" -> Innvilgelsesperioder.create(
                saksopplysninger = saksopplysninger,
                innvilgelsesperiode = virkningsperiode!!,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                tiltaksdeltakelser = valgteTiltaksdeltakelser,
            )

            "REVURDERING_INNVILGELSE" -> virkningsperiode?.let {
                Innvilgelsesperioder.create(
                    saksopplysninger = saksopplysninger,
                    innvilgelsesperiode = it,
                    antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                    tiltaksdeltakelser = valgteTiltaksdeltakelser,
                )
            }

            "OMGJØRING" -> innvilgelsesperiode?.let {
                Innvilgelsesperioder.create(
                    saksopplysninger = saksopplysninger,
                    innvilgelsesperiode = it,
                    antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                    tiltaksdeltakelser = valgteTiltaksdeltakelser,
                )
            }

            else -> throw IllegalStateException("Ugyldig resultat for innvilgelse: ${resultatType}")
        }
    }
}
