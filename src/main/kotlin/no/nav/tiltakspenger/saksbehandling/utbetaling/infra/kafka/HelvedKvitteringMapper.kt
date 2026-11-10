package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeBehandleUtbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeBehandleUtbetalingskvittering.UgyldigInnhold
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingskvittering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsreferanse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus

/**
 * Tolker statusmeldingen fra Hel Ved på `helved.status.v1`.
 * Statusformatet er definert i [Hel Veds statusmodell](https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/Status.kt).
 */
fun tilUtbetalingsstatus(value: String): Either<KunneIkkeBehandleUtbetalingskvittering, Utbetalingskvittering> = either {
    val melding = Either.catch { lesTre(value) }
        .mapLeft { KunneIkkeBehandleUtbetalingskvittering.KunneIkkeTolkeInnhold(it) }.bind()
    ensure(melding.isObject) { UgyldigInnhold }
    val simulering = melding.path("simulering")
    ensure(simulering.isMissingNode || simulering.isBoolean) { UgyldigInnhold }
    val detaljer = melding.path("detaljer")
    ensure(detaljer.isMissingNode || detaljer.isNull || detaljer.isObject) { UgyldigInnhold }
    val referanser = if (detaljer.isObject) {
        val linjer = detaljer.path("linjer")
        ensure(linjer.isArray) { UgyldigInnhold }
        (0 until linjer.size()).map { index ->
            val linje = ensureNotNull(linjer.get(index)) { UgyldigInnhold }
            ensure(linje.isObject) { UgyldigInnhold }
            val behandlingId = linje.path("behandlingId")
            ensure(behandlingId.isString && behandlingId.asText().length == 15) { UgyldigInnhold }
            Utbetalingsreferanse(behandlingId.asString())
        }.toSet()
    } else {
        emptySet()
    }
    if (simulering.asBoolean(false)) return@either Utbetalingskvittering.Simulering(referanser)
    val status = melding.path("status")
    ensure(status.isString) { UgyldigInnhold }
    val utbetalingsstatus = when (status.asString()) {
        // MOTTATT: Hel Ved har tatt imot utbetalingen, men ikke sendt den til økonomisystemet ennå.
        "MOTTATT" -> Utbetalingsstatus.IkkePåbegynt

        "HOS_OPPDRAG" -> Utbetalingsstatus.SendtTilOppdrag

        "OK" -> when {
            detaljer.isObject -> Utbetalingsstatus.Ok
            else -> Utbetalingsstatus.OkUtenUtbetaling
        }

        "FEILET" -> Utbetalingsstatus.FeiletMotOppdrag

        else -> raise(UgyldigInnhold)
    }
    Utbetalingskvittering.Status(utbetalingsstatus, referanser)
}
