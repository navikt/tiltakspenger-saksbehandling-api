package no.nav.tiltakspenger.saksbehandling.distribusjon.infra

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId

private data class DokdistRequest(
    val journalpostId: String,
    val bestillendeFagsystem: String = "IND",
    val dokumentProdApp: String = "Tiltakspenger",
    val distribusjonstype: DistribusjonsType = DistribusjonsType.VEDTAK,
    val distribusjonstidspunkt: Distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
) {
    enum class DistribusjonsType {
        VEDTAK,
        VIKTIG,
        ANNET,
    }

    enum class Distribusjonstidspunkt {
        UMIDDELBART,
        KJERNETID,
    }
}

fun JournalpostId.toDokdistRequest(): String {
    return DokdistRequest(
        journalpostId = this.toString(),
    ).let {
        serialize(it)
    }
}
