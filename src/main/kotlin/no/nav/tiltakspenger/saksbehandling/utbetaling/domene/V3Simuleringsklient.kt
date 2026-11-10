package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor

interface V3Simuleringsklient {
    suspend fun simuler(
        sakId: SakId,
        saksnummer: Saksnummer,
        behandlingId: Ulid,
        fnr: Fnr,
        beregning: Beregning,
        forrigeUtbetalingJson: String?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        saksbehandler: String,
        brukersNavkontor: Navkontor,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata>
}
