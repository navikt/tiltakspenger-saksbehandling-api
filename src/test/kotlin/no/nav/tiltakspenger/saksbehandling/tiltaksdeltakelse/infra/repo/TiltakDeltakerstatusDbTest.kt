package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Statusene eies av Komet og Arena, ikke av oss, så vi må kunne lagre og lese tilbake alle sammen uavhengig av hvilke prodstiene våre tilfeldigvis produserer i dag.
 * Å konstruere elleve deltakelser med hver sin status ville kostet mye for en mapping som ikke rører postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om en status ble omdøpt i begge `when`-ene samtidig — og da er dataen som allerede ligger i `tiltaksdeltaker`-tabellen ulesbar uten at noe slår ut.
 */
class TiltakDeltakerstatusDbTest {

    @Test
    fun `statusene lagres med sitt avtalte navn`() {
        TiltakDeltakerstatus.entries.associateWith { it.toDb() } shouldBe mapOf(
            TiltakDeltakerstatus.VenterPåOppstart to "VenterPåOppstart",
            TiltakDeltakerstatus.Deltar to "Deltar",
            TiltakDeltakerstatus.HarSluttet to "HarSluttet",
            TiltakDeltakerstatus.Avbrutt to "Avbrutt",
            TiltakDeltakerstatus.Fullført to "Fullført",
            TiltakDeltakerstatus.IkkeAktuell to "IkkeAktuell",
            TiltakDeltakerstatus.Feilregistrert to "Feilregistrert",
            TiltakDeltakerstatus.PåbegyntRegistrering to "PåbegyntRegistrering",
            TiltakDeltakerstatus.SøktInn to "SøktInn",
            TiltakDeltakerstatus.Venteliste to "Venteliste",
            TiltakDeltakerstatus.Vurderes to "Vurderes",
        )
    }

    @Test
    fun `statusene leses tilbake fra lagret verdi`() {
        TiltakDeltakerstatus.entries.forEach {
            it.toDb().toTiltakDeltakerstatus() shouldBe it
        }
    }
}
