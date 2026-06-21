<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { staffApi } from '@/api/staff'

const heroDogImage = new URL('../../open-design-export/mqna4pb3-dog1.avif', import.meta.url).href
const catImage = new URL('../../open-design-export/mqnb40hn-cat1.avif', import.meta.url).href
const smallDogImage = new URL('../../open-design-export/mqnaheqe-dog2.avif', import.meta.url).href
const poodleImage = new URL('../../open-design-export/mqnaz45v-dog3.avif', import.meta.url).href

const featuredServices = [
  {
    title: 'Wellness and prevention',
    description: 'Routine exams, vaccine planning, and nutrition support.',
    tag: 'Core care',
    bullets: ['Gentle first visits', 'Easy home-care plans', 'Clear recovery notes'],
    variant: 'accent',
  },
  {
    title: 'Diagnostics and treatment',
    description: 'Lab review, imaging coordination, and calm recovery guidance.',
    tag: 'Specialty support',
  },
  {
    title: 'Cat-first comfort',
    description: 'Quiet handling and calm room flow for feline visits.',
    tag: 'Cat care',
    image: catImage,
    imageAlt: 'A cat portrait used as a service visual.',
  },
  {
    title: 'Puppy guidance',
    description: 'Confident starts for growing dogs and first-time pet parents.',
    tag: 'Puppy care',
    image: smallDogImage,
    imageAlt: 'A small dog portrait used as a service visual.',
  },
  {
    title: 'Follow-up that feels human',
    description: 'Friendly reminders and simple answers after the visit.',
    tag: 'Continuity',
  },
  {
    title: 'Coat and skin concerns',
    description: 'Support for itchiness, sensitivities, and recurring skin issues.',
    tag: 'Skin support',
    image: poodleImage,
    imageAlt: 'A poodle portrait used as a service visual.',
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
      'They explain care options in a calm way, so even a stressful visit feels manageable.',
    title: 'Easy to understand',
    footer: 'For first-time appointments and follow-up questions.',
  },
  {
    quote:
      'Appointments feel paced and gentle, especially for shy pets that need extra patience.',
    title: 'Kind with nervous pets',
    footer: 'Made for pets that do not love clinics on day one.',
  },
]

const trustSignals = [
  'Clear scheduling and visit expectations',
  'Friendly support for everyday pet care needs',
  'Designed for dependable small-clinic operations',
]

const homepageStaff = ref([])
const isLoadingStaff = ref(false)

let revealObserver

const hasHomepageStaff = computed(() => homepageStaff.value.length > 0)

function getInitials(name) {
  return (name || '')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || '')
    .join('')
}

async function loadHomepageStaff() {
  isLoadingStaff.value = true

  try {
    const { data } = await staffApi.listHomepage()
    homepageStaff.value = Array.isArray(data) ? data : []
  } catch {
    homepageStaff.value = []
  } finally {
    isLoadingStaff.value = false
    await nextTick()
    observeRevealItems()
  }
}

function observeRevealItems() {
  const revealItems = document.querySelectorAll('.reveal-section:not(.is-visible), .reveal-card:not(.is-visible)')

  if (!revealObserver) {
    revealItems.forEach((item) => {
      item.classList.add('is-visible')
    })
    return
  }

  revealItems.forEach((item) => {
    revealObserver.observe(item)
  })
}

function setupRevealObserver() {
  if (typeof window === 'undefined' || !('IntersectionObserver' in window)) {
    observeRevealItems()
    return
  }

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
          revealObserver.unobserve(entry.target)
        }
      })
    },
    {
      rootMargin: '0px 0px -12% 0px',
      threshold: 0.16,
    },
  )

  observeRevealItems()
}

onMounted(() => {
  loadHomepageStaff()
  setupRevealObserver()
})

onUnmounted(() => {
  revealObserver?.disconnect()
})
</script>

