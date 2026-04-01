<script setup>
import PageContainer from '@/components/common/PageContainer.vue'

const featuredServices = [
  {
    title: 'Wellness exams',
    description: 'Routine checkups and preventive care for dogs and cats.',
    tag: 'Preventive',
  },
  {
    title: 'Vaccinations',
    description: 'Core and lifestyle-based vaccines with clear scheduling guidance.',
    tag: 'Essential',
  },
  {
    title: 'Dental care',
    description: 'Oral health visits that support comfort, appetite, and long-term wellness.',
    tag: 'Ongoing care',
  },
  {
    title: 'Sick visits',
    description: 'Prompt appointments for common concerns that need timely attention.',
    tag: 'Quick support',
  },
]

const bookingSteps = [
  {
    title: 'Choose care',
    description: "Select the visit type that fits your pet's needs.",
  },
  {
    title: 'Book a time',
    description: 'Pick an appointment that works for your schedule.',
  },
  {
    title: 'Share details',
    description: 'Add a few pet and visit notes before arrival.',
  },
  {
    title: 'Visit prepared',
    description: 'The clinic has the information needed for a smoother check-in.',
  },
]

const testimonials = [
  {
    quote:
      'Booking felt straightforward, and the clinic communication made the whole visit feel calm and well organized.',
    name: 'Megan R.',
    pet: 'Owner of Charlie, Golden Retriever',
  },
  {
    quote:
      'It feels like a real neighborhood clinic experience: friendly staff, clear expectations, and thoughtful follow-up.',
    name: 'Daniel T.',
    pet: 'Owner of Luna, Domestic Shorthair',
  },
]

const trustSignals = [
  'Clear scheduling and visit expectations',
  'Friendly support for everyday pet care needs',
  'Designed for dependable small-clinic operations',
]
</script>

<template>
  <PageContainer>
    <div class="homepage">
      <section class="hero-section">
        <div class="hero-brand">
          <div class="brand-mark" aria-hidden="true">
            <span class="brand-paw">P</span>
          </div>
          <div class="brand-copy">
            <span class="brand-name">PawCare Hub</span>
            <span class="brand-note">Pet clinic booking and care coordination</span>
          </div>
        </div>

        <div class="hero-copy">
          <span class="hero-kicker">Built for local pet clinics</span>
          <h1>
            <span class="headline-light">Simple booking</span>
            <span class="headline-strong">
              for pet owners, with a calmer clinic day for your team.
            </span>
          </h1>
          <p>
            PawCare Hub helps a neighborhood clinic present services clearly, guide families through
            booking, and create a more trustworthy first impression online.
          </p>

          <div class="hero-actions">
            <a href="#services">
              <el-button type="primary" size="large">View Services</el-button>
            </a>
            <a href="#booking-flow">
              <el-button plain size="large">How Booking Works</el-button>
            </a>
          </div>
        </div>
      </section>

      <section id="services" class="content-section">
        <div class="section-heading">
          <span>Featured services</span>
          <h2>Common care options that families can understand quickly.</h2>
          <p>Focused service cards keep the homepage practical and easy to scan.</p>
        </div>

        <div class="card-grid">
          <el-card
            v-for="service in featuredServices"
            :key="service.title"
            class="info-card"
            shadow="hover"
          >
            <div class="card-top">
              <h3>{{ service.title }}</h3>
              <el-tag effect="plain">{{ service.tag }}</el-tag>
            </div>
            <p>{{ service.description }}</p>
          </el-card>
        </div>
      </section>

      <section id="booking-flow" class="content-section">
        <div class="section-heading">
          <span>Booking flow</span>
          <h2>Four clear steps from choosing care to arriving prepared.</h2>
          <p>The flow stays compact while showing how the product supports both owners and staff.</p>
        </div>

        <div class="flow-grid">
          <el-card
            v-for="(step, index) in bookingSteps"
            :key="step.title"
            class="flow-card"
            shadow="hover"
          >
            <div class="flow-number">{{ index + 1 }}</div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </el-card>
        </div>
      </section>

      <section class="content-section trust-section">
        <div class="section-heading">
          <span>Trust and testimonials</span>
          <h2>Warm, credible signals that help the clinic feel established.</h2>
          <p>Short trust markers and realistic testimonials reinforce confidence without adding clutter.</p>
        </div>

        <div class="trust-layout">
          <el-card class="signals-card" shadow="never">
            <h3>Why families feel confident</h3>
            <ul>
              <li v-for="signal in trustSignals" :key="signal">{{ signal }}</li>
            </ul>
          </el-card>

          <div class="testimonial-grid">
            <el-card
              v-for="testimonial in testimonials"
              :key="testimonial.name"
              class="testimonial-card"
              shadow="hover"
            >
              <p class="testimonial-quote">"{{ testimonial.quote }}"</p>
              <strong>{{ testimonial.name }}</strong>
              <span>{{ testimonial.pet }}</span>
            </el-card>
          </div>
        </div>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped>
.homepage {
  display: flex;
  flex-direction: column;
  gap: 84px;
  padding: 28px 0 72px;
}

