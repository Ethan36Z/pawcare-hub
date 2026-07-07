<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { adminBookingsApi } from '@/api/adminBookings'
import { adminStaffApi } from '@/api/adminStaff'
import { clinicOperationsApi } from '@/api/clinicOperations'

const WEEKDAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const CLINIC_DEFAULT_HOURS = {
  MONDAY: [{ startTime: '09:00', endTime: '17:00' }],
  TUESDAY: [{ startTime: '09:00', endTime: '17:00' }],
  WEDNESDAY: [{ startTime: '09:00', endTime: '17:00' }],
  THURSDAY: [{ startTime: '09:00', endTime: '17:00' }],
  FRIDAY: [{ startTime: '09:00', endTime: '17:00' }],
  SATURDAY: [],
  SUNDAY: [],
}
const EXCEPTION_MODE_OPTIONS = [
  { label: 'Unavailable all day', value: 'unavailable' },
  { label: 'Custom working hours', value: 'custom-hours' },
]
const ROLE_FILTER_OPTIONS = [
  { label: 'All roles', value: 'all' },
  { label: 'Doctors', value: 'doctor' },
  { label: 'Nurses', value: 'nurse' },
  { label: 'Other staff', value: 'other' },
]
const ACTIVE_BOOKING_STATUSES = new Set(['Upcoming', 'Confirmed'])

const staffRecords = ref([])
const bookings = ref([])
const weeklyAvailabilityByStaff = ref({})
const scheduleExceptionsByStaff = ref({})
const selectedStaffId = ref('all')
const selectedRoleFilter = ref('all')
const selectedDate = ref(formatDateInput(new Date()))
const drawerStaffId = ref(null)
const currentMonth = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1))
const isLoading = ref(false)
const errorMessage = ref('')
const isDateDrawerOpen = ref(false)
const isExceptionDialogOpen = ref(false)
const isSavingException = ref(false)
const exceptionFormErrorMessage = ref('')
const isWeeklyDialogOpen = ref(false)
const isSavingWeeklyAvailability = ref(false)
const weeklyFormErrorMessage = ref('')
const editingExceptionId = ref(null)
const deletingExceptionId = ref(null)
const exceptionStaffId = ref(null)
const weeklyStaffId = ref(null)
const editingWeeklyDay = ref(null)
const exceptionForm = ref(buildDefaultExceptionForm())
const weeklyForm = ref(buildDefaultWeeklyForm())
const router = useRouter()
const isConflictDialogOpen = ref(false)
const isResolvingConflicts = ref(false)
const conflictErrorMessage = ref('')
const scheduleConflict = ref(null)
const pendingScheduleOperation = ref(null)
const conflictSelections = ref({})

const activeStaffRecords = computed(() => staffRecords.value.filter((staff) => staff.active !== false))
const visibleStaffRecords = computed(() => {
  const staffList =
    selectedStaffId.value === 'all'
      ? activeStaffRecords.value
      : activeStaffRecords.value.filter((staff) => staff.id === selectedStaffId.value)

  return staffList.filter((staff) => matchesRoleFilter(staff, selectedRoleFilter.value))
})
const drawerStaff = computed(() =>
  activeStaffRecords.value.find((staff) => staff.id === drawerStaffId.value) || visibleStaffRecords.value[0] || null
)
const selectedDateLabel = computed(() => formatDisplayDate(selectedDate.value))
const currentMonthLabel = computed(() =>
  currentMonth.value.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
)
const calendarWeeks = computed(() => buildCalendarWeeks(currentMonth.value))
const selectedDateSummary = computed(() => getDateSummary(selectedDate.value))
const selectedStaffSchedule = computed(() =>
  drawerStaff.value ? getStaffScheduleForDate(drawerStaff.value, selectedDate.value) : null
)
const selectedDateBookings = computed(() =>
  bookings.value
    .filter((booking) => bookingDateInput(booking) === selectedDate.value)
    .sort((left, right) => getBookingMinutes(left) - getBookingMinutes(right))
)
const drawerTimeline = computed(() => buildTimelineSlots(selectedDate.value, drawerStaff.value))
const exceptionDialogTitle = computed(() =>
  editingExceptionId.value ? 'Edit Schedule Override' : 'Add Schedule Override'
)
const weeklyDialogTitle = computed(() =>
  editingWeeklyDay.value && weeklyStaffId.value
    ? `Edit ${dayLabel(editingWeeklyDay.value)} Default`
    : 'Edit Weekly Default'
)
const conflictItems = computed(() => scheduleConflict.value?.conflicts || [])
const canResolveConflicts = computed(() =>
  conflictItems.value.length > 0 &&
  conflictItems.value.every((conflict) => Boolean(conflictSelections.value[conflict.bookingId]))
)
const conflictHeadingText = computed(() => {
  const staffName = conflictItems.value[0]?.currentStaffName || 'this staff member'
  const count = conflictItems.value.length
  return `Changing ${staffName}'s schedule affects ${count} existing appointment${count === 1 ? '' : 's'}.`
})

function buildDefaultExceptionForm() {
  return {
    date: selectedDate.value,
    mode: 'unavailable',
    customStartTime: '09:00',
    customEndTime: '17:00',
  }
}

function buildDefaultWeeklyForm() {
  return {
    isOpen: true,
    startTime: '09:00',
    endTime: '17:00',
  }
}

function getApiErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || fallbackMessage
}

function isScheduleConflictError(error) {
  return error?.response?.status === 409 && error?.response?.data?.code === 'SCHEDULE_BOOKING_CONFLICT'
}

