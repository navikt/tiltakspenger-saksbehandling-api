package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import org.junit.jupiter.api.Test

/**
 * Negative tester for radmappingen i `RammebehandlingDb`: databasetilstander prodkoden ikke kan skrive.
 *
 * Testene muterer databasen direkte, og det er selve poenget.
 * De verifiserer at mappingen enten oppdager korrupt data i stedet for å bygge en ugyldig domenemodell, eller faller tilbake på en trygg verdi der raden er eldre enn kolonnen.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class RammebehandlingPostgresRepoNegativTest {

    /**
     * Vedtaksperioden lagres som én `periode`-kolonne, og domenet avviser en periode som mangler den ene enden.
     * Mappingen leser kolonnen med `periodeOrNull` i tillit til den garantien, så testen verifiserer at constrainten faktisk finnes.
     */
    @Test
    fun `vedtaksperiode kan ikke mangle den ene enden`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, rammevedtak) = iverksettSøknadsbehandling(tac = tac)

            val feil = shouldThrow<org.postgresql.util.PSQLException> {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            "update behandling set vedtaksperiode = '(2025-01-01,)'::periode where id = :id",
                            mapOf("id" to rammevedtak.behandlingId.toString()),
                        ).asUpdate,
                    )
                }
            }

            feil.message!! shouldContain "periode_check"
        }
    }

    /**
     * En søknadsbehandling er opprettet fra en søknad og har alltid en `soknad_id`.
     * Mangler den, kan vi ikke bygge søknadsbehandlingen, og mappingen skal si fra.
     */
    @Test
    fun `kaster når søknadsbehandlingen ikke har en søknad`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, rammevedtak) = iverksettSøknadsbehandling(tac = tac)
            val behandlingId = rammevedtak.behandlingId

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update behandling set soknad_id = null where id = :id",
                        mapOf("id" to behandlingId.toString()),
                    ).asUpdate,
                )
            }

            shouldThrowWithMessage<IllegalStateException>(
                "Fant ikke søknad for søknadsbehandling, behandlingsid $behandlingId",
            ) {
                tac.sakContext.sakRepo.hentForSakId(sak.id)
            }
        }
    }

    /**
     * Mappingen bruker `!!` på oppslaget av søknaden, i tillit til fremmednøkkelen `behandling_soknad_id_fkey`.
     * Garantien er ikke sterkere enn migreringen som holder den i live, så testen verifiserer at constrainten faktisk finnes.
     */
    @Test
    fun `soknad_id er beskyttet av en fremmednøkkel`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, rammevedtak) = iverksettSøknadsbehandling(tac = tac)

            shouldThrow<Exception> {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            "update behandling set soknad_id = :ukjent where id = :id",
                            mapOf(
                                "ukjent" to SøknadId.random().toString(),
                                "id" to rammevedtak.behandlingId.toString(),
                            ),
                        ).asUpdate,
                    )
                }
            }.message!! shouldContain "behandling_soknad_id_fkey"
        }
    }

    /**
     * `ventestatus` kom inn som `jsonb DEFAULT NULL` i V107, og radene som fantes da beholdt NULL.
     * Skrivestien setter alltid en verdi, så tilstanden kan bare bygges ved å mutere raden.
     * Leses en slik rad, skal vi få en tom ventestatus — ikke et null-felt på en domenemodell som ikke tåler det.
     */
    @Test
    fun `en rad som er eldre enn ventestatus-kolonnen leses som tom ventestatus`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, rammevedtak) = iverksettSøknadsbehandling(tac = tac)

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update behandling set ventestatus = null where id = :id",
                        mapOf("id" to rammevedtak.behandlingId.toString()),
                    ).asUpdate,
                )
            }

            val behandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.hentRammebehandling(rammevedtak.behandlingId)!!
            behandling.ventestatus shouldBe Ventestatus()
        }
    }

    /**
     * Skrivestien lagrer alltid minst en tom liste i `valgt_hjemmel_har_ikke_rettighet` for en stans, så NULL kan bare finnes på rader som er eldre enn den konvensjonen.
     * Leses en slik rad, skal den bety det samme som en tom liste: ingen valgt hjemmel.
     */
    @Test
    fun `en stans-rad med NULL i valgt_hjemmel leses som ingen valgt hjemmel`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update behandling set valgt_hjemmel_har_ikke_rettighet = null where id = :id",
                        mapOf("id" to revurdering.id.toString()),
                    ).asUpdate,
                )
            }

            val lestBehandling = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)
            (lestBehandling.resultat as Revurderingsresultat.Stans).valgtHjemmel shouldBe null
        }
    }

    /**
     * `manuelt_behandles_grunner` kom inn som nullable i V95, og radene som fantes da beholdt NULL.
     * `Søknadsbehandling.manueltBehandlesGrunner` er en non-null liste, så skrivestien setter alltid en verdi — i verste fall en tom liste.
     * Leses en eldre rad, skal vi få en tom liste framfor å feile.
     */
    @Test
    fun `en rad som er eldre enn manuelt_behandles_grunner leses som tom liste`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, rammevedtak) = iverksettSøknadsbehandling(tac = tac)

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update behandling set manuelt_behandles_grunner = null where id = :id",
                        mapOf("id" to rammevedtak.behandlingId.toString()),
                    ).asUpdate,
                )
            }

            val behandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentRammebehandling(rammevedtak.behandlingId) as Søknadsbehandling
            behandling.manueltBehandlesGrunner shouldBe emptyList()
        }
    }

    /**
     * `FritekstTilVedtaksbrev.create` og `Begrunnelse.create` gir null for en blank streng, så skrivestien kan ikke legge igjen en.
     * Eldre rader kan likevel inneholde det, og da skal en blank tekst leses som fravær av tekst — ikke som en tom fritekst i brevet.
     */
    @Test
    fun `blanke tekstkolonner leses som fravær av tekst`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, rammevedtak) = iverksettSøknadsbehandling(tac = tac)

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update behandling set fritekst_vedtaksbrev = '   ', begrunnelse_vilkårsvurdering = '   ' where id = :id",
                        mapOf("id" to rammevedtak.behandlingId.toString()),
                    ).asUpdate,
                )
            }

            val behandling = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.hentRammebehandling(rammevedtak.behandlingId)!!
            behandling.fritekstTilVedtaksbrev shouldBe null
            behandling.begrunnelseVilkårsvurdering shouldBe null
        }
    }
}
