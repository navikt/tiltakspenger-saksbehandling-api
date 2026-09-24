package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.KometTiltakHendelseDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.hentTiltaksdeltakerHendelserForEksternId
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * En testbegrensning avviser markeringen av deltakeren etter at hendelsen er skrevet.
 * Den negative databasetesten verifiserer at hendelsen og markøren skrives i samme transaksjon.
 * Testen er isolert fordi den endrer skjemaet midlertidig.
 */
class TiltaksdeltakerConsumerNegativTest {
    @Test
    @IsolatedDatabaseTest
    fun `hendelsen rulles tilbake når markering av deltakeren feiler`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val melding = KometTiltakHendelseDTO(
                id = UUID.randomUUID(),
                startDato = 5.januar(2025),
                sluttDato = 5.mai(2025),
                status = KometTiltakHendelseDTO.DeltakerStatusDto(KometDeltakerStatusTypeDTO.DELTAR),
                dagerPerUke = 2.0F,
                prosentStilling = 50.0F,
            )
            val eksternId = melding.id.toString()
            opprettSakOgSøknad(
                tac = tac,
                fnr = Fnr.random(),
                tiltaksdeltakelse = ObjectMother.tiltaksdeltakelse(eksternTiltaksdeltakelseId = eksternId),
            )
            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "ALTER TABLE tiltaksdeltaker ADD CONSTRAINT test_avvis_markering CHECK (siste_ubehandlet_endring IS NULL) NOT VALID",
                    ).asExecute,
                )
            }
            try {
                shouldThrowAny {
                    tac.tiltaksdeltakerKometConsumer.consume(melding.id, serialize(melding))
                }

                tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(eksternId).shouldBeEmpty()
                tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(eksternId).shouldNotBeNull()
                    .sisteUbehandletEndringTidspunkt.shouldBeNull()
            } finally {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf("ALTER TABLE tiltaksdeltaker DROP CONSTRAINT test_avvis_markering").asExecute,
                    )
                }
            }

            tac.tiltaksdeltakerKometConsumer.consume(melding.id, serialize(melding))

            tac.sessionFactory.hentTiltaksdeltakerHendelserForEksternId(eksternId).single()
            tac.tiltakContext.tiltaksdeltakerRepo.hentTiltaksdeltaker(eksternId).shouldNotBeNull()
                .sisteUbehandletEndringTidspunkt.shouldNotBeNull()
        }
    }
}
