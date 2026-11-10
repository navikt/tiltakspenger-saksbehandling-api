package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.tilUtbetalingerDTO
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.Satstype
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.StønadTypeTiltakspenger
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.iverksett.StønadsdataTiltakspengerV2Dto
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.iverksett.UtbetalingV2Dto
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Utbetalingen vi produserer på `tpts.utbetaling-v1` til Hel Ved (versjon 3).
 * Speiler `TpUtbetaling` i `navikt/helved-utbetaling` (`models/main/models/TpDto.kt`).
 * Meldingen inneholder alle meldeperiodene i saken som skal utbetales: en meldeperiode som mangler, blir opphørt hos Hel Ved.
 */
data class HelvedUtbetalingDTO(
    val sakId: String,
    val behandlingId: String,
    val dryrun: Boolean,
    val personident: String,
    val vedtakstidspunkt: LocalDateTime,
    val perioder: List<HelvedPeriodeDTO>,
    val saksbehandler: String?,
    val beslutter: String?,
) {
    override fun toString(): String = "HelvedUtbetalingDTO(sakId=$sakId, behandlingId=$behandlingId, personident=*****)"
}

/**
 * Beløpet gjelder per ukedag i perioden fra og med [fom] til og med [tom].
 * @param meldeperiode Id-en til meldeperiodekjeden.
 * Hel Ved kjeder utbetalingene på den sammen med [stønad] og [barnetillegg].
 * @param betalendeEnhet Enhetsnummeret til Nav-kontoret.
 * @param stønad Navnet på en verdi i [StønadTypeTiltakspenger].
 */
data class HelvedPeriodeDTO(
    val meldeperiode: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val betalendeEnhet: String,
    val barnetillegg: Boolean,
    val beløp: UInt,
    val stønad: String,
)

/**
 * @param forrigeUtbetalingJson Forrige utbetaling vi sendte til Hel Ved, enten med versjon 2 eller versjon 3.
 * Meldeperiodene fra den som ikke er beregnet på nytt, sendes med uendret.
 */
fun VedtattUtbetaling.tilHelvedUtbetalingJson(forrigeUtbetalingJson: String?): String = serialize(
    beregning.tilHelvedUtbetalingDTO(
        saksnummer = saksnummer,
        behandlingId = id,
        fnr = fnr,
        vedtakstidspunkt = opprettet,
        brukersNavkontor = brukerNavkontor,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        forrigeUtbetalingJson = forrigeUtbetalingJson,
        dryrun = false,
    ),
)

fun Beregning.tilHelvedUtbetalingDTO(
    saksnummer: Saksnummer,
    behandlingId: Ulid,
    fnr: Fnr,
    vedtakstidspunkt: LocalDateTime,
    brukersNavkontor: Navkontor,
    saksbehandler: String?,
    beslutter: String?,
    forrigeUtbetalingJson: String?,
    dryrun: Boolean,
): HelvedUtbetalingDTO = HelvedUtbetalingDTO(
    sakId = saksnummer.toString(),
    behandlingId = behandlingId.uuidPart(),
    dryrun = dryrun,
    personident = fnr.verdi,
    vedtakstidspunkt = vedtakstidspunkt,
    perioder = tilUtbetalingerDTO(
        brukersNavkontor = brukersNavkontor,
        forrigeUtbetalingJson = forrigeUtbetalingJson,
    ).map { it.tilHelvedPeriodeDTO() },
    saksbehandler = saksbehandler,
    beslutter = beslutter,
)

private fun UtbetalingV2Dto.tilHelvedPeriodeDTO(): HelvedPeriodeDTO {
    require(satstype == Satstype.DAGLIG) {
        "Hel Ved støtter bare satstype DAGLIG for tiltakspenger, men fikk $satstype"
    }
    return HelvedPeriodeDTO(
        meldeperiode = stønadsdata.meldekortId,
        fom = fraOgMedDato,
        tom = tilOgMedDato,
        betalendeEnhet = stønadsdata.brukersNavKontor,
        barnetillegg = stønadsdata.barnetillegg,
        beløp = beløp,
        stønad = stønadsdata.stønadstype.name,
    )
}

fun HelvedPeriodeDTO.tilUtbetalingV2Dto(): UtbetalingV2Dto = UtbetalingV2Dto(
    beløp = beløp,
    satstype = Satstype.DAGLIG,
    fraOgMedDato = fom,
    tilOgMedDato = tom,
    stønadsdata = StønadsdataTiltakspengerV2Dto(
        stønadstype = StønadTypeTiltakspenger.valueOf(stønad),
        barnetillegg = barnetillegg,
        brukersNavKontor = betalendeEnhet,
        meldekortId = meldeperiode,
    ),
)
