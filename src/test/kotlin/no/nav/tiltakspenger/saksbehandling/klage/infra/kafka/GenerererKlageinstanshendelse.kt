@file:Suppress("unused")

package no.nav.tiltakspenger.saksbehandling.klage.infra.kafka

/** Se https://github.com/navikt/kabal-api/blob/main/docs/schema/behandling-events.json for fasit. */
object GenerererKlageinstanshendelse {
    fun avsluttetJson(
        eventId: String = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
        kildeReferanse: String = "1272760b-f9be-4ad8-99c4-d01823462784",
        kilde: String = "TIL_TIP",
        kabalReferanse: String = "c0aef33a-da01-4262-ab55-1bbdde157e8a",
        avsluttetTidspunkt: String = "2025-01-01T01:02:03.456789",
        utfall: GenererKlageinstanshendelseUtfall,
        journalpostReferanser: List<String> = listOf("123", "456"),
    ): String {
        //language=JSON
        return """
                {
                  "eventId": "$eventId",
                  "kildeReferanse":"$kildeReferanse",
                  "kilde":"$kilde",
                  "kabalReferanse":"$kabalReferanse",
                  "type":"KLAGEBEHANDLING_AVSLUTTET",
                  "detaljer":{
                    "klagebehandlingAvsluttet":{
                      "avsluttet":"$avsluttetTidspunkt",
                      "utfall":"$utfall",
                      "journalpostReferanser":[${journalpostReferanser.joinToString(",")}]
                    }
                  }
                }
        """.trimIndent()
    }

    fun omgjøringsbehandlingAvsluttet(
        eventId: String = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
        kildeReferanse: String = "1272760b-f9be-4ad8-99c4-d01823462784",
        kilde: String = "TIL_TIP",
        kabalReferanse: String = "c0aef33a-da01-4262-ab55-1bbdde157e8a",
        avsluttetTidspunkt: String = "2025-01-01T01:02:03.456789",
        journalpostReferanser: List<String> = listOf("123", "456"),
    ): String {
        //language=JSON
        // Det finnes bare et utfall per 20. feb 2026 (MEDHOLD_ETTER_FVL_35)
        return """
                {
                  "eventId": "$eventId",
                  "kildeReferanse":"$kildeReferanse",
                  "kilde":"$kilde",
                  "kabalReferanse":"$kabalReferanse",
                  "type":"OMGJOERINGSKRAVBEHANDLING_AVSLUTTET",
                  "detaljer":{
                    "omgjoeringskravbehandlingAvsluttet":{
                      "avsluttet":"$avsluttetTidspunkt",
                      "utfall":"MEDHOLD_ETTER_FVL_35",
                      "journalpostReferanser":[${journalpostReferanser.joinToString(",")}]
                    }
                  }
                }
        """.trimIndent()
    }

    private fun behandlingFeilregistrert(
        eventId: String = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
        kildeReferanse: String = "1272760b-f9be-4ad8-99c4-d01823462784",
        kilde: String = "TIL_TIP",
        kabalReferanse: String = "c0aef33a-da01-4262-ab55-1bbdde157e8a",
        feilregistrert: String = "2025-01-01T01:02:03.456789",
        reason: String = "Årsaken til at behandlingen endte opp som feilregistrert.",
        navIdent: String = "Z123456",
        type: GenererKlageinstanshendelseFeilregistrertType,
    ): String {
        //language=JSON
        return """
                {
                  "eventId": "$eventId",
                  "kildeReferanse":"$kildeReferanse",
                  "kilde":"$kilde",
                  "kabalReferanse":"$kabalReferanse",
                  "type":"BEHANDLING_FEILREGISTRERT",
                  "detaljer":{
                    "behandlingFeilregistrertDetaljer":{
                      "feilregistrert":"$feilregistrert",
                      "navIdent":"$navIdent",
                      "reason":"$reason",
                      "type": "$type"
                    }
                  }
                }
        """.trimIndent()
    }

    enum class GenererKlageinstanshendelseUtfall {
        TRUKKET,
        RETUR,
        OPPHEVET,
        MEDHOLD,
        DELVIS_MEDHOLD,
        STADFESTELSE,
        UGUNST,
        AVVIST,
        HENLAGT,
    }

    enum class GenererKlageinstanshendelseFeilregistrertType {
        KLAGE,
        ANKE,
        ANKE_I_TRYGDERETTEN,
        BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
        OMGJOERINGSKRAV,
    }
}
