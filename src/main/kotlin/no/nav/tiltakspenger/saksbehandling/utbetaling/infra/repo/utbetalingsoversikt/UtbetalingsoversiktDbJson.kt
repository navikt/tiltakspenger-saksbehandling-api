package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.personopplysning.Organisasjonsnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Samhandlerident
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Aktør
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertYtelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Skattetrekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Trekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Ytelseskomponent
import java.math.BigDecimal
import java.time.LocalDate

private data class UtbetalingsoversiktDbJson(
    val utbetalinger: List<RegistrertUtbetalingDbJson>,
) {
    data class RegistrertUtbetalingDbJson(
        val utbetaltTil: AktørDbJson,
        val utbetalingsmetode: String,
        val utbetalingsstatus: String,
        val posteringsdato: LocalDate,
        val forfallsdato: LocalDate?,
        val utbetalingsdato: LocalDate?,
        val nettobeløp: BigDecimal?,
        val melding: String?,
        val ytelser: List<RegistrertYtelseDbJson>,
    )

    data class RegistrertYtelseDbJson(
        val ytelsestype: String?,
        val periode: PeriodeDbJson,
        val nettobeløp: BigDecimal,
        val rettighetshaver: AktørDbJson,
        val skattesum: BigDecimal,
        val trekksum: BigDecimal,
        val komponentsum: BigDecimal,
        val komponenter: List<YtelseskomponentDbJson>,
        val trekk: List<TrekkDbJson>,
        val skattetrekk: List<SkattetrekkDbJson>,
        val bilagsnummer: String?,
        val refundertFor: AktørDbJson?,
    )

    data class PeriodeDbJson(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )

    data class AktørDbJson(
        val type: String,
        val ident: String,
    )

    data class YtelseskomponentDbJson(
        val type: String?,
        val satsbeløp: BigDecimal?,
        val satstype: String?,
        val satsantall: Double?,
        val beløp: BigDecimal?,
    )

    data class TrekkDbJson(
        val type: String?,
        val beløp: BigDecimal?,
        val kreditor: String?,
    )

    data class SkattetrekkDbJson(
        val beløp: BigDecimal?,
    )
}

fun List<RegistrertUtbetaling>.toDbJson(): String =
    serialize(
        UtbetalingsoversiktDbJson(
            map { utbetaling ->
                UtbetalingsoversiktDbJson.RegistrertUtbetalingDbJson(
                    utbetaltTil = utbetaling.utbetaltTil.toDb(),
                    utbetalingsmetode = utbetaling.utbetalingsmetode,
                    utbetalingsstatus = utbetaling.utbetalingsstatus,
                    posteringsdato = utbetaling.posteringsdato,
                    forfallsdato = utbetaling.forfallsdato,
                    utbetalingsdato = utbetaling.utbetalingsdato,
                    nettobeløp = utbetaling.nettobeløp,
                    melding = utbetaling.melding,
                    ytelser = utbetaling.ytelser.map { it.toDb() },
                )
            },
        ),
    )

fun String.toRegistrerteUtbetalinger(): List<RegistrertUtbetaling> =
    deserialize<UtbetalingsoversiktDbJson>(this).utbetalinger.map { it.toDomain() }

private fun RegistrertYtelse.toDb() = UtbetalingsoversiktDbJson.RegistrertYtelseDbJson(
    ytelsestype = ytelsestype,
    periode = UtbetalingsoversiktDbJson.PeriodeDbJson(periode.fraOgMed, periode.tilOgMed),
    nettobeløp = nettobeløp,
    rettighetshaver = rettighetshaver.toDb(),
    skattesum = skattesum,
    trekksum = trekksum,
    komponentsum = komponentsum,
    komponenter = komponenter.map {
        UtbetalingsoversiktDbJson.YtelseskomponentDbJson(it.type, it.satsbeløp, it.satstype, it.satsantall, it.beløp)
    },
    trekk = trekk.map { UtbetalingsoversiktDbJson.TrekkDbJson(it.type, it.beløp, it.kreditor) },
    skattetrekk = skattetrekk.map { UtbetalingsoversiktDbJson.SkattetrekkDbJson(it.beløp) },
    bilagsnummer = bilagsnummer,
    refundertFor = refundertFor?.toDb(),
)

private fun Aktør.toDb(): UtbetalingsoversiktDbJson.AktørDbJson =
    when (this) {
        is Aktør.Person -> UtbetalingsoversiktDbJson.AktørDbJson(
            AktørtypeDb.PERSON.name,
            fnr.verdi,
        )

        is Aktør.Organisasjon -> UtbetalingsoversiktDbJson.AktørDbJson(
            AktørtypeDb.ORGANISASJON.name,
            organisasjonsnummer.verdi,
        )

        is Aktør.Samhandler -> UtbetalingsoversiktDbJson.AktørDbJson(
            AktørtypeDb.SAMHANDLER.name,
            ident.verdi,
        )
    }

private fun UtbetalingsoversiktDbJson.RegistrertUtbetalingDbJson.toDomain() = RegistrertUtbetaling(
    utbetaltTil = utbetaltTil.toDomain(),
    utbetalingsmetode = utbetalingsmetode,
    utbetalingsstatus = utbetalingsstatus,
    posteringsdato = posteringsdato,
    forfallsdato = forfallsdato,
    utbetalingsdato = utbetalingsdato,
    nettobeløp = nettobeløp,
    melding = melding,
    ytelser = ytelser.map { it.toDomain() },
)

private fun UtbetalingsoversiktDbJson.RegistrertYtelseDbJson.toDomain() = RegistrertYtelse(
    ytelsestype = ytelsestype,
    periode = Periode(periode.fraOgMed, periode.tilOgMed),
    nettobeløp = nettobeløp,
    rettighetshaver = rettighetshaver.toDomain(),
    skattesum = skattesum,
    trekksum = trekksum,
    komponentsum = komponentsum,
    komponenter = komponenter.map { Ytelseskomponent(it.type, it.satsbeløp, it.satstype, it.satsantall, it.beløp) },
    trekk = trekk.map { Trekk(it.type, it.beløp, it.kreditor) },
    skattetrekk = skattetrekk.map { Skattetrekk(it.beløp) },
    bilagsnummer = bilagsnummer,
    refundertFor = refundertFor?.toDomain(),
)

private fun UtbetalingsoversiktDbJson.AktørDbJson.toDomain(): Aktør =
    when (AktørtypeDb.valueOf(type)) {
        AktørtypeDb.PERSON -> Aktør.Person(Fnr.fromString(ident))
        AktørtypeDb.ORGANISASJON -> Aktør.Organisasjon(Organisasjonsnummer(ident))
        AktørtypeDb.SAMHANDLER -> Aktør.Samhandler(Samhandlerident(ident))
    }

private enum class AktørtypeDb {
    PERSON,
    ORGANISASJON,
    SAMHANDLER,
}