function formatDateInput(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function parseDateInput(dateInput) {
  const [year, month, day] = dateInput.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function formatDisplayDate(dateInput) {
  if (!dateInput) {
    return ''
  }

  return parseDateInput(dateInput).toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
}

function dayKey(dateInput) {
  return parseDateInput(dateInput).toLocaleDateString('en-US', { weekday: 'long' }).toUpperCase()
}

function dayLabel(day) {
  return day.charAt(0) + day.slice(1).toLowerCase()
}

function monthCellLabel(dateInput) {
  return parseDateInput(dateInput).getDate()
}

function formatTime(time) {
  if (!time) {
    return ''
  }

  const [hoursValue, minutesValue] = time.split(':')
  const hours = Number(hoursValue)
  const minutes = Number(minutesValue)
  const meridiem = hours >= 12 ? 'PM' : 'AM'
  const labelHours = hours % 12 || 12
  return `${labelHours}:${`${minutes}`.padStart(2, '0')} ${meridiem}`
}

function formatConflictTimeRange(conflict) {
  return `${formatTime(conflict.startTime)} - ${formatTime(conflict.endTime)}`
}

function formatTimeRange(slot) {
  return `${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}`
}

function formatSlots(slots) {
  if (!slots.length) {
    return 'Closed'
  }

  return slots.map(formatTimeRange).join(', ')
}

function timeToMinutes(time) {
  if (!time) {
    return 0
  }

  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

function minutesToTime(minutes) {
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return `${`${hours}`.padStart(2, '0')}:${`${mins}`.padStart(2, '0')}`
}

function matchesRoleFilter(staff, filterValue) {
  const role = String(staff.role || '').toLowerCase()

  if (filterValue === 'all') {
    return true
  }

  if (filterValue === 'doctor') {
    return role.includes('doctor') || role.includes('veterinarian') || role.includes('vet')
  }

  if (filterValue === 'nurse') {
    return role.includes('nurse') || role.includes('technician') || role.includes('tech')
  }

  return !matchesRoleFilter(staff, 'doctor') && !matchesRoleFilter(staff, 'nurse')
}

function isClinicalStaff(staff) {
  return matchesRoleFilter(staff, 'doctor') || matchesRoleFilter(staff, 'nurse')
}

function getClinicSlots(dateInput) {
  return CLINIC_DEFAULT_HOURS[dayKey(dateInput)] || []
}

function normalizeSlot(slot) {
  return {
    ...slot,
    startTime: slot.startTime?.slice(0, 5),
    endTime: slot.endTime?.slice(0, 5),
  }
}

function getStaffException(staffId, dateInput) {
  return (scheduleExceptionsByStaff.value[staffId] || []).find((item) => item.date === dateInput) || null
}

function getStaffWeeklySlots(staffId, dateInput) {
  return (weeklyAvailabilityByStaff.value[staffId] || [])
    .filter((item) => item.dayOfWeek === dayKey(dateInput) && item.active)
    .map(normalizeSlot)
    .sort((left, right) => left.startTime.localeCompare(right.startTime))
}

function getStaffScheduleForDate(staff, dateInput) {
  const exception = getStaffException(staff.id, dateInput)

  if (exception) {
    if (!exception.available) {
      return {
        staff,
        slots: [],
        source: 'override',
        label: 'Unavailable',
        hasOverride: true,
        exception,
      }
    }

    const slots =
      exception.customStartTime && exception.customEndTime
        ? [
            {
              startTime: exception.customStartTime.slice(0, 5),
              endTime: exception.customEndTime.slice(0, 5),
            },
          ]
        : []

    return {
      staff,
      slots,
      source: 'override',
      label: 'Custom hours',
      hasOverride: true,
      exception,
    }
  }

  const slots = getStaffWeeklySlots(staff.id, dateInput)

  return {
    staff,
    slots,
    source: 'weekly',
    label: slots.length ? 'Weekly default' : 'Unavailable',
    hasOverride: false,
    exception: null,
  }
}

function bookingDateInput(booking) {
  if (!booking?.date) {
    return ''
  }

  const parsed = new Date(booking.date)
  if (Number.isNaN(parsed.getTime())) {
    return ''
  }

  return formatDateInput(parsed)
}

function getBookingMinutes(booking) {
  const time = booking?.time || ''
  const match = time.match(/^(\d{1,2}):(\d{2})\s?(AM|PM)$/i)

  if (!match) {
    return 0
  }

  let hours = Number(match[1])
  const minutes = Number(match[2])
  const meridiem = match[3].toUpperCase()

  if (meridiem === 'PM' && hours !== 12) {
    hours += 12
  }

  if (meridiem === 'AM' && hours === 12) {
    hours = 0
  }

  return hours * 60 + minutes
}

function getDateBookings(dateInput) {
  return bookings.value.filter((booking) => bookingDateInput(booking) === dateInput)
}

function getDateSummary(dateInput) {
  const clinicSlots = getClinicSlots(dateInput)
  const schedules = visibleStaffRecords.value.map((staff) => getStaffScheduleForDate(staff, dateInput))
  const workingSchedules = schedules.filter((schedule) => schedule.slots.length)
  const clinicalSchedules = workingSchedules.filter((schedule) => isClinicalStaff(schedule.staff))
  const overrideCount = schedules.filter((schedule) => schedule.hasOverride).length
  const activeBookings = getDateBookings(dateInput).filter((booking) => ACTIVE_BOOKING_STATUSES.has(booking.status))
  let state = 'closed'
  let label = 'Closed'

  if (clinicSlots.length && !workingSchedules.length) {
    state = 'no-coverage'
    label = 'No coverage'
  } else if (clinicSlots.length && !clinicalSchedules.length) {
    state = 'low'
    label = 'Support only'
  } else if (clinicSlots.length && clinicalSchedules.length === 1) {
    state = 'low'
    label = 'Lean coverage'
  } else if (clinicSlots.length && activeBookings.length) {
    state = 'booked'
    label = 'Booked'
  } else if (clinicSlots.length) {
    state = 'covered'
    label = 'Covered'
  }

  return {
    clinicSlots,
    schedules,
    workingSchedules,
    clinicalSchedules,
    overrideCount,
    activeBookingCount: activeBookings.length,
    state,
    label,
  }
}

function buildCalendarWeeks(monthDate) {
  const year = monthDate.getFullYear()
  const month = monthDate.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startOffset = (firstDay.getDay() + 6) % 7
  const startDate = new Date(year, month, 1 - startOffset)
  const weeks = []

  for (let weekIndex = 0; weekIndex < 6; weekIndex += 1) {
    const week = []
    for (let dayIndex = 0; dayIndex < 7; dayIndex += 1) {
      const date = new Date(startDate)
      date.setDate(startDate.getDate() + weekIndex * 7 + dayIndex)
      const dateInput = formatDateInput(date)
      week.push({
        dateInput,
        inMonth: date >= firstDay && date <= lastDay,
        isToday: dateInput === formatDateInput(new Date()),
        summary: getDateSummary(dateInput),
      })
    }
    weeks.push(week)
  }

  return weeks
}

function buildTimelineSlots(dateInput, staff) {
  const clinicSlots = getClinicSlots(dateInput)
  const baseStart = clinicSlots.length
    ? Math.min(...clinicSlots.map((slot) => timeToMinutes(slot.startTime)))
    : 9 * 60
  const baseEnd = clinicSlots.length
    ? Math.max(...clinicSlots.map((slot) => timeToMinutes(slot.endTime)))
    : 17 * 60
  const staffSlots = staff ? getStaffScheduleForDate(staff, dateInput).slots : []
  const dayBookings = selectedDateBookings.value
  const slots = []

  for (let minute = baseStart; minute < baseEnd; minute += 30) {
    const time = minutesToTime(minute)
    const booking = dayBookings.find((item) => getBookingMinutes(item) === minute)
    const isWorking = staffSlots.some(
      (slot) => minute >= timeToMinutes(slot.startTime) && minute < timeToMinutes(slot.endTime)
    )
    const isClinicOpen = clinicSlots.some(
      (slot) => minute >= timeToMinutes(slot.startTime) && minute < timeToMinutes(slot.endTime)
    )

    slots.push({
      time,
      label: formatTime(time),
      booking,
      isWorking,
      isClinicOpen,
    })
  }

  return slots
}

function getConflictForBooking(booking) {
  return conflictItems.value.find((conflict) => conflict.bookingId === booking?.id) || null
}

function selectCalendarDate(dateInput) {
  selectedDate.value = dateInput
  drawerStaffId.value = selectedStaffId.value === 'all' ? visibleStaffRecords.value[0]?.id ?? null : selectedStaffId.value
  isDateDrawerOpen.value = true
}

function goToPreviousMonth() {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1, 1)
}

function goToNextMonth() {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 1)
}

function goToToday() {
  const today = new Date()
  currentMonth.value = new Date(today.getFullYear(), today.getMonth(), 1)
  selectCalendarDate(formatDateInput(today))
}

function openCreateExceptionDialog(staffId = drawerStaffId.value, dateInput = selectedDate.value) {
  if (!staffId || !dateInput) {
    return
  }

  exceptionStaffId.value = staffId
  editingExceptionId.value = null
  exceptionFormErrorMessage.value = ''
  exceptionForm.value = {
    ...buildDefaultExceptionForm(),
    date: dateInput,
  }
  isExceptionDialogOpen.value = true
}

function openEditExceptionDialog(exceptionItem, staffId = drawerStaffId.value) {
  if (!staffId || !exceptionItem) {
    return
  }

  exceptionStaffId.value = staffId
  editingExceptionId.value = exceptionItem.id
  exceptionFormErrorMessage.value = ''
  exceptionForm.value = {
    date: exceptionItem.date,
    mode: exceptionItem.available ? 'custom-hours' : 'unavailable',
    customStartTime: exceptionItem.customStartTime ? exceptionItem.customStartTime.slice(0, 5) : '09:00',
    customEndTime: exceptionItem.customEndTime ? exceptionItem.customEndTime.slice(0, 5) : '17:00',
  }
  isExceptionDialogOpen.value = true
}

function openSelectedDateOverride() {
  if (!drawerStaff.value) {
    return
  }

  const exception = getStaffException(drawerStaff.value.id, selectedDate.value)
  if (exception) {
    openEditExceptionDialog(exception, drawerStaff.value.id)
  } else {
    openCreateExceptionDialog(drawerStaff.value.id, selectedDate.value)
  }
}

function resetExceptionForm() {
  editingExceptionId.value = null
  exceptionStaffId.value = null
  exceptionFormErrorMessage.value = ''
  exceptionForm.value = buildDefaultExceptionForm()
}

async function handleSaveException() {
  if (!exceptionStaffId.value) {
    return
  }

  isSavingException.value = true
  exceptionFormErrorMessage.value = ''

  const payload = {
    date: exceptionForm.value.date,
    available: exceptionForm.value.mode === 'custom-hours',
    customStartTime: exceptionForm.value.mode === 'custom-hours' ? exceptionForm.value.customStartTime : null,
    customEndTime: exceptionForm.value.mode === 'custom-hours' ? exceptionForm.value.customEndTime : null,
  }
  const operation = {
    type: editingExceptionId.value ? 'exception-update' : 'exception-create',
    staffId: exceptionStaffId.value,
    exceptionId: editingExceptionId.value,
    payload,
  }

  try {
    if (editingExceptionId.value) {
      await clinicOperationsApi.updateStaffScheduleException(
        exceptionStaffId.value,
        editingExceptionId.value,
        payload
      )
    } else {
      await clinicOperationsApi.createStaffScheduleException(exceptionStaffId.value, payload)
    }

    isExceptionDialogOpen.value = false
    await loadStaffSchedule(exceptionStaffId.value)
    resetExceptionForm()
  } catch (error) {
    if (isScheduleConflictError(error)) {
      openScheduleConflictWorkflow(error.response.data, operation)
    } else {
      exceptionFormErrorMessage.value = getApiErrorMessage(error, 'Unable to save this schedule override right now.')
    }
  } finally {
    isSavingException.value = false
  }
}

async function handleDeleteSelectedException() {
  if (!drawerStaff.value || !selectedStaffSchedule.value?.exception) {
    return
  }

  const exception = selectedStaffSchedule.value.exception
  const confirmed = window.confirm(`Delete the override for ${formatDisplayDate(exception.date)}?`)
  if (!confirmed) {
    return
  }

  deletingExceptionId.value = exception.id
  errorMessage.value = ''

  try {
    await clinicOperationsApi.deleteStaffScheduleException(drawerStaff.value.id, exception.id)
    await loadStaffSchedule(drawerStaff.value.id)
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to delete this schedule override right now.')
  } finally {
    deletingExceptionId.value = null
  }
}

function openWeeklyTemplateDialog(staffId = drawerStaffId.value, dateInput = selectedDate.value) {
  if (!staffId || !dateInput) {
    return
  }

  weeklyStaffId.value = staffId
  editingWeeklyDay.value = dayKey(dateInput)
  weeklyFormErrorMessage.value = ''

  const primarySlot = getStaffWeeklySlots(staffId, dateInput)[0]
  weeklyForm.value = {
    isOpen: Boolean(primarySlot),
    startTime: primarySlot?.startTime || '09:00',
    endTime: primarySlot?.endTime || '17:00',
  }

  isWeeklyDialogOpen.value = true
}

function resetWeeklyForm() {
  weeklyStaffId.value = null
  editingWeeklyDay.value = null
  weeklyFormErrorMessage.value = ''
  weeklyForm.value = buildDefaultWeeklyForm()
}

async function handleSaveWeeklyTemplate() {
  if (!weeklyStaffId.value || !editingWeeklyDay.value) {
    return
  }

  isSavingWeeklyAvailability.value = true
  weeklyFormErrorMessage.value = ''

  const existingSlot = (weeklyAvailabilityByStaff.value[weeklyStaffId.value] || [])
    .filter((slot) => slot.dayOfWeek === editingWeeklyDay.value)
    .sort((left, right) => left.startTime.localeCompare(right.startTime))[0]
  const payload = {
    dayOfWeek: editingWeeklyDay.value,
    startTime: weeklyForm.value.startTime,
    endTime: weeklyForm.value.endTime,
    active: weeklyForm.value.isOpen,
  }
  const operation = existingSlot?.id
    ? {
        type: 'availability-update',
        staffId: weeklyStaffId.value,
        availabilityId: existingSlot.id,
        payload,
      }
    : null

  try {
    if (existingSlot?.id) {
      await adminStaffApi.updateAvailability(weeklyStaffId.value, existingSlot.id, payload)
    } else {
      await adminStaffApi.createAvailability(weeklyStaffId.value, payload)
    }

    isWeeklyDialogOpen.value = false
    await loadStaffSchedule(weeklyStaffId.value)
    resetWeeklyForm()
  } catch (error) {
    if (operation && isScheduleConflictError(error)) {
      openScheduleConflictWorkflow(error.response.data, operation)
    } else {
      weeklyFormErrorMessage.value = getApiErrorMessage(error, 'Unable to save weekly availability right now.')
    }
  } finally {
    isSavingWeeklyAvailability.value = false
  }
}

function openScheduleConflictWorkflow(conflictResponse, operation) {
  pendingScheduleOperation.value = operation
  scheduleConflict.value = conflictResponse
  conflictErrorMessage.value = ''
  conflictSelections.value = {}
  isConflictDialogOpen.value = true
}

function cancelScheduleConflictWorkflow() {
  isConflictDialogOpen.value = false
  scheduleConflict.value = null
  pendingScheduleOperation.value = null
  conflictSelections.value = {}
  conflictErrorMessage.value = ''
}

async function handleResolveScheduleConflicts() {
  if (!pendingScheduleOperation.value || !canResolveConflicts.value) {
    return
  }

  isResolvingConflicts.value = true
  conflictErrorMessage.value = ''

  const operation = pendingScheduleOperation.value
  const payload = {
    scheduleChange: operation.payload,
    reassignments: conflictItems.value.map((conflict) => ({
      bookingId: conflict.bookingId,
      replacementStaffId: conflictSelections.value[conflict.bookingId],
    })),
  }

  try {
    if (operation.type === 'exception-create') {
      await clinicOperationsApi.resolveCreateStaffScheduleExceptionConflicts(operation.staffId, payload)
      isExceptionDialogOpen.value = false
      resetExceptionForm()
    } else if (operation.type === 'exception-update') {
      await clinicOperationsApi.resolveUpdateStaffScheduleExceptionConflicts(
        operation.staffId,
        operation.exceptionId,
        payload
      )
      isExceptionDialogOpen.value = false
      resetExceptionForm()
    } else if (operation.type === 'availability-update') {
      await adminStaffApi.resolveAvailabilityConflicts(operation.staffId, operation.availabilityId, payload)
      isWeeklyDialogOpen.value = false
      resetWeeklyForm()
    }

    await loadStaffSchedule(operation.staffId)
    const { data } = await adminBookingsApi.list()
    bookings.value = data
    cancelScheduleConflictWorkflow()
  } catch (error) {
    if (isScheduleConflictError(error)) {
      openScheduleConflictWorkflow(error.response.data, operation)
    } else {
      conflictErrorMessage.value = getApiErrorMessage(error, 'Unable to resolve this schedule conflict right now.')
    }
  } finally {
    isResolvingConflicts.value = false
  }
}

function openConflictBooking(conflict) {
  router.push({
    path: '/admin/bookings',
    query: {
      owner: conflict.ownerEmail || conflict.ownerName,
      bookingId: conflict.bookingId,
    },
  })
}

async function loadStaffSchedule(staffId) {
  errorMessage.value = ''

  try {
    const [availabilityResponse, exceptionsResponse] = await Promise.all([
      adminStaffApi.listAvailability(staffId),
      clinicOperationsApi.listStaffScheduleExceptions(staffId),
    ])
    weeklyAvailabilityByStaff.value = {
      ...weeklyAvailabilityByStaff.value,
      [staffId]: availabilityResponse.data,
    }
    scheduleExceptionsByStaff.value = {
      ...scheduleExceptionsByStaff.value,
      [staffId]: exceptionsResponse.data,
    }
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load schedule data right now.')
  }
}

async function loadAllStaffSchedules(staffList) {
  await Promise.all(staffList.map((staff) => loadStaffSchedule(staff.id)))
}

async function loadPageData() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const [staffResponse, bookingResponse] = await Promise.all([
      adminStaffApi.listForOperations(),
      adminBookingsApi.list(),
    ])
    staffRecords.value = staffResponse.data
    bookings.value = bookingResponse.data
    const firstStaff = activeStaffRecords.value[0]
    drawerStaffId.value = firstStaff?.id ?? null
    await loadAllStaffSchedules(activeStaffRecords.value)
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, 'Unable to load clinic operations right now.')
  } finally {
    isLoading.value = false
  }
}

