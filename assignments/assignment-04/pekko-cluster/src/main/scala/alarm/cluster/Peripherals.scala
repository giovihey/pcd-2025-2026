package alarm.cluster

import alarm.{ ArmingMode, SensorType, Zone }
import alarm.HomeAlarmProtocol.*
import alarm.actors.{ KeypadActor, SensorActor, SirenActor }
import org.apache.pekko.actor.typed.{ ActorRef, Behavior }
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import java.util.UUID

/** The "peripherals" node: it hosts the siren, the keypad and the sensors.
 *  They talk to the control unit, wherever it runs in the cluster, only through messages.
 *  The Command messages below are local: they come from the console of the demo.
 */
object Peripherals:

  sealed trait Command
  final case class EnterPin(pin: String, mode: ArmingMode) extends Command
  final case class TriggerSensor(name: String) extends Command
  case object AskStatus extends Command
  private final case class StatusReceived(status: Status) extends Command

  private val sensorDefinitions = List(
    ("door-perimeter", SensorType.Door, Zone.Perimeter),
    ("window-living", SensorType.Window, Zone.LivingArea),
    ("motion-living", SensorType.Motion, Zone.LivingArea),
    ("motion-upper", SensorType.Motion, Zone.UpperFloor),
  )

  val SensorNames: List[String] = sensorDefinitions.map(_._1)

  /** @param acu the control unit: here the proxy of the Cluster Singleton, which is a normal ActorRef */
  def apply(acu: ActorRef[AlarmCommand]): Behavior[Command] =
    Behaviors.setup: ctx =>
      // the siren lives on this node; the control unit finds it through the receptionist
      val siren = ctx.spawn(SirenActor(), "siren")
      ctx.system.receptionist ! Receptionist.Register(SirenActor.Key, siren)

      val keypad = ctx.spawn(KeypadActor(acu), "keypad")
      val sensors = sensorDefinitions.map { (name, sensorType, zone) =>
        name -> ctx.spawn(SensorActor(UUID.randomUUID(), sensorType, zone, acu), s"sensor-$name")
      }.toMap
      val statusAdapter = ctx.messageAdapter[Status](StatusReceived.apply)

      Behaviors.receiveMessage:
        case EnterPin(pin, mode) =>
          keypad ! KeypadActor.Submit(pin, mode)
          Behaviors.same

        case TriggerSensor(name) =>
          sensors.get(name) match
            case Some(sensor) => sensor ! SensorActor.Trigger
            case None         => ctx.log.warn("Unknown sensor '{}'. Available: {}", name, SensorNames.mkString(", "))
          Behaviors.same

        case AskStatus =>
          acu ! GetStatus(statusAdapter)
          Behaviors.same

        case StatusReceived(Status(state, node)) =>
          ctx.log.info("[STATUS] control unit: {} - hosted on {}", state, node)
          Behaviors.same