package no.nav.tiltakspenger.saksbehandling.ytelser

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataFakeClient
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.UtbetalingDtoTestEx
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Ytelsene fra sokos-utbetaldata hele veien: fra klienten, gjennom saksopplysningene, ut i behandlingsresponsen og ned i `behandling.saksopplysninger`.
 *
 * Utbetaldata-faken svarer med ytelsene [UtbetalingDtoTestEx.riktSvar] mapper til, og at den mappingen er riktig, er pinnet i [no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataHttpClientTest].
 * Her er poenget hva som skjer med ytelsene etter mappingen, slik at utvidelsen av `UtbetalingDto` ikke kan endre verken responsen eller kolonnen.
 *
 * Klassen pinner også perioden vi spør utbetaldata om, som svart boks.
 * Den er ikke saksopplysningsperioden: den strekkes bakover til første dag i måneden før, fordi forrige måned ikke nødvendigvis er utbetalt enda, og fram til siste dag i måneden saksopplysningsperioden slutter i.
 * Utbetaldata godtar ikke datoer frem i tid, så bak-kanten kappes mot dagens dato.
 * Perioden ligger ikke i behandlingsresponsen, så den leses av `oppslagsperiode` i kolonnen.
 */
class YtelserSaksopplysningTest {

    /**
     * Tiltaksdeltakelsen fra `tac.tiltaksdeltakelse()` går fra januar til mars 2023, og testklokka står 1. mai 2025.
     * Oppslagsperioden blir da første dag i måneden før deltakelsen til siste dag i måneden den slutter i, siden dagens dato ligger etter begge.
     */
    private val forventetOppslagsperiode = 1.desember(2022) til 31.mars(2023)

    // language=json
    private val forventedeYtelserJson = """
        [
          {
            "ytelsetype": "Tiltakspenger",
            "perioder": [
              { "fraOgMed": "2025-08-01", "tilOgMed": "2025-08-14" },
              { "fraOgMed": "2025-09-01", "tilOgMed": "2025-09-14" }
            ]
          },
          {
            "ytelsetype": "Dagpenger",
            "perioder": [
              { "fraOgMed": "2025-09-01", "tilOgMed": "2025-09-30" }
            ]
          },
          {
            "ytelsetype": "Ukjent",
            "perioder": [
              { "fraOgMed": "2025-08-18", "tilOgMed": "2025-08-31" }
            ]
          }
        ]
    """.trimIndent()

