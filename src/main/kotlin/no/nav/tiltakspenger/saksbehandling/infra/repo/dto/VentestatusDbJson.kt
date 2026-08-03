package no.nav.tiltakspenger.saksbehandling.infra.repo.dto

import kotliquery.Row
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus

private data class VentestatusDbJson(
    val ventestatusHendelser: List<VentestatusHendelseDbJson>,
)

fun String.toVentestatus(): Ventestatus =
    Ventestatus(
        ventestatusHendelser = deserialize<VentestatusDbJson>(this).ventestatusHendelser.map { it.toSattPåVentBegrunnelse() },
    )

/**
 * Leser `ventestatus`-kolonnen, som er nullable i alle tre tabellene som har den.
 * Rader som er eldre enn kolonnen har NULL, og skal leses som en tom ventestatus.
 *
 * Skrevet som en eksplisitt `if` framfor `stringOrNull(...)?.toVentestatus() ?: Ventestatus()`.
 * Den kjeden gir en gren Kover teller som ubesøkt selv når begge kolonnetilstandene er dekket av tester — alle instruksjonene kjøres, men én av utfallskombinasjonene kan ikke oppstå.
 */
fun Row.ventestatus(kolonne: String = "ventestatus"): Ventestatus {
    val json = stringOrNull(kolonne)
    // TODO jah: Uten å ha sett på dette: Er det slik at nye inserts inserter [] istedenfor null, kan vi vel bare migrere alle som er null og sette kolonnene som non-null?
    // Eksplisitt if er bevisst: elvis-varianten kompilerer til en ekstra null-sjekk som aldri kan slå til, og den ville stått som en permanent udekket gren i grendekningsgaten.
    @Suppress("IfThenToElvis")
    return if (json == null) Ventestatus() else json.toVentestatus()
}

fun Ventestatus.toDbJson(): String = serialize(
    VentestatusDbJson(
        ventestatusHendelser = ventestatusHendelser.map { it.toDbJson() },
    ),
)