.hero-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100vw;
  margin-left: calc(50% - 50vw);
  padding:
    28px
    max(24px, calc((100vw - 1100px) / 2 + 10px))
    42px;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 14% 18%, rgba(255, 237, 184, 0.9), transparent 28%),
    radial-gradient(circle at 82% 30%, rgba(242, 225, 186, 0.42), transparent 26%),
    linear-gradient(180deg, #fff9ee 0%, #f8efda 100%);
  z-index: 0;
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 1;
  padding-top: 8px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: linear-gradient(145deg, #2f6b52 0%, #4f8a70 100%);
  box-shadow: 0 10px 22px rgba(72, 92, 63, 0.12);
}

.brand-paw {
  color: #ffffff;
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.04em;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-name {
  font-size: 1.18rem;
  font-weight: 700;
  color: #385145;
  letter-spacing: -0.02em;
}

.brand-note {
  color: #7a7468;
  font-size: 0.92rem;
}

.hero-copy {
  max-width: 680px;
  padding: 12px 0 22px;
  position: relative;
  z-index: 1;
  animation: heroEnter 700ms ease-out both;
}

.hero-kicker,
.section-heading span {
  display: inline-block;
  color: #7f7356;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  display: block;
  margin: 20px 0 0;
  letter-spacing: -0.04em;
}

.headline-light {
  display: block;
  color: #33483e;
  font-family: "Trebuchet MS", "Segoe UI", sans-serif;
  font-weight: 500;
  font-size: clamp(3rem, 6vw, 5.2rem);
  line-height: 1;
}

.headline-strong {
  display: block;
  max-width: 28ch;
  margin-top: 14px;
  color: #70685c;
  font-family: "Trebuchet MS", "Segoe UI", sans-serif;
  font-weight: 400;
  font-size: clamp(1.12rem, 1.9vw, 1.45rem);
  line-height: 1.5;
  letter-spacing: -0.01em;
}

.hero-copy p {
  max-width: 560px;
  margin: 22px 0 0;
  color: #736d63;
  font-size: 1.02rem;
  line-height: 1.82;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.hero-actions :deep(.el-button--primary) {
  --el-button-bg-color: #3f725d;
  --el-button-border-color: #3f725d;
  --el-button-hover-bg-color: #355f4d;
  --el-button-hover-border-color: #355f4d;
  --el-button-active-bg-color: #2c5141;
  --el-button-active-border-color: #2c5141;
}

.hero-actions :deep(.el-button.is-plain) {
  --el-button-text-color: #5f685e;
  --el-button-bg-color: rgba(255, 253, 248, 0.72);
  --el-button-border-color: rgba(110, 113, 96, 0.2);
  --el-button-hover-text-color: #355f4d;
  --el-button-hover-bg-color: rgba(255, 250, 241, 0.96);
  --el-button-hover-border-color: rgba(63, 114, 93, 0.32);
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.section-heading {
  max-width: 700px;
}

.section-heading h2 {
  margin: 12px 0 0;
  font-size: clamp(1.95rem, 3vw, 2.8rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
  color: #173047;
}

.section-heading p {
  max-width: 560px;
  margin: 14px 0 0;
  color: #5f7484;
  line-height: 1.75;
}

.card-grid,
.flow-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
}

.info-card,
.flow-card,
.signals-card,
.testimonial-card {
  border: 1px solid rgba(28, 60, 88, 0.12);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 32px rgba(21, 40, 61, 0.06);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.card-top h3,
.flow-card h3,
.signals-card h3 {
  margin: 0;
  color: #173047;
  font-size: 1.12rem;
}

.info-card p,
.flow-card p,
.testimonial-quote,
.signals-card li {
  color: #556b7c;
  line-height: 1.68;
}

.flow-card {
  overflow: hidden;
}

.flow-number {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  border-radius: 50%;
  background: #d9e8f5;
  color: #1f5d8c;
  font-weight: 700;
}

.trust-layout {
  display: grid;
  grid-template-columns: minmax(240px, 0.85fr) minmax(0, 1.15fr);
  gap: 18px;
  align-items: start;
}

.signals-card {
  padding: 28px;
  background: linear-gradient(180deg, #eef3ef 0%, #e4ebe7 100%);
}

.signals-card ul {
  margin: 16px 0 0;
  padding-left: 18px;
}

.testimonial-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.testimonial-card strong {
  display: block;
  margin-top: 18px;
  color: #173047;
}

.testimonial-card span {
  display: block;
  margin-top: 5px;
  color: #69808f;
}

@media (max-width: 960px) {
  .homepage {
    gap: 64px;
  }

  .trust-layout,
  .testimonial-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .homepage {
    gap: 52px;
    padding-bottom: 56px;
  }

  .hero-section {
    gap: 18px;
    padding:
      22px
      20px
      32px;
  }

  .hero-copy {
    padding: 8px 0 18px;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-actions a,
  .hero-actions :deep(.el-button) {
    width: 100%;
  }

  .hero-brand {
    padding-top: 2px;
  }
}

@keyframes heroEnter {
  from {
    opacity: 0;
    transform: translateY(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
