package alarm

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import alarm.HomeAlarmProtocol.*
import alarm.actors.SensorActor.Trigger
import alarm.actors.KeypadActor.Submit
import alarm.actors.{KeypadActor, SensorActor, SirenActor}

import java.util.UUID
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.*

object Main:

  /** Actor refs we need to drive the demo from the main thread. */
  private case class Refs(
    keypad: ActorRef[Submit],
    sensor: ActorRef[Trigger.type],
  )

  @main def run(): Unit =

    val config = AlarmConfig(
      correctPin = "1234",
      exitDelay  = 3.seconds,
      entryDelay = 3.seconds,
    )

    // Promise lets us safely pass actor refs from the guardian to the main thread
    val refsPromise = Promise[Refs]()

    val system = ActorSystem(
      Behaviors.setup[Nothing]: ctx =>
        val siren  = ctx.spawn(SirenActor(),                                   "siren")
        val acu    = ctx.spawn(AlarmControlUnit(siren, config),                "alarm-control-unit")
        val keypad = ctx.spawn(KeypadActor(acu),                               "keypad")
        val sensor = ctx.spawn(SensorActor(UUID.randomUUID(), "door", acu),    "front-door")
        refsPromise.success(Refs(keypad, sensor))
        Behaviors.empty
      , "SmartHomeAlarm"
    )

    // Wait until the guardian has spawned all actors
    val refs = Await.result(refsPromise.future, 5.seconds)

    // ── Demo scenario (runs on the main thread — dispatcher stays free) ──────

    println("\n=== [DEMO] Arming the system with correct PIN ===")
    refs.keypad ! Submit("1234")                    // → EXIT DELAY (3s)

    Thread.sleep(4_000)                             // wait for armed state

    println("\n=== [DEMO] Triggering front-door sensor ===")
    refs.sensor ! Trigger                           // → ENTRY DELAY (3s)

    Thread.sleep(2_000)                             // wait a bit, then enter wrong PIN

    println("\n=== [DEMO] Entering wrong PIN ===")
    refs.keypad ! Submit("0000")                    // wrong — still counting down

    Thread.sleep(2_500)                             // let entry delay expire → ALARM

    Thread.sleep(1_000)                             // siren is blaring...

    println("\n=== [DEMO] Entering correct PIN to stop alarm ===")
    refs.keypad ! Submit("1234")                    // → DISARMED

    Thread.sleep(2_000)
    println("\n=== [DEMO] Done ===")
    system.terminate()
