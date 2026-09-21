package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.personopplysning.Organisasjonsnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Samhandlerident
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Aktør
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertYtelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Skattetrekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Trekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMappingfeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Ytelseskomponent

/**
 * Mapper svaret til domenemodellen, uten kontonummer og navn.
 * Første kontraktbrudd stopper mappingen og gir en [UtbetalingsoversiktMappingfeil].
 */
fun List<UtbetalingsoversiktDto>.tilRegistrerteUtbetalinger(): Either<UtbetalingsoversiktMappingfeil, List<RegistrertUtbetaling>> =
    either { map { it.tilRegistrertUtbetaling().bind() } }

private fun UtbetalingsoversiktDto.tilRegistrertUtbetaling(): Either<UtbetalingsoversiktMappingfeil, RegistrertUtbetaling> = either {
    RegistrertUtbetaling(
        utbetaltTil = krev(utbetaltTil, "utbetaltTil").tilAktør("utbetaltTil").bind(),
        utbetalingsmetode = krev(utbetalingsmetode, "utbetalingsmetode"),
        utbetalingsstatus = krev(utbetalingsstatus, "utbetalingsstatus"),
        posteringsdato = krev(posteringsdato, "posteringsdato"),
        forfallsdato = forfallsdato,
        utbetalingsdato = utbetalingsdato,
        nettobeløp = utbetalingNettobeloep,
        melding = utbetalingsmelding,
        ytelser = ytelseListe.map { it.tilRegistrertYtelse().bind() },
    )
}

private fun UtbetalingsoversiktDto.YtelseDto.tilRegistrertYtelse(): Either<UtbetalingsoversiktMappingfeil, RegistrertYtelse> = either {
    ensure(!ytelsesperiode.fom.isAfter(ytelsesperiode.tom)) { UtbetalingsoversiktMappingfeil.UgyldigPeriode("ytelsesperiode") }
    RegistrertYtelse(
        ytelsestype = ytelsestype,
        periode = Periode(fraOgMed = ytelsesperiode.fom, tilOgMed = ytelsesperiode.tom),
        nettobeløp = krev(ytelseNettobeloep, "ytelseNettobeloep"),
        rettighetshaver = krev(rettighetshaver, "rettighetshaver").tilAktør("rettighetshaver").bind(),
        skattesum = krev(skattsum, "skattsum"),
        trekksum = krev(trekksum, "trekksum"),
        komponentsum = krev(ytelseskomponentersum, "ytelseskomponentersum"),
        komponenter = ytelseskomponentListe.map {
            Ytelseskomponent(it.ytelseskomponenttype, it.satsbeloep, it.satstype, it.satsantall, it.ytelseskomponentbeloep)
        },
        trekk = trekkListe.map { Trekk(it.trekktype, it.trekkbeloep, it.kreditor) },
        skattetrekk = skattListe.map { Skattetrekk(it.skattebeloep) },
        bilagsnummer = bilagsnummer,
        refundertFor = refundertForOrg?.tilAktør("refundertForOrg")?.bind(),
    )
}

private fun UtbetalingsoversiktDto.AktoerDto.tilAktør(felt: String): Either<UtbetalingsoversiktMappingfeil, Aktør> = either {
    val aktørtype = krev(aktoertype?.trim()?.takeIf { it.isNotBlank() }, "$felt.aktoertype")
    val identverdi = krev(ident?.trim()?.takeIf { it.isNotBlank() }, "$felt.ident")
    when (aktørtype.uppercase()) {
        "PERSON" -> Aktør.Person(
            ensureNotNull(Fnr.tryFromString(identverdi)) { UtbetalingsoversiktMappingfeil.UgyldigPersonident(felt) },
        )

        "ORGANISASJON" -> Aktør.Organisasjon(Organisasjonsnummer(identverdi))

        "SAMHANDLER" -> Aktør.Samhandler(Samhandlerident(identverdi))

        else -> raise(UtbetalingsoversiktMappingfeil.UkjentAktørtype(felt))
    }
}

private fun <T : Any> Raise<UtbetalingsoversiktMappingfeil>.krev(verdi: T?, felt: String): T =
    ensureNotNull(verdi) { UtbetalingsoversiktMappingfeil.PåkrevdFeltMangler(felt) }
