package no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.dokument.KunneIkkeGenererePdf
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import java.time.Clock
import java.time.LocalDate

typealias GenererKlageForvaltningsmelding = suspend (
    saksnummer: Saksnummer,
    fnr: Fnr,
    saksbehandlerNavIdent: String?,
    tilleggstekst: Brevtekster,
    forhåndsvisning: Boolean,
    innsendingsdato: LocalDate,
    brevdato: LocalDate,
) -> Either<KunneIkkeGenererePdf, PdfOgJson>

fun Klagebehandling.sendForvaltningsmelding(
    kommando: ForvaltningsmeldingKommando,
    clock: Clock,
): Either<KanIkkeSendeForvaltningsmelding, Klagebehandling> {
    krevSaksbehandlerRolle(kommando.saksbehandler)
    if (forvaltningsmelding != null) {
        return KanIkkeSendeForvaltningsmelding.ForvaltningsmeldingErAlleredeSendt.left()
    }
    kanOppdatereIDenneStatusen(null).onLeft {
        return KanIkkeSendeForvaltningsmelding.KanIkkeOppdateres(it).left()
    }
    if (!erSaksbehandlerPåBehandlingen(kommando.saksbehandler)) {
        return KanIkkeSendeForvaltningsmelding.SaksbehandlerMismatch(
            forventetSaksbehandler = this.saksbehandler,
            faktiskSaksbehandler = kommando.saksbehandler.navIdent,
        ).left()
    }
    if (kommando.brevtekster.isEmpty()) {
        return KanIkkeSendeForvaltningsmelding.ManglerTekst.left()
    }
    val nå = nå(clock)
    return this.copy(
        forvaltningsmelding = KlageForvaltningsmelding.ny(
            brevtekst = kommando.brevtekster,
            saksbehandler = kommando.saksbehandler.navIdent,
            sendtTidspunkt = nå,
        ),
        sistEndret = nå,
    ).right()
}

suspend fun Klagebehandling.forhåndsvisForvaltningsmelding(
    kommando: ForvaltningsmeldingKommando,
    generer: GenererKlageForvaltningsmelding,
    clock: Clock,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    val sendt = forvaltningsmelding
    if (sendt != null) {
        return generer(
            saksnummer,
            fnr,
            sendt.saksbehandler,
            sendt.brevtekst,
            false,
            formkrav.innsendingsdato,
            sendt.brevdato ?: LocalDate.now(clock),
        )
    }
    return generer(
        saksnummer,
        fnr,
        saksbehandler,
        kommando.brevtekster,
        true,
        formkrav.innsendingsdato,
        LocalDate.now(clock),
    )
}

/**
 * Kun til bruk av systemet når den endelige forvaltningsmeldingen skal genereres og journalføres.
 */
suspend fun Klagebehandling.genererForvaltningsmelding(
    generer: GenererKlageForvaltningsmelding,
    brevdato: LocalDate,
): Either<KunneIkkeGenererePdf, PdfOgJson> {
    val sendt = requireNotNull(forvaltningsmelding) {
        "Kan ikke generere forvaltningsmelding som ikke er sendt. sakId=$sakId, saksnummer=$saksnummer, klagebehandlingId=$id"
    }
    return generer(
        saksnummer,
        fnr,
        sendt.saksbehandler,
        sendt.brevtekst,
        false,
        formkrav.innsendingsdato,
        brevdato,
    )
}