watch(selectedStaffId, () => {
  if (selectedStaffId.value !== 'all') {
    drawerStaffId.value = selectedStaffId.value
  } else if (!drawerStaffId.value) {
    drawerStaffId.value = visibleStaffRecords.value[0]?.id ?? null
  }
})

watch(selectedRoleFilter, () => {
  if (!visibleStaffRecords.value.some((staff) => staff.id === drawerStaffId.value)) {
    drawerStaffId.value = visibleStaffRecords.value[0]?.id ?? null
  }
})

onMounted(loadPageData)
</script>

<template>
  <section class="operations-page calendar-operations-page">
    <div class="operations-header">
      <div>
        <p class="operations-eyebrow">Clinic Operations</p>
        <h1>Scheduling workspace</h1>
        <p class="operations-copy">
          Review daily staffing, appointment load, weekly defaults, and date-specific overrides from one calendar.
        </p>
      </div>
      <div class="operations-header__actions">
        <el-button class="admin-button admin-button--secondary" @click="goToToday">Today</el-button>
        <el-button class="admin-button admin-button--secondary" @click="openWeeklyTemplateDialog()">
          Edit Weekly Default
        </el-button>
        <el-button
          type="primary"
          class="admin-button admin-button--primary"
          :disabled="!drawerStaff"
          @click="openSelectedDateOverride"
        >
          Edit Date Override
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      class="operations-alert"
    />

    <section class="operations-calendar-shell">
      <div class="calendar-toolbar">
        <div class="month-switcher">
          <el-button circle class="admin-button admin-button--ghost" aria-label="Previous month" @click="goToPreviousMonth">
            <span aria-hidden="true">&lt;</span>
          </el-button>
          <h2>{{ currentMonthLabel }}</h2>
          <el-button circle class="admin-button admin-button--ghost" aria-label="Next month" @click="goToNextMonth">
            <span aria-hidden="true">&gt;</span>
          </el-button>
        </div>

        <div class="calendar-filters">
          <el-select v-model="selectedStaffId" filterable class="admin-control calendar-filter" placeholder="Staff">
            <el-option label="All staff" value="all" />
            <el-option
              v-for="staff in activeStaffRecords"
              :key="staff.id"
              :label="`${staff.name} - ${staff.role}`"
              :value="staff.id"
            />
          </el-select>
          <el-select v-model="selectedRoleFilter" class="admin-control calendar-filter" placeholder="Role">
            <el-option
              v-for="option in ROLE_FILTER_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>
      </div>

      <div class="calendar-legend" aria-label="Calendar coverage legend">
        <span><i class="legend-dot legend-dot--covered"></i>Covered</span>
        <span><i class="legend-dot legend-dot--booked"></i>Booked</span>
        <span><i class="legend-dot legend-dot--low"></i>Lean</span>
        <span><i class="legend-dot legend-dot--no-coverage"></i>No coverage</span>
        <span><i class="legend-dot legend-dot--closed"></i>Closed</span>
      </div>

      <el-skeleton v-if="isLoading" :rows="10" animated />

      <div v-else class="month-grid" role="grid" aria-label="Clinic operations month">
        <div v-for="label in WEEKDAY_LABELS" :key="label" class="weekday-header">
          {{ label }}
        </div>

        <template v-for="week in calendarWeeks" :key="week[0].dateInput">
          <button
            v-for="day in week"
            :key="day.dateInput"
            type="button"
            class="calendar-day"
            :class="[
              `calendar-day--${day.summary.state}`,
              {
                'calendar-day--muted': !day.inMonth,
                'calendar-day--today': day.isToday,
                'calendar-day--selected': day.dateInput === selectedDate,
              },
            ]"
            @click="selectCalendarDate(day.dateInput)"
          >
            <span class="calendar-day__number">{{ monthCellLabel(day.dateInput) }}</span>
            <span class="calendar-day__status">{{ day.summary.label }}</span>
            <span class="calendar-day__meta">
              {{ day.summary.workingSchedules.length }} working
              <span v-if="day.summary.activeBookingCount">/ {{ day.summary.activeBookingCount }} booked</span>
            </span>
            <span v-if="day.summary.overrideCount" class="calendar-day__override">
              {{ day.summary.overrideCount }} override{{ day.summary.overrideCount === 1 ? '' : 's' }}
            </span>
          </button>
        </template>
      </div>
    </section>

    <el-drawer v-model="isDateDrawerOpen" size="min(480px, 94vw)" class="operations-drawer">
      <template #header>
        <div class="drawer-heading">
          <span class="operations-eyebrow">Daily Schedule</span>
          <h2>{{ selectedDateLabel }}</h2>
          <span :class="['admin-badge', `admin-badge--${selectedDateSummary.state === 'no-coverage' ? 'danger' : selectedDateSummary.state === 'low' ? 'warning' : selectedDateSummary.state === 'closed' ? 'closed' : 'open'}`]">
            {{ selectedDateSummary.label }}
          </span>
        </div>
      </template>

      <div class="drawer-content">
        <section class="drawer-summary">
          <div>
            <span>Clinic hours</span>
            <strong>{{ formatSlots(selectedDateSummary.clinicSlots) }}</strong>
          </div>
          <div>
            <span>Working staff</span>
            <strong>{{ selectedDateSummary.workingSchedules.length }}</strong>
          </div>
          <div>
            <span>Active bookings</span>
            <strong>{{ selectedDateSummary.activeBookingCount }}</strong>
          </div>
        </section>

        <section class="drawer-section">
          <div class="drawer-section__header">
            <div>
              <span class="operations-eyebrow">Staff View</span>
              <h3>Availability</h3>
            </div>
            <el-select v-model="drawerStaffId" filterable class="admin-control drawer-staff-select" placeholder="Staff">
              <el-option
                v-for="staff in visibleStaffRecords"
                :key="staff.id"
                :label="`${staff.name} - ${staff.role}`"
                :value="staff.id"
              />
            </el-select>
          </div>

          <div v-if="selectedStaffSchedule" class="staff-schedule-card">
            <div>
              <strong>{{ selectedStaffSchedule.staff.name }}</strong>
              <span>{{ selectedStaffSchedule.staff.role }}</span>
            </div>
            <span :class="['admin-badge', selectedStaffSchedule.hasOverride ? 'admin-badge--custom' : selectedStaffSchedule.slots.length ? 'admin-badge--open' : 'admin-badge--closed']">
              {{ selectedStaffSchedule.label }}
            </span>
            <p>{{ formatSlots(selectedStaffSchedule.slots) }}</p>
          </div>

          <div class="drawer-actions">
            <el-button class="admin-button admin-button--secondary" :disabled="!drawerStaff" @click="openWeeklyTemplateDialog()">
              Edit Weekly Default
            </el-button>
            <el-button class="admin-button admin-button--primary" :disabled="!drawerStaff" @click="openSelectedDateOverride">
              Edit Date Override
            </el-button>
            <el-button
              v-if="selectedStaffSchedule?.hasOverride"
              class="admin-button admin-button--danger"
              :loading="deletingExceptionId === selectedStaffSchedule.exception?.id"
              @click="handleDeleteSelectedException"
            >
              Delete Override
            </el-button>
          </div>
        </section>

        <section class="drawer-section">
          <div class="drawer-section__header">
            <div>
              <span class="operations-eyebrow">Timeline</span>
              <h3>Day plan</h3>
            </div>
          </div>
          <div class="timeline">
            <div
              v-for="slot in drawerTimeline"
              :key="slot.time"
              class="timeline-row"
              :class="{
                'timeline-row--working': slot.isWorking,
                'timeline-row--closed': !slot.isClinicOpen,
                'timeline-row--booking': slot.booking,
                'timeline-row--conflict': getConflictForBooking(slot.booking),
              }"
            >
              <span class="timeline-row__time">{{ slot.label }}</span>
              <span class="timeline-row__bar"></span>
              <span class="timeline-row__label">
                <template v-if="slot.booking">
                  {{ slot.booking.petName }} with {{ slot.booking.staff || 'staff' }}
                  <strong v-if="getConflictForBooking(slot.booking)" class="timeline-row__conflict-label">
                    Conflict
                  </strong>
                </template>
                <template v-else-if="slot.isWorking">Available</template>
                <template v-else>Closed</template>
              </span>
            </div>
          </div>
        </section>

        <section class="drawer-section">
          <div class="drawer-section__header">
            <div>
              <span class="operations-eyebrow">Appointments</span>
              <h3>{{ selectedDateBookings.length }} total</h3>
            </div>
          </div>

          <div v-if="selectedDateBookings.length" class="booking-list">
            <article v-for="booking in selectedDateBookings" :key="booking.id" class="booking-card">
              <div>
                <strong>{{ booking.time }} - {{ booking.petName }}</strong>
                <span>{{ booking.service }} / {{ booking.staff }}</span>
              </div>
              <span :class="['admin-badge', booking.status === 'Cancelled' ? 'admin-badge--cancelled' : booking.status === 'Completed' ? 'admin-badge--completed' : booking.status === 'Confirmed' ? 'admin-badge--confirmed' : 'admin-badge--upcoming']">
                {{ booking.status }}
              </span>
            </article>
          </div>
          <el-empty v-else description="No appointments on this date." />
        </section>
      </div>
    </el-drawer>

    <el-dialog
      v-model="isExceptionDialogOpen"
      :title="exceptionDialogTitle"
      width="min(560px, 92vw)"
      @closed="resetExceptionForm"
    >
      <el-alert
        v-if="exceptionFormErrorMessage"
        :title="exceptionFormErrorMessage"
        type="error"
        :closable="false"
        class="operations-alert"
      />

      <el-form :model="exceptionForm" label-position="top" class="admin-form">
        <el-form-item label="Date">
          <el-date-picker
            v-model="exceptionForm.date"
            type="date"
            value-format="YYYY-MM-DD"
            format="MMMM D, YYYY"
            class="admin-control operations-field"
          />
        </el-form-item>
        <el-form-item label="Override type">
          <el-radio-group v-model="exceptionForm.mode" class="exception-mode-group">
            <el-radio-button
              v-for="option in EXCEPTION_MODE_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="exceptionForm.mode === 'custom-hours'">
          <el-form-item label="Custom start time">
            <el-time-select
              v-model="exceptionForm.customStartTime"
              start="06:00"
              step="00:30"
              end="22:00"
              class="admin-control operations-field"
            />
          </el-form-item>
          <el-form-item label="Custom end time">
            <el-time-select
              v-model="exceptionForm.customEndTime"
              start="06:30"
              step="00:30"
              end="22:30"
              class="admin-control operations-field"
            />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <div class="admin-dialog-actions">
          <el-button class="admin-button admin-button--ghost" @click="isExceptionDialogOpen = false">Cancel</el-button>
          <el-button type="primary" class="admin-button admin-button--primary" :loading="isSavingException" @click="handleSaveException">
            Save Override
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="isWeeklyDialogOpen"
      :title="weeklyDialogTitle"
      width="min(520px, 92vw)"
      @closed="resetWeeklyForm"
    >
      <el-alert
        v-if="weeklyFormErrorMessage"
        :title="weeklyFormErrorMessage"
        type="error"
        :closable="false"
        class="operations-alert"
      />

      <el-form :model="weeklyForm" label-position="top" class="admin-form">
        <el-form-item label="Default status">
          <el-radio-group v-model="weeklyForm.isOpen" class="exception-mode-group">
            <el-radio-button :value="true">Open</el-radio-button>
            <el-radio-button :value="false">Closed</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="weeklyForm.isOpen">
          <el-form-item label="Start time">
            <el-time-select
              v-model="weeklyForm.startTime"
              start="06:00"
              step="00:30"
              end="22:00"
              class="admin-control operations-field"
            />
          </el-form-item>
          <el-form-item label="End time">
            <el-time-select
              v-model="weeklyForm.endTime"
              start="06:30"
              step="00:30"
              end="22:30"
              class="admin-control operations-field"
            />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <div class="admin-dialog-actions">
          <el-button class="admin-button admin-button--ghost" @click="isWeeklyDialogOpen = false">Cancel</el-button>
          <el-button type="primary" class="admin-button admin-button--primary" :loading="isSavingWeeklyAvailability" @click="handleSaveWeeklyTemplate">
            Save Weekly Default
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="isConflictDialogOpen"
      title="Schedule conflict"
      width="min(760px, 94vw)"
      :close-on-click-modal="false"
    >
      <el-alert
        v-if="conflictErrorMessage"
        :title="conflictErrorMessage"
        type="error"
        :closable="false"
        class="operations-alert"
      />

      <div class="conflict-dialog">
        <p class="conflict-dialog__summary">
          {{ conflictHeadingText }} Choose a replacement staff member or open the booking to reschedule it.
        </p>

        <article
          v-for="conflict in conflictItems"
          :key="conflict.bookingId"
          class="conflict-card"
        >
          <div class="conflict-card__details">
            <strong>{{ formatConflictTimeRange(conflict) }}</strong>
            <span>{{ conflict.petName }} / {{ conflict.serviceName }}</span>
            <span>Owner: {{ conflict.ownerName }}</span>
            <span>Current staff: {{ conflict.currentStaffName }}</span>
            <span :class="['admin-badge', conflict.status === 'Confirmed' ? 'admin-badge--confirmed' : 'admin-badge--upcoming']">
              {{ conflict.status }}
            </span>
          </div>

          <div class="conflict-card__resolution">
            <label :for="`replacement-${conflict.bookingId}`">Replacement staff</label>
            <el-select
              :id="`replacement-${conflict.bookingId}`"
              v-model="conflictSelections[conflict.bookingId]"
              class="admin-control operations-field"
              placeholder="Select staff"
              :disabled="!conflict.eligibleReplacements.length"
            >
              <el-option
                v-for="candidate in conflict.eligibleReplacements"
                :key="candidate.staffId"
                :label="`${candidate.staffName} - ${candidate.role}`"
                :value="candidate.staffId"
              />
            </el-select>
            <p v-if="!conflict.eligibleReplacements.length" class="conflict-card__empty">
              No eligible replacement staff are available for this appointment.
            </p>
            <el-button class="admin-button admin-button--secondary" @click="openConflictBooking(conflict)">
              Open booking
            </el-button>
          </div>
        </article>
      </div>

      <template #footer>
        <div class="admin-dialog-actions">
          <el-button class="admin-button admin-button--ghost" @click="cancelScheduleConflictWorkflow">
            Cancel schedule change
          </el-button>
          <el-button
            type="primary"
            class="admin-button admin-button--primary"
            :disabled="!canResolveConflicts"
            :loading="isResolvingConflicts"
            @click="handleResolveScheduleConflicts"
          >
            Reassign and save schedule
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.calendar-operations-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.operations-header,
.operations-calendar-shell {
  border: 1px solid var(--pc-border);
  border-radius: 8px;
  background: #ffffff;
}

