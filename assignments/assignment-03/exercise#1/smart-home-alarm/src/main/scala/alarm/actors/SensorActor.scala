package alarm.actors

import org.apache.pekko.actor.typed.{ ActorRef, Behavior }
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import alarm.HomeAlarmProtocol.*

import java.util.UUID

object SensorActor:

  /** The only message a SensorActor accepts — fire its detection event. */
  case object Trigger

  /** @param sensorId   UUID identifying this sensor
    * @param sensorType human label ("motion", "door", "window")
    * @param acu        the AlarmControlUnit to notify on trigger
    */
  def apply(sensorId: UUID, sensorType: String, acu: ActorRef[AlarmCommand]): Behavior[Trigger.type] =
    Behaviors.receiveMessage:
      case Trigger =>
        acu ! SensorTriggered(sensorId, sensorType)
        Behaviors.same