    @Test
    fun `ytelsene fra utbetaldata havner i behandlingsresponsen og i saksopplysningskolonnen`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            // Saksopplysningene hentes når behandlingen opprettes, så faken må være seedet før det.
            tac.leggTilYtelserFraUtbetaldata(
                fnr = fnr,
                ytelser = UtbetalingDtoTestEx.forventedeYtelserFraRiktSvar,
            )

            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac = tac, fnr = fnr)

            val (_, oppdatertBehandling, respons) = oppdaterSaksopplysningerForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
            )

            val ytelser = oppdatertBehandling.saksopplysninger.ytelser
            ytelser.value shouldBe UtbetalingDtoTestEx.forventedeYtelserFraRiktSvar
            ytelser.oppslagsperiode shouldBe forventetOppslagsperiode
            tac.utbetaldataOppslag.distinct() shouldBe listOf(
                SokosUtbetaldataFakeClient.Oppslag(fnr = fnr, periode = forventetOppslagsperiode),
            )

            JSONObject(respons)
                .getJSONObject("saksopplysninger")
                .getJSONArray("ytelser")
                .toString() shouldEqualJson forventedeYtelserJson

            // language=json
            tac.sessionFactory.ytelserFor(behandling.id) shouldEqualJson """
                {
                  "ytelser": $forventedeYtelserJson,
                  "type": "Treff",
                  "oppslagstidspunkt": "${ytelser.oppslagstidspunkt}",
                  "oppslagsperiode": {
                    "fraOgMed": "2022-12-01",
                    "tilOgMed": "2023-03-31"
                  }
                }
            """.trimIndent()
        }
    }

    /**
     * Deltakelsen slutter allerede på en månedsslutt, så bare fram-kanten flyttes.
     */
    @Test
    fun `fram-kanten er første dag i måneden før saksopplysningsperioden`() {
        pinnPeriodejustering(
            idag = 1.mai(2025),
            tiltaksperiode = 10.februar(2023) til 31.mars(2023),
        ) { ytelser, _ ->
            ytelser.getString("type") shouldBe "IngenTreff"
            //language=json
            ytelser.getJSONObject("oppslagsperiode").toString() shouldEqualJson """
                { "fraOgMed": "2023-01-01", "tilOgMed": "2023-03-31" }
            """.trimIndent()
        }
    }

    /**
     * Deltakelsen starter allerede på en månedsstart, så bare bak-kanten flyttes.
     * Dagens dato ligger langt etter deltakelsen, så kappingen mot dagens dato slår ikke inn.
     */
    @Test
    fun `bak-kanten er månedsslutt når dagens dato har passert den`() {
        pinnPeriodejustering(
            idag = 1.mai(2025),
            tiltaksperiode = 1.februar(2023) til 20.mars(2023),
        ) { ytelser, _ ->
            ytelser.getString("type") shouldBe "IngenTreff"
            //language=json
            ytelser.getJSONObject("oppslagsperiode").toString() shouldEqualJson """
                { "fraOgMed": "2023-01-01", "tilOgMed": "2023-03-31" }
            """.trimIndent()
        }
    }

    /**
     * Deltakelsen løper ut april, men dagens dato er 10. mars.
     * Uten kappingen ville klienten kastet, siden utbetaldata ikke godtar datoer frem i tid.
     */
    @Test
    fun `bak-kanten kappes mot dagens dato når deltakelsen løper videre`() {
        pinnPeriodejustering(
            idag = 10.mars(2025),
            tiltaksperiode = 1.februar(2025) til 20.april(2025),
        ) { ytelser, _ ->
            ytelser.getString("type") shouldBe "IngenTreff"
            //language=json
            ytelser.getJSONObject("oppslagsperiode").toString() shouldEqualJson """
                { "fraOgMed": "2025-01-01", "tilOgMed": "2025-03-10" }
            """.trimIndent()
        }
    }

    /**
     * Hele deltakelsen ligger så langt fram at bak-kanten (dagens dato) havner før fram-kanten.
     * Da har vi ingenting å spørre om, og skal ikke kalle utbetaldata i det hele tatt.
     */
    @Test
    fun `ikke behandlingsgrunnlag når bak-kanten havner før fram-kanten`() {
        pinnPeriodejustering(
            idag = 15.januar(2025),
            tiltaksperiode = 10.mars(2025) til 20.april(2025),
        ) { ytelser, oppslag ->
            ytelser.getString("type") shouldBe "IkkeBehandlingsgrunnlag"
            ytelser.isNull("oppslagsperiode") shouldBe true
            oppslag.shouldBeEmpty()
        }
    }

    /**
     * Starter en søknadsbehandling for en deltakelse i [tiltaksperiode] med dagens dato satt til [idag], og gir [sjekk] `ytelser`-objektet fra kolonnen og oppslagene faken mottok.
     *
     * Ingen ytelser seedes, så treffene er uinteressante her og varianten blir `IngenTreff` — det er oppslagsperioden som pinnes.
     * Byggeren får samme klokke som konteksten, slik at behandlingens egne tidsstempler ikke havner før dagens dato.
     */
    private fun pinnPeriodejustering(
        idag: LocalDate,
        tiltaksperiode: Periode,
        sjekk: (ytelser: JSONObject, oppslag: List<SokosUtbetaldataFakeClient.Oppslag>) -> Unit,
    ) {
        withTestApplicationContextAndPostgres(clock = TikkendeKlokke(fixedClockAt(idag))) { tac ->
            val (_, _, behandling) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
                tiltaksdeltakelse = tac.tiltaksdeltakelse(periode = tiltaksperiode),
                clock = fixedClockAt(idag),
            )

            sjekk(
                JSONObject(tac.sessionFactory.ytelserFor(behandling.id)),
                tac.utbetaldataOppslag,
            )
        }
    }
}

/**
 * Leser `ytelser`-objektet ut av jsonb-kolonnen `behandling.saksopplysninger` med egen SQL.
 *
 * Domenet viser bare [no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser], ikke formatet på disk.
 * Feltnavnene i [no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.YtelserDbJson] er kontrakten mot rader som allerede er lagret, og den kontrakten er usynlig gjennom en rundtur.
 * `oppslagsperiode` finnes heller ikke i behandlingsresponsen, så kolonnen er eneste vei til den.
 */
private fun PostgresSessionFactory.ytelserFor(behandlingId: RammebehandlingId): String = withSession { session ->
    session.run(
        queryOf(
            "select saksopplysninger -> 'ytelser' as ytelser from behandling where id = :id",
            mapOf("id" to behandlingId.toString()),
        ).map { row -> row.string("ytelser") }.asSingle,
    )
}!!
