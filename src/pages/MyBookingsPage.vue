<script setup>
import PageContainer from '@/components/common/PageContainer.vue'

const bookings = [
  {
    petName: 'Charlie',
    service: 'Annual wellness exam',
    date: 'April 18, 2026',
    time: '10:30 AM',
    status: 'Confirmed',
    clinic: 'PawCare Hub Clinic',
    staff: 'Dr. Rivera',
  },
  {
    petName: 'Mochi',
    service: 'Puppy vaccine visit',
    date: 'April 22, 2026',
    time: '2:15 PM',
    status: 'Upcoming',
    clinic: 'PawCare Hub Clinic',
    staff: 'Nurse Patel',
  },
  {
    petName: 'Luna',
    service: 'Dental evaluation',
    date: 'March 12, 2026',
    time: '9:00 AM',
    status: 'Completed',
    clinic: 'PawCare Hub Clinic',
    staff: 'Dr. Rivera',
  },
]
</script>

<template>
  <PageContainer>
    <div class="bookings-page">
      <section class="bookings-header">
        <div class="bookings-header__copy">
          <span class="eyebrow">My bookings</span>
          <h1>Review upcoming visits and past appointments in one place.</h1>
          <p>
            Keep track of your pet's scheduled care, check visit details, and manage appointments
            when plans change.
          </p>
        </div>

        <el-button type="primary" size="large">Book New Visit</el-button>
      </section>

      <section v-if="bookings.length" class="bookings-list">
        <el-card
          v-for="booking in bookings"
          :key="`${booking.petName}-${booking.service}-${booking.date}`"
          class="booking-card"
          shadow="hover"
        >
          <div class="booking-card__top">
            <div>
              <h2>{{ booking.service }}</h2>
              <p class="booking-subtitle">{{ booking.petName }} | {{ booking.date }} at {{ booking.time }}</p>
            </div>
            <el-tag effect="plain">{{ booking.status }}</el-tag>
          </div>

          <div class="booking-meta">
            <div>
              <span>Clinic</span>
              <strong>{{ booking.clinic }}</strong>
            </div>
            <div>
              <span>Staff</span>
              <strong>{{ booking.staff }}</strong>
            </div>
          </div>

          <div class="booking-actions">
            <el-button type="primary">View Details</el-button>
            <el-button plain>Reschedule</el-button>
            <el-button plain>Cancel</el-button>
          </div>
        </el-card>
      </section>

      <section v-else class="empty-state">
        <el-empty description="No visits booked yet. Schedule your first appointment when you're ready.">
          <el-button type="primary">Book New Visit</el-button>
        </el-empty>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped>
.bookings-page {
  display: flex;
  flex-direction: column;
  gap: 34px;
  padding: 28px 0 56px;
}

.bookings-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 30px 28px 28px;
  border: 1px solid rgba(28, 60, 88, 0.1);
  border-radius: 32px;
  background:
    radial-gradient(circle at top left, rgba(255, 239, 194, 0.88), transparent 34%),
    radial-gradient(circle at right center, rgba(244, 228, 196, 0.42), transparent 26%),
    linear-gradient(180deg, #fff9ee 0%, #f8efdc 100%);
}

.bookings-header__copy {
  max-width: 700px;
}

.eyebrow {
  display: inline-block;
  color: #7f7356;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.bookings-header h1 {
  margin: 14px 0 0;
  color: #173047;
  font-size: clamp(2.15rem, 4vw, 3.3rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.bookings-header p {
  max-width: 620px;
  margin: 16px 0 0;
  color: #6b7480;
  line-height: 1.75;
}

.bookings-header :deep(.el-button--primary),
.booking-actions :deep(.el-button--primary),
.empty-state :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

.booking-actions :deep(.el-button.is-plain) {
  --el-button-text-color: #5f685e;
  --el-button-bg-color: rgba(255, 251, 244, 0.86);
  --el-button-border-color: rgba(132, 125, 104, 0.16);
  --el-button-hover-text-color: #355f4d;
  --el-button-hover-bg-color: rgba(255, 252, 246, 1);
  --el-button-hover-border-color: rgba(63, 114, 93, 0.32);
}

.bookings-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.booking-card {
  border: 1px solid rgba(28, 60, 88, 0.12);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 32px rgba(21, 40, 61, 0.06);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    border-color 180ms ease;
}

.booking-card:hover {
  transform: translateY(-3px);
  border-color: rgba(63, 114, 93, 0.18);
  box-shadow: 0 20px 34px rgba(21, 40, 61, 0.08);
}

.booking-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.booking-card h2 {
  margin: 0;
  color: #173047;
  font-size: 1.22rem;
}

.booking-subtitle {
  margin: 8px 0 0;
  color: #6d7680;
}

.booking-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
  padding: 16px 0;
  border-top: 1px solid rgba(28, 60, 88, 0.08);
  border-bottom: 1px solid rgba(28, 60, 88, 0.08);
}

.booking-meta span {
  display: block;
  color: #7a817f;
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.booking-meta strong {
  display: block;
  margin-top: 6px;
  color: #173047;
  font-size: 1rem;
}

.booking-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.empty-state {
  padding: 34px 24px;
  border: 1px dashed rgba(63, 114, 93, 0.24);
  border-radius: 28px;
  background: rgba(255, 251, 244, 0.72);
}

@media (max-width: 760px) {
  .bookings-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .bookings-header :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .bookings-page {
    gap: 28px;
    padding-bottom: 44px;
  }

  .bookings-header {
    padding: 22px 18px 20px;
    border-radius: 24px;
  }

  .booking-card__top,
  .booking-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .booking-meta {
    grid-template-columns: 1fr;
  }

  .booking-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
