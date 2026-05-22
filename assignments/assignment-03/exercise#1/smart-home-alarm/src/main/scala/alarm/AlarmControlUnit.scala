package alarm

import alarm.HomeAlarmProtocol.*
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.*

/** Configuration for the alarm system.
  * @param correctPin   the PIN required to arm/disarm
  * @param exitDelay    how long to wait before arming after PIN entry
  * @param entryDelay   how long the user has to disarm after a sensor triggers
  * @param knownZones   the universe of zones the system knows about.
  *                     [[ArmingMode.FullArm]] arms all of these.
  *                     Defaults to the three built-in zones; override to match
  *                     the actual house layout.
  */
case class AlarmConfig(
  correctPin: String       = "1234",
  exitDelay:  FiniteDuration = 20.seconds,
  entryDelay: FiniteDuration = 15.seconds,
  knownZones: Set[Zone] = Zone.All,
)

object AlarmControlUnit:

  def apply(siren: ActorRef[SirenCommand], config: AlarmConfig = AlarmConfig()): Behavior[AlarmCommand] =
    disarmed(siren, config)

  private def disarmed(siren: ActorRef[SirenCommand], config: AlarmConfig): Behavior[AlarmCommand] =
    Behaviors.setup: ctx =>
      ctx.log.info("[ACU] ▶ State: DISARMED")
      Behaviors.receiveMessage:
        case PinEntered(pin, mode) if pin == config.correctPin =>
          val zones = mode.activeZones(config.knownZones)
          ctx.log.info("[ACU] Correct PIN({}) — starting exit delay ({}s)", mode, config.exitDelay.toSeconds)
          exitDelay(siren, config, zones)

        case PinEntered(_, _) =>
          ctx.log.warn("[ACU] Wrong PIN in DISARMED — ignored")
          Behaviors.same

        case SensorTriggered(id, t, z) =>
          ctx.log.info("[ACU] Sensor [{}/{}/{}] ignored — system disarmed", id, t, z.name)
          Behaviors.same

        case _ => Behaviors.same

  private def exitDelay(siren: ActorRef[SirenCommand], config: AlarmConfig, activeZones: Set[Zone]): Behavior[AlarmCommand] =
    Behaviors.withTimers: timers =>
      timers.startSingleTimer(ExitDelayExpired, config.exitDelay)
      Behaviors.setup: ctx =>
        ctx.log.info(
          "[ACU] ▶ State: EXIT DELAY — {}s — active zones: {}",
          config.exitDelay.toSeconds,
          activeZones.map(_.name).mkString(", "),
        )
        Behaviors.receiveMessage:
          case ExitDelayExpired =>
            ctx.log.info("[ACU] Exit delay expired — system ARMED")
            armed(siren, config, activeZones)

          case SensorTriggered(id, t, z) =>
            ctx.log.info("[ACU] Sensor [{}/{}/{}] ignored — exit delay active", id, t, z.name)
            Behaviors.same

          case PinEntered(_, _) =>
            ctx.log.info("[ACU] PIN ignored during exit delay")
            Behaviors.same

          case _ => Behaviors.same

  private def armed(siren: ActorRef[SirenCommand], config: AlarmConfig, activeZones: Set[Zone]): Behavior[AlarmCommand] =
    Behaviors.setup: ctx =>
      ctx.log.info(
        "[ACU] ▶ State: ARMED — monitoring zones: {}",
        activeZones.map(_.name).mkString(", "),
      )
      Behaviors.receiveMessage:
        case SensorTriggered(id, t, zone) if activeZones.contains(zone) =>
          ctx.log.warn(
            "[ACU] Intrusion! Sensor [{}/{}/{}] — starting entry delay ({}s)",
            id, t, zone.name, config.entryDelay.toSeconds,
          )
          entryDelay(siren, config, activeZones)

        case SensorTriggered(id, t, zone) =>
          // sensor fired but its zone is not armed — silently ignore
          ctx.log.info("[ACU] Sensor [{}/{}/{}] ignored — zone not active", id, t, zone.name)
          Behaviors.same

        case PinEntered(pin, _) if pin == config.correctPin =>
          ctx.log.info("[ACU] Correct PIN while armed — disarming directly")
          disarmed(siren, config)

        case PinEntered(_, _) =>
          ctx.log.warn("[ACU] Wrong PIN while armed")
          Behaviors.same

        case _ => Behaviors.same

  private def entryDelay(siren: ActorRef[SirenCommand], config: AlarmConfig, activeZones: Set[Zone]): Behavior[AlarmCommand] =
    Behaviors.withTimers: timers =>
      timers.startSingleTimer(EntryDelayExpired, config.entryDelay)
      Behaviors.setup: ctx =>
        ctx.log.warn("[ACU] ▶ State: ENTRY DELAY — enter PIN within {}s!", config.entryDelay.toSeconds)
        Behaviors.receiveMessage:
          case PinEntered(pin, _) if pin == config.correctPin =>
            ctx.log.info("[ACU] Correct PIN — disarmed in time!")
            timers.cancel(EntryDelayExpired)
            disarmed(siren, config)

          case PinEntered(_, _) =>
            ctx.log.warn("[ACU] Wrong PIN during entry delay!")
            Behaviors.same

          case EntryDelayExpired =>
            ctx.log.warn("[ACU] Entry delay expired — ALARM TRIGGERED")
            alarmState(siren, config)

          case SensorTriggered(_, _, _) =>
            // additional triggers during countdown are ignored regardless of zone
            ctx.log.info("[ACU] Additional sensor trigger ignored — already counting down")
            Behaviors.same

          case _ => Behaviors.same

  private def alarmState(siren: ActorRef[SirenCommand], config: AlarmConfig): Behavior[AlarmCommand] =
    Behaviors.setup: ctx =>
      ctx.log.warn("[ACU] ▶ State: ALARM 🚨")
      siren ! ActivateSiren
      Behaviors.receiveMessage:
        case PinEntered(pin, _) if pin == config.correctPin =>
          ctx.log.info("[ACU] Correct PIN — stopping alarm and disarming")
          siren ! DeactivateSiren
          disarmed(siren, config)

        case PinEntered(_, _) =>
          ctx.log.warn("[ACU] Wrong PIN in ALARM state")
          Behaviors.same

        case SensorTriggered(_, _, _) =>
          ctx.log.info("[ACU] Sensor trigger ignored — already in alarm")
          Behaviors.same

        case _ => Behaviors.same
