package alarm

import com.fasterxml.jackson.annotation.{ JsonSubTypes, JsonTypeInfo }

/** Describes which zones should be active when the system arms.
 *
 *  - [[FullArm]]          — every zone is monitored (the default).
 *  - [[PartialArm]]       — only the supplied subset of zones is monitored;
 *    sensors in other zones are silently ignored.
 *
 *  The two annotations tell Jackson which concrete class to build when it reads an `ArmingMode`
 *  (it is a field of `PinEntered`, so its type is not known from the message class).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ArmingMode.FullArm.type], name = "full"),
    new JsonSubTypes.Type(value = classOf[ArmingMode.PartialArm], name = "partial")
  )
)
sealed trait ArmingMode:
  /** Returns the set of zones that are active for this mode.
   *
   * @param allZones the universe of known zones; used by [[FullArm]] to
   *                 resolve "all zones" without hard-coding any names.
   */
  def activeZones(allZones: Set[Zone]): Set[Zone]

object ArmingMode:
  case object FullArm extends ArmingMode:
    override def activeZones(allZones: Set[Zone]): Set[Zone] = allZones

    override def toString: String = "Full arming (all zones active)"

  final case class PartialArm(zones: Set[Zone]) extends ArmingMode:
    override def activeZones(allZones: Set[Zone]): Set[Zone] = zones

    override def toString: String = s"PartialArm(${zones.map(_.name).mkString(", ")})"
