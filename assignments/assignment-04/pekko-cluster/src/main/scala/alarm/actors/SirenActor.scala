package alarm.actors

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.receptionist.ServiceKey
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import alarm.HomeAlarmProtocol.*

object SirenActor:

  /** Receptionist key: the siren registers itself under it, so that the control unit can find it
   *  on whatever node it runs.
   */
  val Key: ServiceKey[SirenCommand] = ServiceKey[SirenCommand]("siren")

  def apply(): Behavior[SirenCommand] =
    Behaviors.receiveMessage:
      case ActivateSiren =>
        println("[SIREN] ALARM! ALARM! ALARM!")
        Behaviors.same
      case DeactivateSiren =>
        println("[SIREN] Siren deactivated!")
        Behaviors.same