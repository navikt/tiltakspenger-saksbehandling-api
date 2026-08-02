package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Svarordet ligger i formkrav-jsonb-en på klagebehandlingen, med én gren per svar i hver retning.
 * Én klage har ett svar, så det ville tatt tre klageflyter å nå alle grenene gjennom prodstien — for en mapping som ikke rører postgres.
 *
 * Testen pinner navnene som havner i jsonb-en, ikke bare rundturen.
 */
class KlagefristUnntakSvarordDbTest {

    @Test
    fun `svarordene lagres med sitt avtalte navn`() {
        KlagefristUnntakSvarord.entries.associateWith { KlagefristUnntakSvarordDb.toDbDto(it).name } shouldBe mapOf(
            KlagefristUnntakSvarord.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN to "JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN",
            KlagefristUnntakSvarord.JA_AV_SÆRLIGE_GRUNNER to "JA_AV_SÆRLIGE_GRUNNER",
            KlagefristUnntakSvarord.NEI to "NEI",
        )
    }

    @Test
    fun `svarordene leses tilbake fra lagret navn`() {
        KlagefristUnntakSvarordDb.entries.forEach {
            KlagefristUnntakSvarordDb.toDbDto(it.toDomain()) shouldBe it
        }
    }
}
