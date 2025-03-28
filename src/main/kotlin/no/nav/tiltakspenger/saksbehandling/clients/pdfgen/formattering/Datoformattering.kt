package no.nav.tiltakspenger.saksbehandling.clients.pdfgen.formattering

import java.time.format.DateTimeFormatter
import java.util.Locale

val norskDatoFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        "d. MMMM yyyy",
        Locale
            .Builder()
            .setLanguage("no")
            .setRegion("NO")
            .build(),
    )

val norskTidspunktFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        "d. MMMM yyyy HH:mm:ss",
        Locale
            .Builder()
            .setLanguage("no")
            .setRegion("NO")
            .build(),
    )