.operations-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  padding: 24px;
}

.operations-eyebrow {
  display: block;
  margin: 0 0 6px;
  color: #66746f;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.operations-header h1,
.drawer-heading h2 {
  margin: 0;
  color: #173047;
}

.operations-copy {
  max-width: 720px;
  margin: 10px 0 0;
  color: #65747c;
  line-height: 1.55;
}

.operations-header__actions,
.calendar-filters,
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.operations-alert {
  margin-bottom: 4px;
}

.operations-calendar-shell {
  padding: 18px;
}

.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.month-switcher {
  display: flex;
  align-items: center;
  gap: 10px;
}

.month-switcher h2 {
  min-width: 180px;
  margin: 0;
  color: #173047;
  font-size: 1.2rem;
  text-align: center;
}

.calendar-filter {
  width: 210px;
}

.calendar-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 14px;
  color: #5f6f68;
  font-size: 0.86rem;
}

.calendar-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.legend-dot--covered {
  background: #4f8f6b;
}

.legend-dot--booked {
  background: #2f7f92;
}

.legend-dot--low {
  background: #c48a32;
}

.legend-dot--no-coverage {
  background: #c7655a;
}

.legend-dot--closed {
  background: #aeb7b2;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.weekday-header {
  color: #65747c;
  font-size: 0.78rem;
  font-weight: 700;
  text-align: center;
  text-transform: uppercase;
}

