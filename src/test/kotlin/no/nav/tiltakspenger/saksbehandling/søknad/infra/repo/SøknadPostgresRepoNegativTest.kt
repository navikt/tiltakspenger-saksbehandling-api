package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startBehandlingAvManueltRegistrertSøknad
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

/**
 * Negative tester for [SøknadPostgresRepo]: databasetilstander prodkoden ikke kan skrive.
 *
 * Testene muterer databasen direkte, og det er selve poenget.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class SøknadPostgresRepoNegativTest {

    /**
     * `lagreAvbruttSøknad` sjekker at oppdateringen faktisk traff en rad.
     * Prodstien leser søknaden i samme transaksjon som den avbryter den, så raden er der — vakten finnes for en framtidig kaller som ikke har gjort det.
     * Tilstanden bygges derfor ved å fjerne raden under føttene på en søknad som allerede er avbrutt.
     */
    @Test
    fun `kaster når søknaden som skal avbrytes ikke finnes`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!
            val avbruttSøknad = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.søknader.single()
            avbruttSøknad.avbrutt shouldNotBe null

            // Behandlingen, tiltaket og barnetilleggene har foreign keys til søknaden, så de må slippe taket før raden kan fjernes.
            tac.sessionFactory.withSession { session ->
                val søknadId = mapOf("id" to avbruttSøknad.id.toString())
                session.run(queryOf("update behandling set soknad_id = null where soknad_id = :id", søknadId).asUpdate)
                session.run(queryOf("delete from søknadstiltak where søknad_id = :id", søknadId).asUpdate)
                session.run(queryOf("delete from søknad_barnetillegg where søknad_id = :id", søknadId).asUpdate)
                session.run(queryOf("delete from søknad where id = :id", søknadId).asUpdate)
            }

            shouldThrowWithMessage<RuntimeException>("Kunne ikke lagre avbrutt søknad.") {
                tac.søknadContext.søknadRepo.lagreAvbruttSøknad(avbruttSøknad, null)
            }
        }
    }

    /**
     * `lagreAvbruttSøknad` tar en hel søknad, men er bare meningsfull for en som faktisk er avbrutt.
     * Prodstien avbryter søknaden først og lagrer så, så vakten nås ikke derfra — den finnes for en framtidig kaller som bytter om på rekkefølgen.
     */
    @Test
    fun `kaster når søknaden som skal lagres ikke er avbrutt`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, søknad) = opprettSakOgSøknad(tac)
            søknad.avbrutt shouldBe null

            shouldThrowWithMessage<IllegalArgumentException>("Kan ikke lagre en søknad som ikke er avbrutt") {
                tac.søknadContext.søknadRepo.lagreAvbruttSøknad(søknad, null)
            }
        }
    }

    /**
     * `SøknadDAO.toSakId` leser `sak_id` som non-null, og garantien er `not null`-constrainten fra V241 — ikke domenemodellen alene.
     * Droppes constrainten, forsvinner grunnlaget for lesingen uten at noe annet slår ut, så den verifiseres direkte.
     */
    @Test
    fun `sak_id på søknad kan ikke settes til null`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, søknad) = opprettSakOgSøknad(tac)

            val feil = shouldThrow<org.postgresql.util.PSQLException> {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            "update søknad set sak_id = null where id = :id",
                            mapOf("id" to søknad.id.toString()),
                        ).asUpdate,
                    )
                }
            }

            feil.message shouldContain "sak_id"
        }
    }

    /**
     * Den manuelt satte søknadsperioden lagres som to kolonner som alltid settes sammen.
     * Er bare den ene satt, har vi ingen periode å bygge, og mappingen faller tilbake på ingen periode framfor å gjette på den andre enden.
     */
    @Test
    fun `en halvt satt manuell søknadsperiode leses som ingen periode`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)
            startBehandlingAvManueltRegistrertSøknad(
                tac = tac,
                saksnummer = sak.saksnummer,
                journalpostId = "journalpost-halv-periode",
                manueltSattSøknadsperiodeJson = """{"fraOgMed": "2025-01-01", "tilOgMed": "2025-03-31"}""",
            )
            val papirsøknad = tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)!!
                .søknader.single { it.journalpostId == "journalpost-halv-periode" }
            papirsøknad.manueltSattSøknadsperiode shouldNotBe null

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update søknad set manuelt_satt_soknadsperiode_til_og_med = null where id = :id",
                        mapOf("id" to papirsøknad.id.toString()),
                    ).asUpdate,
                )
            }

            tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)!!
                .søknader.single { it.journalpostId == "journalpost-halv-periode" }
                .manueltSattSøknadsperiode shouldBe null
        }
    }

    /**
     * Et manuelt registrert barn har alltid fornavn og etternavn — domenetypen krever dem, så skrivestien kan ikke legge igjen null.
     * Kolonnene er likevel nullable, fordi PDL-barn deler tabell og kan mangle navn.
     * `checkNotNull` i [BarnetilleggDAO] er vakten mot at et manuelt barn leses inn med hull, og den nås bare ved å mutere raden.
     */
    @Test
    fun `kaster når et manuelt registrert barn mangler navn`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSakOgSøknad(tac)

            @Language("JSON")
            val manueltBarn = """
                [{
                  "fødselsdato": "2018-09-12",
                  "fornavn": "Manuelt",
                  "mellomnavn": null,
                  "etternavn": "Barnesen",
                  "oppholdInnenforEøs": {"svar": "NEI"},
                  "fnr": null
                }]
            """.trimIndent()

            startBehandlingAvManueltRegistrertSøknad(
                tac = tac,
                saksnummer = sak.saksnummer,
                journalpostId = "journalpost-manuelt-barn",
                barnetilleggManuelleJson = manueltBarn,
            )
            val søknadId = tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)!!
                .søknader.single { it.journalpostId == "journalpost-manuelt-barn" }
                .id

            fun muter(kolonne: String) = tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update søknad_barnetillegg set $kolonne = null where søknad_id = :id and type = 'MANUELL'",
                        mapOf("id" to søknadId.toString()),
                    ).asUpdate,
                )
            }

            muter("fornavn")
            shouldThrowWithMessage<IllegalStateException>("Fornavn kan ikke være null for barnetillegg, manuelle barn ") {
                tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)
            }

            // Fornavn tilbake, så det er etternavn-vakten som slår ut i neste steg.
            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update søknad_barnetillegg set fornavn = 'Manuelt' where søknad_id = :id and type = 'MANUELL'",
                        mapOf("id" to søknadId.toString()),
                    ).asUpdate,
                )
            }
            muter("etternavn")
            shouldThrowWithMessage<IllegalStateException>("Etternavn kan ikke være null for barnetillegg, manuelle barn ") {
                tac.sakContext.sakRepo.hentForSaksnummer(sak.saksnummer)
            }
        }
    }
}
