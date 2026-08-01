package no.nav.tiltakspenger.saksbehandling.felles

/**
 * Tester vet at strengen de sender inn ikke er blank, og vil slippe `!!` på hvert kallsted.
 * Bekvemmeligheten hører hjemme her, ikke i prodsignaturen — [Begrunnelse.create] returnerer nullable fordi prod må ta stilling til tom streng.
 */
fun Begrunnelse.Companion.createOrThrow(verdi: String): Begrunnelse = Begrunnelse.create(verdi)!!
