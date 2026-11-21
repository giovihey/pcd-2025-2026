package alarm

import alarm.ArmingMode.FullArm
import org.apache.pekko.actor.typed.ActorRef

import java.util.UUID

object HomeAlarmProtocol:

  /** A sealed trait with case classes / case objects (not a Scala 3 enum): Jackson restores
   *  parameterised enum cases badly, while this shape is the one used in the seminar examples.
   */
  sealed trait AlarmCommand extends CborSerializable

  object AlarmCommand:
    final case class PinEntered(pin: String, mode: ArmingMode = FullArm) extends AlarmCommand
    final case class SensorTriggered(sensorId: UUID, sensorType: SensorType, zone: Zone) extends AlarmCommand
    case object ExitDelayExpired extends AlarmCommand
    case object EntryDelayExpired extends AlarmCommand

    /** Diagnostic query: the control unit answers with the name of its state and the node that hosts it. */
    final case class GetStatus(replyTo: ActorRef[Status]) extends AlarmCommand

  /** Reply to [[AlarmCommand.GetStatus]]. */
  final case class Status(state: String, node: String) extends CborSerializable

  enum SirenCommand extends CborSerializable:
    case ActivateSiren
    case DeactivateSiren

  export AlarmCommand.*, SirenCommand.*
