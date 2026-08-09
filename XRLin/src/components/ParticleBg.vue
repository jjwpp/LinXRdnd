<template>
  <div class="particles" aria-hidden="true">
    <!-- Floating ember/dust particles -->
    <span
      v-for="n in 30"
      :key="'p' + n"
      class="particle"
      :class="particleType(n)"
      :style="particleStyle(n)"
    ></span>
    <!-- Slow drifting magic motes -->
    <span
      v-for="n in 8"
      :key="'m' + n"
      class="mote"
      :style="moteStyle(n)"
    ></span>
    <!-- Large slow golden rune-orbs that breathe -->
    <span
      v-for="n in 4"
      :key="'o' + n"
      class="rune-orb"
      :style="{
        left: ((n * 26 + 8) % 100) + '%',
        top: ((n * 31 + 15) % 80) + '%',
        animationDelay: (n * 1.6) + 's',
        animationDuration: (9 + (n % 3) * 2) + 's',
      }"
    ></span>
  </div>
</template>

<script setup>
function particleType(n) {
  const types = ["gold", "arcane", "ember", "gold"];
  return types[n % types.length];
}

function particleStyle(n) {
  const size = 1.5 + (n % 4) * 0.8;
  const left = ((n * 17 + 3) % 100);
  const delay = (n * 0.7) % 10;
  const duration = 8 + (n % 6) * 2;
  const drift = ((n * 13) % 60) - 30;
  return {
    width: size + "px",
    height: size + "px",
    left: left + "%",
    animationDelay: delay + "s",
    animationDuration: duration + "s",
    "--drift": drift + "px",
    opacity: 0.1 + (n % 5) * 0.06,
  };
}

function moteStyle(n) {
  const size = 3 + (n % 3);
  const left = ((n * 23 + 7) % 100);
  const top = ((n * 31 + 11) % 100);
  const delay = (n * 1.3) % 6;
  const duration = 12 + (n % 4) * 3;
  return {
    width: size + "px",
    height: size + "px",
    left: left + "%",
    top: top + "%",
    animationDelay: delay + "s",
    animationDuration: duration + "s",
  };
}
</script>

<style scoped>
.particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

/* Small floating particles rising upward */
.particle {
  position: absolute;
  bottom: -10px;
  border-radius: 50%;
  animation: floatUp linear infinite;
}

.particle.gold {
  background: var(--gold-bright);
  box-shadow: 0 0 5px var(--gold-glow), 0 0 12px var(--gold-glow), 0 0 20px rgba(201, 162, 39, .15);
}
.particle.arcane {
  background: var(--arcane-glow);
  box-shadow: 0 0 5px var(--arcane-soft), 0 0 12px var(--arcane-soft), 0 0 20px rgba(125, 90, 170, .2);
}
.particle.ember {
  background: var(--ember);
  box-shadow: 0 0 5px var(--ember-soft), 0 0 12px var(--ember-soft), 0 0 18px rgba(196, 90, 42, .18);
}

@keyframes floatUp {
  0% {
    transform: translateY(0) translateX(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: var(--p-opacity, 0.35);
  }
  45% {
    transform: translateY(-48vh) translateX(var(--drift, 20px)) scale(1.3);
  }
  90% {
    opacity: var(--p-opacity, 0.35);
  }
  100% {
    transform: translateY(-110vh) translateX(calc(var(--drift, 20px) * -1)) scale(0.5);
    opacity: 0;
  }
}

/* Larger slow-drifting motes that pulse */
.mote {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, var(--gold-glow), transparent 70%);
  animation: driftPulse ease-in-out infinite;
}

@keyframes driftPulse {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0;
  }
  20% {
    opacity: 0.45;
  }
  50% {
    transform: translate(24px, -34px) scale(1.6);
    opacity: 0.65;
  }
  80% {
    opacity: 0.3;
  }
}

/* Large breathing rune-orbs — soft arcane/gold auras */
.rune-orb {
  position: absolute;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(201, 162, 39, .1) 0%, rgba(93, 58, 138, .06) 40%, transparent 70%);
  border: 1px solid var(--line-gold);
  opacity: 0;
  animation: orbBreathe ease-in-out infinite;
}
.rune-orb::before {
  content: "";
  position: absolute;
  inset: 14px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
  opacity: .4;
}
@keyframes orbBreathe {
  0%, 100% { transform: scale(.7) rotate(0); opacity: 0; }
  30% { opacity: .35; }
  60% { transform: scale(1.1) rotate(60deg); opacity: .5; }
}

@media (prefers-reduced-motion: reduce) {
  .particle, .mote, .rune-orb {
    animation: none;
    opacity: 0.05;
  }
}
</style>
