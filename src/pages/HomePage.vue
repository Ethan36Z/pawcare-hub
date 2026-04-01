<script setup>
import PageContainer from '@/components/common/PageContainer.vue'

const featuredServices = [
  {
    title: 'Wellness exams',
    description: 'Routine checkups, preventive screenings, and personalized care guidance for dogs and cats.',
    details: 'Ideal for annual visits, new pet introductions, and ongoing health monitoring.',
    tag: 'Preventive care',
  },
  {
    title: 'Vaccinations',
    description: 'Core and lifestyle-based vaccine appointments designed around your pet’s age and risk profile.',
    details: 'Includes clear recommendations and reminders to help families stay on schedule.',
    tag: 'Clinic essentials',
  },
  {
    title: 'Dental care',
    description: 'Oral health evaluations and treatment planning to support comfort, appetite, and long-term wellness.',
    details: 'A practical option for pets showing tartar buildup, bad breath, or chewing discomfort.',
    tag: 'Long-term health',
  },
  {
    title: 'Sick visits',
    description: 'Prompt appointments for common concerns like itching, digestive issues, ear problems, and low energy.',
    details: 'Built for fast triage and clear next steps when something does not feel right.',
    tag: 'Same-week care',
  },
]

const bookingSteps = [
  {
    title: 'Choose a service',
    description: 'Pet owners review the clinic’s services and select the type of visit that matches their needs.',
  },
  {
    title: 'Pick a convenient time',
    description: 'Available appointment options help families book around work, school, and daily routines.',
  },
  {
    title: 'Share pet details',
    description: 'Basic information like pet name, species, and visit reason gives the team useful context upfront.',
  },
  {
    title: 'Arrive with confidence',
    description: 'The clinic is ready with visit details, helping the front desk and care team stay organized.',
  },
]

const testimonials = [
  {
    quote:
      'The booking process feels clear and professional, and the clinic team explains everything in a way that helps you feel prepared.',
    name: 'Megan R.',
    pet: 'Owner of Charlie, Golden Retriever',
  },
  {
    quote:
      'It is the kind of neighborhood clinic experience you want: warm staff, efficient check-in, and thoughtful follow-up.',
    name: 'Daniel T.',
    pet: 'Owner of Luna, Domestic Shorthair',
  },
  {
    quote:
      'From routine vaccines to quick sick visits, the communication is consistent and reassuring every step of the way.',
    name: 'Priya S.',
    pet: 'Owner of Mochi, Cavapoo',
  },
]

const trustSignals = [
  'Friendly support for everyday pet care needs',
  'Clear appointment scheduling and visit expectations',
  'Built for small-clinic organization and reliable follow-through',
]
</script>

<template>
  <PageContainer>
    <div class="homepage">
      <section class="hero-section">
        <div class="hero-copy">
          <el-tag class="hero-tag" round type="success">Trusted local pet care</el-tag>
          <h1>Modern booking and clinic coordination for a caring neighborhood practice.</h1>
          <p>
            PawCare Hub gives pet owners a simple way to book care while helping the clinic
            team stay organized, responsive, and ready for every visit.
          </p>

          <div class="hero-actions">
            <a href="#services">
              <el-button type="primary" size="large">Explore Services</el-button>
            </a>
            <a href="#booking-flow">
              <el-button plain size="large">See How Booking Works</el-button>
            </a>
          </div>

          <div class="hero-points">
            <span>Designed for local US pet clinics</span>
            <span>Warm, professional experience</span>
            <span>Mock content ready for iteration</span>
          </div>
        </div>

        <el-card class="hero-panel" shadow="never">
          <p class="eyebrow">Clinic snapshot</p>
          <h2>Support a smoother day from the front desk to the exam room.</h2>
          <p class="panel-copy">
            This homepage direction presents PawCare Hub as a real business product: approachable
            for pet owners, practical for staff, and grounded in trustworthy service language.
          </p>

          <div class="hero-stats">
            <div>
              <strong>4 core</strong>
              <span>service categories featured</span>
            </div>
            <div>
              <strong>3-step+</strong>
              <span>trust-building content areas</span>
            </div>
            <div>
              <strong>1 clear</strong>
              <span>path to booking action</span>
            </div>
          </div>
        </el-card>
      </section>

      <section id="services" class="content-section">
        <div class="section-heading">
          <span>Featured services</span>
          <h2>Care options that feel familiar, useful, and easy to understand.</h2>
          <p>
            The homepage highlights common pet clinic services so owners can quickly identify the
            kind of care they need before booking.
          </p>
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
            <span class="card-note">{{ service.details }}</span>
          </el-card>
        </div>
      </section>

      <section class="content-section team-section">
        <div class="section-heading">
          <span>About the clinic</span>
          <h2>A welcoming team focused on clear communication and dependable care.</h2>
          <p>
            This section introduces the people behind the clinic in a simple, credible way without
            overloading the page with detail.
          </p>
        </div>

        <div class="team-layout">
          <el-card class="team-story" shadow="never">
            <p>
              PawCare Hub is designed for a small local clinic that values personal service,
              organized operations, and a calm visit experience for both pets and their owners.
            </p>
            <p>
              From preventive appointments to follow-up care, the team aims to make each visit feel
              prepared, supportive, and easy to navigate.
            </p>
          </el-card>

          <div class="team-members">
            <el-card class="member-card" shadow="hover">
              <div class="member-header">
                <el-avatar size="large">DR</el-avatar>
                <div>
                  <h3>Dr. Rivera</h3>
                  <p>Lead veterinarian</p>
                </div>
              </div>
              <span>Focused on preventive medicine, client education, and consistent care plans.</span>
            </el-card>

            <el-card class="member-card" shadow="hover">
              <div class="member-header">
                <el-avatar size="large">AT</el-avatar>
                <div>
                  <h3>Ash Thompson</h3>
                  <p>Practice manager</p>
                </div>
              </div>
              <span>Keeps scheduling, communication, and day-to-day clinic flow running smoothly.</span>
            </el-card>
          </div>
        </div>
      </section>

      <section id="booking-flow" class="content-section booking-section">
        <div class="section-heading">
          <span>Booking flow</span>
          <h2>A simple path from browsing services to showing up prepared.</h2>
          <p>
            The flow below keeps expectations clear for pet owners while reinforcing the product’s
            operational value for the clinic.
          </p>
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
          <span>Why pet owners trust the clinic</span>
          <h2>Professional signals that make the business feel established and dependable.</h2>
          <p>
            Testimonials and practical trust markers help the homepage feel real without needing
            live data or backend integrations yet.
          </p>
        </div>

        <div class="trust-layout">
          <el-card class="signals-card" shadow="never">
            <h3>What families can expect</h3>
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
              <p class="testimonial-quote">“{{ testimonial.quote }}”</p>
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
  gap: 32px;
  padding: 8px 0 40px;
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.95fr);
  gap: 24px;
  align-items: stretch;
}

