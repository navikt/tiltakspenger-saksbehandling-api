package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsplan
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Åpningstider
import java.time.LocalDateTime

class UtbetalingsoversiktPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : UtbetalingsoversiktRepo {
    override fun lagre(oversikt: Utbetalingsoversikt, metadata: UtbetalingsoversiktMetadata) {
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                    INSERT INTO utbetalingsoversikt (
                        id,
                        sak_id,
                        hentet,
                        oppslag_fom,
                        oppslag_tom,
                        oppslag_periodetype,
                        resultat,
                        feiltype,
                        utbetalinger,
                        metadata,
                        neste_oppslag,
                        antall_feil_på_rad
                    ) VALUES (
                        :id,
                        :sak_id,
                        :hentet,
                        :oppslag_fom,
                        :oppslag_tom,
                        :oppslag_periodetype,
                        :resultat,
                        :feiltype,
                        :utbetalinger::jsonb,
                        :metadata::jsonb,
                        :neste_oppslag,
                        :antall_feil_pa_rad
                    )
                    """.trimIndent(),
                    "id" to oversikt.id.toString(),
                    "sak_id" to oversikt.sakId.toString(),
                    "hentet" to oversikt.hentet,
                    "oppslag_fom" to oversikt.oppslag.periode.fraOgMed,
                    "oppslag_tom" to oversikt.oppslag.periode.tilOgMed,
                    "oppslag_periodetype" to oversikt.oppslag.periodetype.toDb(),
                    "resultat" to oversikt.toOppslagsresultatDb().toDb(),
                    "feiltype" to when (oversikt) {
                        is Utbetalingsoversikt.Vellykket -> null
                        is Utbetalingsoversikt.Feilet -> oversikt.feiltype.toDb()
                    },
                    "utbetalinger" to when (oversikt) {
                        is Utbetalingsoversikt.Vellykket -> oversikt.utbetalinger.toDbJson()
                        is Utbetalingsoversikt.Feilet -> null
                    },
                    "metadata" to metadata.toDbJson(),
                    "neste_oppslag" to oversikt.plan.nesteOppslag,
                    "antall_feil_pa_rad" to oversikt.plan.antallFeilPåRad,
                ).asUpdate,
            )
        }
    }

    /** «Siste» er høyeste `(hentet, id)`. */
    override fun hentStatusForSak(sakId: SakId): Utbetalingsoversiktstatus = sessionFactory.withSession { session ->
        when (val siste = hentSiste(sakId, session)) {
            null -> Utbetalingsoversiktstatus.IkkeHentet

            is Utbetalingsoversikt.Vellykket -> Utbetalingsoversiktstatus.SisteOppslagVellykket(siste)

            is Utbetalingsoversikt.Feilet -> Utbetalingsoversiktstatus.SisteOppslagFeilet(
                oversikt = siste,
                sisteVellykkede = hentSisteVellykkedeFør(siste, session),
            )
        }
    }

    /**
     * Henter saker som har en OK-utbetaling som kan stå i reskontroen, og som er klare for oppslag, med tidligste frist først.
     * Siste oppslag slås opp per sak, så arbeidet følger antall saker og ikke lengden på historikken.
     */
    override fun hentSakerKlareForOppslag(nå: LocalDateTime, limit: Int): List<SakId> = sessionFactory.withSession { session ->
        session.run(
            sqlQuery(
                """
                WITH sendte_utbetalinger AS (
                    SELECT
                        sak_id,
                        min(sendt_til_utbetaling_tidspunkt) AS forste_sendetidspunkt,
                        max(sendt_til_utbetaling_tidspunkt) AS siste_sendetidspunkt
                    FROM utbetaling
                    WHERE status = 'OK'
                      AND sendt_til_utbetaling_tidspunkt < :sendt_foer
                    GROUP BY sak_id
                )
                SELECT sendte_utbetalinger.sak_id
                FROM sendte_utbetalinger
                LEFT JOIN LATERAL (
                    SELECT
                        hentet,
                        resultat,
                        neste_oppslag
                    FROM utbetalingsoversikt
                    WHERE utbetalingsoversikt.sak_id = sendte_utbetalinger.sak_id
                    ORDER BY hentet DESC, id DESC
                    LIMIT 1
                ) siste_oppslag ON TRUE
                WHERE siste_oppslag.hentet IS NULL
                   OR siste_oppslag.neste_oppslag <= :naa
                   OR (
                        siste_oppslag.resultat = 'VELLYKKET'
                        AND siste_oppslag.hentet < :dagens_start
                        AND sendte_utbetalinger.siste_sendetidspunkt >= :sendt_etter
                   )
                ORDER BY coalesce(siste_oppslag.neste_oppslag, sendte_utbetalinger.forste_sendetidspunkt), sendte_utbetalinger.sak_id
                LIMIT :limit
                """.trimIndent(),
                "naa" to nå,
                "dagens_start" to nå.toLocalDate().atStartOfDay(),
                "sendt_etter" to nå.minusDays(30),
                "sendt_foer" to Åpningstider.senesteSendetidspunktSomKanStåIReskontroen(nå),
                "limit" to limit,
            ).map { SakId.fromString(it.string("sak_id")) }.asList,
        )
    }

    private fun hentSiste(sakId: SakId, session: Session): Utbetalingsoversikt? = session.run(
        sqlQuery(
            """
            SELECT
                id,
                sak_id,
                hentet,
                oppslag_fom,
                oppslag_tom,
                oppslag_periodetype,
                resultat,
                feiltype,
                utbetalinger,
                neste_oppslag,
                antall_feil_på_rad
            FROM utbetalingsoversikt
            WHERE sak_id = :sak_id
            ORDER BY hentet DESC, id DESC
            LIMIT 1
            """.trimIndent(),
            "sak_id" to sakId.toString(),
        ).map { it.toUtbetalingsoversikt() }.asSingle,
    )

    private fun hentSisteVellykkedeFør(feilet: Utbetalingsoversikt.Feilet, session: Session): Utbetalingsoversikt.Vellykket? = session.run(
        sqlQuery(
            """
            SELECT
                id,
                sak_id,
                hentet,
                oppslag_fom,
                oppslag_tom,
                oppslag_periodetype,
                utbetalinger,
                neste_oppslag,
                antall_feil_på_rad
            FROM utbetalingsoversikt
            WHERE sak_id = :sak_id
              AND resultat = 'VELLYKKET'
              AND (hentet, id) < (:hentet, :id)
            ORDER BY hentet DESC, id DESC
            LIMIT 1
            """.trimIndent(),
            "sak_id" to feilet.sakId.toString(),
            "hentet" to feilet.hentet,
            "id" to feilet.id.toString(),
        ).map { it.toVellykket() }.asSingle,
    )

    private fun Row.toUtbetalingsoversikt(): Utbetalingsoversikt = when (string("resultat").toOppslagsresultatDb()) {
        OppslagsresultatDb.VELLYKKET -> toVellykket()

        OppslagsresultatDb.FEILET -> Utbetalingsoversikt.Feilet(
            id = UtbetalingsoversiktId.fromString(string("id")),
            sakId = SakId.fromString(string("sak_id")),
            hentet = localDateTime("hentet"),
            oppslag = toOppslag(),
            plan = toOppslagsplan(),
            feiltype = string("feiltype").toOppslagsfeiltype(),
        )
    }

    private fun Row.toVellykket(): Utbetalingsoversikt.Vellykket = Utbetalingsoversikt.Vellykket(
        id = UtbetalingsoversiktId.fromString(string("id")),
        sakId = SakId.fromString(string("sak_id")),
        hentet = localDateTime("hentet"),
        oppslag = toOppslag(),
        plan = toOppslagsplan(),
        utbetalinger = string("utbetalinger").toRegistrerteUtbetalinger(),
    )

    private fun Row.toOppslag(): Oppslag = Oppslag(
        periode = Periode(localDate("oppslag_fom"), localDate("oppslag_tom")),
        periodetype = string("oppslag_periodetype").toOppslagsperiodetype(),
    )

    private fun Row.toOppslagsplan(): Oppslagsplan = Oppslagsplan(
        nesteOppslag = localDateTime("neste_oppslag"),
        antallFeilPåRad = int("antall_feil_på_rad"),
    )
}
