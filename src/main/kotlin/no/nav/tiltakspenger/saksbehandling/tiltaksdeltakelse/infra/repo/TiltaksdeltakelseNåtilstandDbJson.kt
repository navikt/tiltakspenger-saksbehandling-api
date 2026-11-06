package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.ÅpenPeriode
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arrangør
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Deltakelsesomfang
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.EksternDeltakelseId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.GjennomføringId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kildestatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometårsak
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TeamTiltakstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.felles.lukketEllerÅpenPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilLokalKilde
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Nå-tilstanden fra tiltakshistorikk slik den lagres, i `tiltakshistorikk.verdi` og i oppgavegrunnlaget.
 * Den leses tilbake for å avgjøre om nå-tilstanden er endret siden forrige innslag i tiltakshistorikken, og for å vise tiltakshistorikken til saksbehandler.
 * Feltene settes eksplisitt, slik at formatet i databasen ikke endrer seg stille når libs-domenet gjør det.
 * Kildestatusen lagres slik kilden ga den, og ikke tolket til vår egen deltakerstatus.
 * Tolkningen kan reproduseres fra kildestatus, fraOgMed og tidspunktet nå-tilstanden ble innhentet, og en tolket statusendring står i endringen som lagres ved siden av.
 * Tittel og arrangør lagres ikke, siden de er stedsinformasjon.
 * Innslag som V276 konverterte fra et eldre format har i tillegg feltet `migrertFraTidligereFormat`, som sier hvilke verdier som kan være unøyaktige.
 * Feltet leses ikke tilbake, siden ukjente felter ignoreres.
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

/**
 * Gjenskaper nå-tilstanden fra det som ble lagret.
 * Tittel og arrangør lagres ikke, og leses derfor tilbake som ukjente.
 * Kildestatusen gjenskapes fra koden i kontrakten, og blir en ukjent kildeverdi dersom vi ikke lenger kjenner koden igjen.
 */
fun TiltaksdeltakelseNåtilstandDbJson.tilDomene(): Tiltaksdeltakelse.GirRett {
    val id = EksternDeltakelseId(eksternDeltakelseId)
    val kildestatus = kildestatus.tilDomene()
    val tiltakstype = TiltakstypeSomGirRett.valueOf(tiltakstype)
    val arrangør = Arrangør(hovedenhet = null, underenhet = null)
    val omfang = Deltakelsesomfang(
        deltakelsesprosent = deltakelsesprosent,
        dagerPerUke = dagerPerUke,
        deltidsprosentPåGjennomføring = deltidsprosentPåGjennomføring,
    )
    val gjennomføringId = gjennomføringId?.let { GjennomføringId(it) }
    return when (val periode = lukketEllerÅpenPeriode(fraOgMed?.let(LocalDate::parse), tilOgMed?.let(LocalDate::parse))) {
        is Periode -> Tiltaksdeltakelse.GirRett.MedPeriode(
            id = id,
            kildestatus = kildestatus,
            tiltakstype = tiltakstype,
            tiltakstypenavn = tiltakstypenavn,
            tiltakskodeFraKilden = tiltakskodeFraKilden,
            tittel = null,
            arrangør = arrangør,
            omfang = omfang,
            gjennomføringId = gjennomføringId,
            periode = periode,
        )

        is ÅpenPeriode -> Tiltaksdeltakelse.GirRett.UtenPeriode(
            id = id,
            kildestatus = kildestatus,
            tiltakstype = tiltakstype,
            tiltakstypenavn = tiltakstypenavn,
            tiltakskodeFraKilden = tiltakskodeFraKilden,
            tittel = null,
            arrangør = arrangør,
            omfang = omfang,
            gjennomføringId = gjennomføringId,
            periode = periode,
        )
    }
}

private fun TiltaksdeltakelseNåtilstandDbJson.KildestatusDbJson.tilDomene(): Kildestatus =
    when (kilde.toTiltakskilde()) {
        Tiltakskilde.Arena -> Arenastatus.Type.entries.find { it.name == kodeIKontrakten }
            ?.let { Arenastatus.Kjent(it) }
            ?: Arenastatus.Ukjent(kodeIKontrakten)

        Tiltakskilde.Komet -> {
            val årsak = årsak?.let { kode ->
                Kometstatus.Årsak.entries.find { it.name == kode }?.let { Kometårsak.Kjent(it) } ?: Kometårsak.Ukjent(kode)
            }
            val opprettet = LocalDateTime.parse(checkNotNull(opprettet) { "En lagret Komet-status mangler opprettet-tidspunkt" })
            Kometstatus.Type.entries.find { it.name == kodeIKontrakten }
                ?.let { Kometstatus.Kjent(type = it, årsak = årsak, opprettet = opprettet) }
                ?: Kometstatus.Ukjent(kodeIKontrakten = kodeIKontrakten, årsak = årsak, opprettet = opprettet)
        }

        Tiltakskilde.TeamTiltak -> TeamTiltakstatus.Type.entries.find { it.name == kodeIKontrakten }
            ?.let { TeamTiltakstatus.Kjent(it) }
            ?: TeamTiltakstatus.Ukjent(kodeIKontrakten)
    }
