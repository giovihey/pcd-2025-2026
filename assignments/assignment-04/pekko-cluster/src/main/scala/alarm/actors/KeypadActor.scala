package alarm.actors

import org.apache.pekko.actor.typed.{ ActorRef, Behavior }
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import alarm.HomeAlarmProtocol.*
import alarm.ArmingMode

object KeypadActor:

  /** A PIN submission from the user.
   *
   * @param pin  the digits entered.
   * @param mode requested arming mode; only meaningful when the PIN is used
   *             to arm the system.  Defaults to [[ArmingMode.FullArm]] so
   *             call-sites that don't care about zones need not change.
   */
  case class Submit(pin: String, mode: ArmingMode = ArmingMode.FullArm)

  /** @param acu the AlarmControlUnit to forward PinEntered messages to. */
  def apply(acu: ActorRef[AlarmCommand]): Behavior[Submit] =
    Behaviors.receiveMessage:
      case Submit(pin, mode) =>
        acu ! PinEntered(pin, mode)
        Behaviors.same