.hero-copy,
.hero-panel,
.info-card,
.team-story,
.member-card,
.flow-card,
.signals-card,
.testimonial-card {
  border: 1px solid rgba(217, 226, 242, 0.85);
  border-radius: 24px;
}

.hero-copy {
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(25, 169, 116, 0.14), transparent 30%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(241, 248, 255, 0.96));
  box-shadow: var(--pc-shadow);
}

.hero-tag {
  margin-bottom: 18px;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(2.3rem, 4vw, 4rem);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.hero-copy p {
  margin: 18px 0 0;
  max-width: 640px;
  font-size: 1.05rem;
  line-height: 1.7;
  color: var(--pc-muted);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 26px;
}

.hero-points {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
  color: var(--pc-text);
  font-size: 0.95rem;
}

.hero-points span {
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(217, 226, 242, 0.9);
  border-radius: 999px;
}

.hero-panel {
  padding: 28px;
  background: linear-gradient(180deg, #ffffff 0%, #f4f9f6 100%);
}

.eyebrow,
.section-heading span {
  display: inline-block;
  color: var(--pc-accent);
  font-size: 0.86rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-panel h2 {
  margin: 10px 0 12px;
  font-size: 1.65rem;
  line-height: 1.2;
}

.panel-copy {
  margin: 0;
  color: var(--pc-muted);
  line-height: 1.7;
}

.hero-stats {
  display: grid;
  gap: 14px;
  margin-top: 28px;
}

.hero-stats div {
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(217, 226, 242, 0.85);
  border-radius: 18px;
}

.hero-stats strong {
  display: block;
  margin-bottom: 4px;
  font-size: 1.05rem;
}

.hero-stats span {
  color: var(--pc-muted);
}

.content-section {
  padding: 8px 0;
}

.section-heading {
  max-width: 760px;
  margin-bottom: 20px;
}

.section-heading h2 {
  margin: 10px 0 12px;
  font-size: clamp(1.8rem, 3vw, 2.6rem);
  line-height: 1.15;
}

.section-heading p {
  margin: 0;
  color: var(--pc-muted);
  line-height: 1.7;
}

.card-grid,
.flow-grid,
.testimonial-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
}

.info-card,
.flow-card,
.member-card,
.testimonial-card,
.signals-card,
.team-story {
  background: rgba(255, 255, 255, 0.92);
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
.signals-card h3,
.member-header h3 {
  margin: 0;
  font-size: 1.15rem;
}

.info-card p,
.flow-card p,
.team-story p,
.testimonial-quote {
  margin: 0;
  color: var(--pc-muted);
  line-height: 1.7;
}

.card-note {
  display: inline-block;
  margin-top: 14px;
  color: var(--pc-text);
  font-size: 0.94rem;
}

.team-layout,
.trust-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  gap: 18px;
}

.team-story {
  padding: 28px;
}

.team-story p + p {
  margin-top: 14px;
}

.team-members {
  display: grid;
  gap: 18px;
}

.member-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.member-header p,
.member-card span,
.testimonial-card span {
  color: var(--pc-muted);
}

.flow-card {
  position: relative;
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
  background: var(--pc-primary-soft);
  color: var(--pc-primary);
  font-weight: 700;
}

.signals-card {
  padding: 28px;
}

.signals-card ul {
  margin: 16px 0 0;
  padding-left: 18px;
  color: var(--pc-muted);
  line-height: 1.9;
}

.testimonial-card strong {
  display: block;
  margin-top: 18px;
}

.testimonial-card span {
  display: block;
  margin-top: 6px;
}

@media (max-width: 960px) {
  .hero-section,
  .team-layout,
  .trust-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .homepage {
    gap: 24px;
  }

  .hero-copy,
  .hero-panel,
  .team-story,
  .signals-card {
    padding: 22px;
  }

  .hero-copy h1 {
    font-size: 2.2rem;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-actions a,
  .hero-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
