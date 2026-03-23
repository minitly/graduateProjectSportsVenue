<script setup>
const options = {
  fpsLimit: 60,
  background: {
    color: {
      value: 'transparent'
    }
  },
  particles: {
    number: {
      value: 90,
      density: {
        enable: true,
        area: 900
      }
    },
    color: {
      value: ['#60a5fa', '#22d3ee', '#a78bfa', '#f472b6']
    },
    links: {
      enable: true,
      color: '#93c5fd',
      distance: 160,
      opacity: 0.25,
      width: 1.2
    },
    move: {
      enable: true,
      speed: 1.35,
      direction: 'none',
      outModes: {
        default: 'out'
      }
    },
    opacity: {
      value: { min: 0.25, max: 0.55 }
    },
    size: {
      value: { min: 1, max: 3.8 }
    },
    twinkle: {
      particles: {
        enable: true,
        frequency: 0.6,
        opacity: 0.5
      }
    }
  },
  interactivity: {
    events: {
      onHover: {
        enable: true,
        mode: 'grab'
      },
      onClick: {
        enable: true,
        mode: 'push'
      }
    },
    modes: {
      grab: {
        distance: 180,
        links: {
          opacity: 0.35
        }
      },
      push: {
        quantity: 3
      }
    }
  },
  detectRetina: true
}
</script>

<template>
  <div class="particles-layer" aria-hidden="true">
    <vue-particles id="tsparticles" :options="options" />
  </div>
</template>
