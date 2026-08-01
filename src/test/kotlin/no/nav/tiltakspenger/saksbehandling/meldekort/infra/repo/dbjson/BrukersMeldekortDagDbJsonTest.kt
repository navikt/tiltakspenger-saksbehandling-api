package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.InnmeldtStatus
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Statusene er det brukeren melder inn per dag, og lagres i jsonb-en på brukers meldekort med én gren per status i hver retning.
 * Ett meldekort har fjorten dager, så det ville tatt flere innsendinger å nå alle grenene gjennom prodstien — og noen av statusene setter brukeren aldri selv.
 * Mappingen rører ikke postgres.
 *
 * Testen pinner **den faktiske json-en**, ikke bare rundturen.
 * En ren rundtur er symmetrisk og ville passert selv om en status ble omdøpt i begge `when`-ene samtidig, og da er meldekortene som allerede ligger i databasen ulesbare uten at noe slår ut.
 */
class BrukersMeldekortDagDbJsonTest {

    private val alleDager = InnmeldtStatus.entries.mapIndexed { indeks, status ->
        BrukersMeldekort.BrukersMeldekortDag(dato = (indeks + 1).januar(2025), status = status)
    }

    @Test
    fun `dagene lagres med sitt avtalte navn og dato`() {
        alleDager.toDbJson() shouldBe
            """[{"dato":"2025-01-01","status":"DELTATT_UTEN_LØNN_I_TILTAKET"},""" +
            """{"dato":"2025-01-02","status":"DELTATT_MED_LØNN_I_TILTAKET"},""" +
            """{"dato":"2025-01-03","status":"FRAVÆR_SYK"},""" +
            """{"dato":"2025-01-04","status":"FRAVÆR_SYKT_BARN"},""" +
            """{"dato":"2025-01-05","status":"FRAVÆR_GODKJENT_AV_NAV"},""" +
            """{"dato":"2025-01-06","status":"FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU"},""" +
            """{"dato":"2025-01-07","status":"FRAVÆR_ANNET"},""" +
            """{"dato":"2025-01-08","status":"IKKE_BESVART"},""" +
            """{"dato":"2025-01-09","status":"IKKE_TILTAKSDAG"},""" +
            """{"dato":"2025-01-10","status":"IKKE_RETT_TIL_TILTAKSPENGER"}]"""
    }

    @Test
    fun `dagene leses tilbake fra lagret json`() {
        alleDager.toDbJson().toMeldekortDager() shouldBe alleDager
    }
}