<template>
  <div class="homepage od-homepage">
    <section id="hero" class="od-section od-hero-wrap">
      <div class="od-container od-hero">
        <div class="od-hero-grid">
          <div class="od-hero-copy reveal-section is-visible">
            <p class="od-eyebrow">Compassionate veterinary home for cats and dogs</p>
            <h1>Gentle care for every wag, nap, and nervous first visit.</h1>
            <p class="od-lead">
              PawCare Hub helps families discover services, book confidently, and arrive prepared
              with a clinic experience that feels warm, clear, and trustworthy.
            </p>

            <div class="od-hero-cta">
              <a class="od-btn od-btn-primary" href="#services">Explore Care</a>
              <a class="od-btn od-btn-secondary" href="#booking-flow">How Booking Works</a>
            </div>

            <div class="od-hero-points" aria-label="Care highlights">
              <div class="od-hero-point">
                <strong>Cats</strong>
                <span>Calm handling and quiet support for sensitive patients.</span>
              </div>
              <div class="od-hero-point">
                <strong>Dogs</strong>
                <span>Preventive care, recovery guidance, and everyday support.</span>
              </div>
              <div class="od-hero-point">
                <strong>Same week</strong>
                <span>Clear visit windows for wellness, concerns, and follow-ups.</span>
              </div>
            </div>
          </div>

          <div class="od-hero-visual reveal-section is-visible stagger-1" aria-label="Clinic visual preview">
            <div class="od-hero-shell">
              <div class="od-hero-photo">
                <img :src="heroDogImage" alt="A dog portrait for the PawCare Hub hero image." />
              </div>
              <aside class="od-hero-badge" aria-label="Visit promise">
                <strong>From check-in to tail wag.</strong>
                <p>Soft lighting, unrushed appointments, and plain-language next steps.</p>
              </aside>
              <div class="od-hero-sun" aria-hidden="true">warm care<br />daily</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="services" class="od-section od-section-warm">
      <div class="od-container">
        <div class="od-section-intro reveal-section">
          <p class="od-eyebrow">Pet specialty services</p>
          <h2>Built for everyday wellness, urgent questions, and long-term care.</h2>
          <p class="od-lead">
            Focused service cards keep the homepage practical while matching the warmer clinic tone
            of the Open Design reference.
          </p>
        </div>

        <div class="od-services-grid">
          <article
            v-for="service in featuredServices"
            :key="service.title"
            class="od-service-card reveal-card"
            :class="[
              service.variant === 'accent' ? 'od-service-card--accent' : '',
              service.image ? 'od-service-card--visual' : '',
              `stagger-${(featuredServices.indexOf(service) % 3) + 1}`,
            ]"
          >
            <template v-if="service.image">
              <div class="od-card-pet">
                <img :src="service.image" :alt="service.imageAlt" />
              </div>
              <div class="od-service-caption">
                <span class="od-service-meta">{{ service.tag }}</span>
                <h3>{{ service.title }}</h3>
                <p>{{ service.description }}</p>
              </div>
            </template>

            <div v-else class="od-service-body">
              <span class="od-service-meta">{{ service.tag }}</span>
              <h3>{{ service.title }}</h3>
              <p>{{ service.description }}</p>
              <ul v-if="service.bullets">
                <li v-for="bullet in service.bullets" :key="bullet">{{ bullet }}</li>
              </ul>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section id="reviews" class="od-section od-section-ivory">
      <div class="od-container od-reviews-grid">
        <div class="od-review-feature reveal-section">
          <p class="od-eyebrow">Client notes</p>
          <div class="od-quote-mark" aria-hidden="true">&ldquo;</div>
          <blockquote>
            Booking felt straightforward, and the clinic communication made the whole visit feel calm.
          </blockquote>
          <p class="od-review-author">
            Pet parent feedback shaped into the page voice: warm, reassuring, and easy to trust.
          </p>
        </div>

        <article
          v-for="testimonial in testimonials"
          :key="testimonial.title"
          class="od-testimonial-card reveal-card"
          :class="`stagger-${testimonials.indexOf(testimonial) + 1}`"
        >
          <h3>{{ testimonial.title }}</h3>
          <p>{{ testimonial.quote }}</p>
          <footer>{{ testimonial.footer }}</footer>
        </article>
      </div>
    </section>

    <section id="booking-flow" class="od-section od-section-soft">
      <div class="od-container">
        <div class="od-section-intro reveal-section">
          <p class="od-eyebrow">Booking flow</p>
          <h2>A clear path from choosing care to arriving prepared.</h2>
          <p class="od-lead">
            The existing booking explanation is preserved, but restyled to match the Open Design
            card rhythm.
          </p>
        </div>

        <div class="od-booking-grid">
          <article
            v-for="(step, index) in bookingSteps"
            :key="step.title"
            class="od-booking-card reveal-card"
            :class="`stagger-${(index % 3) + 1}`"
          >
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section id="care-team" class="od-section od-section-deep">
      <div class="od-container od-split-panels">
        <article class="od-team-panel reveal-section">
          <p class="od-eyebrow">Doctors and care team</p>
          <h2>A close team with a soft-spoken bedside manner.</h2>
          <p class="od-lead">
            Public staff profiles from PawCare Hub are shown here when available, keeping the
            existing homepage API behavior intact.
          </p>

          <el-skeleton v-if="isLoadingStaff" :rows="4" animated />

          <div v-else-if="hasHomepageStaff" class="od-team-grid">
            <article
              v-for="staffMember in homepageStaff"
              :key="staffMember.id"
              class="od-team-card reveal-card"
              :class="`stagger-${(homepageStaff.indexOf(staffMember) % 3) + 1}`"
            >
              <div class="od-team-avatar">
                <el-avatar
                  v-if="staffMember.photoUrl"
                  :src="staffMember.photoUrl"
                  :size="58"
                />
                <span v-else>{{ getInitials(staffMember.displayName) }}</span>
              </div>
              <div>
                <strong>{{ staffMember.displayName }}</strong>
                <span>{{ staffMember.title }}</span>
                <p>{{ staffMember.bio }}</p>
              </div>
            </article>
          </div>

          <div v-else class="od-team-grid">
            <article class="od-team-card">
              <strong>Meet our care team</strong>
              <span>Public profiles coming soon</span>
              <p>The clinic can spotlight veterinarians and care staff here as profiles are prepared.</p>
            </article>
            <article
              v-for="signal in trustSignals.slice(0, 2)"
              :key="signal"
              class="od-team-card reveal-card"
              :class="`stagger-${trustSignals.indexOf(signal) + 1}`"
            >
              <strong>{{ signal }}</strong>
              <span>PawCare Hub</span>
              <p>Warm operational details help families understand what to expect before arrival.</p>
            </article>
          </div>
        </article>

        <aside id="visit" class="od-contact-panel reveal-section stagger-1">
          <p class="od-eyebrow">Location and opening hours</p>
          <h2>Visit the clinic</h2>
          <div class="od-contact-card">
            <h3>Neighborhood clinic</h3>
            <p>
              Open for cat and dog appointments, follow-ups, and care questions.
            </p>
            <div class="od-hours-list">
              <div class="od-hours-row"><strong>Mon to Fri</strong><span>8:30 AM - 6:30 PM</span></div>
              <div class="od-hours-row"><strong>Saturday</strong><span>9:00 AM - 3:00 PM</span></div>
              <div class="od-hours-row"><strong>Sunday</strong><span>Closed</span></div>
            </div>
            <div class="od-contact-actions">
              <a class="od-btn od-btn-primary" href="#booking-flow">How booking works</a>
              <a class="od-btn od-btn-secondary" href="#services">Review services</a>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <section class="od-section od-cta-band">
      <div class="od-container reveal-section">
        <p class="od-eyebrow">Ready when they are</p>
        <h2>Bring your cat or dog in with confidence.</h2>
        <p class="od-lead">
          A friendly, modern veterinary homepage for pet parents who want warmth first and clarity
          right behind it.
        </p>
        <div class="od-hero-cta od-hero-cta--center">
          <a class="od-btn od-btn-secondary" href="#services">Start with a wellness visit</a>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.od-homepage {
  width: calc(100% + 40px);
  margin: -32px -20px -48px;
  overflow-x: clip;
  background: #f5f4ed;
  color: #141413;
  font-family: "Segoe UI", Arial, system-ui, -apple-system, sans-serif;
}

