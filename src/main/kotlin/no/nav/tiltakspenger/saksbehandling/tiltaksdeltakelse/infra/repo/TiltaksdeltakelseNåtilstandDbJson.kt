package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kildestatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilLokalKilde

/**
 * Nå-tilstanden fra tiltakshistorikk slik den lagres for sporbarhet, i `tiltaksdeltaker_endring.verdi` og i oppgavegrunnlaget.
 * Den skrives kun, og leses aldri tilbake til domenet.
 * Feltene settes eksplisitt, slik at formatet i databasen ikke endrer seg stille når libs-domenet gjør det.
 * Kildestatusen lagres slik kilden ga den, og ikke tolket til vår egen deltakerstatus.
 * Tolkningen kan reproduseres fra kildestatus, fraOgMed og tidspunktet raden ble lagret, og en tolket statusendring står i endringen som lagres ved siden av.
 * Tittel og arrangør lagres ikke, siden de er stedsinformasjon.
 */
data class TiltaksdeltakelseNåtilstandDbJson(
    val eksternDeltakelseId: String,
    val gjennomføringId: String?,
    val tiltakstype: String,
    val tiltakstypenavn: String,
    val tiltakskodeFraKilden: String,
    val fraOgMed: String?,
    val tilOgMed: String?,
    val kildestatus: KildestatusDbJson,
    val deltakelsesprosent: Float?,
    val dagerPerUke: Float?,
    val deltidsprosentPåGjennomføring: Float?,
) {
    /**
     * [kodeIKontrakten] er statuskoden slik den står i kontrakten, også når vi ikke kjenner den igjen.
     * [årsak] og [opprettet] finnes bare for Komet.
     */
    data class KildestatusDbJson(
        val kilde: String,
        val kodeIKontrakten: String,
        val årsak: String?,
        val opprettet: String?,
    )
}

fun Tiltaksdeltakelse.GirRett.tilNåtilstandDbJson() = TiltaksdeltakelseNåtilstandDbJson(
    eksternDeltakelseId = id.verdi,
    gjennomføringId = gjennomføringId?.verdi,
    tiltakstype = tiltakstype.name,
    tiltakstypenavn = tiltakstypenavn,
    tiltakskodeFraKilden = tiltakskodeFraKilden,
    fraOgMed = fraOgMed?.toString(),
    tilOgMed = tilOgMed?.toString(),
    kildestatus = kildestatus.tilDbJson(),
    deltakelsesprosent = omfang.deltakelsesprosent,
    dagerPerUke = omfang.dagerPerUke,
    deltidsprosentPåGjennomføring = omfang.deltidsprosentPåGjennomføring,
)

private fun Kildestatus.tilDbJson(): TiltaksdeltakelseNåtilstandDbJson.KildestatusDbJson {
    val kometstatus = this as? Kometstatus
    return TiltaksdeltakelseNåtilstandDbJson.KildestatusDbJson(
        kilde = kilde.tilLokalKilde().toDb(),
        kodeIKontrakten = kodeIKontrakten,
        årsak = kometstatus?.årsak?.kodeIKontrakten,
        opprettet = kometstatus?.run { opprettet.toString() },
    )
}
