package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.repo.utbetalingsoversikt

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsfeiltype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversikt

private enum class OppslagsperiodetypeDb {
    UTBETALINGSPERIODE,
    YTELSESPERIODE,
}

fun Oppslagsperiodetype.toDb(): String = when (this) {
    Oppslagsperiodetype.UTBETALINGSPERIODE -> OppslagsperiodetypeDb.UTBETALINGSPERIODE.name
    Oppslagsperiodetype.YTELSESPERIODE -> OppslagsperiodetypeDb.YTELSESPERIODE.name
}

fun String.toOppslagsperiodetype(): Oppslagsperiodetype = when (OppslagsperiodetypeDb.valueOf(this)) {
    OppslagsperiodetypeDb.UTBETALINGSPERIODE -> Oppslagsperiodetype.UTBETALINGSPERIODE
    OppslagsperiodetypeDb.YTELSESPERIODE -> Oppslagsperiodetype.YTELSESPERIODE
}

private enum class OppslagsfeiltypeDb {
    TILGANG_AVVIST,
    TJENESTEFEIL,
    SAK_AVVIST,
    ULESELIG_SVAR,
    UGYLDIG_INNHOLD,
}

fun Oppslagsfeiltype.toDb(): String = when (this) {
    Oppslagsfeiltype.TILGANG_AVVIST -> OppslagsfeiltypeDb.TILGANG_AVVIST.name
    Oppslagsfeiltype.TJENESTEFEIL -> OppslagsfeiltypeDb.TJENESTEFEIL.name
    Oppslagsfeiltype.SAK_AVVIST -> OppslagsfeiltypeDb.SAK_AVVIST.name
    Oppslagsfeiltype.ULESELIG_SVAR -> OppslagsfeiltypeDb.ULESELIG_SVAR.name
    Oppslagsfeiltype.UGYLDIG_INNHOLD -> OppslagsfeiltypeDb.UGYLDIG_INNHOLD.name
}

fun String.toOppslagsfeiltype(): Oppslagsfeiltype = when (OppslagsfeiltypeDb.valueOf(this)) {
    OppslagsfeiltypeDb.TILGANG_AVVIST -> Oppslagsfeiltype.TILGANG_AVVIST
    OppslagsfeiltypeDb.TJENESTEFEIL -> Oppslagsfeiltype.TJENESTEFEIL
    OppslagsfeiltypeDb.SAK_AVVIST -> Oppslagsfeiltype.SAK_AVVIST
    OppslagsfeiltypeDb.ULESELIG_SVAR -> Oppslagsfeiltype.ULESELIG_SVAR
    OppslagsfeiltypeDb.UGYLDIG_INNHOLD -> Oppslagsfeiltype.UGYLDIG_INNHOLD
}

/** Verdiene i kolonnen `resultat`. */
enum class OppslagsresultatDb {
    VELLYKKET,
    FEILET,
}

fun Utbetalingsoversikt.toOppslagsresultatDb(): OppslagsresultatDb = when (this) {
    is Utbetalingsoversikt.Vellykket -> OppslagsresultatDb.VELLYKKET
    is Utbetalingsoversikt.Feilet -> OppslagsresultatDb.FEILET
}

fun OppslagsresultatDb.toDb(): String = name

fun String.toOppslagsresultatDb(): OppslagsresultatDb = OppslagsresultatDb.valueOf(this)
