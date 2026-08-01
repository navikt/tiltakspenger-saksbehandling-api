package no.nav.tiltakspenger.saksbehandling.behandling.domene

/**
 * Tester vet at strengen de sender inn ikke er blank, og vil slippe `!!` på hvert kallsted.
 * Bekvemmeligheten hører hjemme her, ikke i prodsignaturen — [FritekstTilVedtaksbrev.create] returnerer nullable fordi prod må ta stilling til tom streng.
 */
fun FritekstTilVedtaksbrev.Companion.createOrThrow(verdi: String): FritekstTilVedtaksbrev =
    FritekstTilVedtaksbrev.create(verdi)!!
