package alarm

import alarm.ArmingMode.FullArm

import java.util.UUID

object HomeAlarmProtocol:

  /** A sealed trait with case classes / case objects (not a Scala 3 enum): Jackson restores
   *  parameterised enum cases badly, while this shape is the one used in the seminar examples.
   *  The import paths stay the same: `HomeAlarmProtocol.AlarmCommand.PinEntered` etc.
   */
  sealed trait AlarmCommand extends CborSerializable

  object AlarmCommand:
    final case class PinEntered(pin: String, mode: ArmingMode = FullArm) extends AlarmCommand
    final case class SensorTriggered(sensorId: UUID, sensorType: SensorType, zone: Zone) extends AlarmCommand
    case object ExitDelayExpired extends AlarmCommand
    case object EntryDelayExpired extends AlarmCommand

  /** A simple enum, which Jackson handles by name. */
  enum SirenCommand extends CborSerializable:
    case ActivateSiren
    case DeactivateSiren

  export AlarmCommand.*, SirenCommand.*