.od-container {
  width: min(100% - 48px, 1200px);
  margin-inline: auto;
}

.od-section {
  padding-block: clamp(48px, 8vw, 96px);
}

.od-section + .od-section {
  border-top: 1px solid #e8e6dc;
}

.od-section-warm {
  background: #ebe8dd;
}

.od-section-ivory {
  background: #faf9f5;
}

.od-section-soft {
  background: #f1efe7;
}

.od-section-deep {
  background: linear-gradient(180deg, #f7f5ee 0%, #e9e5d8 100%);
}

.od-eyebrow {
  margin: 0 0 16px;
  color: #c96442;
  font-family: "Courier New", ui-monospace, monospace;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.od-lead {
  margin: 0;
  max-width: 54ch;
  color: #5e5d59;
  font-size: clamp(1.06rem, 2vw, 1.28rem);
  line-height: 1.64;
}

.od-homepage h1,
.od-homepage h2,
.od-homepage h3 {
  margin: 0;
  color: #141413;
  font-family: Georgia, "Times New Roman", serif;
  font-weight: 500;
}

.od-homepage h1 {
  max-width: 11ch;
  font-size: clamp(3rem, 7vw, 4.9rem);
  line-height: 1.02;
  letter-spacing: -0.04em;
}

.od-homepage h2 {
  font-size: clamp(2rem, 4vw, 3.25rem);
  line-height: 1.08;
  letter-spacing: -0.025em;
}

.od-homepage h3 {
  font-size: clamp(1.3rem, 2.4vw, 1.9rem);
  line-height: 1.18;
}

.od-hero-wrap {
  position: relative;
  overflow: clip;
  isolation: isolate;
  background: #f5f4ed;
}

.od-hero-wrap::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 86% 12%, rgba(201, 100, 66, 0.2), transparent 44%),
    radial-gradient(circle at 10% 72%, rgba(255, 255, 255, 0.72), transparent 32%);
  pointer-events: none;
  z-index: -1;
}

