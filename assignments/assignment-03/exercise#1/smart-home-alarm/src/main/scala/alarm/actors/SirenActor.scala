package alarm.actors

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import alarm.HomeAlarmProtocol.*

object SirenActor:

  def apply(): Behavior[SirenCommand] =
    Behaviors.receiveMessage:
      case ActivateSiren =>
        println("[SIREN] 🔔 ALARM! ALARM! ALARM!")
        Behaviors.same
      case DeactivateSiren =>
        println("[SIREN] 🔕 Siren deactivated.")
        Behaviors.same
