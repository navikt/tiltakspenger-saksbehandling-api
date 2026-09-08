package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRettDTO
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kildestatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TeamTiltakstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakshistorikk
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Avbrutt
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Feilregistrert
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Fullført
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.HarSluttet
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.IkkeAktuell
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.PåbegyntRegistrering
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.SøktInn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Venteliste
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.VenterPåOppstart
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus.Vurderes
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import java.time.Clock
import java.time.LocalDate

/**
 * Filtrerer og mapper til deltakelser som kan gi rett til tiltakspenger.
 * Deltakelser det er søkt tiltakspenger for skal ikke filtreres bort.
 *
 * Utvalgs- og statusreglene viderefører semantikken fra den avviklede kjeden via `tiltakspenger-tiltak`:
 * kun tiltakstyper som gir rett ([no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelser.girRett]), og kun deltakelser som er søkt for, har datoer, eller venter på oppstart.
 *
 * Et bevisst avvik fra tidligere oppførsel: en tiltakskode vi ikke kjenner felte før hele oppslaget ([IllegalStateException] i mappingen).
 * Nå blir slike rader [Tiltaksdeltakelse.UkjentTiltakstype] i libs-domenet og faller utenfor `girRett` — de varsles av klienten i stedet for å krasje.
 * Det samme gjelder rader med ukjent kildestatus: de kan ikke tolkes til [TiltakDeltakerstatus] og utelates fra uttrekket.
 */
fun Tiltakshistorikk.tilTiltaksdeltakelserFraRegister(
    tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
    clock: Clock,
): TiltaksdeltakelserFraRegister {
    val søktFor = tiltaksdeltakelserDetErSøktTiltakspengerFor.eksterneIder.toSet()
    return TiltaksdeltakelserFraRegister(
        deltakelser.girRett.mapNotNull { deltakelse ->
            val status = deltakelse.kildestatus.tilTiltakDeltakerstatus(deltakelse.fraOgMed, clock)
                ?: return@mapNotNull null
            if (!erRelevant(deltakelse, status, søktFor)) return@mapNotNull null
            TiltaksdeltakelseFraRegister(
                eksternDeltakelseId = deltakelse.id.verdi,
                gjennomføringId = deltakelse.gjennomføringId?.verdi,
                typeNavn = deltakelse.tiltakstypenavn,
                typeKode = deltakelse.tiltakstype.tilDTO(),
                rettPåTiltakspenger = true,
                deltakelseFraOgMed = deltakelse.fraOgMed,
                deltakelseTilOgMed = deltakelse.tilOgMed,
                deltakelseStatus = status,
                deltakelseProsent = deltakelse.omfang.deltakelsesprosent,
                antallDagerPerUke = deltakelse.omfang.dagerPerUke,
                kilde = deltakelse.kildestatus.kilde.tilLokalKilde(),
                deltidsprosentGjennomforing = deltakelse.omfang.deltidsprosentPåGjennomføring?.toDouble(),
            )
        },
    )
}

/**
 * Samme utvalg som [tilTiltaksdeltakelserFraRegister], men med visningsnavn i stedet for full saksopplysning.
 * Visningsnavnet ([Tiltaksdeltakelse.tittel]) inneholder arrangøren, og er derfor stedsinformasjon — ved adressebeskyttelse vises kun tiltakstypenavnet.
 * Faller tilbake på tiltakstypenavnet også når kilden ikke ga noen tittel.
 */
fun Tiltakshistorikk.tilTiltaksdeltakelserMedArrangørnavn(
    harAdressebeskyttelse: Boolean,
    clock: Clock,
): List<TiltaksdeltakelseMedArrangørnavn> {
    return deltakelser.girRett.mapNotNull { deltakelse ->
        val status = deltakelse.kildestatus.tilTiltakDeltakerstatus(deltakelse.fraOgMed, clock)
            ?: return@mapNotNull null
        if (!erRelevant(deltakelse, status, emptySet())) return@mapNotNull null
        TiltaksdeltakelseMedArrangørnavn(
            eksternDeltakelseId = deltakelse.id.verdi,
            typeNavn = deltakelse.tiltakstypenavn,
            typeKode = deltakelse.tiltakstype.tilDTO(),
            deltakelseFraOgMed = deltakelse.fraOgMed,
            deltakelseTilOgMed = deltakelse.tilOgMed,
            visningsnavn = if (harAdressebeskyttelse) {
                deltakelse.tiltakstypenavn
            } else {
                deltakelse.tittel?.verdi ?: deltakelse.tiltakstypenavn
            },
        )
    }
}

/**
 * Deltakelser det er søkt for skal alltid være med.
 * Ellers kreves minst én dato, eller statusen [VenterPåOppstart] — den eneste statusen der datoer kan mangle og som kan være relevant ift tiltakspenger.
 */
private fun erRelevant(
    deltakelse: Tiltaksdeltakelse.GirRett,
    status: TiltakDeltakerstatus,
    søktFor: Set<String>,
): Boolean {
    if (deltakelse.id.verdi in søktFor) return true
    if (deltakelse.fraOgMed != null || deltakelse.tilOgMed != null) return true
    return status == VenterPåOppstart
}