.od-hero {
  padding-block: clamp(44px, 9vw, 92px);
}

.od-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(340px, 0.98fr);
  gap: clamp(32px, 5vw, 72px);
  align-items: center;
}

.od-hero-copy .od-lead {
  margin-top: 20px;
}

.od-hero-cta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.od-hero-cta--center {
  justify-content: center;
}

.od-btn {
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 18px;
  border: 1px solid transparent;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 650;
  transition:
    transform 150ms ease,
    background-color 150ms ease,
    border-color 150ms ease;
}

.od-btn:active {
  transform: translateY(1px);
}

.od-btn-primary {
  color: #faf9f5;
  background: #c96442;
  border-color: #c96442;
  box-shadow: 0 12px 24px rgba(201, 100, 66, 0.18);
}

.od-btn-primary:hover {
  background: #b85a3b;
  border-color: #b85a3b;
}

.od-btn-secondary {
  color: #3d3d3a;
  background: #faf9f5;
  border-color: #e8e6dc;
  box-shadow: 0 0 0 1px #f0eee6;
}

.od-btn-secondary:hover {
  border-color: #3d3d3a;
}

.od-hero-points {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-top: 32px;
  padding-top: 22px;
  border-top: 1px solid #e8e6dc;
}

.od-hero-point {
  min-width: 120px;
}

.od-hero-point strong {
  display: block;
  color: #141413;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(1.75rem, 4vw, 2.65rem);
  font-weight: 500;
  line-height: 1;
}

.od-hero-point span {
  display: block;
  max-width: 20ch;
  margin-top: 8px;
  color: #87867f;
  font-size: 0.82rem;
  line-height: 1.45;
}

.od-hero-visual {
  min-height: 620px;
  display: grid;
  align-items: center;
  position: relative;
}

