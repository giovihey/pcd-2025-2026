package alarm.actors

import org.apache.pekko.actor.typed.{ ActorRef, Behavior }
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import alarm.HomeAlarmProtocol.*
import alarm.{ SensorType, Zone }

import java.util.UUID

object SensorActor:

  /** The only message a SensorActor accepts. */
  case object Trigger

  /** @param sensorId   UUID identifying this sensor
   * @param sensorType the kind of sensor (motion, door, window)
   * @param zone        the zone this sensor belongs to; inserted into every
   *                    [[SensorTriggered]] message so the ACU can filter by zone.
   * @param acu        the AlarmControlUnit to notify on trigger
   */
  def apply(sensorId: UUID, sensorType: SensorType, zone: Zone, acu: ActorRef[AlarmCommand]): Behavior[Trigger.type] =
    Behaviors.receiveMessage:
      case Trigger =>
        acu ! SensorTriggered(sensorId, sensorType, zone)
        Behaviors.same
