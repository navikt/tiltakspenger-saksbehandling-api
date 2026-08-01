package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagehjemmelDb.Companion.toDb
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Klagehjemlene er lovhjemler, og listen er lang: én gren per hjemmel i hver retning.
 * En klagebehandling velger én eller noen få, så det ville tatt tretti klageflyter å nå alle grenene gjennom prodstien — for en mapping som ikke rører postgres.
 *
 * Hjemlene lagres i resultat-jsonb-en på klagebehandlingen ved sitt enum-navn.
 * Testen pinner navnet for hver hjemmel, ikke bare rundturen: en ren rundtur er symmetrisk og ville passert selv om en hjemmel ble omdøpt i begge `when`-ene samtidig, og da er klagene som allerede ligger i databasen ulesbare uten at noe slår ut.
 */
class KlagehjemmelDbTest {

    @Test
    fun `hver klagehjemmel lagres med sitt eget navn, og leses tilbake til samme hjemmel`() {
        // Navnet er kontrakten mot lagret data; at domenehjemmelen mapper tilbake til nøyaktig samme db-variant er det rundturen sikrer.
        KlagehjemmelDb.entries.associateWith { it.toDomain().toDb().name } shouldBe
            KlagehjemmelDb.entries.associateWith { it.name }
    }

    /** Navnene er kontrakten mot klagene som allerede ligger lagret, så de listes ut i sin helhet framfor å utledes. */
    @Test
    fun `navnene som lagres er de vi har avtalt`() {
        KlagehjemmelDb.entries.map { it.name } shouldBe listOf(
            "ARBEIDSMARKEDSLOVEN_2",
            "ARBEIDSMARKEDSLOVEN_13",
            "ARBEIDSMARKEDSLOVEN_13_LØNN",
            "ARBEIDSMARKEDSLOVEN_13_L4",
            "ARBEIDSMARKEDSLOVEN_15",
            "ARBEIDSMARKEDSLOVEN_17",
            "ARBEIDSMARKEDSLOVEN_22",
            "FOLKETRYGDLOVEN_22_15",
            "FOLKETRYGDLOVEN_22_17_A",
            "FORELDELSESLOVEN_10",
            "FORELDELSESLOVEN_2_OG_3",
            "FORVALTNINGSLOVEN_11",
            "FORVALTNINGSLOVEN_17",
            "FORVALTNINGSLOVEN_18_OG_19",
            "FORVALTNINGSLOVEN_28",
            "FORVALTNINGSLOVEN_30",
            "FORVALTNINGSLOVEN_31",
            "FORVALTNINGSLOVEN_32",
            "FORVALTNINGSLOVEN_35",
            "FORVALTNINGSLOVEN_41",
            "FORVALTNINGSLOVEN_42",
            "TILTAKSPENGEFORSKRIFTEN_2",
            "TILTAKSPENGEFORSKRIFTEN_3",
            "TILTAKSPENGEFORSKRIFTEN_5",
            "TILTAKSPENGEFORSKRIFTEN_6",
            "TILTAKSPENGEFORSKRIFTEN_7",
            "TILTAKSPENGEFORSKRIFTEN_8",
            "TILTAKSPENGEFORSKRIFTEN_9",
            "TILTAKSPENGEFORSKRIFTEN_10",
            "TILTAKSPENGEFORSKRIFTEN_11",
        )
    }
}
