package alarm

import alarm.ArmingMode.FullArm

import java.util.UUID

object HomeAlarmProtocol:
  enum AlarmCommand:
    case PinEntered(pin: String, mode: ArmingMode = FullArm)
    case SensorTriggered(sensorId: UUID, sensorType: String, zone: Zone)
    case ExitDelayExpired
    case EntryDelayExpired

  enum SirenCommand:
    case ActivateSiren
    case DeactivateSiren

  export AlarmCommand.*, SirenCommand.*