.calendar-day {
  position: relative;
  min-height: 122px;
  padding: 12px;
  border: 1px solid #d8e3dd;
  border-radius: 8px;
  background: #f8fbf9;
  color: #173047;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.calendar-day:hover,
.calendar-day:focus-visible {
  border-color: #4f8f6b;
  box-shadow: 0 10px 22px rgba(23, 48, 71, 0.1);
  outline: none;
  transform: translateY(-1px);
}

.calendar-day--muted {
  border-color: #e2e7e4;
  background: #f5f6f5;
  color: #7f8a86;
  opacity: 0.62;
}

.calendar-day--selected {
  border-color: #2f6751;
  box-shadow: 0 12px 26px rgba(23, 48, 71, 0.13), inset 0 0 0 2px rgba(47, 103, 81, 0.34);
}

.calendar-day--today .calendar-day__number {
  color: #ffffff;
  background: #3f725d;
}

.calendar-day--covered {
  border-color: #b9dac7;
  background: #edf8f1;
}

.calendar-day--booked {
  border-color: #abd6dc;
  background: #e7f6f7;
}

.calendar-day--low {
  border-color: #ead0a4;
  background: #fff4df;
}

.calendar-day--no-coverage {
  border-color: #e6b9b1;
  background: #fff0ec;
}

.calendar-day--closed {
  border-color: #d9dedb;
  background: #f1f3f2;
}

.calendar-day--muted.calendar-day--covered,
.calendar-day--muted.calendar-day--booked,
.calendar-day--muted.calendar-day--low,
.calendar-day--muted.calendar-day--no-coverage,
.calendar-day--muted.calendar-day--closed {
  border-color: #e2e7e4;
  background: #f5f6f5;
  color: #7f8a86;
}

.calendar-day--muted .calendar-day__meta,
.calendar-day--muted .calendar-day__override {
  color: #8b9591;
}

.calendar-day__number {
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-weight: 700;
}

.calendar-day__status,
.calendar-day__meta,
.calendar-day__override {
  display: block;
  margin-top: 9px;
  line-height: 1.3;
}

.calendar-day__status {
  font-weight: 700;
}

.calendar-day__meta,
.calendar-day__override {
  color: #63726d;
  font-size: 0.84rem;
}

.calendar-day__override {
  color: #8d6328;
}

.drawer-heading {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.drawer-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.drawer-summary div,
.drawer-section,
.staff-schedule-card,
.booking-card {
  border: 1px solid #dce6e0;
  border-radius: 8px;
  background: #ffffff;
}

.drawer-summary div {
  padding: 12px;
}

.drawer-summary span,
.staff-schedule-card span,
.booking-card span {
  display: block;
  color: #66746f;
  font-size: 0.84rem;
}

.drawer-summary strong {
  display: block;
  margin-top: 6px;
  color: #173047;
}

.drawer-section {
  padding: 14px;
}

.drawer-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.drawer-section h3 {
  margin: 0;
  color: #173047;
  font-size: 1rem;
}

.drawer-staff-select {
  width: 220px;
}

.staff-schedule-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  padding: 12px;
  margin-bottom: 12px;
}

.staff-schedule-card strong,
.booking-card strong {
  display: block;
  color: #173047;
}

.staff-schedule-card p {
  grid-column: 1 / -1;
  margin: 0;
  color: #61706a;
}

.timeline {
  display: grid;
  gap: 6px;
}

.timeline-row {
  display: grid;
  grid-template-columns: 72px 8px 1fr;
  gap: 10px;
  align-items: center;
  min-height: 34px;
  color: #61706a;
  font-size: 0.86rem;
}

.timeline-row__bar {
  width: 8px;
  height: 28px;
  border-radius: 999px;
  background: #d5ddd8;
}

.timeline-row--working .timeline-row__bar {
  background: #5d9f78;
}

.timeline-row--booking .timeline-row__bar {
  background: #317f8c;
}

.timeline-row--conflict {
  color: #8d3f37;
}

.timeline-row--conflict .timeline-row__bar {
  background: #d47a70;
}

.timeline-row__conflict-label {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff0ed;
  color: #9c4339;
  font-size: 0.74rem;
  font-weight: 700;
}

.timeline-row--closed {
  color: #909b96;
}

.booking-list {
  display: grid;
  gap: 8px;
}

.booking-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
}