.od-hero-shell {
  position: relative;
  padding: 28px;
  border: 1px solid #e8e6dc;
  border-radius: 36px;
  background: linear-gradient(135deg, #faf9f5 0%, #e8e6dc 100%);
  box-shadow: 0 18px 40px rgba(20, 20, 19, 0.07);
}

.od-hero-photo {
  width: min(100%, 520px);
  min-height: 560px;
  margin-left: auto;
  border: 1px solid #e8e6dc;
  border-radius: 30px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(20, 20, 19, 0.07);
}

.od-hero-photo img {
  width: 100%;
  height: 100%;
  min-height: inherit;
  object-fit: cover;
}

.od-hero-badge {
  position: absolute;
  left: 0;
  bottom: 42px;
  width: min(220px, 56%);
  padding: 24px;
  border: 1px solid #f0eee6;
  border-radius: 24px;
  background: #faf9f5;
  box-shadow: 0 18px 40px rgba(20, 20, 19, 0.07);
}

.od-hero-badge strong {
  display: block;
  margin-bottom: 12px;
  color: #141413;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.75rem;
  font-weight: 500;
  line-height: 1.05;
}

.od-hero-badge p {
  margin: 0;
  color: #5e5d59;
  font-size: 0.9rem;
  line-height: 1.45;
}

.od-hero-sun {
  position: absolute;
  top: 50%;
  left: -12px;
  width: 110px;
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  padding: 18px;
  border-radius: 999px;
  background: #f2eee3;
  color: #141413;
  font-family: "Courier New", ui-monospace, monospace;
  font-size: 0.68rem;
  letter-spacing: 0.14em;
  text-align: center;
  text-transform: uppercase;
  transform: translateY(-50%);
  box-shadow: 0 0 0 1px #e8e6dc, 0 18px 36px rgba(20, 20, 19, 0.07);
}

.od-section-intro {
  max-width: 44rem;
  margin-bottom: 40px;
}

.od-section-intro .od-lead {
  margin-top: 18px;
}

.od-services-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.od-service-card {
  min-height: 272px;
  position: relative;
  overflow: hidden;
  border: 1px solid #f0eee6;
  border-radius: 28px;
  background: #faf9f5;
  box-shadow: 0 0 0 1px #f0eee6;
}

.od-service-card--accent {
  color: #faf9f5;
  background: linear-gradient(180deg, #ce704f 0%, #c96442 100%);
  border-color: rgba(201, 100, 66, 0.45);
}

.od-service-body {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
}

.od-service-body h3 {
  margin-top: auto;
}

.od-service-body p,
.od-service-body li {
  color: #5e5d59;
  font-size: 0.95rem;
  line-height: 1.5;
}

.od-service-card--accent h3,
.od-service-card--accent p,
.od-service-card--accent li,
.od-service-card--accent .od-service-meta {
  color: #faf9f5;
}

.od-service-body p,
.od-service-body ul,
.od-service-caption p {
  margin: 0;
}

.od-service-body ul {
  padding-left: 18px;
}

.od-service-meta {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: #e8e6dc;
  color: #87867f;
  font-family: "Courier New", ui-monospace, monospace;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.od-service-card--accent .od-service-meta {
  background: rgba(250, 249, 245, 0.16);
}

.od-card-pet {
  position: absolute;
  inset: 0;
  background: linear-gradient(145deg, #ded7c8, #f7f1e8);
}

.od-card-pet img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.od-service-caption {
  position: absolute;
  inset: auto 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 132px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(250, 249, 245, 0.88);
  box-shadow: 0 0 0 1px #f0eee6;
  backdrop-filter: blur(14px);
}

.od-service-caption h3 {
  margin-top: auto;
  font-size: 1.25rem;
}

.od-service-caption p {
  color: #5e5d59;
  font-size: 0.78rem;
  line-height: 1.4;
}

.od-reviews-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr 1fr;
  gap: 20px;
  align-items: start;
}

.od-quote-mark {
  margin-bottom: -26px;
  color: rgba(201, 100, 66, 0.22);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 7.5rem;
  line-height: 0.7;
}

.od-review-feature blockquote {
  margin: 0;
  color: #141413;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(1.65rem, 2.4vw, 2.15rem);
  line-height: 1.24;
}

.od-review-author {
  margin: 16px 0 0;
  color: #87867f;
  font-size: 0.9rem;
}

.od-testimonial-card,
.od-booking-card,
.od-team-panel,
.od-contact-panel {
  border: 1px solid #f0eee6;
  border-radius: 28px;
  background: #faf9f5;
  box-shadow: 0 0 0 1px #f0eee6;
}

.od-testimonial-card {
  min-height: 100%;
  padding: 24px;
}

.od-testimonial-card p {
  margin: 14px 0 0;
  color: #5e5d59;
}

.od-testimonial-card footer {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #e8e6dc;
  color: #141413;
  font-size: 0.9rem;
}

.od-booking-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.od-booking-card {
  padding: 22px;
}

.od-booking-card span {
  color: #c96442;
  font-family: "Courier New", ui-monospace, monospace;
  font-weight: 700;
}

.od-booking-card h3 {
  margin-top: 42px;
}

.od-booking-card p {
  margin: 12px 0 0;
  color: #5e5d59;
}

.od-split-panels {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 24px;
}

.od-team-panel,
.od-contact-panel {
  padding: 28px;
  background: rgba(250, 249, 245, 0.84);
}

.od-team-panel .od-lead {
  margin-top: 16px;
}

.od-team-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 28px;
}

.od-team-card {
  display: flex;
  gap: 14px;
  padding: 18px;
  border: 1px solid #e8e6dc;
  border-radius: 22px;
  background: #eee9de;
}

.od-team-avatar {
  width: 58px;
  height: 58px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: #d8caba;
  color: #7b452e;
  font-weight: 750;
}

.od-team-card strong {
  display: block;
  color: #141413;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 1.4rem;
  font-weight: 500;
  line-height: 1.12;
}

.od-team-card span {
  display: block;
  margin: 6px 0 12px;
  color: #87867f;
  font-size: 0.82rem;
}

.od-team-card p {
  margin: 0;
  color: #5e5d59;
  font-size: 0.9rem;
  line-height: 1.5;
}

.od-contact-card {
  margin-top: 22px;
  padding: 22px;
  border: 1px solid #e8e6dc;
  border-radius: 22px;
  background: #faf9f5;
}

.od-contact-card p {
  margin: 12px 0 0;
  color: #5e5d59;
  line-height: 1.6;
}

.od-hours-list {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.od-hours-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e6dc;
  color: #5e5d59;
}

.od-hours-row strong {
  color: #141413;
  font-weight: 600;
}

.od-contact-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.od-cta-band {
  position: relative;
  overflow: hidden;
  text-align: center;
  background: linear-gradient(135deg, #cf704d 0%, #c96442 100%);
}

.od-cta-band::before,
.od-cta-band::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  background: rgba(250, 249, 245, 0.16);
}

.od-cta-band::before {
  width: 320px;
  height: 320px;
  top: -140px;
  right: -60px;
}

.od-cta-band::after {
  width: 210px;
  height: 210px;
  bottom: -80px;
  left: 8%;
}

.od-cta-band .od-container {
  position: relative;
  z-index: 1;
}

.od-cta-band .od-eyebrow,
.od-cta-band h2,
.od-cta-band .od-lead {
  color: #faf9f5;
}

.od-cta-band .od-lead {
  margin: 18px auto 0;
  max-width: 38ch;
}

@media (max-width: 1180px) {
  .od-hero-grid,
  .od-services-grid,
  .od-reviews-grid,
  .od-split-panels {
    grid-template-columns: 1fr;
  }

  .od-hero-visual {
    min-height: auto;
    order: -1;
  }

  .od-hero-photo {
    width: 100%;
    min-height: 420px;
    margin-left: 0;
  }

  .od-hero-badge {
    left: 20px;
    bottom: 20px;
  }

  .od-hero-sun {
    top: auto;
    left: auto;
    right: 18px;
    bottom: -18px;
    transform: none;
  }

  .od-booking-grid,
  .od-team-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .od-homepage {
    width: calc(100% + 40px);
    margin-inline: -20px;
  }

  .od-container {
    width: min(100% - 24px, 1200px);
  }

  .od-hero {
    padding-block: 28px 52px;
  }

  .od-hero-photo {
    min-height: 340px;
  }

  .od-hero-shell {
    padding: 18px;
  }

  .od-hero-badge {
    position: static;
    width: 100%;
    margin-top: 16px;
  }

  .od-hero-sun {
    width: 92px;
    right: 12px;
    bottom: -28px;
    font-size: 0.62rem;
  }

  .od-hero-points {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
  }

  .od-hero-cta,
  .od-contact-actions {
    flex-direction: column;
  }

  .od-btn {
    width: 100%;
  }

  .od-booking-grid,
  .od-team-grid {
    grid-template-columns: 1fr;
  }

  .od-team-card {
    flex-direction: column;
  }

  .od-hours-row {
    grid-template-columns: 1fr;
  }
}
</style>
