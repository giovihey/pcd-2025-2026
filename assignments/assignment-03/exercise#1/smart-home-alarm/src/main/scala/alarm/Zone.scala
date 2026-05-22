package alarm

/** A named zone of the house (e.g., living area, sleeping area, perimeter).
 *
 * Zones are compared by name, so two `Zone("perimeter")` values are equal.
 * Predefined constants live in the companion object; callers are free to define
 * their own by just writing `Zone("my-zone")`.
 */
opaque type Zone = String

object Zone:
  def apply(name: String): Zone = name

  /** Convenience extractor so pattern-matching and logging work naturally. */
  def unapply(z: Zone): Some[String] = Some(z)

  val Perimeter: Zone = Zone("perimeter")
  val LivingArea: Zone = Zone("living-area")
  val SleepingArea: Zone = Zone("sleeping-area")
  val UpperFloor: Zone = Zone("upper-floor")

  val All: Set[Zone] = Set(Perimeter, LivingArea, SleepingArea, UpperFloor)

  extension (z: Zone) def name: String = z
