package no.nav.tiltakspenger.vedtak.clients.datadeling

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.vedtak.Vedtakstype
import java.time.LocalDate

private data class DatadelingVedtakJson(
    val vedtakId: String,
    val sakId: String,
    val saksnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val antallDagerPerMeldeperiode: Int,
    val rettighet: String,
    val fnr: String,
    val opprettet: String,
)

fun Rammevedtak.toDatadelingJson(): String {
    return DatadelingVedtakJson(
        vedtakId = this.id.toString(),
        sakId = this.sakId.toString(),
        saksnummer = this.saksnummer.verdi,
        fom = periode.fraOgMed,
        tom = periode.tilOgMed,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        rettighet = when (this.vedtaksType) {
            Vedtakstype.INNVILGELSE -> "TILTAKSPENGER"
            Vedtakstype.STANS -> "INGENTING"
        },
        fnr = fnr.verdi,
        opprettet = opprettet.toString(),
    ).let { serialize(it) }
}
