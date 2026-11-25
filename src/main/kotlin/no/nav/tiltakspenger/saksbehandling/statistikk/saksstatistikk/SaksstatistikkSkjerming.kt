package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

/** Verdien Team Sak vil ha i stedet for identer og enhet når brukeren har strengt fortrolig adresse og behandlingen ikke ligger hos Vikafossen. */
const val SKJERMET = "SKJERMET"

private const val OPPRETTET_AV_SYSTEM = "system"
private const val NAV_TILTAK_OSLO = "0387"

/** Enhetsnummeret til Nav Vikafossen, som behandler sakene der brukeren har strengt fortrolig adresse. */
const val VIKAFOSSEN = "2103"

/**
 * Oppslagene saksstatistikken trenger for å skjerme behandlinger der brukeren har strengt fortrolig adresse (kode 6).
 * Kode 7 skal ikke skjermes.
 */
interface Skjermingsoppslag {
    suspend fun gjelderKode6(fnr: Fnr): Boolean

    /** Saksbehandlerne med rollen jobber i Vikafossen og bruker fiktive identer. */
    suspend fun harRolleForStrengtFortroligAdresse(navIdent: String): Boolean
}

/** Feltene i [SaksstatistikkDTO] som sier hvem som har ansvaret for behandlingen, og som skjermes ved strengt fortrolig adresse. */
data class SaksstatistikkAnsvar(
    val opprettetAv: String,
    val saksbehandler: String?,
    val ansvarligBeslutter: String?,
    val ansvarligenhet: String,
)

/**
 * Skjermer ansvarsfeltene etter Team Saks krav når brukeren har strengt fortrolig adresse.
 * Behandlingen regnes å ligge hos Vikafossen når saksbehandleren har rollen for strengt fortrolig adresse.
 * Da sendes saksbehandlerens fiktive ident og Vikafossens enhet 2103, ellers `SKJERMET`.
 * Det gir `SKJERMET` også i tiden før Vikafossen har fått behandlingen, slik Team Sak ber om.
 * Opprettet av og ansvarlig beslutter er alltid `SKJERMET`.
 * Felter uten verdi forblir tomme, siden de ikke avslører noe.
 */
suspend fun Skjermingsoppslag.ansvar(
    fnr: Fnr,
    saksbehandler: String?,
    ansvarligBeslutter: String?,
): SaksstatistikkAnsvar {
    if (!gjelderKode6(fnr)) {
        return SaksstatistikkAnsvar(
            opprettetAv = OPPRETTET_AV_SYSTEM,
            saksbehandler = saksbehandler,
            ansvarligBeslutter = ansvarligBeslutter,
            ansvarligenhet = NAV_TILTAK_OSLO,
        )
    }
    val liggerHosVikafossen = saksbehandler != null && harRolleForStrengtFortroligAdresse(saksbehandler)
    return SaksstatistikkAnsvar(
        opprettetAv = SKJERMET,
        saksbehandler = if (liggerHosVikafossen) saksbehandler else saksbehandler?.let { SKJERMET },
        ansvarligBeslutter = ansvarligBeslutter?.let { SKJERMET },
        ansvarligenhet = if (liggerHosVikafossen) VIKAFOSSEN else SKJERMET,
    )
}
