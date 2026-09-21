package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.personopplysning.Organisasjonsnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Samhandlerident

/**
 * Den som fikk pengene, den som hadde retten til ytelsen, eller organisasjonen en refusjon gjelder.
 * Kilden [utleder aktørtypen av identen](https://github.com/navikt/sokos-utbetaldata/blob/main/src/main/kotlin/no/nav/sokos/utbetaldata/api/utbetaling/MapResponseV2.kt).
 * Feltet i oppdragsmeldingen er [`utbetalesTilId`](https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/oppdrag_xml.md).
 * Samhandleridenten kommer fra samhandlerregisteret TSS.
 * Behandlingen dokumenteres i [Behandlingskatalogen](https://behandlingskatalog.ansatt.nav.no/).
 */
sealed interface Aktør {
    data class Person(
        val fnr: Fnr,
    ) : Aktør

    data class Organisasjon(
        val organisasjonsnummer: Organisasjonsnummer,
    ) : Aktør

    data class Samhandler(
        val ident: Samhandlerident,
    ) : Aktør
}