.conflict-dialog {
  display: grid;
  gap: 12px;
}

.conflict-dialog__summary {
  margin: 0;
  color: #52615b;
  line-height: 1.55;
}

.conflict-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 0.75fr);
  gap: 16px;
  padding: 14px;
  border: 1px solid #e6b9b1;
  border-radius: 8px;
  background: #fff8f6;
}

.conflict-card__details,
.conflict-card__resolution {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.conflict-card__details strong {
  color: #173047;
}

.conflict-card__details span,
.conflict-card__resolution label,
.conflict-card__empty {
  color: #5f6f68;
  font-size: 0.9rem;
}

.conflict-card__resolution label {
  font-weight: 700;
}

.conflict-card__empty {
  margin: 0;
  line-height: 1.45;
}

.exception-mode-group {
  display: flex;
  flex-wrap: wrap;
}

.operations-field {
  width: 100%;
}

@media (max-width: 960px) {
  .operations-header,
  .calendar-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .operations-header__actions,
  .calendar-filters {
    justify-content: flex-start;
  }

  .calendar-filter,
  .drawer-staff-select {
    width: 100%;
  }

  .month-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .weekday-header {
    display: none;
  }
}

@media (max-width: 640px) {
  .operations-header,
  .operations-calendar-shell {
    padding: 16px;
  }

  .month-grid,
  .drawer-summary {
    grid-template-columns: 1fr;
  }

  .calendar-day {
    min-height: 108px;
  }

  .drawer-section__header,
  .booking-card,
  .conflict-card {
    flex-direction: column;
  }

  .conflict-card {
    grid-template-columns: 1fr;
  }
}
</style>
