# ADR-0047: An alarm wakes a sleeping device for the rescue and the night

- Status: Accepted
- Date: 2026-11-16

## Context

Two of the collection's timers are coroutine delays: the silence that ends in
a rescue, when the provider has said nothing for twice its round, and the
change of mode at the edges of the night, 23:00 and 07:00. So is the idle
rewrite of the last position and the device's status (ADR-0010). A delay
counts only the time the processor is awake.

A device left on a shelf overnight goes into deep sleep. Its delays then
stand still: it neither rescues nor leaves the night mode until something
else wakes it, and the group sees its position grow old for hours although
the service is running. This was seen on a device whose last position was
written at 05:35 and whose next was written at 10:29, with nothing in
between.

The application has no push messages (DEV-12 is planned and needs a
server) and no AlarmManager or WorkManager work of its own.

## Decision

- An alarm that may fire while the device is idle
  (`setAndAllowWhileIdle`, `ELAPSED_REALTIME_WAKEUP`) wakes the service when
  one of those two timers would have run out: the nearer of the moment the
  silence reaches the rescue and the next edge of the night.
- It is asked for no sooner than nine minutes ahead, the least the platform
  lets through in deep sleep; a nearer moment waits for it.
- It is set on entering a mode, on each position in a mode other than
  Viewing, and after a rescue; it is cancelled when collection stops.
  Viewing needs none: it is held by an open map, on a screen that is lit.
- The alarm takes a partial wake lock for at most a minute, asks the mode to
  be chosen again, makes the waiting rescue look at the clock at once and
  sets the next alarm.
- No permission is asked for: the alarm is not an exact one, and needs
  neither `SCHEDULE_EXACT_ALARM` nor `USE_EXACT_ALARM`.

## Consequences

- A sleeping device rescues and changes mode at about the moment it would
  awake, give or take the platform's nine minutes.
- The idle rewrite of the position and the status written on its round still
  count awake time; a device that stands still in deep sleep says nothing
  between alarms beyond what a rescue writes.
- A change of mode for moving or for the battery is still noticed in deep
  sleep only when the device wakes.
- The edges of the night are taken from the local time when the alarm is
  set; a change of the clock for summer time moves them by an hour until the
  next alarm.
- Unless the application is exempt from battery optimisation, the platform
  keeps its network closed in deep sleep outside the maintenance windows. A
  position found by a rescue there is held by the database's client and
  written when the network opens.
