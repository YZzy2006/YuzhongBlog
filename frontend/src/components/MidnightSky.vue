<template>
  <div class="midnight-sky" v-show="isNight">
    <div class="sky-canvas">
      <div class="stars stars-1"></div>
      <div class="stars stars-2"></div>
      <div class="stars stars-3"></div>
      <div class="meteor m1"></div>
      <div class="meteor m2"></div>
      <div class="meteor m3"></div>
      <div class="moon"></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  isNight: { type: Boolean, default: false }
})
</script>

<style scoped>
.midnight-sky {
  position: fixed;
  inset: 0;
  z-index: -1;
  background: linear-gradient(180deg, #0c1445, #1a1a3e, #0d1b2a);
  animation: fadeIn 2s ease;
}

.sky-canvas {
  width: 100%;
  height: 100%;
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #0c1445, #1a1a3e, #0d1b2a);
}

.stars {
  position: absolute;
  inset: 0;
  background-repeat: repeat;
  pointer-events: none;
}

.stars-1 {
  background-image:
    radial-gradient(1px 1px at 10% 10%, #fff, transparent),
    radial-gradient(1px 1px at 30% 20%, #fff, transparent),
    radial-gradient(1px 1px at 50% 50%, #fff, transparent),
    radial-gradient(1px 1px at 70% 30%, #fff, transparent),
    radial-gradient(1px 1px at 90% 10%, #fff, transparent);
  background-size: 200px 200px;
  animation: twinkle 3s ease-in-out infinite;
}

.stars-2 {
  background-image:
    radial-gradient(1.5px 1.5px at 20% 40%, #fff, transparent),
    radial-gradient(1.5px 1.5px at 60% 85%, #fff, transparent),
    radial-gradient(1.5px 1.5px at 85% 65%, #fff, transparent);
  background-size: 300px 300px;
  animation: twinkle 5s ease-in-out infinite 1s;
}

.stars-3 {
  background-image:
    radial-gradient(2px 2px at 40% 70%, #fff, transparent),
    radial-gradient(2px 2px at 10% 80%, #fff, transparent),
    radial-gradient(2px 2px at 80% 40%, #fff, transparent);
  background-size: 400px 400px;
  animation: twinkle 7s ease-in-out infinite 2s;
}

.meteor {
  position: absolute;
  width: 2px;
  height: 2px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 0 10px 2px rgba(255, 255, 255, 0.5);
  opacity: 0;
  pointer-events: none;
}

.meteor::after {
  content: "";
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 80px;
  height: 1px;
  background: linear-gradient(90deg, #fff, transparent);
}

.m1 { top: 10%; left: 110%; animation: shoot 8s linear infinite; }
.m2 { top: 30%; left: 110%; animation: shoot 12s linear infinite 4s; }
.m3 { top: 50%; left: 110%; animation: shoot 10s linear infinite 2s; }

.moon {
  position: absolute;
  top: 15%;
  right: 15%;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: transparent;
  box-shadow: 15px 15px 0 0 #fdfbd3;
  filter: drop-shadow(0 0 15px rgba(253, 251, 211, 0.4));
  z-index: 10;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes twinkle {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.2; }
}

@keyframes shoot {
  0% { transform: translateX(0) translateY(0) rotate(-35deg); opacity: 0; }
  5% { opacity: 1; }
  15% { transform: translateX(-1500px) translateY(1000px) rotate(-35deg); opacity: 0; }
  100% { transform: translateX(-1500px) translateY(1000px) rotate(-35deg); opacity: 0; }
}
</style>
