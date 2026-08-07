<template>
  <canvas ref="canvasRef" class="ripple-effect-canvas" aria-hidden="true"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const canvasRef = ref(null)

let ctx = null
let ripples = []
let animationId = null

class Ripple {
  constructor(x, y) {
    this.x = x
    this.y = y
    this.r = 0
    this.maxR = 60
    this.opacity = 0.6
    this.velocity = 2.5
  }

  update() {
    this.r += this.velocity
    this.velocity *= 0.96
    this.opacity -= 0.015
  }

  draw() {
    if (!ctx) return
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.r, 0, Math.PI * 2)
    ctx.strokeStyle = `rgba(129, 140, 248, ${this.opacity})`
    ctx.lineWidth = 2
    ctx.stroke()

    ctx.beginPath()
    ctx.arc(this.x, this.y, this.r * 0.5, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(129, 140, 248, ${this.opacity * 0.3})`
    ctx.fill()
  }
}

function resize() {
  const canvas = canvasRef.value
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
}

function handleClick(e) {
  ripples.push(new Ripple(e.clientX, e.clientY))
}

function animate() {
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height)

  ctx.shadowBlur = 15
  ctx.shadowColor = 'rgba(129, 140, 248, 0.5)'

  for (let i = 0; i < ripples.length; i++) {
    ripples[i].update()
    ripples[i].draw()
    if (ripples[i].opacity <= 0) {
      ripples.splice(i, 1)
      i--
    }
  }
  animationId = requestAnimationFrame(animate)
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  if (!ctx) return

  resize()
  window.addEventListener('resize', resize)
  window.addEventListener('click', handleClick)
  animate()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resize)
  window.removeEventListener('click', handleClick)
  ripples = []
})
</script>

<style scoped>
.ripple-effect-canvas {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 9998;
}
</style>
