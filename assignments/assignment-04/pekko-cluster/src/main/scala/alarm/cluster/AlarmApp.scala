package alarm.cluster

import alarm.{ AlarmConfig, AlarmControlUnit, Zone }
import alarm.ArmingMode.{ FullArm, PartialArm }
import alarm.HomeAlarmProtocol.*
import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.pekko.actor.typed.{ ActorRef, ActorSystem, Behavior }
import org.apache.pekko.actor.typed.scaladsl.{ ActorContext, Behaviors }
import org.apache.pekko.cluster.typed.{ ClusterSingleton, ClusterSingletonSettings, SingletonActor }

import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.io.StdIn

/** Demo: a cluster of three nodes in ONE JVM.
 *
 *  - ports 2551 and 2552, role "control": they can host the Cluster Singleton with the control unit
 *    (it runs on the oldest one); 2551 is also the first seed node;
 *  - port 2553, role "peripherals": siren, keypad and sensors, plus a proxy of the singleton.
 *
 *  Run with `.\gradlew.bat runDemo --console=plain` and type commands in the console.
 */
object AlarmApp:

  private val SystemName      = "ClusterSystem"          // must match the seed nodes in application.conf
  private val ControlRole     = "control"
  private val PeripheralsRole = "peripherals"
  private val ControlPorts    = Seq(2551, 2552)
  private val PeripheralsPort = 2553

  /** Short delays, so that the demo is quick. */
  private val demoConfig = AlarmConfig(exitDelay = 5.seconds, entryDelay = 10.seconds)

  private val help =
    """Commands:
      |  status                      state of the control unit and the node that hosts it
      |  pin <pin> [full|zone,zone]  PIN on the keypad (correct PIN: 1234); zones: perimeter, living-area, sleeping-area, upper-floor
      |  trigger <sensor>            door-perimeter | window-living | motion-living | motion-upper
      |  stop <2551|2552>            stop a control node: the singleton moves to the other one
      |  quit""".stripMargin

  def main(args: Array[String]): Unit =
    // the first seed node (2551) must be started first: Seq.map is evaluated in order
    val controlNodes: Map[Int, ActorSystem[Nothing]] =
      ControlPorts.map(port => port -> ActorSystem[Nothing](controlRoot(), SystemName, nodeConfig(port, ControlRole))).toMap
    val peripherals: ActorSystem[Peripherals.Command] =
      ActorSystem[Peripherals.Command](peripheralsRoot(), SystemName, nodeConfig(PeripheralsPort, PeripheralsRole))

    println(help)
    Iterator
      .continually(StdIn.readLine())
      .takeWhile(_ != null)
      .map(_.trim)
      .takeWhile(_ != "quit")
      .foreach(handle(_, controlNodes, peripherals))

    // stop everything and wait, so that the JVM can end
    peripherals.terminate()
    controlNodes.values.foreach(_.terminate())
    Await.ready(peripherals.whenTerminated, 20.seconds)
    controlNodes.values.foreach(system => Await.ready(system.whenTerminated, 20.seconds))

  private def nodeConfig(port: Int, role: String): Config =
    ConfigFactory
      .parseString(
        s"""
           |pekko.remote.artery.canonical.port = $port
           |pekko.cluster.roles = ["$role"]
           |""".stripMargin
      )
      .withFallback(ConfigFactory.load())

  /** Every node asks for the singleton: the one with the role "control" that is the oldest hosts it,
   *  the others only get a proxy, which is a normal ActorRef[AlarmCommand].
   */
  private def singletonProxy(ctx: ActorContext[?]): ActorRef[AlarmCommand] =
    val settings = ClusterSingletonSettings(ctx.system).withRole(ControlRole)
    ClusterSingleton(ctx.system).init(
      SingletonActor(AlarmControlUnit.clustered(demoConfig), "alarm-control-unit").withSettings(settings)
    )

  private def controlRoot(): Behavior[Nothing] =
    Behaviors.setup[Nothing]: ctx =>
      val _ = singletonProxy(ctx)
      Behaviors.empty

  private def peripheralsRoot(): Behavior[Peripherals.Command] =
    Behaviors.setup: ctx =>
      Peripherals(singletonProxy(ctx))

  private def handle(
                      line: String,
                      controlNodes: Map[Int, ActorSystem[Nothing]],
                      peripherals: ActorSystem[Peripherals.Command],
                    ): Unit =
    line.split("\\s+").toList match
      case "status" :: Nil =>
        peripherals ! Peripherals.AskStatus

      case "pin" :: pin :: Nil =>
        peripherals ! Peripherals.EnterPin(pin, FullArm)

      case "pin" :: pin :: zones :: Nil =>
        val mode = if zones == "full" then FullArm else PartialArm(zones.split(",").map(Zone(_)).toSet)
        peripherals ! Peripherals.EnterPin(pin, mode)

      case "trigger" :: sensor :: Nil =>
        peripherals ! Peripherals.TriggerSensor(sensor)

      case "stop" :: port :: Nil =>
        port.toIntOption.flatMap(controlNodes.get) match
          case Some(system) =>
            println(s"Stopping the control node on port $port (graceful leave)...")
            system.terminate()
          case None =>
            println("Unknown control node: use 2551 or 2552")

      case "" :: Nil => ()

      case _ => println(help)