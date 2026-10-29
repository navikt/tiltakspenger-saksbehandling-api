package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.KlageForvaltningsmelding
import java.time.LocalDate
import java.time.LocalDateTime

private data class KlageForvaltningsmeldingDbJson(
    val brevtekst: List<TittelOgTekstDbJson>,
    val saksbehandler: String,
    val sendtTidspunkt: LocalDateTime,
    val brevdato: LocalDate?,
    val journalpostId: String?,
    val journalføringstidspunkt: LocalDateTime?,
    val distribusjonId: String?,
    val distribusjonstidspunkt: LocalDateTime?,
) {
    data class TittelOgTekstDbJson(
        val tittel: String,
        val tekst: String,
    )

    fun toDomain(): KlageForvaltningsmelding = KlageForvaltningsmelding(
        brevtekst = Brevtekster(
            brevtekst.map {
                TittelOgTekst(
                    tittel = NonBlankString.create(it.tittel),
                    tekst = NonBlankString.create(it.tekst),
                )
            },
        ),
        saksbehandler = saksbehandler,
        sendtTidspunkt = sendtTidspunkt,
        brevdato = brevdato,
        journalpostId = journalpostId?.let { JournalpostId(it) },
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId?.let { DistribusjonId(it) },
        distribusjonstidspunkt = distribusjonstidspunkt,
    )
}

fun String.toKlageForvaltningsmelding(): KlageForvaltningsmelding {
    return deserialize<KlageForvaltningsmeldingDbJson>(this).toDomain()
}

fun KlageForvaltningsmelding.toDbJson(): String {
    return serialize(
        KlageForvaltningsmeldingDbJson(
            brevtekst = brevtekst.map {
                KlageForvaltningsmeldingDbJson.TittelOgTekstDbJson(
                    tittel = it.tittel.value,
                    tekst = it.tekst.value,
                )
            },
            saksbehandler = saksbehandler,
            sendtTidspunkt = sendtTidspunkt,
            brevdato = brevdato,
            journalpostId = journalpostId?.toString(),
            journalføringstidspunkt = journalføringstidspunkt,
            distribusjonId = distribusjonId?.toString(),
            distribusjonstidspunkt = distribusjonstidspunkt,
        ),
    )
}
