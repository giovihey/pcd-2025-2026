package alarm.actors

import org.apache.pekko.actor.typed.{ ActorRef, Behavior }
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import alarm.HomeAlarmProtocol.*

object KeypadActor:

  /** Messages the KeypadActor accepts. */
  case class Submit(pin: String)

  /** @param acu  the AlarmControlUnit to forward PinEntered messages to */
  def apply(acu: ActorRef[AlarmCommand]): Behavior[Submit] =
    Behaviors.receiveMessage:
      case Submit(pin) =>
        acu ! PinEntered(pin)
        Behaviors.same