/**
 * Kildens status oversatt til vår egen statusmodell.
 * Returnerer `null` for [Kildestatus.Ukjent] — en status vi ikke kjenner kan ikke tolkes, og deltakelsen utelates fra uttrekket (varsles av klienten).
 *
 * Tabellen viderefører mappingen fra `tiltakspenger-tiltak` sin `toDeltakerStatusDTO` én til én, inkludert Arena `IKKE_MOTT` → [Avbrutt].
 * Det er et bevisst avvik fra libs sin egen [no.nav.tiltakspenger.libs.tiltaksdeltakelse.Deltakerstatus], der fag har avklart at «ikke møtt» ikke er deltakelse — den overstyringen er et eget løft, og skal ikke gjøres i denne mappingen.
 */
fun Kildestatus.tilTiltakDeltakerstatus(
    fraOgMed: LocalDate?,
    clock: Clock,
): TiltakDeltakerstatus? =
    when (this) {
        is Arenastatus.Kjent -> type.tilTiltakDeltakerstatus(fraOgMed, clock)
        is Kometstatus.Kjent -> type.tilTiltakDeltakerstatus()
        is TeamTiltakstatus.Kjent -> type.tilTiltakDeltakerstatus()
        is Kildestatus.Ukjent -> null
    }

private fun Arenastatus.Type.tilTiltakDeltakerstatus(
    fraOgMed: LocalDate?,
    clock: Clock,
): TiltakDeltakerstatus {
    // Arena skiller ikke mellom «tildelt plass» og «deltar»; startdatoen er det eneste skillet vi har.
    val startdatoErFremITid = fraOgMed == null || fraOgMed.isAfter(LocalDate.now(clock))
    return when (this) {
        Arenastatus.Type.DELTAKELSE_AVBRUTT -> Avbrutt
        Arenastatus.Type.FULLFORT -> Fullført
        Arenastatus.Type.GJENNOMFORES -> if (startdatoErFremITid) VenterPåOppstart else Deltar
        Arenastatus.Type.GJENNOMFORING_AVBRUTT -> Avbrutt
        Arenastatus.Type.IKKE_MOTT -> Avbrutt
        Arenastatus.Type.TAKKET_JA_TIL_TILBUD -> Deltar
        Arenastatus.Type.TILBUD -> VenterPåOppstart
        Arenastatus.Type.AKTUELL -> SøktInn
        Arenastatus.Type.AVSLAG -> IkkeAktuell
        Arenastatus.Type.GJENNOMFORING_AVLYST -> IkkeAktuell
        Arenastatus.Type.IKKE_AKTUELL -> IkkeAktuell
        Arenastatus.Type.INFORMASJONSMOTE -> Venteliste
        Arenastatus.Type.TAKKET_NEI_TIL_TILBUD -> IkkeAktuell
        Arenastatus.Type.VENTELISTE -> Venteliste
        Arenastatus.Type.FEILREGISTRERT -> Feilregistrert
    }
}

private fun Kometstatus.Type.tilTiltakDeltakerstatus(): TiltakDeltakerstatus =
    when (this) {
        Kometstatus.Type.UTKAST_TIL_PAMELDING,
        Kometstatus.Type.PABEGYNT_REGISTRERING,
        -> PåbegyntRegistrering

        Kometstatus.Type.AVBRUTT_UTKAST,
        Kometstatus.Type.IKKE_AKTUELL,
        -> IkkeAktuell

        Kometstatus.Type.VENTER_PA_OPPSTART -> VenterPåOppstart

        Kometstatus.Type.DELTAR -> Deltar

        Kometstatus.Type.HAR_SLUTTET -> HarSluttet

        Kometstatus.Type.FEILREGISTRERT -> Feilregistrert

        Kometstatus.Type.SOKT_INN -> SøktInn

        Kometstatus.Type.VURDERES -> Vurderes

        Kometstatus.Type.VENTELISTE -> Venteliste

        Kometstatus.Type.AVBRUTT -> Avbrutt

        Kometstatus.Type.FULLFORT -> Fullført

        // Paritet med tiltakspenger-tiltak: en kladd er ikke delt hos kilden og skal aldri nå oss — om den likevel gjør det, er kontrakten brutt og oppslaget skal feile høylytt.
        Kometstatus.Type.KLADD -> throw IllegalStateException("Kan ikke mappe kladd til intern status")
    }

private fun TeamTiltakstatus.Type.tilTiltakDeltakerstatus(): TiltakDeltakerstatus =
    when (this) {
        TeamTiltakstatus.Type.PAABEGYNT -> PåbegyntRegistrering
        TeamTiltakstatus.Type.MANGLER_GODKJENNING -> SøktInn
        TeamTiltakstatus.Type.KLAR_FOR_OPPSTART -> VenterPåOppstart
        TeamTiltakstatus.Type.GJENNOMFORES -> Deltar
        TeamTiltakstatus.Type.AVSLUTTET -> HarSluttet
        TeamTiltakstatus.Type.AVBRUTT -> Avbrutt
        TeamTiltakstatus.Type.ANNULLERT -> IkkeAktuell
    }

private fun TiltakstypeSomGirRett.tilDTO(): TiltakstypeSomGirRettDTO = TiltakstypeSomGirRettDTO.valueOf(name)

private fun no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakskilde.tilLokalKilde(): Tiltakskilde =
    when (this) {
        no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakskilde.Arena -> Tiltakskilde.Arena
        no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakskilde.Komet -> Tiltakskilde.Komet
        no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltakskilde.TeamTiltak -> Tiltakskilde.TeamTiltak
    }
